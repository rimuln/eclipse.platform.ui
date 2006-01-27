/******************************************************************************* * Copyright (c) 2005 IBM Corporation and others. * All rights reserved. This program and the accompanying materials * are made available under the terms of the Eclipse Public License v1.0 * which accompanies this distribution, and is available at * http://www.eclipse.org/legal/epl-v10.html * * Contributors: *     IBM Corporation - initial API and implementation ******************************************************************************/package org.eclipse.jface.menus;import org.eclipse.jface.window.Window;/** * <p> * An event describing changes to an <code>SMenu</code>. * </p> * <p> * Clients may instantiate this class, but must not extend. * </p> * <p> * <strong>EXPERIMENTAL</strong>. This class or interface has been added as * part of a work in progress. There is a guarantee neither that this API will * work nor that it will remain the same. Please do not use this API without * consulting with the Platform/UI team. * </p> *  * @since 3.2 * @see IMenuListener#menuChanged(MenuEvent) */public final class MenuEvent extends MenuCollectionEvent {	/**	 * The bit used to represent whether the menu has changed its label.	 */	private static final int CHANGED_LABEL = LAST_USED_BIT_MENU_COLLECTION << 1;	/**	 * The menu that has changed; this value is never <code>null</code>.	 */	private final SMenu menu;	/**	 * Creates a new instance of <code>MenuEvent</code>.	 * 	 * @param menu	 *            The instance that has changed; must not be <code>null</code>.	 * @param labelChanged	 *            <code>true</code>, iff the label property changed.	 * @param locationsChanged	 *            <code>true</code> if the locations have changed;	 *            <code>false</code> otherwise.	 * @param dynamicChanged	 *            <code>true</code> if the dynamic class has changed;	 *            <code>false</code> otherwise.	 * @param definedChanged	 *            <code>true</code>, iff the defined property changed.	 * @param visibilityChanged	 *            <code>true</code>, iff the visibility property change.	 * @param window	 *            The window in which the visibility changed; may be	 *            <code>null</code> if the default visibility changed.	 */	public MenuEvent(final SMenu menu, final boolean labelChanged,			final boolean locationsChanged, final boolean dynamicChanged,			final boolean definedChanged, final boolean visibilityChanged,			final Window window) {		super(locationsChanged, dynamicChanged, definedChanged,				visibilityChanged, window);		if (menu == null)			throw new NullPointerException("A menu event needs a menu"); //$NON-NLS-1$		this.menu = menu;		if (labelChanged) {			changedValues |= CHANGED_LABEL;		}	}	/**	 * Returns the instance that changed.	 * 	 * @return the instance that changed. Guaranteed not to be <code>null</code>.	 */	public final SMenu getMenu() {		return menu;	}	/**	 * Returns whether or not the label property changed.	 * 	 * @return <code>true</code>, iff the label property changed.	 */	public final boolean isLabelChanged() {		return ((changedValues & CHANGED_LABEL) != 0);	}}