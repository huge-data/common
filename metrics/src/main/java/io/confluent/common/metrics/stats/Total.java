package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MeasurableStat;
import io.confluent.common.metrics.MetricConfig;

/**
 * 全量统计信号
 *
 * @author wanggang
 *
 */
public class Total implements MeasurableStat {

	private double total;

	public Total() {
		this.total = 0.0;
	}

	public Total(double value) {
		this.total = value;
	}

	@Override
	public void record(MetricConfig config, double value, long now) {
		this.total += value;
	}

	@Override
	public double measure(MetricConfig config, long now) {
		return this.total;
	}

}
