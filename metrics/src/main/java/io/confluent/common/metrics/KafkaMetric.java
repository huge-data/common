package io.confluent.common.metrics;

import io.confluent.common.utils.Time;

/**
 * Kafka指标对象
 *
 * @author wanggang
 *
 */
public final class KafkaMetric implements Metric {

	// 指标名称信息
	private MetricName metricName;
	// 同步锁
	private final Object lock;
	// 时钟
	private final Time time;
	// 指标计算对象
	private final Measurable measurable;
	// 指标计算配置对象
	private MetricConfig config;

	KafkaMetric(Object lock, MetricName metricName, Measurable measurable, MetricConfig config,
			Time time) {
		super();
		this.metricName = metricName;
		this.lock = lock;
		this.measurable = measurable;
		this.config = config;
		this.time = time;
	}

	MetricConfig config() {
		return this.config;
	}

	@Override
	public MetricName metricName() {
		return this.metricName;
	}

	@Override
	public double value() {
		synchronized (this.lock) {
			return value(time.milliseconds());
		}
	}

	/**
	 * 计算指标值
	 *
	 * @param timeMs  时间
	 * @return
	 */
	double value(long timeMs) {
		return this.measurable.measure(config, timeMs);
	}

	/**
	 * 指标计算配置，同步操作
	 *
	 * @param config
	 */
	public void config(MetricConfig config) {
		synchronized (lock) {
			this.config = config;
		}
	}

}
