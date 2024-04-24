package ch.elexis.core.ui.laboratory.controls;

import java.util.Calendar;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
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
import ch.elexis.data.Patient;

public class LaborCompareComposite extends Composite {

	private Form form;
	private final FormToolkit tk = UiDesk.getToolkit();

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

		body.layout();
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
