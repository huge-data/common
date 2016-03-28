package io.confluent.common.config;

import static io.confluent.common.config.ConfigDef.Type.INT;
import static io.confluent.common.config.ConfigDef.Type.STRING;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.Test;

public class AbstractConfigTest {

	@Test
	public void testGetPropsFromFile() {
		// 各参数定义
		ConfigDef def = new ConfigDef() //
				.define("a", INT, 5, ConfigDef.Range.between(0, 14), ConfigDef.Importance.HIGH,
						"docs") //
				.define("b", STRING, "foo", ConfigDef.Importance.HIGH, "docs") //
				.define("c", STRING, "foo", ConfigDef.Importance.HIGH, "docs");
		// 读取配置文件
		Properties props = AbstractConfig.getPropsFromFile("test-conf/test.properties");
		AbstractConfig config = new AbstractConfig(def, props);
		assertEquals(12, config.getInt("a"));
		assertEquals("foobar", config.getString("b"));
		assertEquals("foofoobarbar", config.getString("c"));
	}

}
