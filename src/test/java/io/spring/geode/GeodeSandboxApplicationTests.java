package io.spring.geode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ContextConfiguration(initializers = {GeodeSandboxApplicationTests.GeodeInitializer.class})
class GeodeSandboxApplicationTests {
	static GenericContainer geode;

	@Autowired
	Region<String, String> myRegion;

	@BeforeAll
	void setup() throws IOException, InterruptedException {
		Container.ExecResult result = geode.execInContainer("gfsh", "-e", "connect", "-e", "create region --name=myRegion --type=REPLICATE");
		System.out.println(result.getStdout());
		System.out.println(result.getStderr());
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
			geode = new GenericContainer<>(new ImageFromDockerfile()
					.withFileFromClasspath("init.sh", "docker/init.sh")
					.withFileFromClasspath("Dockerfile", "docker/Dockerfile"));
			geode.start();

			try {
				geode.execInContainer("/usr/local/bin/init.sh");
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			final String locators = "localhost[" + geode.getMappedPort(10334) + "]";
			final String servers = "localhost[" + geode.getMappedPort(40404) + "]";


			Map<String, Object> springGemfireProperties = new HashMap<>();
			springGemfireProperties.put("spring.data.gemfire.pool.servers", servers);
			//springGemfireProperties.put("spring.data.gemfire.locator.host", "localhost");
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

		@Bean
		ApplicationRunner runAdditionalClientCacheInitialization(ConfigurableEnvironment environment) {

			return args -> {
				environment.getPropertySources().iterator().forEachRemaining(propertySource -> {
					if (propertySource instanceof EnumerablePropertySource) {
						EnumerablePropertySource enumerablePropertySource = (EnumerablePropertySource) propertySource;
						Stream.of(enumerablePropertySource.getPropertyNames()).forEach(name -> {
							System.out.println(name + ":" + propertySource.getProperty(name));
						});
					}
				});

			};
		}
	}

}
