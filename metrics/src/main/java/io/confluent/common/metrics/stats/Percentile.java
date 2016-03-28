package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MetricName;

/**
 * 百分数模型
 *
 * @author wanggang
 *
 */
public class Percentile {

	// 指标名称信息
	private final MetricName name;
	// 百分数值
	private final double percentile;

	public Percentile(MetricName name, double percentile) {
		super();
		this.name = name;
		this.percentile = percentile;
	}

	public MetricName name() {
		return this.name;
	}

	public double percentile() {
		return this.percentile;
	}

}
