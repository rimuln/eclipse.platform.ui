/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.registry;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.internal.WorkbenchPlugin;

/**
 * The central manager for view descriptors.
 */
public class ViewRegistry implements IViewRegistry {
	private List views;
	private List categories;
	private Category miscCategory;
/**
 * Create a new ViewRegistry.
 */
public ViewRegistry() {
	views = new ArrayList();
	categories = new ArrayList();
}
/**
 * Add a category to the registry.
 */
public void add(Category desc) {
	/* fix for 1877 */
	if (findCategory(desc.getId()) == null)
		categories.add(desc);
}
/**
 * Add a descriptor to the registry.
 */
public void add(IViewDescriptor desc) {
	views.add(desc);
}
/**
 * Find a descriptor in the registry.
 */
public IViewDescriptor find(String id) {
	Iterator enum = views.iterator();
	while (enum.hasNext()) {
		IViewDescriptor desc = (IViewDescriptor) enum.next();
		if (id.equals(desc.getID())) {
			return desc;
		}
	}
	return null;
}
/**
 * Find a category with a given name.
 */
public Category findCategory(String id) {
	Iterator enum = categories.iterator();
	while (enum.hasNext()) {
		Category cat = (Category) enum.next();
		if (id.equals(cat.getRootPath())) {
			return cat;
		}
	}
	return null;
}
/**
 * Get the list of view categories.
 */
public Category [] getCategories() {
	int nSize = categories.size();
	Category [] retArray = new Category[nSize];
	categories.toArray(retArray);
	return retArray;
}
/**
 * Return the view category count.
 */
public int getCategoryCount() {
	return categories.size();
}
/**
 * Returns the Misc category.
 * This may be null if there are no miscellaneous views.
 */
public Category getMiscCategory() {
	return miscCategory;
}
/**
 * Return the view count.
 */
public int getViewCount() {
	return views.size();
}
/**
 * Get an enumeration of view descriptors.
 */
public IViewDescriptor [] getViews() {
	int nSize = views.size();
	IViewDescriptor [] retArray = new IViewDescriptor[nSize];
	views.toArray(retArray);
	return retArray;
}
/**
 * Adds each view in the registry to a particular category.
 * The view category may be defined in xml.  If not, the view is
 * added to the "misc" category.
 */
public void mapViewsToCategories() {
	Iterator enum = views.iterator();
	while (enum.hasNext()) {
		IViewDescriptor desc = (IViewDescriptor) enum.next();
		Category cat = null;
		String [] catPath = desc.getCategoryPath();
		if (catPath != null) {
			String rootCat = catPath[0];
			cat = (Category)findCategory(rootCat);
		}	
		if (cat != null) {
			cat.addElement(desc);
		} else {
			if (miscCategory == null) {
				miscCategory = new Category();
				categories.add(miscCategory);
			}
			if (catPath != null) {
				// If we get here, this view specified a category which
				// does not exist. Add this view to the 'Other' category
				// but give out a message (to the log only) indicating 
				// this has been done.
				String fmt = "Category {0} not found for view {1}.  This view added to ''{2}'' category."; //$NON-NLS-1$
				WorkbenchPlugin.log(MessageFormat.format(fmt, new Object[]{catPath[0], desc.getID(), miscCategory.getLabel()}));  //$NON-NLS-1$
			}
			miscCategory.addElement(desc);
		}
	}
}
}
