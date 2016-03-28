package io.confluent.common.metrics;

import io.confluent.common.metrics.exceptions.MetricsException;

/**
 * 越界违反异常，也就是当Sensor记录一个Metric边界值的时候抛出该异常
 *
 * @author wanggang
 *
 */
public class QuotaViolationException extends MetricsException {

	private static final long serialVersionUID = -7794124545377307997L;

	public QuotaViolationException(String m) {
		super(m);
	}

}
