/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.jupiter.engine.descriptor;

import static org.junit.jupiter.engine.descriptor.TestFactoryTestDescriptor.createDynamicDescriptor;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.engine.execution.JupiterEngineExecutionContext;
import org.junit.platform.commons.util.Preconditions;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;

/**
 * {@link TestDescriptor} for a {@link DynamicContainer}.
 *
 * @since 5.0
 */
class DynamicContainerTestDescriptor extends JupiterTestDescriptor {

	private final DynamicContainer dynamicContainer;
	private final TestSource testSource;

	DynamicContainerTestDescriptor(UniqueId uniqueId, DynamicContainer dynamicContainer, TestSource testSource) {
		super(uniqueId, dynamicContainer.getDisplayName(), testSource);
		this.dynamicContainer = dynamicContainer;
		this.testSource = testSource;
	}

	@Override
	public Type getType() {
		return Type.CONTAINER;
	}

	@Override
	public SkipResult shouldBeSkipped(JupiterEngineExecutionContext context) throws Exception {
		return SkipResult.doNotSkip();
	}

	@Override
	public JupiterEngineExecutionContext execute(JupiterEngineExecutionContext context,
			DynamicTestExecutor dynamicTestExecutor) throws Exception {
		AtomicInteger index = new AtomicInteger(1);
		try (Stream<? extends DynamicNode> children = dynamicContainer.getChildren()) {
			children.peek(child -> Preconditions.notNull(child, "individual dynamic node must not be null"))
					.map(child -> toDynamicDescriptor(index.getAndIncrement(), child))
					.forEachOrdered(dynamicTestExecutor::execute);
		}
		return context;
	}

	private JupiterTestDescriptor toDynamicDescriptor(int index, DynamicNode childNode) {
		return createDynamicDescriptor(this, childNode, index, testSource);
	}
}
