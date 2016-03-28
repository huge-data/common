package io.confluent.common.utils.zookeeper;

import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.exception.ZkBadVersionException;
import org.I0Itec.zkclient.exception.ZkNoNodeException;
import org.I0Itec.zkclient.exception.ZkNodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZkUtils {

	private static final Logger logger = LoggerFactory.getLogger(ZkUtils.class);

	/**
	 * 确认持久化的路径在Zookeeper中存在，如果不存在则创建该路径
	 *
	 * @param client  Zookeeper客户端
	 * @param path    路径名
	 */
	public static void makeSurePersistentPathExists(ZkClient client, String path) {
		if (!client.exists(path)) {
			// 不会抛出NoNodeException或者NodeExistsException
			client.createPersistent(path, true);
		}
	}

	/**
	 * 创建父路径或上级路径
	 *
	 * @param client Zookeeper客户端
	 * @param path   路径名
	 */
	private static void createParentPath(ZkClient client, String path) {
		String parentDir = path.substring(0, path.lastIndexOf('/'));
		if (parentDir.length() != 0) {
			client.createPersistent(parentDir, true);
		}
	}

	/**
	 * 创建持久化路径，如果父路径不存在则创建父路径
	 *
	 * @param client  Zookeeper客户端
	 * @param path    路径名
	 * @param data    数据
	 */
	public static void createPersistentPath(ZkClient client, String path, String data) {
		try {
			client.createPersistent(path, data);
		} catch (ZkNoNodeException e) {
			createParentPath(client, path);
			client.createPersistent(path, data);
		}
	}

	/**
	 * 更新持久化节点的值，如果父路径不存在则创建父路径，
	 * 不抛出NodeExistException异常，返回更新后的路径zkVersion
	 *
	 * @param client   Zookeeper客户端
	 * @param path     路径名
	 * @param data     数据
	 */
	public static void updatePersistentPath(ZkClient client, String path, String data) {
		try {
			client.writeData(path, data);
		} catch (ZkNoNodeException nne) {
			createParentPath(client, path);
			try {
				client.createPersistent(path, data);
			} catch (ZkNodeExistsException nee) {
				client.writeData(path, data);
			}
		}
	}

	/**
	 * 读取数据
	 *
	 * @param client   Zookeeper客户端
	 * @param path     路径名
	 * @return   Zookeeper数据对象
	 */
	public static ZkData readData(ZkClient client, String path) {
		Stat stat = new Stat();
		String data = client.readData(path, stat);

		return new ZkData(data, stat);
	}

	/**
	 * 读取数据可能为空
	 *
	 * @param client   Zookeeper客户端
	 * @param path     路径名
	 * @return   Zookeeper数据对象
	 */
	public static ZkData readDataMaybeNull(ZkClient client, String path) {
		Stat stat = new Stat();
		String data = client.readData(path, stat);

		return new ZkData(data, stat);
	}

	/**
	 * 根据选择条件更新持久化路径数据
	 * 更新成功则返回新版本号作为true，否则（路径不存在，当前版本号不是期望的版本号）返回-1作为false。
	 * 在根据选择条件更新的时候，如果有ConnectionLossException异常，zkClient将尝试再次更新，
	 * 直到前一个更新成功为止（但是存储的zkVersion不再和期望的版本号匹配了）。这种情况下，系统将运行
	 * optionalChecker去更深入的检查前一次写数据是否真正的成功了。
	 *
	 * @param client   Zookeeper客户端
	 * @param path     路径名
	 * @param data     需要更新的数据
	 * @param expectedVersion   期望版本号
	 * @param customConditionCallback  自定义选择条件回调函数
	 * @return  成功与否，不成功返回-1,成功返回新的版本号
	 */
	public static int conditionalUpdatePersistentPath(ZkClient client, String path, String data,
			int expectedVersion, ConditionalUpdateCallback customConditionCallback) {
		try {
			Stat stat = client.writeDataReturnStat(path, data, expectedVersion);
			logger.debug(String
					.format("Conditional update of path %s with value %s and expected version %d succeeded, returning the new version: %d",
							path, data, expectedVersion, stat.getVersion()));
			return stat.getVersion();
		} catch (ZkBadVersionException e) {
			if (customConditionCallback != null) {
				return customConditionCallback.checker(client, path, data);
			} else {
				logger.warn(String
						.format("Conditional update of path %s with data %s and expected version %d failed due to "
								+ "%s. When there is a ConnectionLossException during the conditional update, "
								+ "ZkClient will retry the update and may fail since the previous update may have "
								+ "succeeded (but the stored zkVersion no longer matches the expected one). "
								+ "In this case, the customConditionCallback is required to further check if the "
								+ "previous write did indeed succeed, but was not passed in here.",
								path, data, expectedVersion, e.getMessage()));
				return -1;
			}
		} catch (Exception e) {
			logger.warn(String
					.format("Conditional update of path %s with data %s and expected version %d failed due to %s",
							path, data, expectedVersion, e.getMessage()));
			return -1;
		}
	}

}
