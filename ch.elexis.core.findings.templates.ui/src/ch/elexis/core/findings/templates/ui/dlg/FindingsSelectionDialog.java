package ch.elexis.core.findings.templates.ui.dlg;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IElementComparer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import ch.elexis.core.findings.templates.model.FindingsTemplate;
import ch.elexis.core.findings.templates.model.FindingsTemplates;

public class FindingsSelectionDialog extends TitleAreaDialog {
	private final FindingsTemplates model;
	private List<FindingsTemplate> selection;
	private TableViewer viewer;
	private boolean multiSelection;
	
	public FindingsSelectionDialog(Shell parentShell, FindingsTemplates model,
		List<FindingsTemplate> selection, boolean multiSelection){
		super(parentShell);
		this.model = model;
		this.selection = selection;
		this.multiSelection = multiSelection;
	}
	
	/**
	 * Create contents of the dialog.
	 * 
	 * @param parent
	 */
	@Override
	protected Control createDialogArea(Composite parent){
		setMessage(multiSelection ? "Wählen Sie Vorlagen aus" : "Wählen Sie eine Vorlage aus");
		setTitle("Befund Vorlage");
		
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		viewer = new TableViewer(composite,
			SWT.FULL_SELECTION | SWT.BORDER | (multiSelection ? SWT.MULTI : SWT.SINGLE));
		viewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		viewer.setContentProvider(new ArrayContentProvider());
		viewer.setLabelProvider(new LabelProvider() {
			@Override
			public String getText(Object element){
				// TODO Auto-generated method stub
				if (element instanceof FindingsTemplate) {
					return ((FindingsTemplate) element).getTitle();
				}
				return "";
			}
		});
		
		viewer.setComparer(new IElementComparer() {
			
			@Override
			public int hashCode(Object element){
				return element.hashCode();
			}
			
			@Override
			public boolean equals(Object a, Object b){
				if (a.equals(b)) {
					return true;
				}
				else if (a instanceof FindingsTemplate && b instanceof FindingsTemplate) {
					FindingsTemplate findingsTemplate1 = (FindingsTemplate) a;
					FindingsTemplate findingsTemplate2 = (FindingsTemplate) b;
					return StringUtils.equals(findingsTemplate1.getTitle(),
						findingsTemplate2.getTitle());
				}
				return false;
			}
		});
		
		viewer.setInput(model.getFindingsTemplates());
		viewer.setSelection(new StructuredSelection(selection));
		return composite;


	}
	
	/**
	 * Create contents of the button bar.
	 * 
	 * @param parent
	 */
	@Override
	protected void createButtonsForButtonBar(Composite parent){
		createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}
	
	@Override
	protected void buttonPressed(int buttonId){
		super.buttonPressed(buttonId);
	}
	
	@Override
	protected void okPressed(){
		selection = new ArrayList<>();
		StructuredSelection structuredSelection = (StructuredSelection) viewer.getSelection();
		for (Object o : structuredSelection.toArray()) {
			if (o instanceof FindingsTemplate) {
				selection.add((FindingsTemplate) o);
			}
		}
		
		super.okPressed();
	}
	
	public List<FindingsTemplate> getSelection(boolean asCopy){
		if (selection == null) {
			selection = Collections.emptyList();
		}
		if (asCopy) {
			List<FindingsTemplate> findingsTemplates = new ArrayList<>();
			for (FindingsTemplate findingsTemplate : selection) {
				findingsTemplates.add(EcoreUtil.copy(findingsTemplate));
			}
			return findingsTemplates;
		}
		return selection;
	}
	
	public FindingsTemplate getSingleSelection(boolean asCopy){
		List<FindingsTemplate> findingsTemplates = getSelection(asCopy);
		if (findingsTemplates.size() > 0) {
			return findingsTemplates.get(0);
		}
		return null;
	}
}
