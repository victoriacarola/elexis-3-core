package ch.elexis.core.ui.laboratory.controls;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
import org.eclipse.swtchart.Range;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.laboratory.controls.model.LaborItemResults;
import ch.elexis.core.ui.laboratory.controls.util.LaborItemResultsComparator;
import ch.elexis.data.LabResult;
import ch.elexis.data.Patient;
import ch.rgw.tools.TimeTool;

public class LaborCompareComposite extends Composite {
	private HashMap<String, HashMap<String, HashMap<String, List<LabResult>>>> grouped;
	private ArrayList<String> groups = new ArrayList<>();
	private Form form;
	private final FormToolkit tk = UiDesk.getToolkit();
	private HashMap<String, LaborItemResults> itemResults = new HashMap<>();

	private HashSet<String> dates = new HashSet<>();

	public List<TimeTool> getDates() {
		ArrayList<TimeTool> ret = new ArrayList<>();
		for (String date : dates) {
			ret.add(new TimeTool(date));
		}
		Collections.sort(ret);
		return ret;
	}
	public LaborCompareComposite(CTabFolder tabFolder, int style) {
		super(tabFolder, style);
		createContent();
		tabFolder.setLayout(new FillLayout());
		tabFolder.layout();
	}

	public LaborCompareComposite(Composite parent, int style) {
		super(parent, style);
		createContent();
		parent.setLayout(new FillLayout());
		parent.layout();
	}

	private void createContent() {
	    setLayout(new GridLayout());
	    form = tk.createForm(this);
	    form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite body = form.getBody();
		body.setLayout(new GridLayout());

		Label fromLabel = new Label(body, SWT.LEFT);
		fromLabel.setText("Von:");
		DateTime fromDate = new DateTime(body, SWT.DATE | SWT.DROP_DOWN);

		Label toLabel = new Label(body, SWT.RIGHT);
		toLabel.setText("Bis:");
		DateTime toDate = new DateTime(body, SWT.DATE | SWT.DROP_DOWN);

		fromDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Calendar now = Calendar.getInstance();
				Calendar selectedFrom = Calendar.getInstance();
				selectedFrom.set(fromDate.getYear(), fromDate.getMonth(), fromDate.getDay());
				if (selectedFrom.after(now)) {
					MessageBox messageBox = new MessageBox(body.getShell(), SWT.ICON_WARNING | SWT.OK);
					messageBox.setMessage("Das 'Von' Datum darf nicht nach dem heutigen Datum liegen.");
					messageBox.open();
					fromDate.setDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
				}
			}
		});

		toDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Calendar now = Calendar.getInstance();
				Calendar selectedTo = Calendar.getInstance();
				selectedTo.set(toDate.getYear(), toDate.getMonth(), toDate.getDay());
				if (selectedTo.before(getDateFromDateTime(fromDate))) {
					MessageBox messageBox = new MessageBox(body.getShell(), SWT.ICON_WARNING | SWT.OK);
					messageBox.setMessage("Das 'Bis' Datum darf nicht vor dem 'Von' Datum liegen.");
					messageBox.open();
					toDate.setDate(fromDate.getYear(), fromDate.getMonth(), fromDate.getDay());
				} else if (selectedTo.after(now)) {
					MessageBox messageBox = new MessageBox(body.getShell(), SWT.ICON_WARNING | SWT.OK);
					messageBox.setMessage("Das 'Bis' Datum darf nicht nach dem heutigen Datum liegen.");
					messageBox.open();
					toDate.setDate(now.get(Calendar.YEAR), now.get(Calendar.MONTH), now.get(Calendar.DAY_OF_MONTH));
				}
			}
		});

		ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
		Action closeAction = new Action("Schließen", SWT.NONE) {
			@Override
			public void run() {
				CTabFolder tabFolder = (CTabFolder) getParent();
				CTabItem[] items = tabFolder.getItems();
				for (CTabItem item : items) {
					if (item.getControl() == LaborCompareComposite.this) {
						item.dispose();
						break;
					}
				}
			}
		};
		closeAction.setImageDescriptor(UiDesk.getImageDescriptor(UiDesk.IMG_DELETE));
		toolBarManager.add(closeAction);
		toolBarManager.createControl(form.getHead());

		Button applyButton = new Button(body, SWT.PUSH);
		applyButton.setText("Aktualisieren");
		

		Chart chart = new Chart(body, SWT.NONE);
		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chart.getTitle().setText("Werte Vergleich");
		chart.getAxisSet().getXAxis(0).getTitle().setText("Datum");
		chart.getAxisSet().getYAxis(0).getTitle().setText("Wert");

		ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Sample Series");
		lineSeries.setLineStyle(LineStyle.SOLID);
		lineSeries.setLineWidth(2);
		lineSeries.setAntialias(SWT.ON);

		double[] ySeries = { 0.1, 0.2, 0.0, 0.3, 0.2, 0.1 };
		double[] xSeries = { 1, 2, 3, 4, 5, 6 };
		lineSeries.setXSeries(xSeries);
		lineSeries.setYSeries(ySeries);

		double xMin = 1 - 0.5;
		double xMax = 6 + 0.5;
		double yMin = 0.0 - 0.05;
		double yMax = 0.3 + 0.05;

		chart.getAxisSet().getXAxis(0).setRange(new Range(xMin, xMax));
		chart.getAxisSet().getYAxis(0).setRange(new Range(yMin, yMax));

		chart.getAxisSet().adjustRange();
	}


	public Object[] getElements(Object inputElement) {
		return groups.toArray();
	}

	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof String) {
			HashMap<String, HashMap<String, List<LabResult>>> itemMap = grouped.get(parentElement);
			ArrayList<LaborItemResults> ret = new ArrayList<>();
			for (String item : itemMap.keySet()) {
				if (itemResults.get(parentElement + "::" + item) != null) { //$NON-NLS-1$
					ret.add(itemResults.get(parentElement + "::" + item)); //$NON-NLS-1$
				}
			}
			Collections.sort(ret, new LaborItemResultsComparator());
			return ret.toArray();
		}
		return null;
	}

	public Object getParent(Object element) {
		return null;
	}

	public boolean hasChildren(Object element) {
		if (element instanceof String) {
			return grouped.get(element) != null && !grouped.get(element).isEmpty();
		}
		return false;
	}
	public void updateChart(ComparisonData data) {
		double[] values = new double[] { data.getValue1(), data.getValue2(), data.getValue3(), data.getValue4(),
				data.getValue5() };
	}

	private Calendar getDateFromDateTime(DateTime dateTime) {
	    Calendar cal = Calendar.getInstance();
	    cal.set(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay());
	    return cal;
	}

	public void reload() {
		// TODO Auto-generated method stub

	}

	public void selectPatient(Patient asPersistentObject) {
		// TODO Auto-generated method stub

	}

}
