/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.core.debug.actions;

public class CeylonOpenReceivingTypeHierarchyAction extends
        CeylonOpenReceivingTypeAction {
    
    /* (non-Javadoc)
     * @see org.eclipse.jdt.internal.debug.ui.actions.OpenTypeAction#isHierarchy()
     */
    @Override
    protected boolean isHierarchy() {
        return true;
    }
}
