/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.core.debug.presentation;

import java.util.regex.Matcher;

import org.eclipse.ceylon.model.typechecker.model.Declaration;

public interface CeylonDebugLabelUpdater {
    Matcher matches(String existingLabel);
    String updateLabel(Matcher matcher, Declaration declaration);
}