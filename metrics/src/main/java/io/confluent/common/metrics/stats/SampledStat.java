package io.confluent.common.metrics.stats;

import io.confluent.common.metrics.MeasurableStat;
import io.confluent.common.metrics.MetricConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * 采样信号
 *
 * 用于记录多个样本计算得到的单个标量值，每个样本是通过一个可配值的窗口记录得到的，
 * 窗口可以是事件数量或者耗时（或者两者，也就是其中任何一个条件满足就可以），计算是对所有样本进行的。
 * 当一个窗口完成后，旧的样本被清楚，然后重新采样。该抽象类的子类使用该类中的基本模式来定义不同的统计计算指标。
 *
 * @author wanggang
 *
 */
public abstract class SampledStat implements MeasurableStat {

	// 样本集合
	protected List<Sample> samples;
	// 初始值
	private double initialValue;
	// 当前采样游标
	private int current = 0;

	public SampledStat(double initialValue) {
		this.initialValue = initialValue;
		this.samples = new ArrayList<>(2);
	}

	/**
	 * 记录采样数据
	 *
	 * 	@param config 指标使用的配置
	 * @param value  需要记录的值
	 * @param timeMs 记录产生的时间，POSIX时间格式，毫秒单位
	 */
	@Override
	public void record(MetricConfig config, double value, long timeMs) {
		Sample sample = current(timeMs);
		if (sample.isComplete(timeMs, config)) {
			// 采样完成，需要进行下一步操作
			sample = advance(config, timeMs);
		}
		update(sample, config, value, timeMs);
		sample.eventCount += 1;
	}

	/**
	 * 向前操作
	 *
	 * @param config   配置
	 * @param timeMs   时间
	 * @return
	 */
	private Sample advance(MetricConfig config, long timeMs) {
		this.current = (this.current + 1) % config.samples();
		if (this.current >= samples.size()) {
			Sample sample = newSample(timeMs);
			this.samples.add(sample);
			return sample;
		} else {
			Sample sample = current(timeMs);
			sample.reset(timeMs);
			return sample;
		}
	}

	/**
	 * 获取新的样本
	 *
	 * @param timeMs 时间
	 * @return
	 */
	protected Sample newSample(long timeMs) {
		return new Sample(this.initialValue, timeMs);
	}

	/**
	 * 样本计算
	 *
	 * @param config   配置
	 * @param now      当前时间
	 */
	@Override
	public double measure(MetricConfig config, long now) {
		purgeObsoleteSamples(config, now);
		return combine(this.samples, config, now);
	}

	/**
	 * 获取当前样本
	 *
	 * @param timeMs 当前时间
	 * @return
	 */
	public Sample current(long timeMs) {
		if (samples.size() == 0) {
			this.samples.add(newSample(timeMs));
		}
		return this.samples.get(this.current);
	}

	/**
	 * 获取最老的样本
	 *
	 * @param now  当前时间
	 * @return
	 */
	public Sample oldest(long now) {
		if (samples.size() == 0) {
			this.samples.add(newSample(now));
		}
		Sample oldest = this.samples.get(0);
		for (int i = 1; i < this.samples.size(); i++) {
			Sample curr = this.samples.get(i);
			if (curr.lastWindowMs < oldest.lastWindowMs) {
				oldest = curr;
			}
		}

		return oldest;
	}

	/**
	 * 更新样本
	 *
	 * @param sample   样本
	 * @param config   配置
	 * @param value    值
	 * @param timeMs   时间爱呢
	 */
	protected abstract void update(Sample sample, MetricConfig config, double value, long timeMs);

	/**
	 * 根据样本集合计算Metric指标值
	 *
	 * @param samples   样本集合
	 * @param config    计算指标的配置
	 * @param now       当前时间
	 * @return   指标计算值
	 */
	public abstract double combine(List<Sample> samples, MetricConfig config, long now);

	/**
	 *  清理过时的样本
	 *
	 * @param config
	 * @param now
	 */
	protected void purgeObsoleteSamples(MetricConfig config, long now) {
		long expireAge = config.samples() * config.timeWindowMs();
		for (int i = 0; i < samples.size(); i++) {
			Sample sample = this.samples.get(i);
			if (now - sample.lastWindowMs >= expireAge) {
				sample.reset(now);
			}
		}
	}

	/**
	 * 样本数据模型
	 *
	 * @author wanggang
	 *
	 */
	protected static class Sample {

		// 初始值，重置的时候使用
		public double initialValue;
		// 事件数
		public long eventCount;
		// 上一次窗口时间
		public long lastWindowMs;
		// 样本值
		public double value;

		public Sample(double initialValue, long now) {
			this.initialValue = initialValue;
			this.eventCount = 0;
			this.lastWindowMs = now;
			this.value = initialValue;
		}

		/**
		 * 重置
		 *
		 * @param now 当前时间
		 */
		public void reset(long now) {
			this.eventCount = 0;
			this.lastWindowMs = now;
			this.value = initialValue;
		}

		/**
		 * 判断采样是否完成
		 *
		 * @param timeMs  当前时间爱呢
		 * @param config  Metric计算配置信息
		 * @return
		 */
		public boolean isComplete(long timeMs, MetricConfig config) {
			return timeMs - lastWindowMs >= config.timeWindowMs()
					|| eventCount >= config.eventWindow();
		}

	}

}
