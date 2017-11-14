/********************************************************************************
 * Copyright (c) {date} Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.preferences;

import static org.eclipse.ceylon.ide.eclipse.code.imports.ModuleImportUtil.addModuleImports;
import static org.eclipse.ceylon.ide.eclipse.code.imports.ModuleImportUtil.makeModuleImportShared;
import static org.eclipse.ceylon.ide.eclipse.code.imports.ModuleImportUtil.removeModuleImports;
import static org.eclipse.ceylon.ide.eclipse.code.imports.ModuleImportUtil.removeSharedAnnotation;
import static org.eclipse.ceylon.ide.eclipse.code.preferences.ModuleImportSelectionDialog.selectModules;
import static org.eclipse.ceylon.ide.eclipse.core.builder.CeylonBuilder.getProjectModules;
import static org.eclipse.ceylon.ide.eclipse.core.builder.CeylonBuilder.getProjectTypeChecker;
import static org.eclipse.ceylon.ide.eclipse.ui.CeylonPlugin.PLUGIN_ID;
import static org.eclipse.ceylon.ide.eclipse.util.EditorUtil.getDocument;
import static org.eclipse.ceylon.ide.eclipse.util.EditorUtil.performChange;
import static org.eclipse.ceylon.ide.eclipse.util.ModuleQueries.getModuleSearchResults;
import static org.eclipse.swt.layout.GridData.FILL_BOTH;
import static org.eclipse.swt.layout.GridData.FILL_HORIZONTAL;
import static org.eclipse.swt.layout.GridData.HORIZONTAL_ALIGN_FILL;
import static org.eclipse.swt.layout.GridData.VERTICAL_ALIGN_BEGINNING;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.wizards.IWizardDescriptor;

import org.eclipse.ceylon.cmr.api.ModuleSearchResult;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.ide.eclipse.code.editor.Navigation;
import org.eclipse.ceylon.ide.eclipse.code.navigator.SourceModuleNode;
import org.eclipse.ceylon.ide.eclipse.code.wizard.NewPackageWizard;
import org.eclipse.ceylon.ide.eclipse.ui.CeylonResources;
import org.eclipse.ceylon.ide.common.model.ProjectSourceFile;
import org.eclipse.ceylon.ide.common.modulesearch.ModuleVersionNode;
import org.eclipse.ceylon.ide.common.typechecker.ProjectPhasedUnit;
import org.eclipse.ceylon.model.typechecker.model.Module;
import org.eclipse.ceylon.model.typechecker.model.ModuleImport;
import org.eclipse.ceylon.model.typechecker.model.Modules;
import org.eclipse.ceylon.model.typechecker.model.Package;
import org.eclipse.ceylon.model.typechecker.model.Unit;

public class CeylonModulePropertiesPage extends PropertyPage 
        implements IWorkbenchPropertyPage {

//    private IResourceChangeListener encodingListener;
    private Table moduleImportsTable;
    private IProject project;
    private IPackageFragment packageFragment;
    
//    @Override
//    public void dispose() {
//        if (encodingListener!=null) {
//            getWorkspace().removeResourceChangeListener(encodingListener);
//            encodingListener = null;
//        }
//        super.dispose();
//    }
  
    private NewPackageWizard openPackageWizard() {
        IWizardDescriptor descriptor = 
                PlatformUI.getWorkbench()
                    .getNewWizardRegistry()
                    .findWizard(PLUGIN_ID 
                            + ".newPackageWizard");
        if (descriptor!=null) {
            try {
                NewPackageWizard wizard = 
                        (NewPackageWizard) 
                            descriptor.createWizard();
                wizard.init(PlatformUI.getWorkbench(), 
                        new StructuredSelection(getElement()));
                Shell shell = 
                        Display.getCurrent()
                            .getActiveShell();
                WizardDialog wd = 
                        new WizardDialog(shell, wizard);
                wd.setTitle(wizard.getWindowTitle());
                wd.open();
                return wizard;
            }
            catch (CoreException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
        
    @Override
    protected Control createContents(Composite parent) {
        initProjectAndModule();
        if (packageFragment==null) return parent;
        createModuleInfoBlock(parent);
        createPackagesBlock(parent);
        createModulesBlock(parent);
        createModuleDescriptorLink(parent);
        return parent;
    }

    public void createModuleDescriptorLink(Composite parent) {
        new Label(parent, SWT.SEPARATOR|SWT.HORIZONTAL)
                .setLayoutData(new GridData(FILL_HORIZONTAL));
        
        IFolder folder = 
                (IFolder) 
                    packageFragment.getResource();
        final IFile moduleDescriptor = 
                folder.getFile("module.ceylon");
        Link openDescriptorLink = new Link(parent, 0);
        GridData gd = 
                GridDataFactory.swtDefaults()
                    .align(SWT.FILL, SWT.CENTER)
                    .indent(0, 6)
                    .create();
        openDescriptorLink.setLayoutData(gd);
        openDescriptorLink.setText("<a>Edit module descriptor...</a>");
        openDescriptorLink.addSelectionListener(
                new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getShell().close();
                Navigation.gotoLocation(moduleDescriptor, 0);
            }
        });
    }

    private void initProjectAndModule() {
        IAdaptable element = getElement();
        IFile file = (IFile) element.getAdapter(IFile.class);
        if (file==null) {
            SourceModuleNode node = 
                    (SourceModuleNode) 
                        element.getAdapter(
                                SourceModuleNode.class);
            if (node==null) {
                packageFragment = 
                        (IPackageFragment) 
                            element.getAdapter(
                                    IPackageFragment.class);
            }
            else {
                for (IPackageFragment pf: 
                        node.getPackageFragments()) {
                    String mname = 
                            node.getModule()
                                .getNameAsString();
                    if (pf.getElementName().equals(mname)) {
                        packageFragment = pf;
                        break;
                    }
                }
            }
        }
        else {
            IJavaElement javaElement = 
                    JavaCore.create(file.getParent());
            if (javaElement instanceof IPackageFragment) {
                packageFragment = 
                        (IPackageFragment) javaElement;
            }
        }
        if (packageFragment!=null) {
            project = 
                    packageFragment.getJavaProject()
                        .getProject();
        }
    }

    public Module getModule() {
        if (project==null) return null;
        Modules projectModules = getProjectModules(project);
        if (projectModules==null) return null;
        for (Module m: projectModules.getListOfModules()) {
            String pname = packageFragment.getElementName();
            if (m.getNameAsString().equals(pname)) {
                return m; 
            }
        }
        return null;
    }
    
    private void createModulesBlock(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText("Imported modules:");
        
        Composite composite = 
                new Composite(parent, SWT.NONE);
        GridData cgd = new GridData(FILL_BOTH);
        cgd.grabExcessHorizontalSpace = true;
        cgd.grabExcessVerticalSpace = true;
        composite.setLayoutData(cgd);
        GridLayout layout = new GridLayout(3, true);
        layout.marginWidth=0;
        composite.setLayout(layout);
        
        moduleImportsTable = new Table(composite, 
                SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
        moduleImportsTable.setHeaderVisible(true);
        moduleImportsTable.setLinesVisible(true);
        GridData gd = new GridData(FILL_BOTH);
        gd.horizontalSpan=2;
        gd.verticalSpan=4;
        gd.grabExcessHorizontalSpace = true;
//        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 100;
        gd.widthHint = 300;
        moduleImportsTable.setLayoutData(gd);
        
        TableColumn column = 
                new TableColumn(moduleImportsTable, 
                        SWT.NONE, 0);
        column.setText("Module/Version");
        column.setWidth(180);
        TableColumn versionColumn = 
                new TableColumn(moduleImportsTable, 
                        SWT.NONE, 1);
        versionColumn.setText("Version");
        versionColumn.setWidth(70);
        TableColumn sharedColumn = 
                new TableColumn(moduleImportsTable, 
                        SWT.NONE, 2);
        sharedColumn.setText("Transitivity");
        sharedColumn.setWidth(70);
        for (ModuleImport mi: getModule().getImports()) {
            TableItem item = 
                    new TableItem(moduleImportsTable, 
                            SWT.NONE);
            item.setImage(CeylonResources.MODULE);
            item.setText(mi.getModule().getNameAsString());
            item.setText(1, mi.getModule().getVersion());
            item.setText(2, mi.isExport() ? "shared" : "");
        }
        
        GridData bgd = 
                new GridData(
                        VERTICAL_ALIGN_BEGINNING |
                        HORIZONTAL_ALIGN_FILL);
        bgd.grabExcessHorizontalSpace=false;
        bgd.widthHint = 50;
        
        Button sharedButton = 
                new Button(composite, SWT.PUSH);
        sharedButton.setText("Toggle Shared");
        sharedButton.setLayoutData(bgd);
        sharedButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                makeShared(moduleImportsTable.getSelection());
//                moduleImportsTable.redraw();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        Button addButton = 
                new Button(composite, SWT.PUSH);
        addButton.setText("Add Imports...");
        addButton.setLayoutData(bgd);
        addButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                selectAndAddModules();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        Button removeButton = 
                new Button(composite, SWT.PUSH);
        removeButton.setText("Remove Selected");
        removeButton.setLayoutData(bgd);
        removeButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                removeSelectedModules();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

//        Label l = new Label(parent, SWT.NONE);
//        l.setText("(Checked modules are exported.)");
        
    }

    private void makeShared(TableItem[] items) {
        List<String> list = new ArrayList<String>();
        for (TableItem item: items) {
            String name = item.getText();
            if (!name.equals(Module.LANGUAGE_MODULE_NAME)) {
                list.add(name);
            }
        }
        makeModuleImportShared(project, getModule(), 
                list.toArray(new String[0]));
        for (TableItem item: items) {
            String name = item.getText();
            if (!name.equals(Module.LANGUAGE_MODULE_NAME)) {
                item.setText(2, 
                        item.getText(2).isEmpty() ? 
                                "shared" : "");
            }
        }
    }
    
    private void createPackagesBlock(Composite parent) {
        Label label = new Label(parent, SWT.NONE);
        label.setText("Packages:");
        
        Composite composite = 
                new Composite(parent, SWT.NONE);
        GridData cgd = new GridData(FILL_HORIZONTAL);
        cgd.grabExcessHorizontalSpace = true;
        composite.setLayoutData(cgd);
        GridLayout layout = new GridLayout(3, true);
        layout.marginWidth=0;
        composite.setLayout(layout);
        
        final Table packagesTable = new Table(composite, 
                 SWT.MULTI | SWT.V_SCROLL | SWT.BORDER);
        packagesTable.setHeaderVisible(true);
        packagesTable.setLinesVisible(true);
        GridData gd = new GridData(FILL_HORIZONTAL);
        gd.horizontalSpan=2;
        gd.verticalSpan=2;
        gd.grabExcessHorizontalSpace = true;
//        gd.grabExcessVerticalSpace = true;
        gd.heightHint = 100;
        gd.widthHint = 300;
        packagesTable.setLayoutData(gd);
        TableColumn column = 
                new TableColumn(packagesTable, 
                        SWT.NONE, 0);
        column.setText("Package");
        column.setWidth(250);
        TableColumn sharedColumn = 
                new TableColumn(packagesTable, 
                        SWT.NONE, 1);
        sharedColumn.setText("Visibility");
        sharedColumn.setWidth(70);
        for (Package p: getModule().getPackages()) {
            TableItem item = 
                    new TableItem(packagesTable, 
                            SWT.NONE, 0);
            item.setImage(CeylonResources.PACKAGE);
            item.setText(p.getNameAsString());
            item.setText(1, p.isShared() ? "shared" : "");
        }
        
        GridData bgd = 
                new GridData(
                        VERTICAL_ALIGN_BEGINNING |
                        HORIZONTAL_ALIGN_FILL);
        bgd.grabExcessHorizontalSpace=false;
        bgd.widthHint = 50;
        
        Button sharedButton = 
                new Button(composite, SWT.PUSH);
        sharedButton.setText("Toggle Shared");
        sharedButton.setLayoutData(bgd);
        sharedButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                for (TableItem item: packagesTable.getSelection()) {
                    makeShared(item);
                }
//                packagesTable.redraw();
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });
        
        Button createPackageButton = 
                new Button(composite, SWT.PUSH);
        createPackageButton.setText("Create Package...");
        createPackageButton.setLayoutData(bgd);
        createPackageButton.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                NewPackageWizard wiz = openPackageWizard();
                if (wiz.isCreated()) {
                    IPackageFragment pfr = 
                            wiz.getPackageFragment();
                    TableItem item = 
                            new TableItem(packagesTable, 
                                    SWT.NONE);
                    item.setImage(CeylonResources.PACKAGE);
                    item.setText(pfr.getElementName());
                    item.setText(1, wiz.isShared() ? "shared" : "");
                }
            }
            @Override
            public void widgetDefaultSelected(SelectionEvent e) {}
        });

//        Label l = new Label(parent, SWT.NONE);
//        l.setText("(Checked packages are shared.)");
        
        new Label(parent, SWT.SEPARATOR|SWT.HORIZONTAL)
                .setLayoutData(new GridData(FILL_HORIZONTAL));

    }

    private void makeShared(TableItem item) {
        Package pkg = 
                getModule()
                    .getPackage(item.getText());
        Unit unit = pkg.getUnit();
        ProjectPhasedUnit<IProject,IResource,IFolder,IFile> phasedUnit;
        if (unit instanceof ProjectSourceFile) {
            ProjectSourceFile<IProject,IResource,IFolder,IFile> ceylonUnit = 
                    (ProjectSourceFile<IProject,IResource,IFolder,IFile>) unit;
            phasedUnit = ceylonUnit.getPhasedUnit();
            TextFileChange textFileChange = 
                    new TextFileChange(
                            "Make Package Shared", 
                            phasedUnit.getResourceFile());
            textFileChange.setEdit(new MultiTextEdit());
            IDocument doc = getDocument(textFileChange);
            Tree.PackageDescriptor pd = 
                    phasedUnit.getCompilationUnit()
                        .getPackageDescriptors()
                        .get(0);
            if (pkg.isShared()) {
                removeSharedAnnotation(textFileChange, doc, 
                        pd.getAnnotationList());
            }
            else {
                textFileChange.addEdit(new InsertEdit(
                        pd.getStartIndex(), "shared "));
            }
            performChange(textFileChange);
            item.setText(1, !pkg.isShared() ? "shared" : "");
        }
        else {
            item.setChecked(!item.getChecked());
        }
    }
    
    private void createModuleInfoBlock(Composite parent) {
        Composite composite = 
                new Composite(parent, SWT.NONE);
        GridData cgd = new GridData(FILL_HORIZONTAL);
//        cgd.grabExcessHorizontalSpace = true;
        composite.setLayoutData(cgd);
        GridLayout layout = new GridLayout(2, true);
        layout.marginWidth=0;
        composite.setLayout(layout);
        Label label = new Label(composite, SWT.NONE);
        label.setText("Module name: ");
        label = new Label(composite, SWT.NONE);
        label.setText(packageFragment.getElementName());
        
        /*Label img = new Label(composite, SWT.BORDER);
        Image image = CeylonPlugin.getInstance()
                .getImageRegistry().get(CEYLON_NEW_MODULE);
        img.setImage(image);
        img.setSize(image.getBounds().width, image.getBounds().height);
        GridData igd = new GridData(HORIZONTAL_ALIGN_END|VERTICAL_ALIGN_END);
        igd.verticalSpan=4;
//        igd.horizontalSpan=2;
        igd.grabExcessHorizontalSpace=true;
        img.setLayoutData(igd);*/
        
        label = new Label(composite, SWT.NONE);
        label.setText("Project name: ");
        label = new Label(composite, SWT.NONE);
        label.setText(project.getName());
        
        /*label = new Label(composite, SWT.NONE);
        label.setText("Module workspace path: ");
        label = new Label(composite, SWT.NONE);
        label.setText(pf.getResource().getFullPath().toPortableString());
        
        label = new Label(composite, SWT.NONE);
        label.setText("Default source file encoding: ");
        final Link link = new Link(composite, SWT.NONE);
        try {
            final IFolder f = (IFolder) pf.getResource();
            link.setText(f.getDefaultCharset() + " <a>(Change...)</a>");
            link.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    IWorkbenchPreferenceContainer container = (IWorkbenchPreferenceContainer) getContainer();
                    container.openPage("org.eclipse.ui.propertypages.info.file", null);
                }
            });
            getWorkspace().addResourceChangeListener(encodingListener=new IResourceChangeListener() {
                @Override
                public void resourceChanged(IResourceChangeEvent event) {
                    if (event.getType()==IResourceChangeEvent.POST_CHANGE) {
                        Display.getDefault().asyncExec(new Runnable() {
                            public void run() {
                                try {
                                    if (!link.isDisposed()) {
                                        link.setText(f.getDefaultCharset() + 
                                                " <a>(Change...)</a>");
                                    }
                                }
                                catch (CoreException e) {
                                    e.printStackTrace();
                                }
                            }
                        });
                    }
                }
            });
        }
        catch (CoreException e) {
            e.printStackTrace();
        }*/
        
        new Label(parent, SWT.SEPARATOR|SWT.HORIZONTAL)
                .setLayoutData(new GridData(FILL_HORIZONTAL));
        
    }
    
    @Override
    public void createControl(Composite parent) {
        noDefaultAndApplyButton();
        super.createControl(parent);
    }

    private void selectAndAddModules() {
        ModuleImportContentProvider contentProvider = 
                new ModuleImportContentProvider(getModule(), 
                        project) {
            @Override
            public ModuleSearchResult getModules(String prefix) {
                return getModuleSearchResults(prefix, getModule(),
                        getProjectTypeChecker(project), project);
            }
        };
        ModuleImportSelectionDialog dialog = 
                new ModuleImportSelectionDialog(getShell(), 
                        contentProvider);
        Map<String, ModuleVersionNode> added = 
                selectModules(dialog, project);
        addModuleImports(project, getModule(), added);
        for (Map.Entry<String, ModuleVersionNode> entry: 
                added.entrySet()) {
            TableItem item = 
                    new TableItem(moduleImportsTable, 
                            SWT.NONE);
            item.setImage(CeylonResources.MODULE);
            item.setText(entry.getKey());
            item.setText(1, entry.getValue().getVersion());
        }
    }

    private void removeSelectedModules() {
        int[] selection = 
                moduleImportsTable.getSelectionIndices();
        List<String> names = new ArrayList<String>();
        List<Integer> removed = new ArrayList<Integer>();
        for (int index: selection) {
            TableItem item = moduleImportsTable.getItem(index);
            String name = item.getText();
            if (!name.equals(Module.LANGUAGE_MODULE_NAME)) {
                names.add(name);
                removed.add(index);
            }
        }
        removeModuleImports(project, getModule(), names);
        int[] indices = new int[removed.size()];
        for (int i=0; i<removed.size(); i++) {
            indices[i] = removed.get(i);
        }
        moduleImportsTable.remove(indices);
    }
    
}
