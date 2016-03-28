package io.confluent.common.metrics.stats;

import static org.junit.Assert.assertEquals;
import io.confluent.common.metrics.stats.Histogram.BinScheme;
import io.confluent.common.metrics.stats.Histogram.ConstantBinScheme;
import io.confluent.common.metrics.stats.Histogram.LinearBinScheme;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public class HistogramTest {

	private static final double EPS = 0.0000001d;

	public static void main(String[] args) {
		Random random = new Random();
		System.out.println("[-100, 100]:");
		for (Histogram.BinScheme scheme : Arrays.asList(new Histogram.ConstantBinScheme(1000, -100,
				100), new Histogram.ConstantBinScheme(100, -100, 100), new ConstantBinScheme(10,
				-100, 100))) {
			Histogram h = new Histogram(scheme);
			for (int i = 0; i < 10000; i++) {
				h.record(200.0 * random.nextDouble() - 100.0);
			}
			for (double quantile = 0.0; quantile < 1.0; quantile += 0.05) {
				System.out.printf("%5.2f: %.1f, ", quantile, h.value(quantile));
			}
			System.out.println();
		}

		System.out.println("[0, 1000]");
		for (BinScheme scheme : Arrays.asList(new LinearBinScheme(1000, 1000), new LinearBinScheme(
				100, 1000), new LinearBinScheme(10, 1000))) {
			Histogram h = new Histogram(scheme);
			for (int i = 0; i < 10000; i++) {
				h.record(1000.0 * random.nextDouble());
			}
			for (double quantile = 0.0; quantile < 1.0; quantile += 0.05) {
				System.out.printf("%5.2f: %.1f, ", quantile, h.value(quantile));
			}
			System.out.println();
		}
	}

	@Test
	public void testHistogram_基于常量容器模型测试直方图() {
		Histogram.BinScheme scheme = new Histogram.ConstantBinScheme(12, -5, 5);
		Histogram hist = new Histogram(scheme);
		for (int i = -5; i < 5; i++) {
			hist.record(i);
		}
		for (int i = 0; i < 10; i++) {
			assertEquals(scheme.fromBin(i + 1), hist.value(i / 10.0 + EPS), EPS);
		}
	}

	@Test
	public void testConstantBinScheme_测试常量容器模型() {
		// 定义容器中小容器个数为5,数值范围[-5,5]
		Histogram.ConstantBinScheme scheme = new Histogram.ConstantBinScheme(5, -5, 5);
		assertEquals("值小于下限，则放入第一个小容器中，下标为0", 0, scheme.toBin(-5.01));
		assertEquals("值大于上限，则放入最后一个小容器中，下标为4", 4, scheme.toBin(5.01));
		assertEquals("Check boundary of bucket 1", 1, scheme.toBin(-5));
		assertEquals("Check boundary of bucket 4", 4, scheme.toBin(5));
		assertEquals("Check boundary of bucket 3", 3, scheme.toBin(4.9999));
		checkBinningConsistency(new Histogram.ConstantBinScheme(4, 0, 5));
		checkBinningConsistency(scheme);
	}

	@Test
	public void testLinearBinScheme_测试线性容器模型() {
		Histogram.LinearBinScheme scheme = new Histogram.LinearBinScheme(10, 10);
		checkBinningConsistency(scheme);
	}

	/**
	 * 检查容器一致性
	 *
	 * @param scheme 容器模型
	 */
	private void checkBinningConsistency(Histogram.BinScheme scheme) {
		for (int bin = 0; bin < scheme.bins(); bin++) {
			double fromBin = scheme.fromBin(bin);
			int binAgain = scheme.toBin(fromBin + EPS);
			assertEquals("unbinning and rebinning the bin " + bin + " gave a different result ("
					+ fromBin + " was placed in bin " + binAgain + " )", bin, binAgain);
		}
	}

}