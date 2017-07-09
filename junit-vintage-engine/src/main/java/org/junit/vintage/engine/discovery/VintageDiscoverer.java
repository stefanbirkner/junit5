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

import static java.util.Arrays.asList;
import static org.junit.platform.commons.meta.API.Usage.Internal;
import static org.junit.platform.engine.Filter.adaptFilter;
import static org.junit.platform.engine.Filter.composeFilters;
import static org.junit.platform.engine.support.filter.ClasspathScanningSupport.buildClassNamePredicate;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Stream;

import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.Filter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.discovery.ClassNameFilter;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.filter.ExclusionReasonConsumingFilter;

/**
 * @since 4.12
 */
@API(Internal)
public class VintageDiscoverer {

	private static final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();
	private final Logger logger;
	private final TestClassRequestResolver testClassRequestResolver;

	public VintageDiscoverer(Logger logger) {
		this.logger = logger;
		testClassRequestResolver = new TestClassRequestResolver(logger);
	}

	public TestDescriptor discover(EngineDiscoveryRequest discoveryRequest, UniqueId engineId) {
		EngineDescriptor engineDescriptor = new EngineDescriptor(engineId, "JUnit Vintage");
		// @formatter:off
		createTestClassRequests(discoveryRequest)
				.map(testClassRequest -> testClassRequestResolver.createRunnerTestDescriptor(testClassRequest, engineId))
				.filter(Objects::nonNull)
				.forEach(engineDescriptor::addChild);
		// @formatter:on
		return engineDescriptor;
	}

	private Stream<TestClassRequest> createTestClassRequests(EngineDiscoveryRequest discoveryRequest) {
		TestClassCollector collector = collectTestClasses(discoveryRequest);
		List<ClassNameFilter> allClassNameFilters = discoveryRequest.getDiscoveryFiltersByType(ClassNameFilter.class);
		Filter<Class<?>> adaptedFilter = adaptFilter(composeFilters(allClassNameFilters), Class::getName);
		Filter<Class<?>> classFilter = new ExclusionReasonConsumingFilter<>(adaptedFilter,
			(testClass, reason) -> logger.fine(() -> String.format("Class %s was excluded by a class filter: %s",
				testClass.getName(), reason.orElse("<unknown reason>"))));
		return collector.toRequests(classFilter.toPredicate().and(loggingPotentialJUnit4TestClassPredicate()));
	}

	private TestClassCollector collectTestClasses(EngineDiscoveryRequest discoveryRequest) {
		TestClassCollector collector = new TestClassCollector();
		for (DiscoverySelectorResolver<?> selectorResolver : getAllDiscoverySelectorResolvers(discoveryRequest)) {
			resolveSelectorsOfSingleType(discoveryRequest, selectorResolver, collector);
		}
		return collector;
	}

	private List<DiscoverySelectorResolver<?>> getAllDiscoverySelectorResolvers(EngineDiscoveryRequest request) {
		Predicate<String> classNamePredicate = buildClassNamePredicate(request);
		return asList( //
			new ClasspathRootSelectorResolver(classNamePredicate), //
			new PackageNameSelectorResolver(classNamePredicate), //
			new ClassSelectorResolver(), //
			new MethodSelectorResolver(), //
			new UniqueIdSelectorResolver(logger)//
		);
	}

	private <T extends DiscoverySelector> void resolveSelectorsOfSingleType(EngineDiscoveryRequest discoveryRequest,
			DiscoverySelectorResolver<T> selectorResolver, TestClassCollector collector) {
		discoveryRequest.getSelectorsByType(selectorResolver.getSelectorClass()).forEach(
			selector -> selectorResolver.resolve(selector, collector));
	}

	private Predicate<Class<?>> loggingPotentialJUnit4TestClassPredicate() {
		return testClass -> {
			boolean isPotentialTestClass = isPotentialJUnit4TestClass.test(testClass);

			if (!isPotentialTestClass) {
				logger.info(() -> String.format("Class %s could not be resolved", testClass.getName()));
			}

			return isPotentialTestClass;
		};
	}
}
