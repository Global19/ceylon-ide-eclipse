/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.core.debug.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.jface.preference.IPreferenceStore;

import org.eclipse.ceylon.ide.eclipse.ui.CeylonPlugin;

public class CeylonDebugPreferenceInitializer extends AbstractPreferenceInitializer {

    public CeylonDebugPreferenceInitializer() {}

    static String ACTIVE_FILTERS_LIST = "activeStepFilters";
    static String INACTIVE_FILTERS_LIST = "inactiveStepFilters";
    static String USE_STEP_FILTERS = "useStepFilters";
    static String DEBUG_AS_JAVACODE = "debugAsJavaCode";
    static String FILTER_DEFAULT_ARGUMENTS_CODE = "filterDefaultArgumentsCode";
    
    @Override
    public void initializeDefaultPreferences() {
        IPreferenceStore store = CeylonPlugin.getPreferences();
        store.setDefault(ACTIVE_FILTERS_LIST,
                "org.jboss.modules.*,ceylon.modules.*");
        store.setDefault(INACTIVE_FILTERS_LIST,
                "ceylon.language.*,org.eclipse.ceylon.*,java.lang.*");
        store.setDefault(USE_STEP_FILTERS, true);
        store.setDefault(FILTER_DEFAULT_ARGUMENTS_CODE, false);
        store.setDefault(DEBUG_AS_JAVACODE, false);
    }
}
