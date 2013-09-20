package com.redhat.ceylon.eclipse.code.refactor;

import static com.redhat.ceylon.eclipse.code.refactor.RenameRefactoring.LINK_PATTERN;
import static com.redhat.ceylon.eclipse.core.builder.CeylonBuilder.getProjectTypeChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.ReplaceEdit;

import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.model.Declaration;
import com.redhat.ceylon.compiler.typechecker.model.Package;
import com.redhat.ceylon.compiler.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.BaseMemberOrTypeExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.BaseType;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.ImportMemberOrType;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.QualifiedMemberOrTypeExpression;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.QualifiedType;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.StringLiteral;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;
import com.redhat.ceylon.eclipse.core.vfs.IFileVirtualFile;

public class RenameJavaElementRefactoringParticipant extends RenameParticipant {

	private IMember javaDeclaration;

	protected boolean initialize(Object element) {
		javaDeclaration= (IMember) element;
		return getProjectTypeChecker(javaDeclaration.getJavaProject().getProject())!=null;
	}

	public String getName() {
		return "Rename participant for Ceylon source";
	}
	
	public RefactoringStatus checkConditions(IProgressMonitor pm, 
	        CheckConditionsContext context) {
		return new RefactoringStatus();
	}

	public Change createChange(IProgressMonitor pm) throws CoreException {
		
        final IProject project = javaDeclaration.getJavaProject().getProject();
		final String newName = getArguments().getNewName();
        final String oldName = javaDeclaration.getElementName();
		
        final HashMap<IFile,Change> changes= new HashMap<IFile,Change>();
        TypeChecker tc = getProjectTypeChecker(project);
        if (tc==null) return null;
        final Pattern wikiRef = Pattern.compile(LINK_PATTERN);
		for (PhasedUnit phasedUnit: tc.getPhasedUnits().getPhasedUnits()) {
            final List<ReplaceEdit> edits = new ArrayList<ReplaceEdit>();
            Tree.CompilationUnit cu = phasedUnit.getCompilationUnit();
            cu.visit(new Visitor() {
                @Override
                public void visit(ImportMemberOrType that) {
                    super.visit(that);
                    visitIt(that.getIdentifier(), that.getDeclarationModel());
                }
                @Override
                public void visit(QualifiedMemberOrTypeExpression that) {
                    super.visit(that);
                    visitIt(that.getIdentifier(), that.getDeclaration());
                }
                @Override
                public void visit(BaseMemberOrTypeExpression that) {
                    super.visit(that);
                    visitIt(that.getIdentifier(), that.getDeclaration());
                }
                @Override
                public void visit(BaseType that) {
                    super.visit(that);
                    visitIt(that.getIdentifier(), that.getDeclarationModel());
                }
                @Override
                public void visit(QualifiedType that) {
                    super.visit(that);
                    visitIt(that.getIdentifier(), that.getDeclarationModel());
                }
                protected void visitIt(Tree.Identifier id, Declaration dec) {
                    visitIt(id.getText(), id.getStartIndex(), dec);
                }
                protected void visitIt(String name, Integer offset, Declaration dec) {
                    if (dec!=null && dec.getQualifiedNameString()
                            .equals(getQualifiedName(javaDeclaration)) &&
                            name.equals(javaDeclaration.getElementName())) {
                        edits.add(new ReplaceEdit(offset, oldName.length(), newName));
                    }
                }
                protected String getQualifiedName(IMember dec) {
                    IJavaElement parent = dec.getParent();
                    if (parent instanceof ICompilationUnit) {
                        return parent.getParent().getElementName() + "::" + 
                                dec.getElementName();
                    }
                    else if (dec.getDeclaringType()!=null) {
                        return getQualifiedName(dec.getDeclaringType()) + "." + 
                                dec.getElementName();
                    }
                    else {
                        return "@";
                    }
                }
                @Override
                public void visit(StringLiteral that) {
                    super.visit(that);
                    //TODO: fix copy/paste from RenameRefactoring
                    Matcher m = wikiRef.matcher(that.getToken().getText());
                    while (m.find()) {
                        String pgroup = m.group(3);
                        String tgroup = m.group(5); int tloc = m.start(5);
                        String mgroup = m.group(7); int mloc = m.start(7);
                        Declaration base;
                        if (pgroup==null) {
                            base = that.getScope().getMemberOrParameter(that.getUnit(), 
                                    tgroup, null, false);
                        }
                        else {
                            Package pack = that.getUnit().getPackage().getModule()
                                    .getPackage(pgroup);
                            base = pack==null ? null : pack.getDirectMember(tgroup, null, false);
                        }
                        Integer offset = that.getStartIndex();
                        if (base!=null) {
                            visitIt(tgroup, offset+tloc, base);
                        }
                        if (base instanceof TypeDeclaration && mgroup!=null) {
                            Declaration qualified = ((TypeDeclaration) base).getMember(mgroup, null, false);
                            if (qualified!=null) {
                                visitIt(mgroup, offset+mloc, qualified);
                            }
                        }
                    }
                }
            });
            if (!edits.isEmpty()) {
                try {
                    IFile file = ((IFileVirtualFile) phasedUnit.getUnitFile()).getFile();
                    TextFileChange change= new TextFileChange(file.getName(), file);
                    change.setEdit(new MultiTextEdit());
                    changes.put(file, change);
                    for (ReplaceEdit edit: edits) {
                        change.addEdit(edit);
                    }
                }       
                catch (Exception e) { 
                    e.printStackTrace(); 
                }
            }
		}
		
		if (changes.isEmpty())
			return null;
		
		CompositeChange result= new CompositeChange("Ceylon source changes");
		for (Iterator<Change> iter= changes.values().iterator(); iter.hasNext();) {
			result.add((Change) iter.next());
		}
		return result;
	}
}