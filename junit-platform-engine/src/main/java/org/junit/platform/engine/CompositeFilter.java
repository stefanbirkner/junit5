/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.platform.engine;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static org.junit.platform.engine.FilterResult.included;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Predicate;

import org.junit.platform.commons.util.Preconditions;

/**
 * Combines a collection of {@link Filter Filters} into a new filter that will
 * include elements if and only if all of the filters in the specified collection
 * include it.
 *
 * @since 1.0
 */
class CompositeFilter<T> implements Filter<T> {

	@SuppressWarnings("rawtypes")
	private static final Filter ALWAYS_INCLUDED_FILTER = new Filter() {
		@Override
		public FilterResult apply(Object obj) {
			return ALWAYS_INCLUDED_RESULT;
		}

		@Override
		public Predicate toPredicate() {
			return obj -> true;
		}
	};

	private static final FilterResult ALWAYS_INCLUDED_RESULT = included("Always included");
	private static final FilterResult INCLUDED_BY_ALL_FILTERS = included("Element was included by all filters.");

	@SuppressWarnings("unchecked")
	static <T> Filter<T> alwaysIncluded() {
		return ALWAYS_INCLUDED_FILTER;
	}

	private final Collection<Filter<T>> filters;

	CompositeFilter(Collection<? extends Filter<T>> filters) {
		this.filters = new ArrayList<>(Preconditions.notEmpty(filters, "filters must not be empty"));
	}

	@Override
	public FilterResult apply(T element) {
		return filters.stream()
				.map(filter -> filter.apply(element))
				.filter(FilterResult::excluded)
				.findFirst()
				.orElse(INCLUDED_BY_ALL_FILTERS);
	}

	@Override
	public Predicate<T> toPredicate() {
		return filters.stream()
				.map(Filter::toPredicate)
				.reduce(Predicate::and)
				.get(); // it's safe to call get() here because the constructor ensures filters is not empty
	}

	@Override
	public String toString() {
		return filters.stream()
				.map(Object::toString)
				.map(value -> format("(%s)", value))
				.collect(joining(" and "));
	}

}
