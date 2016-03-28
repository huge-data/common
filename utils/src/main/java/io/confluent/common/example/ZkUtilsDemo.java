package io.confluent.common.example;

import io.confluent.common.utils.zookeeper.ZkData;
import io.confluent.common.utils.zookeeper.ZkUtils;

import org.I0Itec.zkclient.ZkClient;

/**
 * Zookeeper客户端使用示例
 *
 * @author wanggang
 *
 */
public class ZkUtilsDemo {

	public static void main(String[] args) {
		// Zookeeper客户端传入serverstring参数说明：
		// 1、ip:port逗号分割： kafka1:2181,kafka2:2181,kafka3:2181
		// 2、增加路径，主目录为path： kafka1:2181,kafka2:2181,kafka3:2181/path
		String servers = "kafka01:2181,kafka02:2181,kafka03:2181";
		ZkClient zkClient = new ZkClient(servers);
		System.err.println(zkClient.getChildren("/kafka"));
		System.err.println(zkClient.getChildren("/kafka/brokers/topics"));
		ZkData zkData = ZkUtils.readDataMaybeNull(zkClient, "/kafka/brokers/topics");
		System.out.println(zkData);
		System.out.println(zkData.getStat().getNumChildren());
	}

}
