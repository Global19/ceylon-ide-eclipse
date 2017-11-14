/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
import org.eclipse.ceylon.ide.common.model {
    CeylonProjects,
    CeylonProject
}

import org.eclipse.core.resources {
    IProject,
    IResource,
    IFolder,
    IFile
}

shared object ceylonModel extends CeylonProjects<IProject,IResource,IFolder,IFile>() {
    shared actual CeylonProject<IProject,IResource,IFolder,IFile> newNativeProject(IProject nativeProject) =>
            EclipseCeylonProject(nativeProject);
}