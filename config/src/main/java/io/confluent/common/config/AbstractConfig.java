package io.confluent.common.config;

import io.confluent.common.Configurable;
import io.confluent.common.utils.Utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  配置基类
 *
 * @author wanggang
 *
 */
public class AbstractConfig {

	private final Logger logger = LoggerFactory.getLogger(getClass());

	/* 存放已经请求使用的配置信息，用来筛选出未使用的配置 */
	private final Set<String> used;

	/* 原始的配置信息 */
	private final Map<String, ?> originals;

	/* 解析后的配置信息 */
	private final Map<String, Object> values;

	@SuppressWarnings("unchecked")
	public AbstractConfig(ConfigDef definition, Map<?, ?> originals) {
		/* 检查所有的key都是string类型 */
		for (Object key : originals.keySet()) {
			if (!(key instanceof String)) {
				throw new ConfigException(key.toString(), originals.get(key),
						"Key must be a string.");
			}
		}
		// 强制转换成key为string类型的Map
		this.originals = (Map<String, ?>) originals;
		this.values = definition.parse(this.originals);
		this.used = Collections.synchronizedSet(new HashSet<String>());
		logAll();
	}

	protected Object get(String key) {
		if (!values.containsKey(key)) {
			throw new ConfigException(String.format("Unknown configuration '%s'", key));
		}
		used.add(key);
		return values.get(key);
	}

	public int getInt(String key) {
		return (Integer) get(key);
	}

	public long getLong(String key) {
		return (Long) get(key);
	}

	public double getDouble(String key) {
		return (Double) get(key);
	}

	@SuppressWarnings("unchecked")
	public List<String> getList(String key) {
		return (List<String>) get(key);
	}

	public boolean getBoolean(String key) {
		return (Boolean) get(key);
	}

	public String getString(String key) {
		return (String) get(key);
	}

	public Class<?> getClass(String key) {
		return (Class<?>) get(key);
	}

	public Set<String> unused() {
		Set<String> keys = new HashSet<>(originals.keySet());
		keys.removeAll(used);
		return keys;
	}

	/**
	 * 打印出所有参数配置信息
	 */
	private void logAll() {
		StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" values: ");
		sb.append(Utils.NL);
		for (Map.Entry<String, Object> entry : this.values.entrySet()) {
			sb.append('\t');
			sb.append(entry.getKey());
			sb.append(" = ");
			sb.append(entry.getValue());
			sb.append(Utils.NL);
		}
		logger.info(sb.toString());
	}

	/**
	 * 打印出所有未使用的配置参数
	 */
	public void logUnused() {
		for (String key : unused()) {
			logger.warn("The configuration {} = {} was supplied but isn't a known config.", key,
					this.originals.get(key));
		}
	}

	/**
	 * 通过给定的参数名获取该类的实例，如果对象实现了Configurable则使用configure方法配置
	 *
	 * @param key 参数名，即类名
	 * @param t   实现接口的类
	 * @return 该类的一个实例类
	 */
	public <T> T getConfiguredInstance(String key, Class<T> t) {
		Class<?> c = getClass(key);
		if (c == null) {
			return null;
		}
		Object o = Utils.newInstance(c);

		if (!t.isInstance(o)) {
			throw new RuntimeException(c.getName() + " is not an instance of " + t.getName());
		}
		if (o instanceof Configurable) {
			((Configurable) o).configure(this.originals);
		}

		return t.cast(o);
	}

	/**
	 * 获取实例列表
	 */
	public <T> List<T> getConfiguredInstances(String key, Class<T> t) {
		List<String> klasses = getList(key);
		List<T> objects = new ArrayList<>();

		for (String klass : klasses) {
			Class<?> c;
			try {
				c = Class.forName(klass);
			} catch (ClassNotFoundException e) {
				throw new ConfigException(key, klass, "Class " + klass + " could not be found.");
			}
			if (c == null) {
				return null;
			}
			Object o = Utils.newInstance(c);
			if (!t.isInstance(o)) {
				throw new RuntimeException(c.getName() + " is not an instance of " + t.getName());
			}
			if (o instanceof Configurable) {
				((Configurable) o).configure(this.originals);
			}
			objects.add(t.cast(o));
		}

		return objects;
	}

	/**
	 * 从文件中获取配置信息对象，注意这里的默认主目录是src/main/resources
	 *
	 * @param propsFile        配置文件名
	 * @return   配置对象实例
	 * @throws ConfigException
	 */
	public static Properties getPropsFromFile(String propsFile) throws ConfigException {
		Properties props = new Properties();
		if (propsFile == null) {
			return props;
		}
		try (FileInputStream propStream = new FileInputStream(propsFile);) {
			//		try (InputStream propStream = AbstractConfig.class.getClassLoader().getResourceAsStream(propsFile);) {
			props.load(propStream);
		} catch (IOException e) {
			throw new ConfigException("Couldn't load properties from " + propsFile, e);
		}

		return props;
	}

}