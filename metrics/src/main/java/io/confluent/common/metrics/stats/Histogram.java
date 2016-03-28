package io.confluent.common.metrics.stats;

/**
 * 直方图模型
 *
 * @author wanggang
 *
 */
public class Histogram {

	private final BinScheme binScheme;
	private final float[] hist;
	private double count;

	public Histogram(BinScheme binScheme) {
		this.hist = new float[binScheme.bins()];
		this.count = 0.0f;
		this.binScheme = binScheme;
	}

	public void record(double value) {
		this.hist[binScheme.toBin(value)] += 1.0f;
		this.count += 1.0f;
	}

	public double value(double quantile) {
		if (count == 0.0d) {
			return Double.NaN;
		}
		float sum = 0.0f;
		float quant = (float) quantile;
		for (int i = 0; i < this.hist.length - 1; i++) {
			sum += this.hist[i];
			if (sum / count > quant) {
				return binScheme.fromBin(i);
			}
		}
		return Float.POSITIVE_INFINITY;
	}

	public float[] counts() {
		return this.hist;
	}

	public void clear() {
		for (int i = 0; i < this.hist.length; i++) {
			this.hist[i] = 0.0f;
		}
		this.count = 0;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder('{');
		for (int i = 0; i < this.hist.length - 1; i++) {
			b.append(String.format("%.10f", binScheme.fromBin(i)));
			b.append(':');
			b.append(String.format("%.0f", this.hist[i]));
			b.append(',');
		}
		b.append(Float.POSITIVE_INFINITY);
		b.append(':');
		b.append(this.hist[this.hist.length - 1]);
		b.append('}');
		return b.toString();
	}

	/**
	 * 容器模型
	 *
	 * @author wanggang
	 *
	 */
	public interface BinScheme {

		/**
		 * 返回容器数
		 */
		public int bins();

		/**
		 * 加入值到容器中
		 *
		 * @param 小容器下标
		 */
		public int toBin(double value);

		/**
		 * 从容器中取值
		 */
		public double fromBin(int bin);

	}

	/**
	 * 常量容器模型
	 *
	 * @author wanggang
	 *
	 */
	public static class ConstantBinScheme implements BinScheme {

		// 最小值
		private final double min;
		// 最大值
		private final double max;
		// 桶数，至少两个桶
		// 下标为0的桶：         存放<min的值
		// 下标为bins-1的桶：   存放>max的值
		private final int bins;
		// 桶宽，除了边界的两个桶
		private final double bucketWidth;

		public ConstantBinScheme(int bins, double min, double max) {
			if (bins < 2) {
				throw new IllegalArgumentException("Must have at least 2 bins.");
			}
			this.min = min;
			this.max = max;
			this.bins = bins;
			// bins=2时，bucketWidth=Infinity
			this.bucketWidth = (max - min) / (bins - 2);
		}

		@Override
		public int bins() {
			return this.bins;
		}

		@Override
		public double fromBin(int b) {
			if (b == 0) {
				return Double.NEGATIVE_INFINITY;
			} else if (b == bins - 1) {
				return Double.POSITIVE_INFINITY;
			} else {
				return min + (b - 1) * bucketWidth;
			}
		}

		@Override
		public int toBin(double x) {
			if (x < min) {
				return 0;
			} else if (x > max) {
				return bins - 1;
			} else {
				return (int) ((x - min) / bucketWidth) + 1;
			}
		}

	}

	/**
	 * 线性容器模型
	 *
	 * @author wanggang
	 *
	 */
	public static class LinearBinScheme implements BinScheme {

		private final int bins;
		private final double max;
		private final double scale;

		public LinearBinScheme(int numBins, double max) {
			this.bins = numBins;
			this.max = max;
			this.scale = max / (numBins * (numBins - 1) / 2);
		}

		@Override
		public int bins() {
			return this.bins;
		}

		@Override
		public double fromBin(int b) {
			if (b == this.bins - 1) {
				return Float.POSITIVE_INFINITY;
			} else {
				double unscaled = (b * (b + 1.0)) / 2.0;
				return unscaled * this.scale;
			}
		}

		@Override
		public int toBin(double x) {
			if (x < 0.0d) {
				throw new IllegalArgumentException("Values less than 0.0 not accepted.");
			} else if (x > this.max) {
				return this.bins - 1;
			} else {
				double scaled = x / this.scale;
				return (int) (-0.5 + Math.sqrt(2.0 * scaled + 0.25));
			}
		}

	}

}
