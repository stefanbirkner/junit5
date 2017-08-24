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

import java.util.function.Predicate;
import java.util.stream.Stream;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.discovery.ClassSelector;

/**
 * @since 4.12
 */
class ClassSelectorResolver {

	public Stream<Class<?>> resolve(EngineDiscoveryRequest request, Predicate<Class<?>> classFilter) {
		// @formatter:off
		return request.getSelectorsByType(ClassSelector.class)
			.stream()
			.map(ClassSelector::getJavaClass)
			.filter(classFilter);
		// @formatter:on
	}

}
