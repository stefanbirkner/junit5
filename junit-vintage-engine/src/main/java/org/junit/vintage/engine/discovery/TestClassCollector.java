/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.vintage.engine.discovery;

import static java.util.stream.Stream.concat;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * @since 4.12
 */
class TestClassCollector {

	private final Set<Class<?>> completeTestClasses;
	private final Map<Class<?>, List<RunnerTestDescriptorAwareFilter>> filteredTestClasses = new LinkedHashMap<>();

	TestClassCollector(Set<Class<?>> completeTestClasses) {
		this.completeTestClasses = completeTestClasses;
	}

	void addFiltered(Class<?> testClass, RunnerTestDescriptorAwareFilter filter) {
		filteredTestClasses.computeIfAbsent(testClass, key -> new LinkedList<>()).add(filter);
	}

	Stream<TestClassRequest> toRequests() {
		return concat(completeRequests(), filteredRequests());
	}

	private Stream<TestClassRequest> completeRequests() {
		return completeTestClasses.stream().map(TestClassRequest::new);
	}

	private Stream<TestClassRequest> filteredRequests() {
		// @formatter:off
		return filteredTestClasses.entrySet()
				.stream()
				.map(entry -> new TestClassRequest(entry.getKey(), entry.getValue()));
		// @formatter:on
	}

}
