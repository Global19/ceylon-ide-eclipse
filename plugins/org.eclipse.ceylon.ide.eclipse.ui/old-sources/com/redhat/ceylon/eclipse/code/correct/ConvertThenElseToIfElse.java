/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.code.correct;

import static org.eclipse.ceylon.ide.eclipse.java2ceylon.Java2CeylonProxies.utilJ2C;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.ReplaceEdit;

import org.eclipse.ceylon.compiler.typechecker.tree.CustomTree;
import org.eclipse.ceylon.compiler.typechecker.tree.Node;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.BaseMemberExpression;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.ConditionList;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.Expression;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.Return;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.SpecifierOrInitializerExpression;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.Statement;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.Term;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.ThenOp;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.ValueModifier;
import org.eclipse.ceylon.ide.eclipse.util.Nodes;
import org.eclipse.ceylon.model.typechecker.model.Type;

@Deprecated
class ConvertThenElseToIfElse extends CorrectionProposal {
    
    ConvertThenElseToIfElse(int offset, TextChange change) {
        super("Convert to 'if' 'else' statement", change, new Region(offset, 0));
    }
    
    static void addConvertToIfElseProposal(IDocument doc,
                Collection<ICompletionProposal> proposals, IFile file,
                Statement statement) {
        try {
            String action;
            String declaration = null;
            Node operation;
            if (statement instanceof Tree.Return) {
                Tree.Return returnOp = (Return) statement;
                action = "return ";
                if (returnOp.getExpression() == null || returnOp.getExpression().getTerm() == null) {
                    return;
                }
                operation = returnOp.getExpression().getTerm();
                
            } else if (statement instanceof Tree.ExpressionStatement) {
                Tree.ExpressionStatement expressionStmt = (Tree.ExpressionStatement) statement;
                if (expressionStmt.getExpression() == null) {
                    return;
                }
                Tree.Expression expression = expressionStmt.getExpression();
                if (expression.getTerm()==null) {
                    return;
                }
                if (! (expression.getTerm() instanceof Tree.AssignOp)) {
                    return;
                }
                Tree.AssignOp assignOp = (Tree.AssignOp) expression.getTerm();
                action = getTerm(doc, assignOp.getLeftTerm()) + " = ";
                operation = assignOp.getRightTerm();
            } else if (statement instanceof Tree.SpecifierStatement) {
                Tree.SpecifierStatement specifierStmt = (Tree.SpecifierStatement) statement;
                if (specifierStmt.getRefinement()) return;
                action = getTerm(doc, specifierStmt.getBaseMemberExpression()) + " = ";
                operation = specifierStmt.getSpecifierExpression().getExpression();
            } else if (statement instanceof CustomTree.AttributeDeclaration) {
                CustomTree.AttributeDeclaration attrDecl = (CustomTree.AttributeDeclaration) statement;
                if (attrDecl.getIdentifier()==null) {
                    return;
                }
                String identifier = getTerm(doc, attrDecl.getIdentifier());
                String annotations = "";
                if (!attrDecl.getAnnotationList().getAnnotations().isEmpty()) {
                    annotations = getTerm(doc, attrDecl.getAnnotationList()) + " ";
                }
                String type;
                if (attrDecl.getType() instanceof ValueModifier) {
                    ValueModifier valueModifier = (ValueModifier) attrDecl.getType();
                    Type typeModel = valueModifier.getTypeModel();
                    if (typeModel==null) return;
                    type = typeModel.asString();
                    
                } else {
                    type = getTerm(doc, attrDecl.getType());
                }
                
                declaration = annotations + type + " " + identifier + ";";
                SpecifierOrInitializerExpression sie = attrDecl.getSpecifierOrInitializerExpression();
                if (sie==null || sie.getExpression()==null) return;
                action = identifier + " = ";
                operation = sie.getExpression().getTerm();
            } else {
                return;
            }
            
            String test;
            String elseTerm;
            String thenTerm;
            
            while (operation instanceof Expression) {
                //If Operation is enclosed in parenthesis we need to get down through them: return (test then x else y);
                operation = ((Expression) operation).getTerm();
             }
            
            if (operation instanceof Tree.DefaultOp) {
                Tree.DefaultOp defaultOp = (Tree.DefaultOp) operation;
                if (defaultOp.getLeftTerm() instanceof Tree.ThenOp) {
                    Tree.ThenOp thenOp = (ThenOp) defaultOp.getLeftTerm();
                    thenTerm = getTerm(doc, thenOp.getRightTerm());
                    test = getTerm(doc, thenOp.getLeftTerm());
                } else {
                    Term leftTerm = defaultOp.getLeftTerm();
                    String leftTermStr = getTerm(doc, leftTerm);
                    if (leftTerm instanceof BaseMemberExpression) {
                        thenTerm = leftTermStr;
                        test = "exists " + leftTermStr;            
                    } else  {
                        String id = Nodes.nameProposals(leftTerm)[0];
                        test = "exists " + id + " = " + leftTermStr;
                        thenTerm = id;
                    }    
                }
                elseTerm = getTerm(doc, defaultOp.getRightTerm());
            } else if (operation instanceof Tree.ThenOp) {
                Tree.ThenOp thenOp = (ThenOp) operation;
                thenTerm = getTerm(doc, thenOp.getRightTerm());
                test = getTerm(doc, thenOp.getLeftTerm());
                elseTerm = "null";
            } else if (operation instanceof Tree.IfExpression) {
                Tree.IfExpression ie = (Tree.IfExpression) operation;
                thenTerm = getTerm(doc, ie.getIfClause().getExpression());
                elseTerm = getTerm(doc, ie.getElseClause().getExpression());
                ConditionList cl = ie.getIfClause().getConditionList();
                test = getTerm(doc, cl);
            }
            else {
                return;
            }

            String baseIndent = utilJ2C().indents().getIndent(statement, doc);
            String indent = utilJ2C().indents().getDefaultIndent();
            
            test = removeEnclosingParentesis(test);
            
            StringBuilder replace = new StringBuilder();
            String delim = utilJ2C().indents().getDefaultLineDelimiter(doc);
            if (declaration != null) {
                replace.append(declaration)
                        .append(delim)
                        .append(baseIndent);
            }
            replace.append("if (").append(test).append(") {")
                    .append(delim)
                    .append(baseIndent).append(indent).append(action).append(thenTerm).append(";")
                    .append(delim)
                    .append(baseIndent).append("}")
                    .append(delim)
                    .append(baseIndent).append("else {")
                    .append(delim)
                    .append(baseIndent).append(indent).append(action).append(elseTerm).append(";")
                    .append(delim)
                    .append(baseIndent).append("}");

            TextChange change = new TextFileChange("Convert to If Else", file);
            change.setEdit(new ReplaceEdit(statement.getStartIndex(), 
                    statement.getDistance(), 
                    replace.toString()));
            proposals.add(new ConvertThenElseToIfElse(statement.getStartIndex(), change));
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private static String removeEnclosingParentesis(String s) {
        if (s.charAt(0) == '(' && s.charAt(s.length() - 1) == ')') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
    
    private static String getTerm(IDocument doc, Node node) throws BadLocationException {
        return doc.get(node.getStartIndex(), node.getDistance());
    }
}