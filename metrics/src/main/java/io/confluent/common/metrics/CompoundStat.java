package io.confluent.common.metrics;

import java.util.List;

/**
 * 组合信号接口，用于表示一组基于同一个计算方法得到的Metric
 *
 * 例如：可以用于存储多个百分数值来展示统计直方图
 *
 * @author wanggang
 *
 */
public interface CompoundStat extends Stat {

	// 命名计算器列表
	public List<NamedMeasurable> stats();

	/**
	 * 命名计算器
	 *
	 * @author wanggang
	 *
	 */
	public static class NamedMeasurable {

		// 指标名称信息
		private final MetricName name;
		// 计算器
		private final Measurable stat;

		public NamedMeasurable(MetricName name, Measurable stat) {
			super();
			this.name = name;
			this.stat = stat;
		}

		public MetricName name() {
			return name;
		}

		public Measurable stat() {
			return stat;
		}

	}

}
