package io.confluent.common.config;

import static io.confluent.common.config.ConfigDef.Type.BOOLEAN;
import static io.confluent.common.config.ConfigDef.Type.CLASS;
import static io.confluent.common.config.ConfigDef.Type.DOUBLE;
import static io.confluent.common.config.ConfigDef.Type.INT;
import static io.confluent.common.config.ConfigDef.Type.LIST;
import static io.confluent.common.config.ConfigDef.Type.LONG;
import static io.confluent.common.config.ConfigDef.Type.STRING;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import io.confluent.common.config.ConfigDef.Type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;

public class ConfigDefTest {

	@Test
	public void testBasicTypes() {
		ConfigDef def = new ConfigDef()
				.define("a", INT, 5, ConfigDef.Range.between(0, 14), ConfigDef.Importance.HIGH,
						"docs").define("b", LONG, ConfigDef.Importance.HIGH, "docs")
				.define("c", STRING, "hello", ConfigDef.Importance.HIGH, "docs")
				.define("d", LIST, ConfigDef.Importance.HIGH, "docs")
				.define("e", DOUBLE, ConfigDef.Importance.HIGH, "docs")
				.define("f", CLASS, ConfigDef.Importance.HIGH, "docs")
				.define("g", BOOLEAN, ConfigDef.Importance.HIGH, "docs")
				.define("h", Type.BOOLEAN, ConfigDef.Importance.HIGH, "docs")
				.define("i", Type.BOOLEAN, ConfigDef.Importance.HIGH, "docs");

		Properties props = new Properties();
		props.put("a", "1   ");
		props.put("b", 2);
		props.put("d", " a , b, c");
		props.put("e", 42.5d);
		props.put("f", String.class.getName());
		props.put("g", "true");
		props.put("h", "FalSE");
		props.put("i", "TRUE");

		Map<String, Object> vals = def.parse(props);
		assertEquals(1, vals.get("a"));
		assertEquals(2L, vals.get("b"));
		assertEquals("hello", vals.get("c"));
		assertEquals(asList("a", "b", "c"), vals.get("d"));
		assertEquals(42.5d, vals.get("e"));
		assertEquals(String.class, vals.get("f"));
		assertEquals(true, vals.get("g"));
		assertEquals(false, vals.get("h"));
		assertEquals(true, vals.get("i"));
	}

	@Test
	public void testOverride() {
		ConfigDef def = new ConfigDef()
				.define("a", INT, 0, ConfigDef.Range.between(0, 0), ConfigDef.Importance.HIGH,
						"doc")
				.define("b", STRING, "original", ConfigDef.Importance.HIGH, "doc")
				.define("c", STRING, ConfigDef.ValidString.in(Arrays.asList("", "foo")),
						ConfigDef.Importance.HIGH, "doc")
				.define("d", STRING, ConfigDef.Importance.HIGH, "doc")
				.defineOverride("a", INT, 4, ConfigDef.Range.between(0, 14),
						ConfigDef.Importance.HIGH, "doc")
				.defineOverride("b", STRING, "override", ConfigDef.Importance.HIGH, "doc")
				.defineOverride("c", STRING, ConfigDef.ValidString.in(Arrays.asList("", "bar")),
						ConfigDef.Importance.HIGH, "doc")
				.defineOverride("d", STRING, ConfigDef.Importance.HIGH, "doc");

		Properties props = new Properties();
		props.put("c", "bar");
		props.put("d", "foo");

		Map<String, Object> vals = def.parse(props);
		assertEquals(4, vals.get("a"));
		assertEquals("override", vals.get("b"));
		assertEquals("bar", vals.get("c"));
		assertEquals("foo", vals.get("d"));
	}

	@Test(expected = ConfigException.class)
	public void testInvalidDefault() {
		new ConfigDef().define("a", Type.INT, "hello", ConfigDef.Importance.HIGH, "docs");
	}

	@Test(expected = ConfigException.class)
	public void testNullDefault() {
		new ConfigDef().define("a", Type.INT, null, null, null, "docs");
	}

	@Test(expected = ConfigException.class)
	public void testMissingRequired() {
		new ConfigDef().define("a", Type.INT, ConfigDef.Importance.HIGH, "docs").parse(
				new HashMap<String, Object>());
	}

	@Test(expected = ConfigException.class)
	public void testDefinedTwice() {
		new ConfigDef().define("a", Type.STRING, ConfigDef.Importance.HIGH, "docs").define("a",
				Type.INT, ConfigDef.Importance.HIGH, "docs");
	}

	@Test(expected = ConfigException.class)
	public void testOverrideNotPreviouslyDefined() {
		new ConfigDef().defineOverride("a", Type.STRING, ConfigDef.Importance.HIGH, "docs");
	}

	@Test
	public void testBadInputs() {
		testBadInputs(Type.INT, "hello", null, "42.5", 42.5, Long.MAX_VALUE,
				Long.toString(Long.MAX_VALUE), new Object());
		testBadInputs(Type.LONG, "hello", null, "42.5", Long.toString(Long.MAX_VALUE) + "00",
				new Object());
		testBadInputs(Type.DOUBLE, "hello", null, new Object());
		testBadInputs(Type.STRING, new Object());
		testBadInputs(Type.LIST, 53, new Object());
		testBadInputs(Type.BOOLEAN, "hello", "truee", "fals");
	}

	private void testBadInputs(Type type, Object... values) {
		for (Object value : values) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", value);
			ConfigDef def = new ConfigDef().define("name", type, ConfigDef.Importance.HIGH, "docs");
			try {
				def.parse(m);
				fail("Expected a config exception on bad input for value " + value);
			} catch (ConfigException e) {
				// this is good
			}
		}
	}

	@Test(expected = ConfigException.class)
	public void testInvalidDefaultRange() {
		@SuppressWarnings("unused")
		ConfigDef def = new ConfigDef().define("name", Type.INT, -1,
				ConfigDef.Range.between(0, 10), ConfigDef.Importance.HIGH, "docs");
	}

	@Test(expected = ConfigException.class)
	public void testInvalidDefaultString() {
		@SuppressWarnings("unused")
		ConfigDef def = new ConfigDef().define("name", Type.STRING, "bad",
				ConfigDef.ValidString.in(Arrays.asList("valid", "values")),
				ConfigDef.Importance.HIGH, "docs");
	}

	@Test
	public void testValidators() {
		testValidators(Type.INT, ConfigDef.Range.between(0, 10), 5, new Object[] { 1, 5, 9 },
				new Object[] { -1, 11 });
		testValidators(Type.STRING,
				ConfigDef.ValidString.in(Arrays.asList("good", "values", "default")), "default",
				new Object[] { "good", "values", "default" }, new Object[] { "bad", "inputs" });
	}

	private void testValidators(Type type, ConfigDef.Validator validator, Object defaultVal,
			Object[] okValues, Object[] badValues) {
		ConfigDef def = new ConfigDef().define("name", type, defaultVal, validator,
				ConfigDef.Importance.HIGH, "docs");

		for (Object value : okValues) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", value);
			def.parse(m);
		}

		for (Object value : badValues) {
			Map<String, Object> m = new HashMap<String, Object>();
			m.put("name", value);
			try {
				def.parse(m);
				fail("Expected a config exception due to invalid value " + value);
			} catch (ConfigException e) {
				// this is good
			}
		}
	}
}
