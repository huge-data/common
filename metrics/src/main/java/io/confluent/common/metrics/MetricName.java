package io.confluent.common.metrics;

import io.confluent.common.utils.Utils;

import java.util.HashMap;
import java.util.Map;

/**
 * 指标命名类，该类封装了单个Metric的名称、逻辑组和其相关的属性信息
 *
 * 其中，group和tags参数可以用来创建唯一的metric名称，用在往JMX或者任何自定义Report中报告数据时使用。
 * <p>
 * 例如: 标准的JMX MBean可以这样构造 <b>domainName:type=group,key1=val1,key2=val2</b>
 * <p>
 * 使用示例:
 * <pre>{@code
 * // 构建metrics:
 * // Metric和Sensor的全局仓库
 * Metrics metrics = new Metrics();
 *
 * // 获取Sensor
 * Sensor sensor = metrics.sensor("message-sizes");
 *
 * // Metric属性列表
 * Map<String, String> metricTags = new LinkedHashMap<>();
 * metricTags.put("client-id", "producer-1");
 * metricTags.put("topic", "topic");
 *
 * // 向Sensor中添加MetricName
 * MetricName metricName = new MetricName("message-size-avg", "producer-metrics", "average message size", metricTags);
 * sensor.add(metricName, new Avg());
 * metricName = new MetricName("message-size-max", "producer-metrics", metricTags);
 * sensor.add(metricName, new Max());
 *
 * // 当发送信息时记录大小
 * sensor.record(messageSize);
 * }</pre>
 *
 * @author wanggang
 *
 */
public final class MetricName {

	// 该Metric的名称
	private final String name;
	// 该Metric所在的逻辑组名称
	private final String group;
	// 该Metric的描述，可选字段
	private final String description;
	// 该Metric的附加键值对属性信息，可选字段
	private Map<String, String> tags;
	private int hash = 0;

	/**
	 * @param name        名称
	 * @param group       组名
	 * @param description 描述
	 * @param tags        属性
	 */
	public MetricName(String name, String group, String description, Map<String, String> tags) {
		this.name = Utils.notNull(name);
		this.group = Utils.notNull(group);
		this.description = Utils.notNull(description);
		this.tags = Utils.notNull(tags);
	}

	/**
	 * @param name        名称
	 * @param group       组名
	 * @param description 描述
	 * @param tags        属性，按照键值对顺序
	 */
	public MetricName(String name, String group, String description, String... keyValue) {
		this(name, group, description, getTags(keyValue));
	}

	private static Map<String, String> getTags(String... keyValue) {
		if ((keyValue.length % 2) != 0) {
			throw new IllegalArgumentException("keyValue needs to be specified in paris");
		}
		Map<String, String> tags = new HashMap<>();

		for (int i = 0; i < (keyValue.length / 2); i++) {
			tags.put(keyValue[i], keyValue[i + 1]);
		}

		return tags;
	}

	/**
	 * @param name  名称
	 * @param group 组名
	 * @param tags  属性
	 */
	public MetricName(String name, String group, Map<String, String> tags) {
		this(name, group, "", tags);
	}

	/**
	 * @param name        名称
	 * @param group       组名
	 * @param description 描述
	 */
	public MetricName(String name, String group, String description) {
		this(name, group, description, new HashMap<String, String>());
	}

	/**
	 * @param name  名称
	 * @param group 组名
	 */
	public MetricName(String name, String group) {
		this(name, group, "", new HashMap<String, String>());
	}

	public String name() {
		return this.name;
	}

	public String group() {
		return this.group;
	}

	public Map<String, String> tags() {
		return this.tags;
	}

	public String description() {
		return this.description;
	}

	@Override
	public int hashCode() {
		if (hash != 0) {
			return hash;
		}
		final int prime = 31;
		int result = 1;
		result = prime * result + ((group == null) ? 0 : group.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		this.hash = result;

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MetricName other = (MetricName) obj;
		if (group == null) {
			if (other.group != null) {
				return false;
			}
		} else if (!group.equals(other.group)) {
			return false;
		}
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.equals(other.name)) {
			return false;
		}
		if (tags == null) {
			if (other.tags != null) {
				return false;
			}
		} else if (!tags.equals(other.tags)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "MetricName [name=" + name + ", group=" + group + ", description=" + description
				+ ", tags=" + tags + "]";
	}

}