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

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.junit.platform.commons.meta.API;
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
	private final TestClassRequestResolver resolver;

	public VintageDiscoverer(Logger logger) {
		this.logger = logger;
		this.resolver = new TestClassRequestResolver(logger);
	}

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
		Predicate<Class<?>> classFilter = createTestClassPredicate(discoveryRequest);
		Set<Class<?>> unrestrictedTestClasses = collectUnrestrictedTestClasses(discoveryRequest, classFilter);
		TestClassCollector collector = new TestClassCollector(unrestrictedTestClasses);
		for (DiscoverySelectorResolver selectorResolver : getAllDiscoverySelectorResolvers()) {
			selectorResolver.resolve(discoveryRequest, classFilter, collector);
		}
		return collector;
	}

	private List<DiscoverySelectorResolver> getAllDiscoverySelectorResolvers() {
		return asList( //
			new MethodSelectorResolver(), //
			new UniqueIdSelectorResolver(logger)//
		);
	}

	private Predicate<Class<?>> createTestClassPredicate(EngineDiscoveryRequest discoveryRequest) {
		List<ClassNameFilter> allClassNameFilters = discoveryRequest.getFiltersByType(ClassNameFilter.class);
		Filter<Class<?>> adaptedFilter = adaptFilter(composeFilters(allClassNameFilters), Class::getName);
		Filter<Class<?>> classFilter = new ExclusionReasonConsumingFilter<>(adaptedFilter,
			(testClass, reason) -> logger.fine(() -> String.format("Class %s was excluded by a class filter: %s",
				testClass.getName(), reason.orElse("<unknown reason>"))));
		return classFilter.toPredicate().and(isPotentialJUnit4TestClass);
	}

	private Set<Class<?>> collectUnrestrictedTestClasses(EngineDiscoveryRequest discoveryRequest,
			Predicate<Class<?>> classFilter) {
		Predicate<String> classNamePredicate = buildClassNamePredicate(discoveryRequest);
		Set<Class<?>> unrestrictedTestClasses = new HashSet<>();
		new ClasspathRootSelectorResolver(classNamePredicate).resolve(discoveryRequest, classFilter).forEach(
			unrestrictedTestClasses::add);
		new PackageNameSelectorResolver(classNamePredicate).resolve(discoveryRequest, classFilter).forEach(
			unrestrictedTestClasses::add);
		new ClassSelectorResolver().resolve(discoveryRequest, classFilter).forEach(unrestrictedTestClasses::add);
		return unrestrictedTestClasses;
	}
}
