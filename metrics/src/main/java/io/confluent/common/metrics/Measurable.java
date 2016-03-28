package io.confluent.common.metrics;

/**
 * 计算Metric的值
 *
 * @author wanggang
 *
 */
public interface Measurable {

	/**
	 * 计算指标值
	 *
	 * @param config  指标对应的配置
	 * @param now     指标产生的时间，POSIX格式，毫秒单位
	 * @return 计算值
	 */
	public double measure(MetricConfig config, long now);

}
