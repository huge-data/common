package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MetricConfig;

import java.util.List;

/**
 * 采样信号 {@link SampledStat} 的最大值
 *
 * @author wanggang
 *
 */
public final class Max extends SampledStat {

	public Max() {
		super(Double.NEGATIVE_INFINITY);
	}

	@Override
	protected void update(Sample sample, MetricConfig config, double value, long now) {
		sample.value = Math.max(sample.value, value);
	}

	@Override
	public double combine(List<Sample> samples, MetricConfig config, long now) {
		double max = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < samples.size(); i++) {
			max = Math.max(max, samples.get(i).value);
		}
		return max;
	}

}
