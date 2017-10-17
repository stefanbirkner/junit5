/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.vintage.engine.discovery;

import static java.util.Arrays.asList;
import static org.apiguardian.api.API.Status.INTERNAL;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassNamePredicate;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

import org.apiguardian.api.API;
import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;

/**
 * @since 4.12
 */
@API(status = INTERNAL, since = "4.12")
public class VintageDiscoverer {

	private static final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();
	private final CompleteTestClassesResolver completeTestClassesResolver = new CompleteTestClassesResolver();
	private final TestClassRequestResolver resolver = new TestClassRequestResolver();

	private final List<DiscoverySelectorResolver> selectorResolvers = asList(
	// @formatter:off
			new MethodSelectorResolver(),
			new UniqueIdSelectorResolver()
	// @formatter:on
	);

	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId uniqueId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(uniqueId, "JUnit Vintage");
		// @formatter:off
		collectTestClasses(discoveryRequest)
				.toRequests()
				.map(request -> resolver.createRunnerTestDescriptor(request, uniqueId))
				.filter(Objects::nonNull)
				.forEach(engineDescriptor::addChild);
		// @formatter:on
		return engineDescriptor;
	}

	private TestClassCollector collectTestClasses(EngineDiscoveryRequest discoveryRequest) {
		Predicate<String> namePredicate = buildClassNamePredicate(discoveryRequest);
		ClassFilter classFilter = ClassFilter.of(namePredicate, isPotentialJUnit4TestClass);
		Set<Class<?>> completeTestClasses = completeTestClassesResolver.resolve(discoveryRequest, namePredicate);
		TestClassCollector collector = new TestClassCollector(completeTestClasses);
		for (DiscoverySelectorResolver selectorResolver : selectorResolvers) {
			selectorResolver.resolve(discoveryRequest, classFilter, collector);
		}
		return collector;
	}

}
