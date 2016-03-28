package io.confluent.common.metrics;

import java.util.concurrent.TimeUnit;

/**
 * Metric指标计算的配置类
 *
 * @author wanggang
 *
 */
public class MetricConfig {

	// 数值限制
	private Quota quota;
	// 样本数
	private int samples;
	// 窗口大小，事件数，默认最大值
	private long eventWindow;
	// 窗口大小，耗时，默认30秒
	private long timeWindowMs;
	// 时间单位
	private TimeUnit unit;

	public MetricConfig() {
		super();
		this.quota = null;
		this.samples = 2;
		this.eventWindow = Long.MAX_VALUE;
		this.timeWindowMs = TimeUnit.MILLISECONDS.convert(30, TimeUnit.SECONDS);
		this.unit = TimeUnit.SECONDS;
	}

	public Quota quota() {
		return this.quota;
	}

	public MetricConfig quota(Quota quota) {
		this.quota = quota;
		return this;
	}

	public long eventWindow() {
		return eventWindow;
	}

	public MetricConfig eventWindow(long window) {
		this.eventWindow = window;
		return this;
	}

	public long timeWindowMs() {
		return timeWindowMs;
	}

	public MetricConfig timeWindow(long window, TimeUnit unit) {
		this.timeWindowMs = TimeUnit.MILLISECONDS.convert(window, unit);
		return this;
	}

	public int samples() {
		return this.samples;
	}

	public MetricConfig samples(int samples) {
		if (samples < 1) {
			throw new IllegalArgumentException("The number of samples must be at least 1.");
		}
		this.samples = samples;
		return this;
	}

	public TimeUnit timeUnit() {
		return unit;
	}

	public MetricConfig timeUnit(TimeUnit unit) {
		this.unit = unit;
		return this;
	}

}
