package io.confluent.common.metrics;

/**
 * 可计算的指标，表示一个 {@link Stat} 可以进行计算 {@link Measurable} (例如：可以计算得到单个浮点值)
 * 该接口可以用于大多数简单的统计，例如：{@link zx.soft.common.metrics.stats.Avg},
 * {@link zx.soft.common.metrics.stats.Max}, {@link zx.soft.common.metrics.stats.Count} 等。
 *
 * @author wanggang
 *
 */
public interface MeasurableStat extends Stat, Measurable {

}
