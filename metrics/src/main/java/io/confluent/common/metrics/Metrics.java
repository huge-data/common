package io.confluent.common.metrics;

import io.confluent.common.utils.CopyOnWriteMap;
import io.confluent.common.utils.SystemTime;
import io.confluent.common.utils.Time;
import io.confluent.common.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * 批量Sensor和Metric的注册表，即仓库
 *
 * <p> Metric是一种有名称的数值计算指标，Sensor是用于实时记录数值计算指标的。
 *  每个Sensor有0个以上相关的Metric。例如，一个Sensor如果代表信息大小的话，那么Sensor里面的
 *  Metric就可能表示该Sensor所记录一连串信息大小的统计计算值，像平均值、最大值等。
 * <p>
 * 使用示例:
 * <pre>
 * // 构建metrics:
 * // Metric和Sensor的全局仓库
 * Metrics metrics = new Metrics();
 *
 * Sensor sensor = metrics.sensor("message-sizes");
 *
 * MetricName metricName = new MetricName("message-size-avg", "producer-metrics");
 * sensor.add(metricName, new Avg());
 * metricName = new MetricName("message-size-max", "producer-metrics");
 * sensor.add(metricName, new Max());
 *
 * // 发送信息的时候记录大小
 * sensor.record(messageSize);
 * </pre>
 */
public class Metrics {

	// 计算Metric配置信息
	private final MetricConfig config;
	// Metric并发列表
	private final ConcurrentMap<MetricName, KafkaMetric> metrics;
	// Snesor并发列表
	private final ConcurrentMap<String, Sensor> sensors;
	// Metric的Reporter列表
	private final List<MetricsReporter> reporters;
	// 时钟，用于Metric中
	private final Time time;

	/**
	 * 构造函数，使用默认配置
	 */
	public Metrics() {
		this(new MetricConfig());
	}

	/**
	 * 构造函数，使用时钟参数
	 *
	 * @param time  时钟对象
	 */
	public Metrics(Time time) {
		this(new MetricConfig(), new ArrayList<MetricsReporter>(0), time);
	}

	/**
	 * 构造函数，使用给定配置
	 *
	 * @param defaultConfig 用于所有Metric的默认配置，但是不覆盖原有的配置
	 */
	public Metrics(MetricConfig defaultConfig) {
		this(defaultConfig, new ArrayList<MetricsReporter>(0), new SystemTime());
	}

	/**
	 * 构造函数，使用给定配置和Reporter列表
	 *
	 * @param defaultConfig Metric配置
	 * @param reporters     Reporter列表
	 * @param time          时钟对象
	 */
	public Metrics(MetricConfig defaultConfig, List<MetricsReporter> reporters, Time time) {
		this.config = defaultConfig;
		this.sensors = new CopyOnWriteMap<String, Sensor>();
		this.metrics = new CopyOnWriteMap<MetricName, KafkaMetric>();
		this.reporters = Utils.notNull(reporters);
		this.time = time;
		for (MetricsReporter reporter : reporters) {
			reporter.init(new ArrayList<KafkaMetric>());
		}
	}

	/**
	 * 获取一个Sensor
	 *
	 * @param name   Sensor名称
	 * @return Sensor或者null
	 */
	public Sensor getSensor(String name) {
		return this.sensors.get(Utils.notNull(name));
	}

	/**
	 * 获取或者创建一个Sensor，根据唯一名称，并且没有父Sensor
	 *
	 * @param name   名称
	 * @return Sensor
	 */
	public Sensor sensor(String name) {
		return sensor(name, null, (Sensor[]) null);
	}

	/**
	 * 获取或者创建一个Sensor，根据唯一名称以及0个以上的父Sensor数组，
	 * 所有的父Sensor将会收到这个Sensor的记录信息。
	 *
	 * @param name    名称
	 * @param parents 父Sensor数组
	 * @return Sensor
	 */
	public Sensor sensor(String name, Sensor... parents) {
		return sensor(name, null, parents);
	}

	/**
	 * 获取或者创建一个Sensor，根据唯一名称以及0个以上的父Sensor数组，
	 * 所有的父Sensor将会收到这个Sensor的记录信息。
	 *
	 * @param name    名称
	 * @param config  配置
	 * @param parents 父Sensor数组
	 * @return Sensor
	 */
	public synchronized Sensor sensor(String name, MetricConfig config, Sensor... parents) {
		Sensor s = getSensor(name);
		if (s == null) {
			s = new Sensor(this, name, parents, config == null ? this.config : config, time);
			this.sensors.put(name, s);
		}

		return s;
	}

	/**
	 * 添加一个Metric，该Metric不会和其他Sensor相关
	 *
	 * @param metricName  Metric名称对象
	 * @param measurable  Metric的计算器
	 */
	public void addMetric(MetricName metricName, Measurable measurable) {
		addMetric(metricName, null, measurable);
	}

	/**
	 * 添加一个Metric，该Metric不会和其他Sensor相关
	 *
	 * @param metricName  Metric名称对象
	 * @param config      计算Metric的配置
	 * @param measurable  Metric的计算器
	 */
	public synchronized void addMetric(MetricName metricName, MetricConfig config,
			Measurable measurable) {
		KafkaMetric m = new KafkaMetric(new Object(), Utils.notNull(metricName),
				Utils.notNull(measurable), config == null ? this.config : config, time);
		registerMetric(m);
	}

	/**
	 * 添加一个MetricReporter
	 *
	 * @param reporter 报告器
	 */
	public synchronized void addReporter(MetricsReporter reporter) {
		Utils.notNull(reporter).init(new ArrayList<KafkaMetric>(metrics.values()));
		this.reporters.add(reporter);
	}

	/**
	 * 注册Metric，即将Metric添加到Metric中
	 *
	 * @param metric KafkaMetric对象
	 */
	synchronized void registerMetric(KafkaMetric metric) {
		MetricName metricName = metric.metricName();
		if (this.metrics.containsKey(metricName)) {
			throw new IllegalArgumentException("A metric named '" + metricName
					+ "' already exists, " + "can't register another one.");
		}
		this.metrics.put(metricName, metric);
		for (MetricsReporter reporter : reporters) {
			reporter.metricChange(metric);
		}
	}

	/**
	 * 获取当前所有metricName索引维护的Metric
	 */
	public Map<MetricName, KafkaMetric> metrics() {
		return this.metrics;
	}

	/**
	 * 关闭仓库，不进行Report了
	 */
	public void close() {
		for (MetricsReporter reporter : this.reporters) {
			reporter.close();
		}
	}

}
