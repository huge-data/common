package io.confluent.common.metrics;

/**
 * 用于监控的数值Metric接口
 *
 * @author wanggang
 *
 */
public interface Metric {

	/**
	 * Metric的名称
	 */
	public MetricName metricName();

	/**
	 * Metric的值
	 */
	public double value();

}
