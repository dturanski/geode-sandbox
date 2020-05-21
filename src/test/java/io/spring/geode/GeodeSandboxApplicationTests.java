package io.spring.geode;

import java.io.IOException;
import java.time.Duration;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientRegionFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
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
			Container.ExecResult result = geode.execInContainer("gfsh", "start", "locator","--name=Locator1");
			System.out.println(result.getStdout());
			System.out.println(result.getStderr());

			result = geode.execInContainer("gfsh", "start", "server");
			System.out.println(result.getStdout());
			System.out.println(result.getStderr());

			geode.waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(60)));
			result = geode.execInContainer("gfsh", "status", "locator","--name=Locator1");
			System.out.println(result.getStdout());
			System.out.println(result.getStderr());

		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
//		geode.addExposedPorts(10334, 40404);
		final String locators = "localhost[" + geode.getMappedPort(10334)+"]";
		System.out.println( "http server:" + geode.getMappedPort(7070));
		System.setProperty("spring.data.gemfire.pool.locators",locators);
		System.out.println( geode.getLogs());
		System.out.println(geode.isRunning());
	}

	@Autowired
	Region<String, String> myRegion;

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
				ClientRegionFactory factory = ((ClientCache) gemfireCache).createClientRegionFactory(ClientRegionShortcut.CACHING_PROXY);
				return factory.create("myRegion");
		}
	}

}
