package io.spring.geode;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Future;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.testcontainers.containers.GenericContainer;

public class GeodeContainer extends GenericContainer {
	private static Logger logger = LoggerFactory.getLogger(GeodeContainer.class);

	public final static String DEFAULT_IMAGE = "apachegeode/geode:1.12.0";

	public GeodeContainer() {
		super(DEFAULT_IMAGE);
	}

	public GeodeContainer(@NonNull String dockerImageName) {
		super(dockerImageName);
	}
	public GeodeContainer(@NonNull Future<String> image) {
		super(image);
	}

	public ExecResult execGfsh(String... command) {
		return execInContainer(Gfsh.command(command).commandParts());
	}
	@Override
	public ExecResult execInContainer(String... command) {
		try {
			ExecResult execResult =  super.execInContainer(command);
			logger.debug("stdout: {}", execResult.getStdout());
			if (execResult.getExitCode() != 0 ) {
				logger.warn("stdout: {}", execResult.getStdout());
				logger.warn("stderr: {}", execResult.getStderr());
			}
			return execResult;
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public static class Gfsh {

		public static Command command(String... gfshCommands) {
			return new Command(gfshCommands);
		}

		static class Command {
			private final List<String> commandParts = new LinkedList<>();

			private Command(String... gfshCommands) {
				Assert.notEmpty(gfshCommands, "at least one command is required");
				for (String gfshCommand : gfshCommands) {
					Assert.hasText(gfshCommand, "command must contain text");
					if (commandParts.size() == 0) {
						commandParts.add("gfsh");
					}
					commandParts.add("-e");
					commandParts.add(gfshCommand);
				}
			}

			public String[] commandParts() {
				return commandParts.toArray(new String[commandParts.size()]);
			}

			public String toString() {
				return StringUtils.collectionToDelimitedString(commandParts, ",");
			}
		}
	}

}
