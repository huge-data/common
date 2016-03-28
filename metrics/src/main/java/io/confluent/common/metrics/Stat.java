package io.confluent.common.metrics;

/**
 * Stat表示一种量化指标信号，如：平均值、最大值等
 *
 * @author wanggang
 *
 */
public interface Stat {

	/**
	 * 记录数据
	 *
	 * @param config 指标使用的配置
	 * @param value  需要记录的值
	 * @param timeMs 记录产生的时间，POSIX时间格式，毫秒单位
	 */
	public void record(MetricConfig config, double value, long timeMs);

}
