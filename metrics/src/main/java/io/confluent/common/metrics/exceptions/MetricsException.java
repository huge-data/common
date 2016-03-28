package io.confluent.common.metrics.exceptions;

/**
 * 指标异常
 *
 * @author wanggang
 *
 */
public class MetricsException extends RuntimeException {

	private static final long serialVersionUID = 301758010257524715L;

	public MetricsException(String message, Throwable cause) {
		super(message, cause);
	}

	public MetricsException(String message) {
		super(message);
	}

	public MetricsException(Throwable cause) {
		super(cause);
	}

	public MetricsException() {
		super();
	}

}
