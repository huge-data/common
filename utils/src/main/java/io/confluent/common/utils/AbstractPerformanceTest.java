package io.confluent.common.utils;

/**
 * 性能测试抽象类
 *
 * @author wanggang
 *
 */
public abstract class AbstractPerformanceTest {

	private static final long NS_PER_MS = 1000000L;
	private static final long NS_PER_SEC = 1000 * NS_PER_MS;
	private static final long MIN_SLEEP_NS = 2 * NS_PER_MS;
	private static final long TARGET_RATE_UPDATE_PERIOD_MS = 100;

	protected PerformanceStats stats;

	protected long targetIterationRate = 0;

	public AbstractPerformanceTest(long numEvents, int reportingInterval) {
		stats = new PerformanceStats(numEvents, reportingInterval);
	}

	/**
	 * 默认报告间隔时间为5秒
	 *
	 * @param numEvents
	 */
	public AbstractPerformanceTest(long numEvents) {
		this(numEvents, 5000);
	}

	/**
	 * 性能测试时，每次迭代执行一次
	 */
	protected abstract void doIteration(PerformanceStats.Callback cb);

	/**
	 * 如果测试达到完成的标准，返回true
	 */
	protected abstract boolean finished(int iteration);

	/**
	 * 如果测试运行速度比目标速度块，则返回true
	 */
	protected abstract boolean runningFast(int iteration, float elapsed);

	/**
	 * 返回每秒的目标迭代数
	 *
	 * 0表示没有速率限制条件，当测试的想得到一个值的时候（除了迭代速率是一个常量），
	 * 该方法将会周期性地执行，例如：吞吐量
	 *
	 * @return 目标迭代数
	 */
	protected float getTargetIterationRate(int iteration, float elapsed) {
		return targetIterationRate;
	}

	/**
	 * 运行性能测试，逼近 {@link #getTargetIterationRate(int, float)} 返回的目标比率
	 *
	 * @throws InterruptedException 中断异常
	 */
	protected void run() throws InterruptedException {
		long sleepDeficitNs = 0;
		long start = System.currentTimeMillis();
		long targetRateUpdated = start;
		float currentTargetRate = getTargetIterationRate(0, 0);
		long sleepTime = (long) (NS_PER_SEC / currentTargetRate);
		for (int i = 0; !finished(i); i++) {
			long sendStart = System.currentTimeMillis();

			/*
			 * Maybe sleep a little to control throughput. Sleep time can be a bit inaccurate for
			 * times < 1 ms so instead of sleeping each time instead wait until a minimum sleep time
			 * accumulates (the "sleep deficit") and then make up the whole deficit in one longer sleep.
			 *
			 * The ordering of this code and the call to doIteration is the opposite of what you
			 * might expect because we need to be able to respond immediately to quick changes in
			 * target rate at the very start of the test when the user overrides #targetRate(), and
			 * be careful about the iteration # and elapsed time parameters passed to #targetRate()
			 * and #runningFast(). The code is simpler/requires fewer calls to System#currentTimeMillis
			 * in this order.
			 */
			if (currentTargetRate > 0) {
				float elapsed = (sendStart - start) / 1000.f;
				if (i == 1
						|| (i > 1 && sendStart - targetRateUpdated > TARGET_RATE_UPDATE_PERIOD_MS)) {
					currentTargetRate = getTargetIterationRate(i, elapsed);
					sleepTime = (long) (NS_PER_SEC / currentTargetRate);
					targetRateUpdated = sendStart;
				}
				if (i > 0 && elapsed > 0 && runningFast(i, elapsed)) {
					sleepDeficitNs += sleepTime;
					if (sleepDeficitNs >= MIN_SLEEP_NS) {
						long sleepMs = sleepDeficitNs / 1000000;
						long sleepNs = sleepDeficitNs - sleepMs * 1000000;
						Thread.sleep(sleepMs, (int) sleepNs);
						sleepDeficitNs = 0;
					}
				}
			}

			PerformanceStats.Callback cb = stats.nextCompletion(sendStart);
			doIteration(cb);
		}

		// 打印最终结果
		stats.printTotal();
	}

	/**
	 * 运行性能测试，逼近固定目标比率
	 *
	 * 如果使用该方法，不能重写 {@link #getTargetIterationRate(int, float)} 方法。
	 *
	 * @param iterationsPerSec        每秒迭代次数
	 * @throws InterruptedException   中断异常
	 */
	protected void run(long iterationsPerSec) throws InterruptedException {
		targetIterationRate = iterationsPerSec;
		run();
	}

}
