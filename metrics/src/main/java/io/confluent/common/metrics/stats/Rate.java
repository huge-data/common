package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MeasurableStat;
import io.confluent.common.metrics.MetricConfig;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 指定数量的比率
 *
 * 默认是在采样窗口上通过耗时分割出的采样统计得到的观察样本集合
 *
 * 然而，通过采样信号 {@link SampledStat} 的实现类来记录实时数据，
 * 例如间隔时间的值的数量，或者其他这类值。
 *
 * @author wanggang
 *
 */
public class Rate implements MeasurableStat {

	private final TimeUnit unit;
	private final SampledStat stat;

	public Rate() {
		this(TimeUnit.SECONDS);
	}

	public Rate(TimeUnit unit) {
		this(unit, new SampledTotal());
	}

	public Rate(SampledStat stat) {
		this(TimeUnit.SECONDS, stat);
	}

	public Rate(TimeUnit unit, SampledStat stat) {
		this.stat = stat;
		this.unit = unit;
	}

	public String unitName() {
		return unit.name().substring(0, unit.name().length() - 2).toLowerCase();
	}

	@Override
	public void record(MetricConfig config, double value, long timeMs) {
		this.stat.record(config, value, timeMs);
	}

	@Override
	public double measure(MetricConfig config, long now) {
		double value = stat.measure(config, now);
		double elapsed = convert(now - stat.oldest(now).lastWindowMs);
		return value / elapsed;
	}

	private double convert(long time) {
		switch (unit) {
		case NANOSECONDS:
			return time * 1000.0 * 1000.0;
		case MICROSECONDS:
			return time * 1000.0;
		case MILLISECONDS:
			return time;
		case SECONDS:
			return time / (1000.0);
		case MINUTES:
			return time / (60.0 * 1000.0);
		case HOURS:
			return time / (60.0 * 60.0 * 1000.0);
		case DAYS:
			return time / (24.0 * 60.0 * 60.0 * 1000.0);
		default:
			throw new IllegalStateException("Unknown unit: " + unit);
		}
	}

	/**
	 * 采样值总数类
	 *
	 * @author wanggang
	 *
	 */
	public static class SampledTotal extends SampledStat {

		public SampledTotal() {
			super(0.0d);
		}

		@Override
		protected void update(Sample sample, MetricConfig config, double value, long timeMs) {
			sample.value += value;
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

}
