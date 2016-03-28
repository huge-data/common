package io.confluent.common.metrics;

import io.confluent.common.metrics.exceptions.MetricsException;

import java.lang.management.ManagementFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JMX报告器，基于指标名称的动态MBean（可管理的Java对象）
 *
 * @author wanggang
 *
 */
public class JmxReporter implements MetricsReporter {

	private static final Logger logger = LoggerFactory.getLogger(JmxReporter.class);

	// 同步锁
	private static final Object lock = new Object();
	// KafkaMetric对象管理类 键值对集合
	private final Map<String, KafkaMbean> mbeans = new HashMap<>();
	// 前缀字符串
	private String prefix;

	public JmxReporter() {
		this("");
	}

	/**
	 * 构造函数，所有Metric都使用该前缀
	 *
	 * @param prefix  前缀字符串
	 */
	public JmxReporter(String prefix) {
		this.prefix = prefix;
	}

	@Override
	public void configure(Map<String, ?> configs) {
		// @ TODO
	}

	@Override
	public void init(List<KafkaMetric> metrics) {
		synchronized (lock) {
			// 添加属性（也就是添加KafkaMetric对象）
			for (KafkaMetric metric : metrics) {
				addAttribute(metric);
			}
			// 注册MBean
			for (KafkaMbean mbean : mbeans.values()) {
				reregister(mbean);
			}
		}
	}

	@Override
	public void metricChange(KafkaMetric metric) {
		synchronized (lock) {
			// 添加属性并注册
			KafkaMbean mbean = addAttribute(metric);
			reregister(mbean);
		}
	}

	/**
	 * 添加属性信息并返回，如果由KafkaMetric得到的KafkaMbean不存在则添加
	 *
	 * @param metric Kafka指标
	 * @return KafkaMbean对象
	 */
	private KafkaMbean addAttribute(KafkaMetric metric) {
		try {
			MetricName metricName = metric.metricName();
			String mBeanName = getMBeanName(metricName);
			if (!this.mbeans.containsKey(mBeanName))
				mbeans.put(mBeanName, new KafkaMbean(mBeanName));
			KafkaMbean mbean = this.mbeans.get(mBeanName);
			mbean.setAttribute(metricName.name(), metric);
			return mbean;
		} catch (JMException e) {
			throw new MetricsException("Error creating mbean attribute for metricName :"
					+ metric.metricName(), e);
		}
	}

	/**
	 * 获取KafkaMetric对象管理类名称
	 *
	 * @param metricName 指标名称信息
	 * @return 标准的JMX MBean名称格式： domainName:type=metricType,key1=val1,key2=val2
	 */
	private String getMBeanName(MetricName metricName) {
		StringBuilder mBeanName = new StringBuilder();
		mBeanName.append(prefix);
		mBeanName.append(":type=");
		mBeanName.append(metricName.group());
		for (Map.Entry<String, String> entry : metricName.tags().entrySet()) {
			if (entry.getKey().length() <= 0 || entry.getValue().length() <= 0)
				continue;
			mBeanName.append(",");
			mBeanName.append(entry.getKey());
			mBeanName.append("=");
			mBeanName.append(entry.getValue());
		}

		return mBeanName.toString();
	}

	/**
	 * 注销KafkaMetric对象管理类
	 *
	 * @param mbean KafkaMetric对象管理类
	 */
	private void unregister(KafkaMbean mbean) {
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();
		try {
			if (server.isRegistered(mbean.name())) {
				server.unregisterMBean(mbean.name());
			}
		} catch (JMException e) {
			throw new MetricsException("Error unregistering mbean", e);
		}
	}

	/**
	 * 重新注册KafkaMetric对象管理类
	 *
	 * @param mbean KafkaMetric对象管理类
	 */
	private void reregister(KafkaMbean mbean) {
		unregister(mbean);
		try {
			ManagementFactory.getPlatformMBeanServer().registerMBean(mbean, mbean.name());
		} catch (JMException e) {
			throw new MetricsException("Error registering mbean " + mbean.name(), e);
		}
	}

	@Override
	public void close() {
		synchronized (lock) {
			for (KafkaMbean mbean : this.mbeans.values()) {
				unregister(mbean);
			}
		}
	}

	/**
	 * KafkaMetric对象管理类
	 *
	 * @author wanggang
	 *
	 */
	private static class KafkaMbean implements DynamicMBean {

		// 对象名
		private final ObjectName objectName;
		// Kafka指标键值对集合
		private final Map<String, KafkaMetric> metrics;

		public KafkaMbean(String mbeanName) throws MalformedObjectNameException {
			this.metrics = new HashMap<>();
			this.objectName = new ObjectName(mbeanName);
		}

		public ObjectName name() {
			return objectName;
		}

		/**
		 * 设置属性，也就是添加管理的KafkaMetric对象
		 *
		 * @param name
		 * @param metric
		 */
		public void setAttribute(String name, KafkaMetric metric) {
			this.metrics.put(name, metric);
		}

		/**
		 * 根据KafkaMetric名称获取其对应的指标计算值
		 */
		@Override
		public Object getAttribute(String name) throws AttributeNotFoundException, MBeanException,
				ReflectionException {
			if (this.metrics.containsKey(name)) {
				return this.metrics.get(name).value();
			} else {
				throw new AttributeNotFoundException("Could not find attribute " + name);
			}
		}

		@Override
		public AttributeList getAttributes(String[] names) {
			try {
				AttributeList list = new AttributeList();
				for (String name : names) {
					list.add(new Attribute(name, getAttribute(name)));
				}
				return list;
			} catch (Exception e) {
				logger.error("Error getting JMX attribute: ", e);
				return new AttributeList();
			}
		}

		@Override
		public MBeanInfo getMBeanInfo() {
			MBeanAttributeInfo[] attrs = new MBeanAttributeInfo[metrics.size()];
			int i = 0;
			for (Map.Entry<String, KafkaMetric> entry : this.metrics.entrySet()) {
				String attribute = entry.getKey();
				KafkaMetric metric = entry.getValue();
				attrs[i] = new MBeanAttributeInfo(attribute, double.class.getName(), metric
						.metricName().description(), true, false, false);
				i += 1;
			}
			return new MBeanInfo(this.getClass().getName(), "", attrs, null, null, null);
		}

		@Override
		public Object invoke(String name, Object[] params, String[] sig) throws MBeanException,
				ReflectionException {
			throw new UnsupportedOperationException("Invoke not allowed.");
		}

		@Override
		public void setAttribute(Attribute attribute) throws AttributeNotFoundException,
				InvalidAttributeValueException, MBeanException, ReflectionException {
			throw new UnsupportedOperationException("Set not allowed.");
		}

		@Override
		public AttributeList setAttributes(AttributeList list) {
			throw new UnsupportedOperationException("Set not allowed.");
		}

	}

}
