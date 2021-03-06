/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.test.eclipse.plugin.model;

public interface TestRunListener {

    void testRunAdded(TestRun testRun);

    void testRunRemoved(TestRun testRun);

    void testRunStarted(TestRun testRun);

    void testRunFinished(TestRun testRun);

    void testRunInterrupted(TestRun testRun);

    void testStarted(TestRun testRun, TestElement testElement);

    void testFinished(TestRun testRun, TestElement testElement);

}