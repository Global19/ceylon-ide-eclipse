package com.redhat.ceylon.eclipse.code.editor;

import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.ASTRING_LITERAL;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.AVERBATIM_STRING;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.LINE_COMMENT;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.MULTI_COMMENT;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.STRING_LITERAL;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.VERBATIM_STRING;
import static com.redhat.ceylon.compiler.typechecker.parser.CeylonLexer.WS;
import static com.redhat.ceylon.eclipse.code.editor.CeylonEditor.AUTO_FOLD_COMMENTS;
import static com.redhat.ceylon.eclipse.code.editor.CeylonEditor.AUTO_FOLD_IMPORTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.antlr.runtime.CommonToken;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.ui.internal.editors.text.EditorsPlugin;

import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.compiler.typechecker.tree.Visitor;

/**
 * FolderBase is an abstract base type for a source-text folding service.
 * It is intended to support extensions for language-specific folders.
 * The class is abstract only with respect to a method that sends a
 * visitor to an AST, as both the visitor and AST node types are language
 * specific.
 * 
 * @author suttons@us.ibm.com
 * @author rfuhrer@watson.ibm.com
 * @author Gavin King
 */
public class FoldingUpdater {
    
    private final CeylonSourceViewer sourceViewer;

    private boolean firstTime = true;
    
    // Maps new annotations to positions
    private final HashMap<Annotation,Position> newAnnotations = new HashMap<Annotation, Position>();
    private HashMap<Annotation,Position> oldAnnotations = new HashMap<Annotation, Position>();
    
    public FoldingUpdater(CeylonSourceViewer sourceViewer) {
        this.sourceViewer = sourceViewer;
    }

    /**
     * Make a folding annotation that corresponds to the extent of text
     * represented by a given program entity. Usually, this will be an
     * AST node, but it can be anything for which the language's
     * ISourcePositionLocator can produce an offset/end offset.
     * 
     * @param n an Object representing a program entity
     */
//    public void makeAnnotation(Object n) {
//        makeAnnotation(n, false);
//    }
    
    /**
     * Make a folding annotation that corresponds to the extent of text
     * represented by a given program entity. Usually, this will be an
     * AST node, but it can be anything for which the language's
     * ISourcePositionLocator can produce an offset/end offset.
     * 
     * @param n an Object representing a program entity
     */
//    public void makeAnnotation(Object n, boolean collapsed) {
//        makeAnnotation(getStartOffset(n), getLength(n), collapsed);
//    }

    /**
     * Make a folding annotation that corresponds to the given range of text.
     * 
     * @param start        The starting offset of the text range
     * @param len        The length of the text range
     */
    public ProjectionAnnotation makeAnnotation(int start, int len) {
        ProjectionAnnotation annotation= new ProjectionAnnotation();
        len = advanceToEndOfLine(start, len);
        newAnnotations.put(annotation, new Position(start, len));
        return annotation;
    }

    protected int advanceToEndOfLine(int start, int len) {
        IDocument doc = sourceViewer.getDocument();
        try {
            int line = doc.getLineOfOffset(start+len);
            while (start+len<doc.getLength() && 
                    Character.isWhitespace(doc.getChar(start+len)) &&
                    doc.getLineOfOffset(start+len)==line) {
                len++;
            }
        }
        catch (BadLocationException e) {
            e.printStackTrace();
        }
        return len;
    }
    
    /**
     * Make a folding annotation that corresponds to the given range of text.
     * 
     * @param start        The starting offset of the text range
     * @param len        The length of the text range
     */
    public void makeAnnotation(int start, int len, boolean collapsed) {
        ProjectionAnnotation annotation = new ProjectionAnnotation(collapsed);
        len = advanceToEndOfLine(start, len);
        newAnnotations.put(annotation, new Position(start, len));
    }

    /**
     * Update the folding structure for a source text, where the text and its
     * AST are represented by a given parse controller and the folding structure
     * is represented by annotations in a given annotation model.
     * 
     * This is the principal routine of the folding updater.
     * 
     * The implementation provided here makes use of a local class
     * FoldingUpdateStrategy, to which the task of updating the folding
     * structure is delegated.
     * 
     * updateFoldingStructure is synchronized because, at least on file opening,
     * it can be called more than once before the first invocation has completed.
     * This can lead to inconsistent calculations resulting in the absence of
     * folding annotations in newly opened files.
     * 
     * @param ast                    The AST for the source text
     * @param annotationModel        A structure of projection annotations that
     *                                represent the foldable elements in the source
     *                                text
     */
    public synchronized void updateFoldingStructure(Tree.CompilationUnit ast, 
    		List<CommonToken> tokens) {
        try {
        	
        	ProjectionAnnotationModel annotationModel = sourceViewer.getProjectionAnnotationModel(); 
            if (ast==null||annotationModel==null) {
                // We can't create annotations without an AST
                return;
            }
        
            // But, since here we have the AST ...
            sendVisitorToAST(ast, tokens);
            
            /*
            // Update the annotation model if there have been changes
            // but not otherwise (since update leads to redrawing of the    
            // source in the editor, which is likely to be unwelcome if
            // there haven't been any changes relevant to folding)
            boolean updateNeeded = false;
            if (firstTime) {
                // Should just be the first time through
                updateNeeded = true;
            } 
            else {
                // Check to see whether the current and previous annotations
                // differ in any significant way; if not, then there's no
                // reason to update the annotation model.
                // Note:  This test may be implemented in various ways that may
                // be more or less simple, efficient, correct, etc.  (The
                // default test provided below is simplistic although quick and
                // usually effective.)
                updateNeeded = differ(oldAnnotations, newAnnotations);
            }
            
            // Need to curtail calls to modifyAnnotations() because these lead to calls
            // to fireModelChanged(), which eventually lead to calls to updateFoldingStructure,
            // which lead back here, which would lead to another call to modifyAnnotations()
            // (unless those were curtailed)
            if (updateNeeded) {*/
                List<Annotation> deletions = new ArrayList<Annotation>(oldAnnotations.size());
                for (Map.Entry<Annotation,Position> e: oldAnnotations.entrySet()) {
                    if (!newAnnotations.containsValue(e.getValue())) {
                        deletions.add(e.getKey());
                    }
                }
                Map<Annotation, Position> additions = new HashMap<Annotation, Position>(newAnnotations.size());
                for (Map.Entry<Annotation,Position> e: newAnnotations.entrySet()) {
                    if (!oldAnnotations.containsValue(e.getValue())) {
                        additions.put(e.getKey(), e.getValue());
                    }
                }
                annotationModel.modifyAnnotations(deletions.toArray(new Annotation[0]), additions, null);
                // Capture the latest set of annotations in a form that can be used the next
                // time that it is necessary to modify the annotations
                for (Annotation a: deletions) {
                    oldAnnotations.remove(a);
                }
                oldAnnotations.putAll(additions);
            //}

            newAnnotations.clear();        
        } 
        catch (Exception e) {
            e.printStackTrace();
        }
    }    


    /**
     * A method to test whether there has been a significant change in the folding
     * annotations for a source text.  The method works by comparing two lists of
     * annotations, nominally the "old" and "new" annotations.  It returns true iff
     * there is considered to be a "significant" difference in the two lists, where
     * the meaning of "significant" is defined by the implementation of this method.
     * 
     * The default implementation provided here is a simplistic test of the difference
     * between two lists, considering only their size.  This may work well enough much
     * of the time as the comparisons between lists should be made very frequently,
     * actually more frequently than the rate at which the typical human user will
     * edit the program text so as to affect the AST so as to affect the lists.  Thus
     * most changes of lists will entail some change in the number of elements at some
     * point that will be observed here.  This will not work for certain very rapid
     * edits of source text (e.g., rapid replacement of elements).
     * 
     * This method should be overridden in language-specific implementations of the
     * folding updater where a more sophisticated test is desired.    
     *     
     * @param list1        A list of annotations (nominally the "old" annotations)
     * @param list2        A list of annotations (nominally the "new" annotations)
     * @return            true iff there has been a "significant" difference in the
     *                     two given lists of annotations
     * 
     */
    protected boolean differ(Map<Annotation,Position> old, Map<Annotation,Position> current) {
        if (old.size() != current.size()) {
            return true;
        }
        return false;
    }

    /**
     * Send a visitor to an AST representing a program in order to construct the
     * folding annotations.  Both the visitor type and the AST node type are language-
     * dependent, so this method is abstract.
     * 
     * @param newAnnotations    A map of annotations to text positions
     * @param annotations        A listing of the annotations in newAnnotations, that is,
     *                             a listing of keys to the map of text positions
     * @param ast                An Object that will be taken to represent an AST node
     */
    public void sendVisitorToAST(Tree.CompilationUnit ast, List<CommonToken> tokens) {
        final boolean autofoldImports;
        final boolean autofoldComments;
        if (firstTime) {
            IPreferenceStore store = EditorsPlugin.getDefault().getPreferenceStore();
            store.setDefault(AUTO_FOLD_IMPORTS, true);
            autofoldImports = store.getBoolean(AUTO_FOLD_IMPORTS);
            store.setDefault(AUTO_FOLD_COMMENTS, true);
            autofoldComments = store.getBoolean(AUTO_FOLD_COMMENTS);
            firstTime = false;
        }
        else {
            autofoldImports = false;
            autofoldComments = false;
        }
    	for (int i=0; i<tokens.size(); i++) {
            CommonToken token = tokens.get(i);
            int type = token.getType();
            if (type==MULTI_COMMENT ||
                type==STRING_LITERAL ||
                type==ASTRING_LITERAL ||
                type==VERBATIM_STRING ||
                type==AVERBATIM_STRING) {
                if (isMultilineToken(token)) {
                    ProjectionAnnotation ann = makeAnnotation(token, token);
                    if (autofoldComments && ann!=null && type==MULTI_COMMENT) {
                        ann.markCollapsed();
                    }
                }
            }
            if (type==LINE_COMMENT) {
                CommonToken until = token;
                int j=i+1;
                CommonToken next = tokens.get(j);
                while (next.getType()==LINE_COMMENT ||
                        next.getType()==WS) {
                    if (next.getType()==LINE_COMMENT) {
                        until = next;
                        i = j;
                    }
                    next = tokens.get(++j);
                }
                ProjectionAnnotation ann = foldIfNecessary(token, until);
                if (ann!=null && autofoldComments) {
                    ann.markCollapsed();
                }
            }
        }
        Tree.CompilationUnit cu = (Tree.CompilationUnit) ast;
        new Visitor() {
            @Override 
            public void visit(Tree.ImportList importList) {
                super.visit(importList);
                if (!importList.getImports().isEmpty()) {
                    ProjectionAnnotation ann = foldIfNecessary(importList);
                    if (autofoldImports && ann!=null) {
                        ann.markCollapsed();
                    }
                }
            }
            /*@Override 
            public void visit(Tree.Import that) {
                super.visit(that);
                foldIfNecessary(that);
            }*/
            @Override 
            public void visit(Tree.Body that) {
                super.visit(that);
                if (that.getToken()!=null) { //for "else if"
                    foldIfNecessary(that);
                }
            }
            @Override 
            public void visit(Tree.NamedArgumentList that) {
                super.visit(that);
                foldIfNecessary(that);
            }
            @Override 
            public void visit(Tree.ModuleDescriptor that) {
                super.visit(that);
                foldIfNecessary(that);
            }
        }.visit(cu);
    }

    private ProjectionAnnotation foldIfNecessary(Node node) {
        CommonToken token = (CommonToken) node.getToken();
        CommonToken endToken = (CommonToken) node.getEndToken();
        if (endToken.getLine()-token.getLine()>0) {
            return makeAnnotation(token, endToken);
        }
        else {
            return null;
        }
    }
    
    private ProjectionAnnotation foldIfNecessary(CommonToken start, CommonToken end) {
        if (end.getLine()>start.getLine()) {
            return makeAnnotation(start, end);
        }
        else {
            return null;
        }
    }
    
    private boolean isMultilineToken(CommonToken token) {
        return token.getText().indexOf('\n')>0 ||
                token.getText().indexOf('\r')>0;
    }

    private ProjectionAnnotation makeAnnotation(CommonToken start, CommonToken end) {
        return makeAnnotation(start.getStartIndex(), 
                end.getStopIndex()-start.getStartIndex()+1);
    }
    
}