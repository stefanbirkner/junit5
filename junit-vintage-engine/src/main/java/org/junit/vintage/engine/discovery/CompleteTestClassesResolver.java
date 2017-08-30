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

import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInClasspathRoot;
import static org.junit.platform.commons.util.ReflectionUtils.findAllClassesInPackage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.platform.commons.util.ClassFilter;
import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.ClasspathRootSelector;
import org.junit.platform.engine.discovery.PackageSelector;

class CompleteTestClassesResolver {

	private static final IsPotentialJUnit4TestClass isPotentialJUnit4TestClass = new IsPotentialJUnit4TestClass();

	Set<Class<?>> resolve(EngineDiscoveryRequest request, Predicate<String> namePredicate) {
		Set<Class<?>> testClasses = new LinkedHashSet<>();
		ClassFilter classFilter = ClassFilter.of(namePredicate, isPotentialJUnit4TestClass);
		resolveClasspathRootSelector(request, classFilter).forEach(testClasses::addAll);
		resolvePackageSelector(request, classFilter).forEach(testClasses::addAll);
		resolveClassSelector(request, classFilter).forEach(testClasses::add);
		return testClasses;
	}

	private Stream<List<Class<?>>> resolveClasspathRootSelector(EngineDiscoveryRequest request,
			ClassFilter classFilter) {
		// @formatter:off
		return request.getSelectorsByType(ClasspathRootSelector.class)
				.stream()
				.map(ClasspathRootSelector::getClasspathRoot)
				.map(root -> findAllClassesInClasspathRoot(root, classFilter));
		// @formatter:on
	}

	private Stream<List<Class<?>>> resolvePackageSelector(EngineDiscoveryRequest request,
			ClassFilter classFilter) {
		// @formatter:off
		return request.getSelectorsByType(PackageSelector.class)
				.stream()
				.map(PackageSelector::getPackageName)
				.map(packageName -> findAllClassesInPackage(packageName, classFilter));
		// @formatter:on
	}

	private Stream<? extends Class<?>> resolveClassSelector(EngineDiscoveryRequest request,
			ClassFilter classFilter) {
		// @formatter:off
		return request.getSelectorsByType(ClassSelector.class)
				.stream()
				.map(ClassSelector::getJavaClass)
				.filter(classFilter);
		// @formatter:on
	}
}
