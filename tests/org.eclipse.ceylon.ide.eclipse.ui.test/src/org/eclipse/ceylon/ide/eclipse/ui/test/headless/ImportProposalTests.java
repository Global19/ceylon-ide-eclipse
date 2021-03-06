/********************************************************************************
 * Copyright (c) 2011-2017 Red Hat Inc. and/or its affiliates and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 1.0 which is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * SPDX-License-Identifier: EPL-1.0
 ********************************************************************************/
package org.eclipse.ceylon.ide.eclipse.ui.test.headless;

import static org.junit.Assert.assertEquals;
import static org.eclipse.ceylon.ide.eclipse.code.correct.ImportProposals.importProposals;

import ceylon.collection.HashSet;
import java.util.List;

import org.antlr.runtime.CommonToken;
import org.junit.Test;

import org.eclipse.ceylon.compiler.java.runtime.model.TypeDescriptor;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.Identifier;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.ImportMemberOrType;
import org.eclipse.ceylon.compiler.typechecker.tree.Tree.ImportMemberOrTypeList;
import org.eclipse.ceylon.model.typechecker.model.Declaration;

public class ImportProposalTests {
    @Test
    public void testDelimiter1() {
    	ImportMemberOrTypeList imtl = prepareImportMemberOrTypeList();
		HashSet<Declaration> ignoredDeclarations = new HashSet<Declaration>(TypeDescriptor.klass(Declaration.class));
		
		String result = importProposals().formatImportMembers("\n", "  ", ignoredDeclarations, imtl);
		assertEquals(result, "{\n  Bar,\n  Foo\n}");
		
	}
    
    @Test
    public void testDelimiter2() {
    	ImportMemberOrTypeList imtl = prepareImportMemberOrTypeList();
		HashSet<Declaration> ignoredDeclarations = new HashSet<Declaration>(TypeDescriptor.klass(Declaration.class));
		
		String result = importProposals().formatImportMembers("\r\n", "  ", ignoredDeclarations, imtl);
		assertEquals(result, "{\r\n  Bar,\r\n  Foo\r\n}");
	}

    @Test
    public void testDelimiter3() {
    	ImportMemberOrTypeList imtl = prepareImportMemberOrTypeList();
		HashSet<Declaration> ignoredDeclarations = new HashSet<Declaration>(TypeDescriptor.klass(Declaration.class));
		
		String result = importProposals().formatImportMembers("|||", "  ", ignoredDeclarations, imtl);
		assertEquals(result, "{|||  Bar,|||  Foo|||}");
	}
    
    @Test
    public void testSingleImport1() {
    	ImportMemberOrTypeList imtl = prepareSingleImportMemberOrTypeList();
		HashSet<Declaration> ignoredDeclarations = new HashSet<Declaration>(TypeDescriptor.klass(Declaration.class));
		
		String result = importProposals().formatImportMembers("\r\n", "  ", ignoredDeclarations, imtl);
		assertEquals(result, "{\r\n  Bar\r\n}");
	}
    
    @Test
    public void testSingleImport2() {
    	ImportMemberOrTypeList imtl = prepareSingleImportMemberOrTypeList();
		HashSet<Declaration> ignoredDeclarations = new HashSet<Declaration>(TypeDescriptor.klass(Declaration.class));
		
		String result = importProposals().formatImportMembers("\n", "  ", ignoredDeclarations, imtl);
		assertEquals(result, "{\n  Bar\n}");
	}


    @Test
    public void testSingleImport3() {
    	ImportMemberOrTypeList imtl = prepareSingleImportMemberOrTypeList();
		HashSet<Declaration> ignoredDeclarations = new HashSet<Declaration>(TypeDescriptor.klass(Declaration.class));
		
		String result = importProposals().formatImportMembers("|||", "  ", ignoredDeclarations, imtl);
		assertEquals(result, "{|||  Bar|||}");
	}
    
	private ImportMemberOrTypeList prepareSingleImportMemberOrTypeList() {
		CommonToken noToken = new CommonToken(0);
		ImportMemberOrTypeList imtl = new ImportMemberOrTypeList(noToken);
    	List<ImportMemberOrType> importMembers = imtl.getImportMemberOrTypes();
    	
    	ImportMemberOrType importMember = new ImportMemberOrType(noToken);
    	importMember.setAlias(null);
    	importMember.setText("Bar");
    	Identifier identifier = new Identifier(noToken);
    	identifier.setText("Bar");
		importMember.setIdentifier(identifier);
    	importMembers.add(importMember);
    	
		return imtl;
	}

	private ImportMemberOrTypeList prepareImportMemberOrTypeList() {
		CommonToken noToken = new CommonToken(0);
		ImportMemberOrTypeList imtl = new ImportMemberOrTypeList(noToken);
    	List<ImportMemberOrType> importMembers = imtl.getImportMemberOrTypes();
    	
    	ImportMemberOrType importMember = new ImportMemberOrType(noToken);
    	importMember.setAlias(null);
    	importMember.setText("Bar");
    	Identifier identifier = new Identifier(noToken);
    	identifier.setText("Bar");
		importMember.setIdentifier(identifier);
    	importMembers.add(importMember);
    	
    	importMember = new ImportMemberOrType(noToken);
    	importMember.setAlias(null);
    	importMember.setText("Foo");
    	identifier = new Identifier(noToken);
    	identifier.setText("Foo");
		importMember.setIdentifier(identifier);
    	importMembers.add(importMember);
		return imtl;
	}

}
