package io.confluent.common.metrics;

import io.confluent.common.metrics.stats.Avg;
import io.confluent.common.metrics.stats.Total;

import org.junit.Test;

public class JmxReporterTest {

	@Test
	public void testJmxRegistration() throws Exception {
		Metrics metrics = new Metrics();
		metrics.addReporter(new JmxReporter());
		Sensor sensor = metrics.sensor("kafka.requests");
		sensor.add(new MetricName("pack.bean1.avg", "grp1"), new Avg());
		sensor.add(new MetricName("pack.bean2.total", "grp2"), new Total());
		Sensor sensor2 = metrics.sensor("kafka.blah");
		sensor2.add(new MetricName("pack.bean1.some", "grp1"), new Total());
		sensor2.add(new MetricName("pack.bean2.some", "grp1"), new Total());
		metrics.close();
	}

}
