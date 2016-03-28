package io.confluent.common.metrics;

import io.confluent.common.utils.Time;
import io.confluent.common.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 传感器类，将连续的数值序列应用到关联的Metric集合中
 *
 * 例如：一个用于信息大小的传感器，使用 {@link #record(double)} 接口记录一个信息大小序列，
 * 并且维护一个关于请求大小（例如：平均值、最大值）的Metric集合。
 *
 * @author wanggang
 *
 */
public final class Sensor {

	// 批量Sensor和Metric的注册表
	private final Metrics registry;
	// 传感器名称
	private final String name;
	// 父传感器数组
	private final Sensor[] parents;
	// 指标信号列表
	private final List<Stat> stats;
	// Kafka指标列表
	private final List<KafkaMetric> metrics;
	// 指标计算配置
	private final MetricConfig config;
	// 时钟
	private final Time time;

	Sensor(Metrics registry, String name, Sensor[] parents, MetricConfig config, Time time) {
		super();
		this.registry = registry;
		this.name = Utils.notNull(name);
		this.parents = parents == null ? new Sensor[0] : parents;
		this.metrics = new ArrayList<>();
		this.stats = new ArrayList<>();
		this.config = config;
		this.time = time;
		checkForest(new HashSet<Sensor>());
	}

	/**
	 * 检查事故否循环依赖，递归调用
	 *
	 * @param sensors 传感器集合
	 */
	private void checkForest(Set<Sensor> sensors) {
		if (!sensors.add(this)) {
			throw new IllegalArgumentException("Circular dependency in sensors: " + name()
					+ " is its own parent.");
		}
		for (int i = 0; i < parents.length; i++) {
			parents[i].checkForest(sensors);
		}
	}

	/**
	 * 传感器注册名，惟一的
	 */
	public String name() {
		return this.name;
	}

	/**
	 * 记录产生的事件，{@link #record(double) record(1.0)} 的简写
	 */
	public void record() {
		record(1.0);
	}

	/**
	 * 使用该传感器记录事件数据
	 *
	 * @param value  需要记录的值
	 * @throws QuotaViolationException 越界异常
	 */
	public void record(double value) {
		record(value, time.milliseconds());
	}

	/**
	 * 按照时间记录事件数据
	 *
	 * 该方法因为重用时间戳比 {@link #record(double)} 稍微快些
	 *
	 * @param value   需要记录的值
	 * @param timeMs  当前时间，POSIX格式，毫秒单位
	 * @throws QuotaViolationException  越界异常
	 */
	public void record(double value, long timeMs) {
		synchronized (this) {
			// 增加所有指标信号的记录数据
			for (int i = 0; i < this.stats.size(); i++) {
				this.stats.get(i).record(config, value, timeMs);
			}
			checkQuotas(timeMs);
		}
		// 在所有父传感器中增加记录数据
		for (int i = 0; i < parents.length; i++) {
			parents[i].record(value, timeMs);
		}
	}

	/**
	 * 检查某个时间所有metric是否违反了越界条件
	 *
	 * @param timeMs  时间
	 */
	private void checkQuotas(long timeMs) {
		for (int i = 0; i < this.metrics.size(); i++) {
			KafkaMetric metric = this.metrics.get(i);
			MetricConfig config = metric.config();
			if (config != null) {
				Quota quota = config.quota();
				if (quota != null) {
					if (!quota.acceptable(metric.value(timeMs))) {
						throw new QuotaViolationException(metric.metricName()
								+ " is in violation of its quota of " + quota.bound());
					}
				}
			}
		}
	}

	/**
	 * 注册（添加）一个组合信号，基于该传感器来注册，并且不进行配置覆盖
	 *
	 * @param stat  组合信号
	 */
	public void add(CompoundStat stat) {
		add(stat, null);
	}

	/**
	 * 注册（添加）一个组合信号，基于该传感器来注册，得到多个计算数据（例如统计直方图数据）
	 *
	 * @param stat   组合信号
	 * @param config 信号配置，如果为null则使用该传感器默认配置
	 */
	public synchronized void add(CompoundStat stat, MetricConfig config) {
		// 添加该组合信号
		this.stats.add(Utils.notNull(stat));
		// 对该组合信号中的每个命名计算器进行指标注册和添加
		for (CompoundStat.NamedMeasurable m : stat.stats()) {
			KafkaMetric metric = new KafkaMetric(this, m.name(), m.stat(),
					config == null ? this.config : config, time);
			this.registry.registerMetric(metric);
			this.metrics.add(metric);
		}
	}

	/**
	 * 基于该传感器注册（添加）一个Metric
	 *
	 * @param metricName Metric名称信息
	 * @param stat 需要保留的统计信号
	 */
	public void add(MetricName metricName, MeasurableStat stat) {
		add(metricName, stat, null);
	}

	/**
	 * 基于该传感器注册一个Metric
	 *
	 * @param metricName  Metric名称信息
	 * @param stat        需要保留的统计信号
	 * @param config      该Metric的配置，如果为null则使用该传感器默认配置
	 */
	public synchronized void add(MetricName metricName, MeasurableStat stat, MetricConfig config) {
		KafkaMetric metric = new KafkaMetric(new Object(), Utils.notNull(metricName),
				Utils.notNull(stat), config == null ? this.config : config, time);
		this.registry.registerMetric(metric);
		this.metrics.add(metric);
		this.stats.add(stat);
	}

	/**
	 * 返回不可改变的Metric集合，同步操作
	 *
	 * @return
	 */
	synchronized List<KafkaMetric> metrics() {
		return Collections.unmodifiableList(this.metrics);
	}

}
