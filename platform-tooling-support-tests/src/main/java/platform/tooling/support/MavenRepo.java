/*
 * Copyright 2015-2021 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * https://www.eclipse.org/legal/epl-v20.html
 */

package platform.tooling.support;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class MavenRepo {

	private MavenRepo() {
	}

	public static Path dir() {
		return Path.of(System.getProperty("maven.repo"));
	}

	public static Path jar(String artifactId) {
		return artifact(artifactId, fileName -> fileName.endsWith(".jar") //
				&& !fileName.endsWith("-sources.jar") //
				&& !fileName.endsWith("-javadoc.jar"));
	}

	public static Path gradleModuleMetadata(String artifactId) {
		return artifact(artifactId, fileName -> fileName.endsWith(".module"));
	}

	public static Path pom(String artifactId) {
		return artifact(artifactId, fileName -> fileName.endsWith(".pom"));
	}

	private static Path artifact(String artifactId, Predicate<String> fileNamePredicate) {
		var parentDir = dir() //
				.resolve(Helper.groupId(artifactId).replace('.', File.separatorChar)) //
				.resolve(artifactId) //
				.resolve(Helper.version(artifactId));
		try (var files = Files.list(parentDir)) {
			return files.filter(
				file -> fileNamePredicate.test(file.getFileName().toString())).findFirst().orElseThrow();
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

}
