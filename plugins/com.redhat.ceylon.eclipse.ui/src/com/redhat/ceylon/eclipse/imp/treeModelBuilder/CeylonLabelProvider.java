package com.redhat.ceylon.eclipse.imp.treeModelBuilder;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.services.ILabelProvider;
import org.eclipse.imp.utils.MarkerUtils;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;

import com.redhat.celyon.eclipse.ui.CeylonPlugin;
import com.redhat.celyon.eclipse.ui.ICeylonResources;
import com.redhat.ceylon.compiler.typechecker.tree.Node;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;


public class CeylonLabelProvider implements ILabelProvider {
	private Set<ILabelProviderListener> fListeners = new HashSet<ILabelProviderListener>();

	private static ImageRegistry sImageRegistry = CeylonPlugin.getInstance()
			.getImageRegistry();

	private static Image DEFAULT_IMAGE = sImageRegistry
			.get(ICeylonResources.CEYLON_DEFAULT_IMAGE);
	private static Image FILE_IMAGE = sImageRegistry
			.get(ICeylonResources.CEYLON_FILE);
	private static Image FILE_WITH_WARNING_IMAGE = sImageRegistry
			.get(ICeylonResources.CEYLON_FILE_WARNING);
	private static Image FILE_WITH_ERROR_IMAGE = sImageRegistry
			.get(ICeylonResources.CEYLON_FILE_ERROR);

	private static Image CLASS = sImageRegistry
			.get(ICeylonResources.CEYLON_CLASS);
	private static Image INTERFACE = sImageRegistry
			.get(ICeylonResources.CEYLON_INTERFACE);
	private static Image LOCAL_CLASS = sImageRegistry
			.get(ICeylonResources.CEYLON_LOCAL_CLASS);
	private static Image LOCAL_INTERFACE = sImageRegistry
			.get(ICeylonResources.CEYLON_LOCAL_INTERFACE);
	private static Image METHOD = sImageRegistry
			.get(ICeylonResources.CEYLON_METHOD);
	private static Image ATTRIBUTE = sImageRegistry
			.get(ICeylonResources.CEYLON_ATTRIBUTE);
	private static Image LOCAL_METHOD = sImageRegistry
			.get(ICeylonResources.CEYLON_LOCAL_METHOD);
	private static Image LOCAL_ATTRIBUTE = sImageRegistry
			.get(ICeylonResources.CEYLON_LOCAL_ATTRIBUTE);


	public Image getImage(Object element) {
		if (element instanceof IFile) {
			// TODO:  rewrite to provide more appropriate images
			IFile file = (IFile) element;
			int sev = MarkerUtils.getMaxProblemMarkerSeverity(file,
					IResource.DEPTH_ONE);

			switch (sev) {
			case IMarker.SEVERITY_ERROR:
				return FILE_WITH_ERROR_IMAGE;
			case IMarker.SEVERITY_WARNING:
				return FILE_WITH_WARNING_IMAGE;
			default:
				return FILE_IMAGE;
			}
		}
		ModelTreeNode n = (ModelTreeNode) element;
		
		return getImageFor(n);
	}

	private Image getImageFor(ModelTreeNode n) {
		if (n.getCategory()==-1) return null;
		return getImageFor((Node) n.getASTNode());
	}

	public static Image getImageFor(Node n) {
		if (n instanceof Tree.Declaration) {
			Tree.Declaration d = (Tree.Declaration) n;
			boolean shared = d.getDeclarationModel().isShared();
			if (n instanceof Tree.AnyClass) {
				if (shared) {
					return CLASS;
				}
				else {
					return LOCAL_CLASS;
				}
			}
			else if (n instanceof Tree.AnyInterface) {
				if (shared) {
					return INTERFACE;
				}
				else { 
					return LOCAL_INTERFACE;
				}
			}
			else if (n instanceof Tree.AnyMethod) {
				if (shared) {
					return METHOD;
				}
				else {
					return LOCAL_METHOD;
				}
			}
			else {
				if (shared) {
					return ATTRIBUTE;
				}
				else {
					return LOCAL_ATTRIBUTE;
				}
			}
		}
		else {
			return DEFAULT_IMAGE;
		}
	}

	public String getText(Object element) {
		return getLabelFor( (ModelTreeNode) element );
	}

	private String getLabelFor(ModelTreeNode n) {
		return getLabelFor((Node) n.getASTNode());
	}

	public static String getLabelFor(Node n) {
		
		//TODO: it would be much better to render types
		//      from the tree nodes instead of from the
		//      model nodes
		
		if (n instanceof Tree.AnyClass) {
			Tree.AnyClass ac = (Tree.AnyClass) n;
			return "class " + name(ac.getIdentifier()) +
					parameters(ac.getTypeParameterList()) +
					parameters(ac.getParameterList());
		}
		else if (n instanceof Tree.AnyInterface) {
			Tree.AnyInterface ai = (Tree.AnyInterface) n;
			return "interface " + name(ai.getIdentifier()) + 
					parameters(ai.getTypeParameterList());
		}
		else if (n instanceof Tree.ObjectDefinition) {
			Tree.ObjectDefinition ai = (Tree.ObjectDefinition) n;
			return "object " + name(ai.getIdentifier());
		}
		else if (n instanceof Tree.TypedDeclaration) {
			Tree.TypedDeclaration td = (Tree.TypedDeclaration) n;
			String label = type(td.getType()) + 
					" " + name(td.getIdentifier());
			if (n instanceof Tree.AnyMethod) {
				Tree.AnyMethod am = (Tree.AnyMethod) n;
				label += parameters(am.getTypeParameterList()) +
						parameters(am.getParameterLists().get(0));
			}
			return label;
		}
				
		return "<something>";
	}
	
	private static String type(Tree.Type type) {
		if (type==null) {
			return "<Unknown>";
		}
		else {
			return type.getTypeModel().getProducedTypeName();
		}
	}
	
	private static String name(Tree.Identifier id) {
		if (id==null) {
			return "<unknown>";
		}
		else {
			return id.getText();
		}
	}

	private static String parameters(Tree.ParameterList pl) {
		if (pl==null ||
				pl.getParameters().isEmpty()) {
			return "()";
		}
		String label = "(";
		for (Tree.Parameter p: pl.getParameters()) {
			label += type(p.getType()) + 
					" " + name(p.getIdentifier()) + ", ";
		}
		return label.substring(0, label.length()-2) + ")";
	}

	private static String parameters(Tree.TypeParameterList tpl) {
		if (tpl==null ||
				tpl.getTypeParameterDeclarations().isEmpty()) {
			return "";
		}
		String label = "<";
		for (Tree.TypeParameterDeclaration p: tpl.getTypeParameterDeclarations()) {
			label += name(p.getIdentifier()) + ", ";
		}
		return label.substring(0, label.length()-2) + ">";
	}

	public void addListener(ILabelProviderListener listener) {
		fListeners.add(listener);
	}

	public void dispose() {}

	public boolean isLabelProperty(Object element, String property) {
		return false;
	}

	public void removeListener(ILabelProviderListener listener) {
		fListeners.remove(listener);
	}
}
