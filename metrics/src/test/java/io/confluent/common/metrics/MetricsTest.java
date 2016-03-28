package io.confluent.common.metrics;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.confluent.common.metrics.stats.Avg;
import io.confluent.common.metrics.stats.Count;
import io.confluent.common.metrics.stats.Gauge;
import io.confluent.common.metrics.stats.Max;
import io.confluent.common.metrics.stats.Min;
import io.confluent.common.metrics.stats.Percentile;
import io.confluent.common.metrics.stats.Percentiles;
import io.confluent.common.metrics.stats.Rate;
import io.confluent.common.metrics.stats.Total;
import io.confluent.common.utils.MockTime;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class MetricsTest {

	// 误差率
	private static double EPS = 0.000001;

	MockTime time = new MockTime();
	Metrics metrics = new Metrics(new MetricConfig(),
			Arrays.asList((MetricsReporter) new JmxReporter()), time);

	@Test
	public void testMetricName_测试指标命名() {
		MetricName n1 = new MetricName("name", "group", "description", "key1", "value1");
		Map<String, String> tags = new HashMap<>();
		tags.put("key1", "value1");
		MetricName n2 = new MetricName("name", "group", "description", tags);
		assertEquals("通过不同方式创建的MetricName应该相同", n1, n2);

		try {
			new MetricName("name", "group", "description", "key1");
			fail("使用非键值对创建的MetricName会抛出异常");
		} catch (IllegalArgumentException e) {
			// DO NOTHING
		}
	}

	@Test(expected = IllegalArgumentException.class)
	public void testMetricNameWithNoKeyValuePairs_指标命名的标签必须成对() {
		new MetricName("name", "group", "description", "key1");
	}

	@Test
	public void testSimpleStats_测试采样信号() throws Exception {
		// 常量计算器
		ConstantMeasurable measurable = new ConstantMeasurable();

		metrics.addMetric(new MetricName("direct.measurable", "grp1",
				"The fraction of time an appender waits for space allocation."), measurable);

		Sensor s = metrics.sensor("test.sensor");
		s.add(new MetricName("test.avg", "grp1"), new Avg());
		s.add(new MetricName("test.max", "grp1"), new Max());
		s.add(new MetricName("test.min", "grp1"), new Min());
		s.add(new MetricName("test.rate", "grp1"), new Rate(TimeUnit.SECONDS));
		s.add(new MetricName("test.occurences", "grp1"), new Rate(TimeUnit.SECONDS, new Count()));
		s.add(new MetricName("test.count", "grp1"), new Count());
		s.add(new Percentiles(100, -100, 100, Percentiles.BucketSizing.CONSTANT, new Percentile(
				new MetricName("test.median", "grp1"), 50.0), new Percentile(new MetricName(
				"test.perc99_9", "grp1"), 99.9)));

		Sensor s2 = metrics.sensor("test.sensor2");
		s2.add(new MetricName("s2.total", "grp1"), new Total());
		// s2进行了一次记录统计
		s2.record(5.0);

		// 这里对s进行了10次记录统计，s2没有
		for (int i = 0; i < 10; i++) {
			// 记录0到9十个数据
			s.record(i);
		}

		// 暂停2秒
		time.sleep(2000);

		// 常量计算一次，得到原来的记录值
		assertEquals("s2 reflects the constant value", 5.0,
				metrics.metrics().get(new MetricName("s2.total", "grp1")).value(), EPS);
		// 10个数据的平均值
		assertEquals("Avg(0...9) = 4.5", 4.5,
				metrics.metrics().get(new MetricName("test.avg", "grp1")).value(), EPS);
		// 10个数据的最大值
		assertEquals("Max(0...9) = 9", 9.0,
				metrics.metrics().get(new MetricName("test.max", "grp1")).value(), EPS);
		// 10个数据的最小值
		assertEquals("Min(0...9) = 0", 0.0,
				metrics.metrics().get(new MetricName("test.min", "grp1")).value(), EPS);
		// 10个数据的值的速率，45/2
		assertEquals("Rate(0...9) = 22.5", 22.5,
				metrics.metrics().get(new MetricName("test.rate", "grp1")).value(), EPS);
		// 10个数据的发生频率，10/2
		assertEquals("Occurences(0...9) = 5", 5.0,
				metrics.metrics().get(new MetricName("test.occurences", "grp1")).value(), EPS);
		// 10个数据的总次数
		assertEquals("Count(0...9) = 10", 10.0,
				metrics.metrics().get(new MetricName("test.count", "grp1")).value(), EPS);
	}

	@Test
	public void testHierarchicalSensors_测试传感器层级包含关系() {
		Sensor parent1 = metrics.sensor("test.parent1");
		parent1.add(new MetricName("test.parent1.count", "grp1"), new Count());
		Sensor parent2 = metrics.sensor("test.parent2");
		parent2.add(new MetricName("test.parent2.count", "grp1"), new Count());
		Sensor child1 = metrics.sensor("test.child1", parent1, parent2);
		child1.add(new MetricName("test.child1.count", "grp1"), new Count());
		Sensor child2 = metrics.sensor("test.child2", parent1);
		child2.add(new MetricName("test.child2.count", "grp1"), new Count());
		Sensor grandchild = metrics.sensor("test.grandchild", child1);
		grandchild.add(new MetricName("test.grandchild.count", "grp1"), new Count());

		// 每个sensor增加一次记录，默认值为1.0
		parent1.record();
		parent2.record();
		child1.record();
		child2.record();
		grandchild.record();

		double p1 = parent1.metrics().get(0).value(); // 4
		double p2 = parent2.metrics().get(0).value(); // 3
		double c1 = child1.metrics().get(0).value(); // 2
		double c2 = child2.metrics().get(0).value(); // 1
		double gc = grandchild.metrics().get(0).value(); // 1

		// 每个metric的count计算值应该等于其子sensor数+1
		assertEquals(1.0, gc, EPS);
		assertEquals(1.0, c2, EPS);
		assertEquals(1.0 + gc, c1, EPS);
		assertEquals(1.0 + c1, p2, EPS);
		assertEquals(1.0 + c1 + c2, p1, EPS);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testBadSensorHiearchy_测试传感器循环层级引用错误() {
		Sensor p = metrics.sensor("parent");
		Sensor c1 = metrics.sensor("child1", p);
		Sensor c2 = metrics.sensor("child2", p);
		// 调用失败，层级引用不能形成闭环
		metrics.sensor("gc", c1, c2);
	}

	@Test
	public void testEventWindowing_测试事件数窗口_只有一个窗口() {
		Count count = new Count();
		MetricConfig config = new MetricConfig().eventWindow(1).samples(2);
		// 第一个记录
		count.record(config, 1.0, time.milliseconds());
		// 第二个记录
		count.record(config, 1.0, time.milliseconds());
		assertEquals(2.0, count.measure(config, time.milliseconds()), EPS);
		// 第三个记录，因为样本数为2,所以第一个记录没有了
		count.record(config, 1.0, time.milliseconds());
		// 第一个记录超时，被移除
		assertEquals(2.0, count.measure(config, time.milliseconds()), EPS);
	}

	@Test
	public void testEventWindowing_测试事件数窗口_有多个窗口() {
		Count count = new Count();
		MetricConfig config = new MetricConfig().eventWindow(2).samples(2);
		count.record(config, 1.0, time.milliseconds());
		count.record(config, 1.0, time.milliseconds());
		count.record(config, 1.0, time.milliseconds());
		count.record(config, 1.0, time.milliseconds());
		assertEquals(4.0, count.measure(config, time.milliseconds()), EPS);
		count.record(config, 1.0, time.milliseconds());
		System.err.println(count.measure(config, time.milliseconds()));
		assertEquals(3.0, count.measure(config, time.milliseconds()), EPS);
	}

	@Test
	public void testTimeWindowing_测试时间窗口_只有一个窗口() {
		Count count = new Count();
		MetricConfig config = new MetricConfig().timeWindow(1, TimeUnit.MILLISECONDS).samples(2);
		count.record(config, 1.0, time.milliseconds());
		time.sleep(1);
		count.record(config, 1.0, time.milliseconds());
		assertEquals(2.0, count.measure(config, time.milliseconds()), EPS);
		time.sleep(1);
		// 第一个记录超时，被移除
		count.record(config, 1.0, time.milliseconds());
		assertEquals(2.0, count.measure(config, time.milliseconds()), EPS);
	}

	@Test
	public void testTimeWindowing_测试时间窗口_有多个窗口() {
		Count count = new Count();
		MetricConfig config = new MetricConfig().timeWindow(2, TimeUnit.MILLISECONDS).samples(2);
		count.record(config, 1.0, time.milliseconds());
		time.sleep(1);
		count.record(config, 1.0, time.milliseconds());
		time.sleep(1);
		count.record(config, 1.0, time.milliseconds());
		time.sleep(1);
		count.record(config, 1.0, time.milliseconds());
		assertEquals(4.0, count.measure(config, time.milliseconds()), EPS);
		time.sleep(1);
		// 第一个记录超时，被移除
		count.record(config, 1.0, time.milliseconds());
		assertEquals(3.0, count.measure(config, time.milliseconds()), EPS);
	}

	@Test
	public void testOldDataHasNoEffect_测试旧数据没有产生影响() {
		Max max = new Max();
		long windowMs = 100;
		int samples = 2;
		MetricConfig config = new MetricConfig().timeWindow(windowMs, TimeUnit.MILLISECONDS)
				.samples(samples);
		max.record(config, 50, time.milliseconds());
		assertEquals(50, max.measure(config, time.milliseconds()), EPS);
		time.sleep(samples * windowMs);
		assertEquals(Double.NEGATIVE_INFINITY, max.measure(config, time.milliseconds()), EPS);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testDuplicateMetricName_测试重复的指标命名() {
		metrics.sensor("test").add(new MetricName("test", "grp1"), new Avg());
		metrics.sensor("test2").add(new MetricName("test", "grp1"), new Total());
	}

	@Test
	public void testQuotas_测试越界() {
		Sensor sensor = metrics.sensor("test");
		sensor.add(new MetricName("test1.total", "grp1"), new Total(),
				new MetricConfig().quota(Quota.lessThan(5.0)));
		sensor.add(new MetricName("test2.total", "grp1"), new Total(),
				new MetricConfig().quota(Quota.moreThan(0.0)));
		sensor.record(5.0);
		try {
			// 虽然记录值总和越界了，但是test1.total的值累加到6.0了
			sensor.record(1.0);
			fail("Should have gotten a quota violation.");
		} catch (QuotaViolationException e) {
			// DO NOTHING
		}
		assertEquals(6.0, metrics.metrics().get(new MetricName("test1.total", "grp1")).value(), EPS);
		try {
			sensor.record(1.0);
			fail("Should have gotten a quota violation.");
		} catch (QuotaViolationException e) {
			// DO NOTHING
		}
		assertEquals(7.0, metrics.metrics().get(new MetricName("test1.total", "grp1")).value(), EPS);
		// 进行-7.0累加，总和为0.0
		sensor.record(-7.0);
		try {
			// 总和为-1.0,越界了
			sensor.record(-1.0);
			fail("Should have gotten a quota violation.");
		} catch (QuotaViolationException e) {
			// DO NOTHING
		}
	}

	@Test
	public void testPercentiles_测试组合信号() {
		int buckets = 100;
		Percentiles percs = new Percentiles(4 * buckets, 0.0, 100.0, //
				Percentiles.BucketSizing.CONSTANT, //
				new Percentile(new MetricName("test.p25", "grp1"), 25), //
				new Percentile(new MetricName("test.p50", "grp1"), 50), //
				new Percentile(new MetricName("test.p75", "grp1"), 75));
		MetricConfig config = new MetricConfig().eventWindow(50).samples(2);
		Sensor sensor = metrics.sensor("test", config);
		sensor.add(percs);
		Metric p25 = metrics.metrics().get(new MetricName("test.p25", "grp1"));
		Metric p50 = metrics.metrics().get(new MetricName("test.p50", "grp1"));
		Metric p75 = metrics.metrics().get(new MetricName("test.p75", "grp1"));

		// 记录两个窗口的连续值
		for (int i = 0; i < buckets; i++) {
			sensor.record(i);
		}

		assertEquals(25, p25.value(), 1.0);
		assertEquals(50, p50.value(), 1.0);
		assertEquals(75, p75.value(), 1.0);

		for (int i = 0; i < buckets; i++) {
			sensor.record(0.0);
		}

		assertEquals(0.0, p25.value(), 1.0);
		assertEquals(0.0, p50.value(), 1.0);
		assertEquals(0.0, p75.value(), 1.0);
	}

	@Test
	public void testGauge_测试最新的记录值() {
		ConstantMeasurable measurable = new ConstantMeasurable();

		metrics.addMetric(new MetricName("direct.measurable", "grp1", ""), measurable);
		Sensor sensor = metrics.sensor("test.sensor");
		sensor.add(new MetricName("test.gauge", "grp1"), new Gauge(10.0));
		//  当前最新值为10.0
		assertEquals("Sensor should reflect last recorded value", 10.0,
				metrics.metrics().get(new MetricName("test.gauge", "grp1")).value(), EPS);
		// 记录一次后，当前的最新值为11.0
		sensor.record(11.0);
		assertEquals("Sensor should reflect last recorded value", 11.0,
				metrics.metrics().get(new MetricName("test.gauge", "grp1")).value(), EPS);
	}

	/**
	 * 常量计算器
	 *
	 * @author wanggang
	 *
	 */
	public static class ConstantMeasurable implements Measurable {

		public double value = 10.0;

		@Override
		public double measure(MetricConfig config, long now) {
			return value;
		}

	}

}
