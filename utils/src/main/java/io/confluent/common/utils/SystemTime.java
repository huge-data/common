package io.confluent.common.utils;

/**
 * 系统时钟类
 *
 * @author wanggang
 *
 */
public class SystemTime implements Time {

	@Override
	public long milliseconds() {
		return System.currentTimeMillis();
	}

	@Override
	public long nanoseconds() {
		return System.nanoTime();
	}

	@Override
	public void sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException e) {
			// TODO
		}
	}

}
