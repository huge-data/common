package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MetricConfig;

import java.util.List;

/**
 * 采样信号 {@link SampledStat} 平均值
 *
 * @author wanggang
 *
 */
public class Avg extends SampledStat {

	public Avg() {
		super(0.0);
	}

	@Override
	protected void update(Sample sample, MetricConfig config, double value, long now) {
		sample.value += value;
	}

	@Override
	public double combine(List<Sample> samples, MetricConfig config, long now) {
		double total = 0.0;
		long count = 0;
		for (int i = 0; i < samples.size(); i++) {
			Sample s = samples.get(i);
			total += s.value;
			count += s.eventCount;
		}
		return total / count;
	}

}
