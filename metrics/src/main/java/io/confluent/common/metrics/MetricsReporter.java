package io.confluent.common.metrics;

import io.confluent.common.Configurable;

import java.util.List;

/**
 * 指标报告器插件接口，监听监测指标计算数据并报告
 *
 * @author wanggang
 *
 */
public interface MetricsReporter extends Configurable {

	/**
	 * 当Reporter注册时，用来注册所有存在的Metric
	 *
	 * @param metrics 但其所有存在的Metric
	 */
	public void init(List<KafkaMetric> metrics);

	/**
	 * 指标更改，当一个Metric更新或者添加时调用
	 *
	 * @param metric Kafka指标
	 */
	public void metricChange(KafkaMetric metric);

	/**
	 * 指标仓库关闭
	 */
	public void close();

}
