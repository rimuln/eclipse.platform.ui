package org.eclipse.jface.preference;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog; // disambiguate from SWT Dialog
import org.eclipse.jface.resource.*;
import org.eclipse.jface.util.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import java.util.*;
import java.util.List; // disambiguate from SWT List

/**
 * A preference dialog is a hierarchical presentation of preference
 * pages.  Each page is represented by a node in the tree shown
 * on the left hand side of the dialog; when a node is selected, the
 * corresponding page is shown on the right hand side.
 */
public class PreferenceDialog extends Dialog implements IPreferencePageContainer {
	/**
	 * Title area fields
	 */
	public static final String PREF_DLG_TITLE_IMG = "preference_dialog_title_image";//$NON-NLS-1$
	public static final String PREF_DLG_IMG_TITLE_ERROR = "preference_dialog_title_error_image";//$NON-NLS-1$
	static {
		ImageRegistry reg = JFaceResources.getImageRegistry();
		reg.put(PREF_DLG_TITLE_IMG, ImageDescriptor.createFromFile(PreferenceDialog.class, "images/pref_dialog_title.gif"));//$NON-NLS-1$
		reg.put(PREF_DLG_IMG_TITLE_ERROR, ImageDescriptor.createFromFile(PreferenceDialog.class, "images/title_error.gif"));//$NON-NLS-1$
	}
	 
	private static final RGB ERROR_BACKGROUND_RGB = new RGB(230, 226, 221);

	private Composite titleArea;
	private CLabel messageLabel;
	private Label titleImage;
	private Color titleAreaColor;

	private String message;
	private Color normalMsgAreaBackground;
	private Color errorMsgAreaBackground;
	private Image errorMsgImage;

	/**
	 * Preference store, initially <code>null</code> meaning none.
	 *
	 * @see #setPreferenceStore
	 */
	private IPreferenceStore preferenceStore;

	/**
	 * The current preference page, or <code>null</code> if
	 * there is none.
	 */
	private IPreferencePage currentPage;

	/**
	 * The preference manager.
	 */
	private PreferenceManager preferenceManager;
	
	/**
	 * The main control for this dialog.
	 */
	private Composite body;

	/**
	 * The Composite in which a page is shown.
	 */
	private Composite pageContainer;

	/**
	 * The minimum page size; 400 by 400 by default.
	 *
	 * @see #setMinimumPageSize
	 */
	private Point minimumPageSize = new Point(400,400);

	/**
	 * The OK button.
	 */
	private Button okButton;

	/**
	 * The Cancel button.
	 */
	private Button cancelButton;

	/**
	 * The Help button; <code>null</code> if none.
	 */
	private Button helpButton = null;
	
	/**
	 * Indicates whether help is available; <code>false</code> by default.'
	 *
	 * @see #setHelpAvailable
	 */
	private boolean isHelpAvailable = false;

	/**
	 * The tree control.
	 */
	private Tree tree;

	/**
	 * The current tree item.
	 */
	private TreeItem currentTreeItem;

	/**
	 * Layout for the page container.
	 *
	 * @see #pageContainer
	 */
	private class PageLayout extends Layout {
		public void layout(Composite composite, boolean force) {
			Rectangle rect = composite.getClientArea();
			Control [] children = composite.getChildren();
			for (int i= 0; i < children.length; i++) {
				children[i].setSize(rect.width, rect.height);
			}
		}
		public Point computeSize(Composite composite, int wHint, int hHint, boolean force) {
			if (wHint != SWT.DEFAULT && hHint != SWT.DEFAULT)
				return new Point(wHint, hHint);
			int x= minimumPageSize.x;
			int y= minimumPageSize.y;
			
			Control[] children= composite.getChildren();
			for (int i= 0; i < children.length; i++) {
				Point size= children[i].computeSize(SWT.DEFAULT, SWT.DEFAULT, force);
				x= Math.max(x, size.x);
				y= Math.max(y, size.y);
			}				
			if (wHint != SWT.DEFAULT) x = wHint;
			if (hHint != SWT.DEFAULT) y = hHint;
			return new Point(x, y);
		}	
	}
	
/**
 * Creates a new preference dialog under the control of the given preference 
 * manager.
 *
 * @param shell the parent shell
 * @param manager the preference manager
 */
public PreferenceDialog(Shell parentShell, PreferenceManager manager) {
	super(parentShell);
	preferenceManager = manager;
}
/* (non-Javadoc)
 * Method declared on Dialog.
 */
protected void buttonPressed(int buttonId) {
	switch (buttonId) {
		case IDialogConstants.OK_ID : {
			okPressed();
			return;
		}
		case IDialogConstants.CANCEL_ID : {
			cancelPressed();
			return;
		}
		case IDialogConstants.HELP_ID : {
			helpPressed();
			return;
		}
	}
}
/* (non-Javadoc)
 * Method declared on Dialog.
 */
protected void cancelPressed() {
	// Inform all pages that we are cancelling
	Iterator nodes = preferenceManager.getElements(PreferenceManager.PRE_ORDER).iterator();
	while (nodes.hasNext()) {
		IPreferenceNode node = (IPreferenceNode) nodes.next();
		if (node.getPage() != null) {
			if(!node.getPage().performCancel())
				return;
		}
	}
	setReturnCode(CANCEL);
	close();
}
/* (non-Javadoc)
 * Method declared on Window.
 */
public boolean close() {
	List nodes = preferenceManager.getElements(PreferenceManager.PRE_ORDER);
	for (int i = 0; i < nodes.size(); i++){
		IPreferenceNode node = (IPreferenceNode) nodes.get(i);
			node.disposeResources();
	}
	return super.close();
}
/* (non-Javadoc)
 * Method declared on Window.
 */
protected void configureShell(Shell newShell) {
	super.configureShell(newShell);
	newShell.setText(JFaceResources.getString("PreferenceDialog.title"));//$NON-NLS-1$

	// Register help listener on the shell
	newShell.addHelpListener(new HelpListener() {
		public void helpRequested(HelpEvent event) {
			// call perform help on the current page
			if (currentPage != null) {
				currentPage.performHelp();
			}	
		}
	});
}
/* (non-Javadoc)
 * Method declared on Dialog.
 */
protected void createButtonsForButtonBar(Composite parent) {
	// create OK and Cancel buttons by default
	okButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
	getShell().setDefaultButton(okButton);
	cancelButton = createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	if (isHelpAvailable) {
		helpButton = createButton(parent, IDialogConstants.HELP_ID, IDialogConstants.HELP_LABEL, false);
	}
}
/* (non-Javadoc)
 * Method declared on Dialog.
 */
protected Control createContents(Composite parent) {
	Control control = super.createContents(parent);

	// Add the first page
	selectFirstItem();

	return control;
}
/* (non-Javadoc)
 * Method declared on Dialog.
 */
protected Control createDialogArea(Composite parent) {
	GridData gd;
	Composite composite = (Composite)super.createDialogArea(parent);
	((GridLayout) composite.getLayout()).numColumns = 2;
	
	// Build the tree an put it into the composite.
	createTree(composite);
	gd = new GridData(GridData.FILL_VERTICAL);
	gd.widthHint = 150;
	gd.verticalSpan = 2;
	tree.setLayoutData(gd);

	// Build the title area and separator line
	Composite titleComposite = new Composite(composite, SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	layout.verticalSpacing = 0;
	layout.horizontalSpacing = 0;
	titleComposite.setLayout(layout);
	titleComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	
	createTitleArea(titleComposite);

	Label titleBarSeparator = new Label(titleComposite, SWT.HORIZONTAL | SWT.SEPARATOR);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	titleBarSeparator.setLayoutData(gd);

	// Build the Page container
	pageContainer = createPageContainer(composite);
	pageContainer.setLayoutData(new GridData(GridData.FILL_BOTH));
	pageContainer.setFont(parent.getFont());

	// Build the separator line
	Label separator = new Label(composite, SWT.HORIZONTAL | SWT.SEPARATOR);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	gd.horizontalSpan = 2;
	separator.setLayoutData(gd);

	return composite;
}
/**
 * Creates the inner page container.
 */
private Composite createPageContainer(Composite parent) {
	Composite result = new Composite(parent, SWT.NULL);
	result.setLayout(new PageLayout());
	return result;
}
/**
 * Creates the wizard's title area.
 *
 * @param parent the SWT parent for the title area composite
 * @return the created title area composite
 */
private Composite createTitleArea(Composite parent) {
	// Create the title area which will contain
	// a title, message, and image.
	titleArea = new Composite(parent, SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.marginHeight = 0;
	layout.marginWidth = 0;
	layout.verticalSpacing = 0;
	layout.horizontalSpacing = 0;
	layout.numColumns = 2;
	
	// Get the background color for the title area
	Display display = parent.getDisplay();
	Color bg = display.getSystemColor(SWT.COLOR_WHITE);
	
	GridData layoutData = new GridData(GridData.FILL_BOTH);
	titleArea.setLayout(layout);
	titleArea.setLayoutData(layoutData);
	titleArea.setBackground(bg);

	// Add a dispose listener
	titleArea.addDisposeListener(new DisposeListener() {
		public void widgetDisposed(DisposeEvent e) {
			if (titleAreaColor != null)
				titleAreaColor.dispose();
			if (errorMsgAreaBackground != null)
				errorMsgAreaBackground.dispose();
		}
	});


	// Message label
	messageLabel = new CLabel(titleArea, SWT.LEFT);
	messageLabel.setBackground(bg);
	messageLabel.setText(" ");//$NON-NLS-1$
	messageLabel.setFont(JFaceResources.getBannerFont());
	
	final IPropertyChangeListener fontListener = new IPropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent event) {
			if(JFaceResources.BANNER_FONT.equals(event.getProperty()) ||
				JFaceResources.DIALOG_FONT.equals(event.getProperty())) {
				updateMessage();
			}
		}
	};
	
	messageLabel.addDisposeListener(new DisposeListener() {
		public void widgetDisposed(DisposeEvent event) {
			JFaceResources.getFontRegistry().removeListener(fontListener);
		}
	});
	
	JFaceResources.getFontRegistry().addListener(fontListener);
	
	
	GridData gd = new GridData(GridData.FILL_BOTH);
	messageLabel.setLayoutData(gd);

	// Title image
	titleImage = new Label(titleArea, SWT.LEFT);
	titleImage.setBackground(bg);
	titleImage.setImage(JFaceResources.getImage(PREF_DLG_TITLE_IMG));
	gd = new GridData(); 
	gd.horizontalAlignment = gd.END;
	titleImage.setLayoutData(gd);

	return titleArea;
}


/**
 * Creates a Tree/TreeItem structure that reflects the page hierarchy.
 */
private void createTree(Composite parent) {
	if (tree != null)
		tree.dispose();

	tree = new Tree(parent, SWT.BORDER);

	tree.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(final SelectionEvent event) {
			BusyIndicator.showWhile(tree.getDisplay(),new Runnable(){
				public void run() {
					Object data = event.item.getData();
					if (data instanceof IPreferenceNode) {
						if (!isCurrentPageValid()) {
							showPageFlippingAbortDialog();
							selectCurrentPageAgain();
						} else
							if (!showPage((IPreferenceNode) data)) {
								// Page flipping wasn't successful
								showPageFlippingAbortDialog();
								selectCurrentPageAgain();
							} else {
								// Everything went well
								currentTreeItem = (TreeItem) event.item;
							}
							
						// Keep focus in tree.  See bugs 2692, 2621, and 6775.
						tree.setFocus();
					}
				}
			});
		}
		public void widgetDefaultSelected(final SelectionEvent event) {
			TreeItem[] selection = tree.getSelection();
			if (selection.length > 0) 
				selection[0].setExpanded(!selection[0].getExpanded());
		}
	});
	IPreferenceNode node = preferenceManager.getRoot();
	IPreferenceNode[] subnodes = node.getSubNodes();
	for (int i = 0; i < subnodes.length; i++){
		createTreeItemFor(tree, subnodes[i]);
	}
}
/**
 * Creates a TreeItem structure that reflects to the page hierarchy.
 */
private void createTreeItemFor(Widget parent, IPreferenceNode node) {
	TreeItem item = null;
	if (parent instanceof Tree)
		item = new TreeItem((Tree) parent, SWT.DEFAULT);
	else
		item = new TreeItem((TreeItem) parent, SWT.DEFAULT);

	item.setData(node);
	item.setText(node.getLabelText());
	Image image = node.getLabelImage();
	if (image != null) {
		item.setImage(image);
	}

	IPreferenceNode[] subnodes = node.getSubNodes();
	for (int i = 0; i < subnodes.length; i++){
		createTreeItemFor(item, subnodes[i]);
	}
}
/**
 * Returns the preference mananger used by this preference dialog.
 *
 * @return the preference mananger
 */
public PreferenceManager getPreferenceManager() {
	return preferenceManager;
}
/* (non-Javadoc)
 * Method declared on IPreferencePageDialog.
 */
public IPreferenceStore getPreferenceStore() {
	return preferenceStore;
}
/**
 * Notifies that the user has pressed the Save button and no page has vetoed.
 * <p>
 * The default implementation of this framework method does nothing.
 * Subclasses should override to save the preferences.
 * </p>
 */
protected void handleSave() {
}
/**
 * Notifies that the window's close button was pressed, 
 * the close menu was selected, or the ESCAPE key pressed.
 * <p>
 * The default implementation of this framework method
 * sets the window's return code to <code>CANCEL</code>
 * and closes the window using <code>close</code>.
 * Subclasses may extend or reimplement.
 * </p>
 */
protected void handleShellCloseEvent() {
	// handle the same as pressing cancel
	cancelPressed();
}
/**
 * Notifies of the pressing of the Help button.
 * <p>
 * The default implementation of this framework method
 * calls <code>performHelp</code> on the currently active page.
 * </p>
 */
protected void helpPressed() {
	if (currentPage != null) {
		currentPage.performHelp();
	}
}
/**
 * Returns whether the current page is valid.
 *
 * @return <code>false</code> if the current page is not valid, or
 *  or <code>true</code> if the current page is valid or there is
 *  no current page
*/
protected boolean isCurrentPageValid() {
	if (currentPage == null)
		return true;
	else
		return currentPage.isValid();
}
/**
 * The preference dialog implementation of this <code>Dialog</code>
 * framework method sends <code>performOk</code> to all pages of the 
 * preference dialog, then calls <code>handleSave</code> on this dialog
 * to save any state, and then calls <code>hardClose</code> to close
 * this dialog.
 */
protected void okPressed() {
	// Notify all the pages and give them a chance to abort
	Iterator nodes = preferenceManager.getElements(PreferenceManager.PRE_ORDER).iterator();
	while (nodes.hasNext()) {
		IPreferenceNode node = (IPreferenceNode) nodes.next();
		if (node.getPage() != null) {
			if(!node.getPage().performOk())
				return;
		}
	}

	// Give subclasses the choice to save the state of the
	// preference pages.
	handleSave();

	close();
}
/**
 * Selects the page determined by <code>currentTreeItem</code> in
 * the page hierarchy.
 */
private void selectCurrentPageAgain() {
	tree.setSelection(new TreeItem[] { currentTreeItem });
	currentPage.setVisible(true);
}
/**
 * Selects the first item in the tree of preference pages.
 */
protected void selectFirstItem() {
	if (tree != null) {
		int count = tree.getItemCount();
		if (count > 0) {
			TreeItem[] items = tree.getItems();
			Object data = items[0].getData();
			if (data instanceof IPreferenceNode) {
				tree.setSelection(new TreeItem[] { items[0] });
				currentTreeItem = items[0];
				showPage((IPreferenceNode) data);
				
				// Keep focus in tree.  See bugs 2692, 2621, and 6775.
				tree.setFocus();
			}
		}
	}
}
/**
 * Display the given error message. The currently displayed message
 * is saved and will be redisplayed when the error message is set
 * to <code>null</code>.
 *
 * @param errorMessage the errorMessage to display or <code>null</code>
 */
public void setErrorMessage(String errorMessage) {
	if (errorMessage == null) {
		if (messageLabel.getImage() != null) {
			// we were previously showing an error
			messageLabel.setBackground(normalMsgAreaBackground);
			messageLabel.setImage(null);
			titleImage.setImage(JFaceResources.getImage(PREF_DLG_TITLE_IMG));
			titleArea.layout(true);
		}

		// show the message
		setMessage(message);

	} else {
		messageLabel.setText(errorMessage);
		if (messageLabel.getImage() == null) {
			// we were not previously showing an error
						
			// lazy initialize the error background color and image
			if (errorMsgAreaBackground == null) {
				errorMsgAreaBackground = new Color(messageLabel.getDisplay(), ERROR_BACKGROUND_RGB);
				errorMsgImage = JFaceResources.getImage(PREF_DLG_IMG_TITLE_ERROR);
			}

			// show the error	
			normalMsgAreaBackground = messageLabel.getBackground();
			messageLabel.setBackground(errorMsgAreaBackground);
			messageLabel.setImage(errorMsgImage);
			titleImage.setImage(null);
			titleArea.layout(true);
		}
	}
}
/**
 * Sets whether a Help button is available for this dialog.
 * <p>
 * Clients must call this framework method before the dialog's control
 * has been created.
 * <p>
 *
 * @param b <code>true</code> to include a Help button, 
 *  and <code>false</code> to not include one (the default)
 */
public void setHelpAvailable(boolean b) {
	isHelpAvailable = b;
}
/**
 * Set the message text. If the message line currently displays an error,
 * the message is stored and will be shown after a call to clearErrorMessage
 */
public void setMessage(String newMessage) {
	message = newMessage;
	if (message == null)
		message = "";//$NON-NLS-1$
	if (messageLabel.getImage() == null) 
		// we are not showing an error
		messageLabel.setText(message);
}
/**
 * Sets the minimum page size.
 *
 * @param minWidth the minimum page width
 * @param minHeight the minimum page height
 * @see #setMinimumPageSize(org.eclipse.swt.graphics.Point)
 */
public void setMinimumPageSize(int minWidth, int minHeight) {
	minimumPageSize.x = minWidth;
	minimumPageSize.y = minHeight;
}
/**
 * Sets the minimum page size.
 *
 * @param size the page size encoded as
 *   <code>new Point(width,height)</code>
 * @see #setMinimumPageSize(int,int)
 */
public void setMinimumPageSize(Point size) {
	minimumPageSize.x = size.x;
	minimumPageSize.y = size.y;
}
/**
 * Sets the preference store for this preference dialog.
 *
 * @param store the preference store
 * @see #getPreferenceStore
 */
public void setPreferenceStore(IPreferenceStore store) {
	Assert.isNotNull(store);
	preferenceStore = store;
}
/**
 * Changes the shell size to the given size, ensuring that
 * it is no larger than the display bounds.
 * 
 * @param width the shell width
 * @param height the shell height
 */
private void setShellSize(int width, int height) {
	Rectangle bounds = getShell().getDisplay().getBounds();
	getShell().setSize(Math.min(width, bounds.width), Math.min(height, bounds.height));
}
/**
 * Shows the preference page corresponding to the given preference node.
 * Does nothing if that page is already current.
 *
 * @param node the preference node, or <code>null</code> if none
 * @return <code>true</code> if the page flip was successful, and
 * <code>false</code> is unsuccessful
 */
protected boolean showPage(IPreferenceNode node) {
	if (node == null)
		return false;

	// Create the page if nessessary
	if (node.getPage() == null)
		node.createPage();

	if (node.getPage() == null)
		return false;
		
	IPreferencePage newPage = node.getPage();
	if (newPage == currentPage)
		return true;
		
	Control currentWindow = null;
	if (currentPage != null) {
		if (!currentPage.okToLeave())
			return false;
	};

	IPreferencePage oldPage = currentPage;
	currentPage = newPage;

	// Set the new page's container
	currentPage.setContainer(this);

	// Ensure that the page control has been created
	// (this allows lazy page control creation)
	if (currentPage.getControl() == null) 
		currentPage.createControl(pageContainer);

	// Force calculation of the page's description label because
	// label can be wrapped.
	Point contentSize = currentPage.computeSize();
	// Do we need resizing. Computation not needed if the
	// first page is inserted since computing the dialog's
	// size is done by calling dialog.open().
	if (oldPage != null) {
		Rectangle rect= pageContainer.getClientArea();
		Point containerSize= new Point(rect.width, rect.height);
		int hdiff= contentSize.x - containerSize.x;
		int vdiff= contentSize.y - containerSize.y;

		if (hdiff > 0 || vdiff > 0) {
			hdiff= Math.max(0, hdiff);
			vdiff= Math.max(0, vdiff);
			Shell shell= getShell();
			Point shellSize= shell.getSize();
			setShellSize(shellSize.x + hdiff, shellSize.y + vdiff);
		} else if (hdiff < 0 || vdiff < 0) {
			newPage.setSize(containerSize);
		}
	}

	// Make the new page visible
	currentPage.setVisible(true);
	if (oldPage != null)
		oldPage.setVisible(false);

	// update the dialog controls
	update();

	return true;
}
/**
 * Shows the "Page Flipping abort" dialog.
 */
private void showPageFlippingAbortDialog() {
	MessageDialog dialog = new MessageDialog(getShell(), JFaceResources.getString("AbortPageFlippingDialog.title"), null, JFaceResources.getString("AbortPageFlippingDialog.message"), MessageDialog.WARNING, new String[] { IDialogConstants.OK_LABEL }, 0);//$NON-NLS-2$//$NON-NLS-1$
	dialog.open();
}
/**
 * Updates this dialog's controls to reflect the current page.
 */
protected void update(){
	// Update the title bar
	updateTitle();

	// Update the message line
	updateMessage();

	// Update the buttons
	updateButtons();
}
/* (non-Javadoc)
 * Method declared on IPreferenceContainer
 */
public void updateButtons() {
	okButton.setEnabled(isCurrentPageValid());
}
/* (non-Javadoc)
 * Method declared on IPreferencePageContainer.
 */
public void updateMessage() {
	String pageMessage = currentPage.getMessage();
	String pageErrorMessage = currentPage.getErrorMessage();

	// Adjust the font
	if (pageMessage == null && pageErrorMessage == null)
		messageLabel.setFont(JFaceResources.getBannerFont());
	else
		messageLabel.setFont(JFaceResources.getDialogFont());

	// Set the message and error message	
	if (pageMessage == null) {
		setMessage(currentPage.getTitle());
	} else {
		setMessage(pageMessage);
	}
	setErrorMessage(pageErrorMessage);
}
/* (non-Javadoc)
 * Method declared on IPreferencePageContainer.
 */
public void updateTitle() {
	updateMessage();
}
}
