package org.eclipse.ui.views.navigator;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.util.ArrayList;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.help.WorkbenchHelp;

/**
 * Implements the go to resource action. Opens a dialog and set
 * the navigator selection with the resource selected by
 * the user.
 */
public class GotoResourceAction extends ResourceNavigatorAction {
/**
 * Creates a new instance of the class.
 * @since 2.0
 */
public GotoResourceAction(IResourceNavigator navigator, String label) {
	super(navigator, label);
	WorkbenchHelp.setHelp(this, INavigatorHelpContextIds.GOTO_RESOURCE_ACTION);
}
/**
 * Collect all resources in the workbench and add them to the <code>resources</code>
 * list.
 */
private void collectAllResources(IContainer container,ArrayList resources,ResourcePatternFilter filter) {
	try {
		IResource members[] = container.members();
		for (int i = 0; i < members.length; i++){
			IResource r = members[i];
			if(filter.select(getNavigator().getViewer(),null,r))
				resources.add(r);
			if(r.getType() != IResource.FILE)
				collectAllResources((IContainer)r,resources,filter);
		}
	} catch (CoreException e) {
	}
}
/**
 * Collect all resources in the workbench open a dialog asking
 * the user to select a resource and change the selection in
 * the navigator.
 */
public void run() {
	IContainer cont = (IContainer)getViewer().getInput();
	ArrayList resources = new ArrayList();
	collectAllResources(cont,resources,getNavigator().getPatternFilter());
	IResource resourcesArray[] = new IResource[resources.size()];
	resources.toArray(resourcesArray);
	GotoResourceDialog dialog = new GotoResourceDialog(getShell(),resourcesArray);
 	dialog.open();
 	Object[] result = dialog.getResult();
 	if (result == null || result.length == 0 || result[0] instanceof IResource == false)
 		return;
	 
	IResource selection = (IResource) result[0];
	getViewer().setSelection(new StructuredSelection(selection),true);
}
}
