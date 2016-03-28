package io.confluent.common;

import java.util.Map;

/**
 * 混合风格的接口，通过反射机制实例化，需要传入配置参数
 *
 * @author wanggang
 *
 */
public interface Configurable {

	/**
	 * 使用键值对配置
	 */
	public void configure(Map<String, ?> configs);

}