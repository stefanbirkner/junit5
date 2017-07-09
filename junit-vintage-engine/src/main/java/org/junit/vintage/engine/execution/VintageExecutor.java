/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.junit.vintage.engine.execution;

import static org.junit.platform.commons.meta.API.Usage.Internal;
import static org.junit.platform.engine.TestExecutionResult.failed;
import static org.junit.platform.engine.TestExecutionResult.successful;

import java.util.logging.Logger;

import org.junit.platform.commons.meta.API;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.runner.JUnitCore;
import org.junit.vintage.engine.descriptor.RunnerTestDescriptor;

/**
 * @since 4.12
 */
@API(Internal)
public class VintageExecutor {

	private final Logger logger;

	public VintageExecutor(Logger logger) {
		this.logger = logger;
	}

	public void execute(ExecutionRequest request) {
		EngineExecutionListener engineExecutionListener = request.getEngineExecutionListener();
		TestDescriptor engineTestDescriptor = request.getRootTestDescriptor();
		engineExecutionListener.executionStarted(engineTestDescriptor);
		executeAllChildren(engineTestDescriptor, engineExecutionListener);
		engineExecutionListener.executionFinished(engineTestDescriptor, successful());
	}

	private void executeAllChildren(TestDescriptor engineTestDescriptor,
			EngineExecutionListener engineExecutionListener) {
		// @formatter:off
		engineTestDescriptor.getChildren()
				.stream()
				.map(RunnerTestDescriptor.class::cast)
				.forEach(descriptor -> execute(descriptor, engineExecutionListener));
		// @formatter:on
	}

	private void execute(RunnerTestDescriptor runnerTestDescriptor, EngineExecutionListener engineExecutionListener) {
		TestRun testRun = new TestRun(runnerTestDescriptor, logger);
		JUnitCore core = new JUnitCore();
		core.addListener(new RunListenerAdapter(testRun, engineExecutionListener));
		try {
			core.run(runnerTestDescriptor.toRequest());
		}
		catch (Throwable t) {
			reportUnexpectedFailure(testRun, runnerTestDescriptor, failed(t), engineExecutionListener);
		}
	}

	private void reportUnexpectedFailure(TestRun testRun, RunnerTestDescriptor runnerTestDescriptor,
			TestExecutionResult result, EngineExecutionListener engineExecutionListener) {
		if (testRun.isNotStarted(runnerTestDescriptor)) {
			engineExecutionListener.executionStarted(runnerTestDescriptor);
		}
		engineExecutionListener.executionFinished(runnerTestDescriptor, result);
	}

}
