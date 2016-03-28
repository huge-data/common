package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MetricConfig;

import java.util.List;

/**
 * 采样信号 {@link SampledStat} 的最小值
 *
 * @author wanggang
 *
 */
public class Min extends SampledStat {

	public Min() {
		super(Double.MIN_VALUE);
	}

	@Override
	protected void update(Sample sample, MetricConfig config, double value, long now) {
		sample.value = Math.min(sample.value, value);
	}

	@Override
	public double combine(List<Sample> samples, MetricConfig config, long now) {
		double min = Double.MAX_VALUE;
		for (int i = 0; i < samples.size(); i++) {
			min = Math.min(min, samples.get(i).value);
		}
		return min;
	}

}
