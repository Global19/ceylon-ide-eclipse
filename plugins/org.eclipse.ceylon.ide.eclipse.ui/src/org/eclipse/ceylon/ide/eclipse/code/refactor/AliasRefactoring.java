/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.refactor;

import static org.eclipse.ceylon.ide.eclipse.java2ceylon.Java2CeylonProxies.utilJ2C;
import static org.eclipse.ceylon.ide.eclipse.util.EditorUtil.getDocument;
import static org.eclipse.ceylon.ide.eclipse.util.Nodes.findToplevelStatement;
import static org.eclipse.ltk.core.refactoring.RefactoringStatus.createErrorStatus;

import java.util.ArrayList;
import java.util.List;

import org.antlr.runtime.CommonToken;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.ui.IEditorPart;

import org.eclipse.ceylon.compiler.typechecker.context.PhasedUnit;
import org.eclipse.ceylon.compiler.typechecker.tree.Node;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.compiler.typechecker.tree.Visitor;
import org.eclipse.ceylon.ide.eclipse.util.Nodes;
import org.eclipse.ceylon.ide.common.util.escaping_;
import org.eclipse.ceylon.model.typechecker.model.Class;
import org.eclipse.ceylon.model.typechecker.model.Interface;
import org.eclipse.ceylon.model.typechecker.model.Parameter;
import org.eclipse.ceylon.model.typechecker.model.ParameterList;
import org.eclipse.ceylon.model.typechecker.model.Type;
import org.eclipse.ceylon.model.typechecker.model.TypeDeclaration;
import org.eclipse.ceylon.model.typechecker.model.Unit;

public class AliasRefactoring extends AbstractRefactoring {
    
    private static class FindAliasedTypeVisitor 
            extends Visitor {
        private Type type;
        private List<Node> nodes = new ArrayList<Node>();

        private FindAliasedTypeVisitor(Type type) {
            this.type = type;
        }
        
        public List<Node> getNodes() {
            return nodes;
        }
        
        @Override
        public void visit(Tree.Type that) {
            super.visit(that);
            if (!(that instanceof Tree.SyntheticVariable)) {
                Type t = that.getTypeModel();
                if (t!=null && type.isExactly(t)) {
                    nodes.add(that);
                }
            }
        }
        @Override
        public void visit(Tree.BaseTypeExpression that) {
            super.visit(that);
            TypeDeclaration td = type.getDeclaration();
            if (isClassWithParameters(td)) {
                Type t = that.getTarget().getType();
                if (t!=null && type.isExactly(t)) {
                    nodes.add(that);
                }
            }
        }

    }

    private static boolean isClassWithParameters(
            TypeDeclaration td) {
        return td instanceof Class && 
                !td.isTuple() &&
                ((Class) td).getParameterList()!=null;
    }
    
    private String newName;
    private String typeString;
    private final Type type;
//    private boolean renameValuesAndFunctions;
    
    public Node getNode() {
        return node;
    }

    public AliasRefactoring(IEditorPart editor) {
        super(editor);
        if (rootNode!=null) {
            if (node instanceof Tree.Type) {
                Tree.Type t = (Tree.Type) node;
                type = t.getTypeModel();
                newName = null;
                typeString = Nodes.text(t, tokens);
            }
            else {
                type = null;
            }
        }
        else {
            type = null;
        }
    }
    
    @Override
    public boolean getEnabled() {
        return type!=null &&
                project != null;
    }

    public int getCount() {
        return type==null ? 
                0 : countDeclarationOccurrences();
    }
    
    @Override
    protected boolean searchInFile(PhasedUnit pu) {
        return inSamePackage(pu) && 
                super.searchInFile(pu);
    }
    
    @Override
    int countReferences(Tree.CompilationUnit cu) {
        FindAliasedTypeVisitor frv = 
                new FindAliasedTypeVisitor(type);
        cu.visit(frv);
        return frv.getNodes().size();
    }

    public String getName() {
        return "Introduce Type Alias";
    }

    public RefactoringStatus checkInitialConditions(
            IProgressMonitor pm)
                    throws CoreException, 
                           OperationCanceledException {
        // Check parameters retrieved from editor context
        return new RefactoringStatus();
    }

    public RefactoringStatus checkFinalConditions(
            IProgressMonitor pm)
                    throws CoreException, 
                           OperationCanceledException {
        if (newName==null || 
                !newName.matches("^[a-zA-Z_]\\w*$")) {
            return createErrorStatus(
                    "Not a legal Ceylon identifier");
        }
        else if (escaping_.get_().isKeyword(newName)) {
            return createErrorStatus(
                    "'" + newName + "' is a Ceylon keyword");
        }
        else {
            int ch = newName.codePointAt(0);
            if (!Character.isUpperCase(ch)) {
                return createErrorStatus(
                        "Not an initial uppercase identifier");
            }
        }
        /*Declaration existing = declaration.getContainer()
                        .getMemberOrParameter(declaration.getUnit(), 
                                newName, null, false);
        if (null!=existing && !existing.equals(declaration)) {
            return createWarningStatus("An existing declaration named '" +
                newName + "' already exists in the same scope");
        }*/
        return new RefactoringStatus();
    }
    
    private int aliasOffset;
    private int insertedLength;
    private int insertedLocation;
    
    int getAliasOffset() {
        return aliasOffset;
    }
    
    int getAliasLength() {
        return typeString.length();
    }
    
    public int getInsertedLength() {
        return insertedLength;
    }
    
    public int getInsertedLocation() {
        return insertedLocation;
    }
    
    @Override
    protected void refactorInFile(TextChange tfc, 
            CompositeChange cc, 
            Tree.CompilationUnit root,
            List<CommonToken> tokens) {
        tfc.setEdit(new MultiTextEdit());
        if (type!=null) {
            Unit editorUnit = 
                    editor.getParseController()
                        .getLastCompilationUnit()
                        .getUnit();
            Unit unit = root.getUnit();
            if (editorUnit.getPackage()
                    .equals(unit.getPackage())) {
                IDocument doc = getDocument(tfc);
                String delim = 
                        utilJ2C().indents()
                            .getDefaultLineDelimiter(document);
                if (newName!=null) {
                    for (Node node: getNodesToRename(root)) {
                        renameNode(tfc, node, root);
                    }
                }
                if (unit.getFilename()
                        .equals(editorUnit.getFilename())) {
                    Type t = getType();
                    TypeDeclaration td = t.getDeclaration();
                    StringBuffer header = new StringBuffer();
                    Tree.Statement statement = 
                            findToplevelStatement(
                                    this.rootNode, 
                                    this.node);
                    int insertLoc = 
                            statement==null ?
                                doc.getLength() :
                                statement.getStartIndex();
                    aliasOffset = insertLoc;
//                            doc.getLength() + 
//                            delim.length()*2;
                    if (td.isShared()) {
                        header.append("shared ");
                        aliasOffset += 7;
                    }
                    StringBuffer args = new StringBuffer();
                    String initialName = getInitialName();
                    if (isClassWithParameters(td)) {
                        Class c = (Class) td;
                        aliasOffset += 6;
                        header.append("class ")
                            .append(initialName)
                            .append("(");
                        args.append("(");
                        boolean first = true;
                        ParameterList pl = 
                                c.getParameterList();
                        for (Parameter p: 
                            pl.getParameters()) {
                            if (first) {
                                first = false;
                            }
                            else {
                                header.append(", ");
                                args.append(", ");
                            }
                            String ptype = 
                                    t.getTypedParameter(p)
                                        .getFullType()
                                        .asString(unit);
                            String pname = p.getName();
                            header.append(ptype) 
                                .append(" ")
                                .append(pname);
                            args.append(pname);
                        }
                        header.append(")");
                        args.append(")");
                    }
                    else if (td instanceof Interface) {
                        aliasOffset += 10;
                        header.append("interface ")
                            .append(initialName);
                    }
                    else {
                        aliasOffset += 6;
                        header.append("alias ")
                            .append(initialName);
                    }
                    String indent = 
                            utilJ2C().indents()
                                .getDefaultIndent();
                    String text = 
                            header + delim + 
                            indent + indent +
                            "=> " + t.asString(unit) + 
                            args + ";" +
                            delim + delim;
                    insertedLength = text.length();
                    insertedLocation = insertLoc;
                    tfc.addEdit(new InsertEdit(
                            insertLoc, text));
                }
            }
//            if (renameValuesAndFunctions) { 
//                for (Tree.Identifier id: getIdentifiersToRename(root)) {
//                    renameIdentifier(tfc, id, root);
//                }
//            }
        }
        if (cc!=null && tfc.getEdit().hasChildren()) {
            cc.add(tfc);
        }
    }

    String getInitialName() {
        return newName==null ? 
                typeString : newName;
    }
    
    public List<Node> getNodesToRename(
            Tree.CompilationUnit root) {
        FindAliasedTypeVisitor frv = 
                new FindAliasedTypeVisitor(type);
        root.visit(frv);
        return frv.getNodes();
    }
    
    protected void renameNode(TextChange tfc, Node node, 
            Tree.CompilationUnit root) {
        tfc.addEdit(new ReplaceEdit(
                node.getStartIndex(),
                node.getDistance(),
                newName));
    }
    
    /*public boolean isRenameValuesAndFunctions() {
        return renameValuesAndFunctions;
    }
    
    public void setRenameValuesAndFunctions(boolean renameLocals) {
        this.renameValuesAndFunctions = renameLocals;
    }*/
    
    public void setNewName(String text) {
        newName = text;
    }
    
    public Type getType() {
        return type;
    }

    public String getNewName() {
        return newName;
    }
    
    @Override
    protected boolean isAffectingOtherFiles() {
        return true;
    }

}
