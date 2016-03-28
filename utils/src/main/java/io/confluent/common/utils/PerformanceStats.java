package io.confluent.common.utils;

import java.util.Arrays;

/**
 * 性能标量信号
 *
 * @author wanggang
 *
 */
public class PerformanceStats {

	// 起始时间
	private long start;
	// 窗口起始值
	private long windowStart;
	// 延迟数组
	private int[] latencies;
	// 采样数
	private int sampling;
	// 迭代数
	private int iteration;
	// 索引
	private int index;
	// 计数器
	private long count;
	// 字节数
	private long bytes;
	// 最大延迟
	private int maxLatency;
	// 总延迟
	private long totalLatency;
	// 窗口数
	private long windowCount;
	// 窗口最大延迟
	private int windowMaxLatency;
	// 窗口总延迟
	private long windowTotalLatency;
	// 窗口字节数
	private long windowBytes;
	// 报告间隔时间
	private long reportingInterval;

	public PerformanceStats(long numRecords, int reportingInterval) {
		this.start = System.currentTimeMillis();
		this.windowStart = System.currentTimeMillis();
		this.index = 0;
		this.iteration = 0;
		this.sampling = (int) (numRecords / Math.min(numRecords, 500000));
		this.latencies = new int[(int) (numRecords / this.sampling) + 1];
		this.index = 0;
		this.maxLatency = 0;
		this.totalLatency = 0;
		this.windowCount = 0;
		this.windowMaxLatency = 0;
		this.windowTotalLatency = 0;
		this.windowBytes = 0;
		this.totalLatency = 0;
		this.reportingInterval = reportingInterval;
	}

	public void record(int iter, int latency, int records, long bytes, long time) {
		this.count += records;
		this.bytes += bytes;
		this.totalLatency += latency;
		this.maxLatency = Math.max(this.maxLatency, latency);
		this.windowCount += records;
		this.windowBytes += bytes;
		this.windowTotalLatency += latency;
		this.windowMaxLatency = Math.max(windowMaxLatency, latency);
		if (iter % this.sampling == 0) {
			this.latencies[index] = latency;
			this.index++;
		}
		// 报告最近的性能
		if (time - windowStart >= reportingInterval) {
			printWindow();
			newWindow();
		}
	}

	/**
	 * 下一次完成
	 *
	 * @param start   起始时间
	 * @return    回调函数
	 */
	public Callback nextCompletion(long start) {
		Callback cb = new Callback(this.iteration, start);
		this.iteration++;
		return cb;
	}

	/**
	 * 打印窗口
	 */
	public void printWindow() {
		long elapsed = System.currentTimeMillis() - windowStart;
		double recsPerSec = 1000.0 * windowCount / elapsed;
		double mbPerSec = 1000.0 * this.windowBytes / elapsed / (1024.0 * 1024.0);
		System.out
				.printf("%d records processed, %.1f records/sec (%.2f MB/sec), %.1f ms avg latency, %.1f max latency.\n",
						windowCount, recsPerSec, mbPerSec, windowTotalLatency
								/ (double) windowCount, (double) windowMaxLatency);
	}

	/**
	 * 新窗口
	 */
	public void newWindow() {
		this.windowStart = System.currentTimeMillis();
		this.windowCount = 0;
		this.windowMaxLatency = 0;
		this.windowTotalLatency = 0;
		this.windowBytes = 0;
	}

	/**
	 * 打印所有
	 */
	public void printTotal() {
		long elapsed = System.currentTimeMillis() - start;
		double recsPerSec = 1000.0 * count / elapsed;
		double mbPerSec = 1000.0 * this.bytes / elapsed / (1024.0 * 1024.0);
		int[] percs = percentiles(this.latencies, index, 0.5, 0.95, 0.99, 0.999);
		System.out
				.printf("%d records processed, %f records/sec (%.2f MB/sec), %.2f ms avg latency, %.2f ms max latency, %d ms 50th, %d ms 95th, %d ms 99th, %d ms 99.9th.\n",
						count, recsPerSec, mbPerSec, totalLatency / (double) count,
						(double) maxLatency, percs[0], percs[1], percs[2], percs[3]);
	}

	/**
	 * 计算百分数
	 *
	 * @param latencies    延迟数组
	 * @param count        计数器
	 * @param percentiles  百分比数组
	 * @return
	 */
	private static int[] percentiles(int[] latencies, int count, double... percentiles) {
		int size = Math.min(count, latencies.length);
		Arrays.sort(latencies, 0, size);
		int[] values = new int[percentiles.length];
		for (int i = 0; i < percentiles.length; i++) {
			int index = (int) (percentiles[i] * size);
			values[i] = latencies[index];
		}
		return values;
	}

	/**
	 * 回调函数
	 *
	 * @author wanggang
	 *
	 */
	public final class Callback {

		// 起始时间
		private final long start;
		// 迭代次数
		private final int iteration;

		public Callback(int iter, long start) {
			this.start = start;
			this.iteration = iter;
		}

		/**
		 * 正在完成
		 *
		 * @param records  记录数
		 * @param bytes    字节数
		 */
		public void onCompletion(int records, long bytes) {
			long now = System.currentTimeMillis();
			int latency = (int) (now - start);
			PerformanceStats.this.record(iteration, latency, records, bytes, now);
		}

	}

}
