package io.confluent.common.utils.zookeeper;

import org.apache.zookeeper.data.Stat;

/**
 * Zookeeper数据模型
 *
 * @author wanggang
 *
 */
public class ZkData {

	// 数据
	private final String data;
	// 信息对象
	private final Stat stat;

	public ZkData(String data, Stat stat) {
		this.data = data;
		this.stat = stat;
	}

	public String getData() {
		return this.data;
	}

	public Stat getStat() {
		return this.stat;
	}

}
