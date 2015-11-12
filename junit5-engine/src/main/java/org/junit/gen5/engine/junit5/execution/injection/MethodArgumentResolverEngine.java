/*
 * Copyright 2015 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.gen5.engine.junit5.execution.injection;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

import org.junit.gen5.engine.junit5.descriptor.*;
import org.junit.gen5.engine.junit5.execution.TestExecutionContext;

// for a 'real' solution see: org.springframework.web.method.support.HandlerMethodArgumentResolver
public class MethodArgumentResolverEngine {

	//todo: when introducing the extension mechanism this instance will have to come from outside
	MethodArgumentResolverRegistry resolverRegistry = new PrimitiveMethodArgumentResolverRegistry();

	/**
	 * prepare a list of objects as arguments for the execution of this test method
	 *
	 * @param testExecutionContext the test execution context for the underlying (test) method
	 * @return a list of Objects to be used as arguments in the method call - will be an empty list in case of no-arg methods
	 * @throws ArgumentResolutionException
	 */
	public List<Object> prepareArguments(TestExecutionContext testExecutionContext) throws ArgumentResolutionException {
		return this.doPrepareArguments(testExecutionContext);
	}

	private List<Object> doPrepareArguments(TestExecutionContext testExecutionContext)
			throws ArgumentResolutionException {
		Method testMethod = testExecutionContext.getDescriptor().getTestMethod();

		List<Object> arguments = new ArrayList<>();

		if (testMethod.getParameterCount() > 0) {
			Parameter[] parameters = testMethod.getParameters();
			for (Parameter parameter : parameters) {
				Object newInstance = this.resolveArgumentForMethodParameter(parameter, testExecutionContext);
				arguments.add(newInstance);
			}
		}

		return arguments;
	}

	private Object resolveArgumentForMethodParameter(Parameter parameter, TestExecutionContext testExecutionContext)
			throws ArgumentResolutionException {

		try {

			List<MethodArgumentResolver> matchingResolvers = this.resolverRegistry.getMethodArgumentResolvers().stream().filter(
				argumentResolver -> argumentResolver.supports(parameter)).collect(Collectors.toList());
			if (matchingResolvers.size() > 1) {
				throw new ArgumentResolutionException("Too many resolvers found for parameter: " + parameter);
			}
			if (matchingResolvers.size() == 0) {
				throw new ArgumentResolutionException("No resolver found for parameter: " + parameter);
			}
			return matchingResolvers.get(0).resolveArgumentForMethodParameter(parameter, testExecutionContext);
		}
		catch (Exception cause) {
			throw new ArgumentResolutionException(cause);
		}
	}

}
