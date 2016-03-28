package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MeasurableStat;
import io.confluent.common.metrics.MetricConfig;

/**
 * 最新（当前）记录的值
 *
 * @author wanggang
 *
 */
public class Gauge implements MeasurableStat {

	private double currentValue;

	public Gauge() {
		this.currentValue = 0.0;
	}

	public Gauge(double value) {
		this.currentValue = value;
	}

	@Override
	public void record(MetricConfig config, double value, long now) {
		this.currentValue = value;
	}

	@Override
	public double measure(MetricConfig config, long now) {
		return this.currentValue;
	}

}
