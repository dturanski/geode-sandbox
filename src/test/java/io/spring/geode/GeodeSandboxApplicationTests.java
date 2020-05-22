package io.spring.geode;

import java.io.IOException;
import java.time.Duration;
import java.util.stream.Stream;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GeodeSandboxApplicationTests {
	static final GenericContainer geode;

	static {
		geode = new GenericContainer<>("apachegeode/geode:1.12.0");
		geode.withCommand("tail","-f","/dev/null").start();
		try {
			Container.ExecResult result = geode.execInContainer("gfsh", "-e","start locator --name=Locator1 --hostname-for-clients=localhost");
			System.out.println(result.getStdout());
			System.out.println(result.getStderr());

			result = geode.execInContainer("gfsh", "-e", "connect", "-e", "start server --name=Server1 --hostname-for-clients=localhost");
			System.out.println(result.getStdout());
			System.out.println(result.getStderr());

			geode.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));
			result = geode.execInContainer("gfsh","-e","connect","-e","status locator --name Locator1");
			System.out.println(result.getStdout());
			System.out.println(result.getStderr());



		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		geode.addExposedPorts(10334, 40404);
		final String locators = "localhost[" + geode.getMappedPort(10334)+"]";
		final String servers = "localhost[" + geode.getMappedPort(40404)+"]";
		System.out.println( "http server:" + geode.getMappedPort(7070));
		//System.setProperty("spring.data.gemfire.pool.locators",locators);
		System.setProperty("spring.data.gemfire.pool.servers",servers);
		System.setProperty("spring.data.gemfire.locator.host","localhost");
		System.setProperty("spring.data.gemfire.locator.port",String.valueOf(geode.getMappedPort(10334)));
		System.out.println( geode.getLogs());
		System.out.println(geode.isRunning());
	}

	@Autowired
	Region<String, String> myRegion;

	@BeforeAll
	static void setup() throws IOException, InterruptedException {
		Container.ExecResult result = geode.execInContainer("gfsh", "-e", "connect","-e", "create region --name=myRegion --type=REPLICATE");
		System.out.println(result.getStdout());
		System.out.println(result.getStderr());
	}

	@Test
	void test() {

		System.out.println(geode.isRunning());

		myRegion.put("hello","world");
		assertThat(myRegion.get("hello")).isEqualTo("world");

	}

	@SpringBootApplication
	@ClientCacheApplication
	static class App {
		@Bean
		Region<String,String> myRegion(GemFireCache gemfireCache) {
				ClientCache clientCache = (ClientCache) gemfireCache;
				ClientRegionFactory factory = ((ClientCache) gemfireCache).createClientRegionFactory(ClientRegionShortcut.PROXY);
				return factory.create("myRegion");
		}

		@Bean
		ApplicationRunner runAdditionalClientCacheInitialization(ConfigurableEnvironment environment) {

			return args -> {
				environment.getPropertySources().iterator().forEachRemaining(propertySource -> {
					if (propertySource instanceof  EnumerablePropertySource) {
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
