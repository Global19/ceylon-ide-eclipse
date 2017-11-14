/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.java2ceylon;

import org.eclipse.ltk.core.refactoring.Change;

import org.eclipse.ceylon.ide.common.platform.PlatformServices;
import org.eclipse.ceylon.ide.common.platform.TextChange;

public interface PlatformJ2C {
    PlatformServices platformServices();
    
    Change getNativeChange(Object cc);
    
    TextChange newChange(String desc, Object doc);
}