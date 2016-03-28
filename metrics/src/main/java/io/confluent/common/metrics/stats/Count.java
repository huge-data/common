package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MetricConfig;

import java.util.List;

/**
 * 采样信号 {@link SampledStat} 计数器
 *
 * @author wanggang
 *
 */
public class Count extends SampledStat {

	public Count() {
		super(0);
	}

	@Override
	protected void update(Sample sample, MetricConfig config, double value, long now) {
		sample.value += 1.0;
	}

	@Override
	public double combine(List<Sample> samples, MetricConfig config, long now) {
		double total = 0.0;
		for (int i = 0; i < samples.size(); i++) {
			total += samples.get(i).value;
		}
		return total;
	}

}
