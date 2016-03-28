package io.confluent.common.utils.zookeeper;

import org.I0Itec.zkclient.ZkClient;

/**
 * 条件更新回调函数接口
 *
 * @author wanggang
 *
 */
public interface ConditionalUpdateCallback {

	/**
	 * 校验方法
	 *
	 * @param client Zookeeper客户端
	 * @param path   路径
	 * @param data   数据
	 * @return
	 */
	public int checker(ZkClient client, String path, String data);

}
