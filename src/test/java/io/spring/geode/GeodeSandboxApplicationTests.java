package io.spring.geode;

import java.util.HashMap;
import java.util.Map;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = {GeodeSandboxApplicationTests.GeodeInitializer.class})
class GeodeSandboxApplicationTests {
	static GeodeContainer geode;

	@Autowired
	Region<String, String> myRegion;

	@BeforeAll
	void setup() {
		geode.execGfsh("connect", "create region --name=myRegion --type=REPLICATE");
	}

	@Test
	void test() {
		assertThat(geode.isRunning()).isTrue();
		myRegion.put("hello", "world");
		assertThat(myRegion.get("hello")).isEqualTo("world");

	}

	static class GeodeInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
		@Override
		public void initialize(ConfigurableApplicationContext applicationContext) {

			geode = new GeodeContainer();
			geode.withCommand("tail", "-f", "/dev/null").start();

			geode.execGfsh("start locator --name=Locator1 --hostname-for-clients=localhost");
			geode.execGfsh("connect", "start server --name=Server1 --hostname-for-clients=localhost");

			final String servers = "localhost[" + geode.getMappedPort(40404) + "]";

			Map<String, Object> springGemfireProperties = new HashMap<>();
			springGemfireProperties.put("spring.data.gemfire.pool.servers", servers);
			springGemfireProperties.put("spring.data.gemfire.locator.port", geode.getMappedPort(10334));

			applicationContext.getEnvironment()
					.getPropertySources().addLast(new MapPropertySource("spring.gemfire.properties",
					springGemfireProperties));
		}
	}

	@SpringBootApplication
	@ClientCacheApplication
	static class App {
		@Bean
		Region<String, String> myRegion(GemFireCache gemfireCache) {
			ClientRegionFactory factory = ((ClientCache) gemfireCache).createClientRegionFactory(ClientRegionShortcut.PROXY);
			return factory.create("myRegion");
		}
	}
}
