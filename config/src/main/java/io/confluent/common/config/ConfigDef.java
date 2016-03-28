package io.confluent.common.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *  参数定义类
 * <p> 参数定义字段包括：参数类型、默认值、参数说明和任意参数值验证逻辑
 * <p> 使用示例：
 *
 * <pre>
 * // 参数定义
 * ConfigDef defs = new ConfigDef();
 * defs.define("config_name", Type.STRING, "default_string_value", "This_configuration_is_used_for_something.");
 * defs.define("another_config_name", Type.INT, 42, Range.atLeast(0), "More_documentation_on_this_config.");
 *
 * // 加入配置文件
 * Properties props = new Properties();
 * props.setProperty("config_name", "some_value");
 * Map<String, Object> configs = defs.parse(props);
 *
 * // 返回字符串 "some_value"
 * String someConfig = (String) configs.get("config_name");
 * // 返回默认值 42
 * int anotherConfig = (Integer) configs.get("another_config_name");
 * </pre>
 *
 * 该配置类既可以单独使用，也可以和 {@link AbstractConfig} 一起使用，
 * {@link AbstractConfig} 提供读取配置信息的额外功能。
 *
 * @author wanggang
 *
 */
public class ConfigDef {

	// 默认配置值为空字符串
	private static final Object NO_DEFAULT_VALUE = "";

	private final Map<String, ConfigKey> configKeys = new HashMap<>();

	/**
	 * 返回在 {@linkplain ConfigDef} 中定义的非修改属性名集合
	 *
	 * @return 包含键名非修改 {@link Set} 实例集合
	 */
	public Set<String> names() {
		return Collections.unmodifiableSet(configKeys.keySet());
	}

	/**
	 * 参数定义
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param defaultValue  参数默认值
	 * @param validator     参数验证器
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef define(String name, Type type, Object defaultValue, Validator validator,
			Importance importance, String documentation) {
		if (configKeys.containsKey(name)) {
			throw new ConfigException("Configuration " + name + " is defined twice.");
		}
		Object parsedDefault = (defaultValue == NO_DEFAULT_VALUE) ? NO_DEFAULT_VALUE : parseType(
				name, defaultValue, type);
		configKeys.put(name, new ConfigKey(name, type, parsedDefault, validator, importance,
				documentation));
		return this;
	}

	/**
	 * 覆盖参数定义
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param defaultValue  参数默认值
	 * @param validator     参数验证器
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef defineOverride(String name, Type type, Object defaultValue,
			Validator validator, Importance importance, String documentation) {
		if (!configKeys.containsKey(name)) {
			throw new ConfigException("Configuration " + name
					+ " is defined as an override but does not exist.");
		}
		Object parsedDefault = (defaultValue == NO_DEFAULT_VALUE) ? NO_DEFAULT_VALUE : parseType(
				name, defaultValue, type);
		configKeys.put(name, new ConfigKey(name, type, parsedDefault, validator, importance,
				documentation));
		return this;
	}

	/**
	 * 参数定义，不使用参数验证器
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param defaultValue  参数默认值
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef define(String name, Type type, Object defaultValue, Importance importance,
			String documentation) {
		return define(name, type, defaultValue, null, importance, documentation);
	}

	/**
	 * 覆盖参数定义，不使用参数验证器
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param defaultValue  参数默认值
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef defineOverride(String name, Type type, Object defaultValue,
			Importance importance, String documentation) {
		return defineOverride(name, type, defaultValue, null, importance, documentation);
	}

	/**
	 * 定义必要参数，使用默认的空字符串
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param validator     参数验证器
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef define(String name, Type type, Validator validator, Importance importance,
			String documentation) {
		return define(name, type, NO_DEFAULT_VALUE, validator, importance, documentation);
	}

	/**
	 * 覆盖定义必要参数，使用默认的空字符串
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param validator     参数验证器
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef defineOverride(String name, Type type, Validator validator,
			Importance importance, String documentation) {
		return defineOverride(name, type, NO_DEFAULT_VALUE, validator, importance, documentation);
	}

	/**
	 * 定义必要参数，使用默认的空字符串和空参数验证器
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param validator     参数验证器
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef define(String name, Type type, Importance importance, String documentation) {
		return define(name, type, NO_DEFAULT_VALUE, null, importance, documentation);
	}

	/**
	 * 覆盖定义必要参数，使用默认的空字符串和空参数验证器
	 *
	 * @param name          参数名
	 * @param type          参数类型
	 * @param validator     参数验证器
	 * @param importance    参数重要级别: 可能需要改变的重要程度
	 * @param documentation 参数说明
	 * @return 自定义配置ConfigDef对象
	 */
	public ConfigDef defineOverride(String name, Type type, Importance importance,
			String documentation) {
		return defineOverride(name, type, NO_DEFAULT_VALUE, null, importance, documentation);
	}

	/**
	 * 根据参数定义信息解析并且验证参数
	 * 输入是一个包含参数的Map，检查Map的key是否都是string类型，value可以使任何合适的类型。
	 * 传入的Map可以直接是java.util.Properties实例，或者动态构造的Map
	 *
	 * @param props 需要解析和验证的参数Map
	 * @return 解析和验证后的参数Map. 其中key是参数名，value是解析后的合适类型
	 */
	public Map<String, Object> parse(Map<?, ?> props) {
		/* 解析所有已知key */
		Map<String, Object> values = new HashMap<>();
		for (ConfigKey key : configKeys.values()) {
			Object value;
			if (props.containsKey(key.name)) { // 包含键名
				value = parseType(key.name, props.get(key.name), key.type);
			} else if (key.defaultValue == NO_DEFAULT_VALUE) { // 默认值为空字符串
				throw new ConfigException("Missing required configuration \"" + key.name
						+ "\" which has no default value.");
			} else {
				value = key.defaultValue;
			}
			if (key.validator != null) {
				key.validator.ensureValid(key.name, value);
			}
			values.put(key.name, value);
		}
		return values;
	}

	/**
	 * 根据给定的期望参数类型解析出参数值
	 *
	 * @param name  参数名
	 * @param value 参数值
	 * @param type  期望参数类型
	 * @return 解析出来的参数值对象
	 */
	private Object parseType(String name, Object value, Type type) {
		try {
			String trimmed = null;
			if (value instanceof String) {
				trimmed = ((String) value).trim();
			}
			switch (type) {
			case BOOLEAN:
				if (value instanceof String) {
					if (trimmed.equalsIgnoreCase("true")) {
						return true;
					} else if (trimmed.equalsIgnoreCase("false")) {
						return false;
					} else {
						throw new ConfigException(name, value,
								"Expected value to be either true or false");
					}
				} else if (value instanceof Boolean) {
					return value;
				} else {
					throw new ConfigException(name, value,
							"Expected value to be either true or false");
				}
			case STRING:
				if (value instanceof String) {
					return trimmed;
				} else {
					throw new ConfigException(name, value,
							"Expected value to be a string, but it was a "
									+ value.getClass().getName());
				}
			case INT:
				if (value instanceof Integer) {
					return value;
				} else if (value instanceof String) {
					return Integer.parseInt(trimmed);
				} else {
					throw new ConfigException(name, value, "Expected value to be an number.");
				}
			case LONG:
				if (value instanceof Integer) {
					return ((Integer) value).longValue();
				}
				if (value instanceof Long) {
					return value;
				} else if (value instanceof String) {
					return Long.parseLong(trimmed);
				} else {
					throw new ConfigException(name, value, "Expected value to be an number.");
				}
			case DOUBLE:
				if (value instanceof Number) {
					return ((Number) value).doubleValue();
				} else if (value instanceof String) {
					return Double.parseDouble(trimmed);
				} else {
					throw new ConfigException(name, value, "Expected value to be an number.");
				}
			case LIST:
				if (value instanceof List) {
					return value;
				} else if (value instanceof String) {
					if (trimmed.isEmpty()) {
						return Collections.emptyList();
					} else {
						return Arrays.asList(trimmed.split("\\s*,\\s*", -1));
					}
				} else {
					throw new ConfigException(name, value, "Expected a comma separated list.");
				}
			case CLASS:
				if (value instanceof Class) {
					return value;
				} else if (value instanceof String) {
					return Class.forName(trimmed);
				} else {
					throw new ConfigException(name, value,
							"Expected a Class instance or class name.");
				}
			default:
				throw new IllegalStateException("Unknown type.");
			}
		} catch (NumberFormatException e) {
			throw new ConfigException(name, value, "Not a number of type " + type);
		} catch (ClassNotFoundException e) {
			throw new ConfigException(name, value, "Class " + value + " could not be found.");
		}
	}

	/**
	 * 参数类型，分为：Boolean、String、long、double、List、Class
	 */
	public enum Type {
		BOOLEAN, STRING, INT, LONG, DOUBLE, LIST, CLASS;
	}

	/**
	 * 参数重要级别，分为：高、中、低
	 *
	 */
	public enum Importance {
		HIGH, MEDIUM, LOW
	}

	/**
	 * 参数验证器接口
	 */
	public interface Validator {
		public void ensureValid(String name, Object o);
	}

	/**
	 * 数值范围验证器
	 */
	public static class Range implements Validator {

		// 参数最小值
		private final Number min;
		// 参数最大值
		private final Number max;

		private Range(Number min, Number max) {
			this.min = min;
			this.max = max;
		}

		/**
		 * 设置验证区间为[min,...)
		 *
		 * @param min 参数最小值
		 */
		public static Range atLeast(Number min) {
			return new Range(min, null);
		}

		/**
		 * 设置验证区间为[...,max]
		 *
		 * @param max 参数最大值
		 */
		public static Range atMore(Number max) {
			return new Range(null, max);
		}

		/**
		 * 设置验证区间为[min,max]
		 *
		 * @param min 参数最小值
		 * @param max 参数最大值
		 * @return
		 */
		public static Range between(Number min, Number max) {
			return new Range(min, max);
		}

		/**
		 * 注意：输入的参数值对象必须为Number类型
		 */
		@Override
		public void ensureValid(String name, Object o) {
			Number n = (Number) o;
			if (min != null && n.doubleValue() < min.doubleValue()) {
				throw new ConfigException(name, o, "Value must be at least " + min);
			}
			if (max != null && n.doubleValue() > max.doubleValue()) {
				throw new ConfigException(name, o, "Value must be no more than " + max);
			}
		}

		@Override
		public String toString() {
			if (min == null) {
				return "(...," + max + "]";
			} else if (max == null) {
				return "[" + min + ",...)";
			} else {
				return "[" + min + ",...," + max + "]";
			}
		}

	}

	/**
	 * 字符串验证器
	 */
	public static class ValidString implements Validator {

		// 验证字符串列表模板
		List<String> validStrings;

		private ValidString(List<String> validStrings) {
			this.validStrings = validStrings;
		}

		public static ValidString in(List<String> validStrings) {
			return new ValidString(validStrings);
		}

		/**
		 * 注意：输入的参数值对象必须为String类型
		 */
		@Override
		public void ensureValid(String name, Object o) {
			String s = (String) o;
			// 模板中包含该字符串认为是验证通过
			if (!validStrings.contains(s)) {
				throw new ConfigException(name, o, "String must be one of:" + join(validStrings));
			}
		}

		@Override
		public String toString() {
			return "[" + join(validStrings) + "]";
		}

		private String join(List<String> list) {
			StringBuilder sb = new StringBuilder();
			boolean first = true;
			for (String item : list) {
				if (first) {
					first = false;
				} else {
					sb.append(",");
				}
				sb.append(item);
			}
			return sb.toString();
		}
	}

	/**
	 * 参数对象
	 */
	private static class ConfigKey {

		// 参数名
		public final String name;
		// 参数类型
		public final Type type;
		// 参数说明
		public final String documentation;
		// 参数默认值
		public final Object defaultValue;
		// 参数验证器
		public final Validator validator;
		// 参数重要级别
		public final Importance importance;

		public ConfigKey(String name, Type type, Object defaultValue, Validator validator,
				Importance importance, String documentation) {
			super();
			this.name = name;
			this.type = type;
			this.defaultValue = defaultValue;
			this.validator = validator;
			this.importance = importance;
			if (this.validator != null) {
				this.validator.ensureValid(name, defaultValue);
			}
			this.documentation = documentation;
		}

		public boolean hasDefault() {
			return this.defaultValue != NO_DEFAULT_VALUE;
		}

	}

	/**
	 * 生成HTML列表文件
	 */
	public String toHtmlTable() {
		List<ConfigDef.ConfigKey> configs = sortedConfigs();
		StringBuilder sb = new StringBuilder();
		sb.append("<table>\n");
		sb.append("<tr>\n");
		sb.append("<th>Name</th>\n");
		sb.append("<th>Type</th>\n");
		sb.append("<th>Default</th>\n");
		sb.append("<th>Importance</th>\n");
		sb.append("<th>Description</th>\n");
		sb.append("</tr>\n");
		for (ConfigKey def : configs) {
			sb.append("<tr>\n");
			sb.append("<td>");
			sb.append(def.name);
			sb.append("</td>");
			sb.append("<td>");
			sb.append(def.type.toString().toLowerCase());
			sb.append("</td>");
			sb.append("<td>");
			sb.append((def.defaultValue == null) ? "" : def.defaultValue);
			sb.append("</td>");
			sb.append("<td>");
			sb.append(def.importance.toString().toLowerCase());
			sb.append("</td>");
			sb.append("<td>");
			sb.append(def.documentation);
			sb.append("</td>");
			sb.append("</tr>\n");
		}
		sb.append("</table>");

		return sb.toString();
	}

	/**
	 * 生成结构化的文本，可以嵌入到Sphinx中进行搜索的文档
	 */
	public String toRst() {
		List<ConfigDef.ConfigKey> configs = sortedConfigs();
		StringBuilder sb = new StringBuilder();

		for (ConfigKey def : configs) {
			sb.append("``");
			sb.append(def.name);
			sb.append("``\n");
			for (String docLine : def.documentation.split("\n")) {
				if (docLine.length() == 0) {
					continue;
				}
				sb.append("  ");
				sb.append(docLine);
				sb.append("\n\n");
			}
			sb.append("  * Type: ");
			sb.append(def.type.toString().toLowerCase());
			sb.append("\n");
			if (def.defaultValue != null) {
				sb.append("  * Default: ");
				if (def.type == Type.STRING) {
					sb.append("\"");
					sb.append(def.defaultValue);
					sb.append("\"");
				} else {
					sb.append(def.defaultValue);
				}
				sb.append("\n");
			}
			sb.append("  * Importance: ");
			sb.append(def.importance.toString().toLowerCase());
			sb.append("\n\n");
		}

		return sb.toString();
	}

	/**
	 * 获取排序后的参数列表：首先根据有无默认值排序，然后通过重要级别和名称来排序
	 */
	private List<ConfigDef.ConfigKey> sortedConfigs() {
		// 首先根据有无默认值排序，然后根据重要级别和名称排序
		List<ConfigDef.ConfigKey> configs = new ArrayList<>(this.configKeys.values());
		Collections.sort(configs, new Comparator<ConfigDef.ConfigKey>() {

			@Override
			public int compare(ConfigDef.ConfigKey k1, ConfigDef.ConfigKey k2) {
				// 根据有无默认值排序，无默认值最小
				if (!k1.hasDefault() && k2.hasDefault()) {
					return -1;
				} else if (!k2.hasDefault() && k1.hasDefault()) {
					return 1;
				}

				// 根据重要级别排序
				int cmp = k1.importance.compareTo(k2.importance);
				if (cmp == 0)
				// 根据名称的字母
				{
					return k1.name.compareTo(k2.name);
				} else {
					return cmp;
				}
			}

		});

		return configs;
	}

}
