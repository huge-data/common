package io.confluent.common.config;

/**
 * 配置异常类，如果传入无效的配置则抛出该异常
 *
 * @author wanggang
 *
 */
public class ConfigException extends RuntimeException {

	private static final long serialVersionUID = -4622860046208198517L;

	public ConfigException(String message) {
		super(message);
	}

	public ConfigException(String name, Object value) {
		this(name, value, null);
	}

	public ConfigException(String name, Object value, String message) {
		super("Invalid value " + value + " for configuration " + name
				+ (message == null ? "" : ": " + message));
	}

}
