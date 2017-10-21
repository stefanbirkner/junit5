/*
 * Copyright 2015-2017 the original author or authors.
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v2.0 which
 * accompanies this distribution and is available at
 *
 * http://www.eclipse.org/legal/epl-v20.html
 */

package org.junit.api.tools;

import static java.lang.String.format;

import java.io.PrintWriter;
import java.util.EnumSet;
import java.util.List;

import org.apiguardian.api.API.Status;

/**
 * @since 1.0
 */
abstract class AbstractApiReportWriter implements ApiReportWriter {

	private final ApiReport apiReport;

	AbstractApiReportWriter(ApiReport apiReport) {
		this.apiReport = apiReport;
	}

	@Override
	public void printReportHeader(PrintWriter out) {
		out.println(h1("@API Declarations"));
		out.println();
		out.println(paragraph(
			format("Discovered %d types with %s declarations.", this.apiReport.getTypes().size(), code("@API"))));
		out.println();
	}

	@Override
	public void printDeclarationInfo(PrintWriter out, EnumSet<Status> statuses) {
		this.apiReport.getDeclarationsMap().entrySet().stream()
				.filter(e -> statuses.contains(e.getKey()))
				.forEach(e -> printDeclarationSection(e.getKey(), e.getValue(), out));
	}

	protected void printDeclarationSection(Status status, List<Class<?>> types, PrintWriter out) {
		printDeclarationSectionHeader(status, types, out);
		if (types.size() > 0) {
			printDeclarationTableHeader(out);
			types.forEach(type -> printDeclarationTableRow(type, out));
			printDeclarationTableFooter(out);
			out.println();
		}
	}

	protected void printDeclarationSectionHeader(Status status, List<Class<?>> types, PrintWriter out) {
		out.println(h2(format("@API(%s)", status)));
		out.println();
		out.println(paragraph(format("Discovered %d " + code("@API(%s)") + " declarations.", types.size(), status)));
		out.println();
	}

	protected abstract String h1(String header);

	protected abstract String h2(String header);

	protected abstract String code(String element);

	protected String paragraph(String element) {
		return element;
	}

	protected abstract void printDeclarationTableHeader(PrintWriter out);

	protected abstract void printDeclarationTableRow(Class<?> type, PrintWriter out);

	protected abstract void printDeclarationTableFooter(PrintWriter out);

	protected String getKind(Class<?> type) {
		if (type.isAnnotation()) {
			return "annotation";
		}
		if (type.isEnum()) {
			return "enum";
		}
		if (type.isInterface()) {
			return "interface";
		}
		return "class";
	}

}
