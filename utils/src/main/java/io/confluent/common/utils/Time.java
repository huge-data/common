package io.confluent.common.utils;

/**
 * 时钟接口，基于clock时间，用于单元测试类中
 *
 * @author wanggang
 *
 */
public interface Time {

	/**
	 * 当前时间，毫秒
	 */
	public long milliseconds();

	/**
	 * 当前时间，纳秒
	 */
	public long nanoseconds();

	/**
	 * 休眠ms秒
	 */
	public void sleep(long ms);

}
