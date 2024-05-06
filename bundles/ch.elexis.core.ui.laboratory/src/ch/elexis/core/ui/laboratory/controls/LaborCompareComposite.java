/*
 *@author  Victoria Carola Adolph
 * @version 1.0
 * @since   06.05.2024
 * 
 * Diese Klasse beinhaltet die Histogramm Ansicht. 
 * Diese dient dazu die getesteten Laborwerte des Patienten in einem Diagramm zu veranschlichen 
 * und diese auf eine bestimmte Zeitspanne miteinander vergleichen zu können.
 * 
 * Funktionen:
 * - Kalender-Widgets zur Festlegung eines Start- und Enddatums für den Vergleich.
 * - Schaltfläche zur Aktualisierung des Diagramms mit den ausgewählten Daten.
 * - Liniendiagramm zur anschaulichen Darstellung von Laborergebnissen und deren Entwicklung.
 * 
 * Diese Klasse umfasst Methoden für:
 * - Die Erstellung und Handhabung von Kalender-Widgets.
 * - Die Reaktion auf Benutzeraktionen, wie Datumsauswahl und Diagrammaktualisierung.
 * - Die Aktualisierung des Liniendiagramms anhand der gewählten Laborergebnisse.
 * - Das Abrufen ausgewählter Ergebnisse zur Weiterverarbeitung.
 * 
 * Externe Komponenten:
 * - SWT-Widgets wie Tree, Button und DateTime.
 * - SWTChart für Diagramm-Visualisierung.
 * - JFace TreeViewer für baumbasierte Darstellung.
 * 
 * @see ch.elexis.core.ui.laboratory.controls.model.LaborItemResults
 * @see ch.elexis.data.LabResult
 */

package ch.elexis.core.ui.laboratory.controls;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParsePosition;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.ISeriesSet;
import org.eclipse.ui.forms.widgets.FormToolkit;

import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.laboratory.controls.model.LaborItemResults;
import ch.elexis.data.LabResult;
import ch.rgw.tools.TimeTool;

public class LaborCompareComposite extends Composite {

	public static final String COLUMN_DATE_KEY = "labresult.date"; //$NON-NLS-1$
	{
	}

	public static final String ID = "ch.elexis.LaborCompareComposite"; //$NON-NLS-1$
	private Button updateChartsButton;
	private TreeViewer viewer;
	private final FormToolkit tk = UiDesk.getToolkit();
	private DateTime fromDate;
	private DateTime toDate;
	private TreeViewerFocusCellManager focusCell;
	private List<LaborItemResults> selectedItems = new ArrayList<>();
	private TreeViewerColumn newColumn;
	private List<TreeViewerColumn> resultColumns = new ArrayList<TreeViewerColumn>();
	private List<LaborItemResults> Laborwert;
	private Chart chart;

	public LaborCompareComposite(Composite parent, int style) {
		super(parent, style);
		setLayout(new GridLayout());
		Composite calendarButtonComposite = new Composite(this, SWT.NONE);
		GridLayout calendarButtonLayout = new GridLayout(2, false);
		calendarButtonLayout.marginWidth = 47;
		calendarButtonLayout.marginTop = 20;
		calendarButtonLayout.marginBottom = -5;
		calendarButtonComposite.setLayout(calendarButtonLayout);
		calendarButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		createCalendar(calendarButtonComposite);

		createUpdateChartsButton(calendarButtonComposite);

		createLineChart();

	}

//	CALENDAR---------------------------------------------------------------------------------------------------->

	private void createCalendar(Composite parent) {
		Composite calendarComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(2, false);
		calendarComposite.setLayout(layout);
		calendarComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, false, false));

		fromDate = new DateTime(calendarComposite, SWT.DATE | SWT.DROP_DOWN);
		toDate = new DateTime(calendarComposite, SWT.DATE | SWT.DROP_DOWN);

		fromDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		toDate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		fromDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				LocalDate from = getDate(fromDate);
				LocalDate to = getDate(toDate);
				LocalDate currentDate = LocalDate.now();

				if (from.isAfter(to)) {
					setDate(fromDate, to);
					showMessage("Das Startdatum kann nicht später als das Enddatum sein.");

				}
				if (to.isAfter(currentDate)) {
					setDate(toDate, currentDate);
					showMessage("Das Enddatum kann nicht in der Zukunft liegen.");
				}
			}
		});
		toDate.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				LocalDate from = getDate(fromDate);
				LocalDate to = getDate(toDate);
				LocalDate currentDate = LocalDate.now();

				if (from.isAfter(to)) {
					setDate(fromDate, to);
					showMessage("Das Startdatum kann nicht später als das Enddatum sein.");
				}

				if (to.isAfter(currentDate)) {
					setDate(toDate, currentDate);
					showMessage("Das Enddatum kann nicht in der Zukunft liegen.");
				}
			}
		});
	}

	private void showMessage(String message) {
		MessageBox messageBox = new MessageBox(fromDate.getShell(), SWT.OK | SWT.ICON_INFORMATION);
		messageBox.setMessage(message);
		messageBox.open();
	}

	private LocalDate getDate(DateTime dateTime) {
		int day = dateTime.getDay();
		int month = dateTime.getMonth();
		int year = dateTime.getYear();
		return LocalDate.of(year, month + 1, day);
	}

	private void setDate(DateTime dateTime, LocalDate date) {
		dateTime.setDate(date.getYear(), date.getMonthValue() - 1, date.getDayOfMonth());
	}

//	BUTTON--------------------------------------------------------------------------------------------------------->

	private void createUpdateChartsButton(Composite parent) {
		updateChartsButton = new Button(parent, SWT.PUSH | SWT.FLAT);
		updateChartsButton.setText("Diagramm aktualisieren");
		GridData gridData = new GridData(SWT.END, SWT.CENTER, false, false);
		gridData.widthHint = 150;
		gridData.heightHint = 25;
		updateChartsButton.setLayoutData(gridData);

		updateChartsButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				updateCharts();
			}
		});
	}

	private void updateCharts() {
		updateLineChartWithSelectedResults();
	}

//	LINECHART ---------------------------------------------------------------------------------------------->

	private void createLineChart() {
		chart = new Chart(this, SWT.NONE);
		chart.getTitle().setText(
				"Gewünschte Datumsspanne aktualisieren um Werte zu vergleichen.");
		chart.getAxisSet().getXAxis(0).getTitle().setText("Datum");
		chart.getAxisSet().getYAxis(0).getTitle().setText("Wert");

		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chart.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				updateChartData(selectedItems);
			}
		});
		chart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == SWT.BUTTON3) {
				}
			}
		});
		this.layout();
	}

	public void updateChartData(List<LaborItemResults> selectedItems2) {
		if (chart != null && !selectedItems.isEmpty()) {
			updateLineChartWithSelectedResults();
		}
	}

	private void updateLineChartWithSelectedResults() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
		LocalDate fromDate = getDate(this.fromDate);
		LocalDate toDate = getDate(this.toDate);

		ISeriesSet seriesSet = chart.getSeriesSet();
		Color[] colors = new Color[] { new Color(chart.getDisplay(), 255, 0, 0),
				new Color(chart.getDisplay(), 0, 255, 0), new Color(chart.getDisplay(), 0, 0, 255),
				new Color(chart.getDisplay(), 255, 255, 0), new Color(chart.getDisplay(), 128, 0, 128), };

		ISeries[] oldSeries = seriesSet.getSeries();
		for (ISeries series : oldSeries) {
			seriesSet.deleteSeries(series.getId());
		}
		int colorIndex = 0;
		for (LaborItemResults itemResult : selectedItems) {
			List<DateValuePair> pairs = new ArrayList<>();
			for (LabResult result : itemResult.getAllResults()) {
				LocalDate date = LocalDate.parse(result.getDate(), formatter);
				if ((date.isAfter(fromDate) || date.isEqual(fromDate))
						&& (date.isBefore(toDate) || date.isEqual(toDate))) {
					try {
						double value = Double.parseDouble(result.getResult());
						pairs.add(new DateValuePair(date, value));
					} catch (NumberFormatException e) {
					}
				}
			}

			Collections.sort(pairs);

			double[] xSeries = new double[pairs.size()];
			double[] ySeries = new double[pairs.size()];
			String[] dateLabels = new String[pairs.size()];

			for (int i = 0; i < pairs.size(); i++) {
				DateValuePair pair = pairs.get(i);
				xSeries[i] = i;
				ySeries[i] = pair.getValue();
				dateLabels[i] = pair.getDate().format(formatter);
			}

			if (pairs.size() > 1) {
				ILineSeries lineSeries = (ILineSeries) seriesSet.createSeries(SeriesType.LINE,
						"Serie " + itemResult.getItem());
				lineSeries.setXSeries(xSeries);
				lineSeries.setYSeries(ySeries);
				lineSeries.setLineColor(colors[colorIndex % colors.length]);
				lineSeries.setSymbolType(ILineSeries.PlotSymbolType.CIRCLE);
			} else if (pairs.size() == 1) {
				ILineSeries lineSeries = (ILineSeries) seriesSet.createSeries(SeriesType.LINE,
						"Serie " + itemResult.getItem());
				lineSeries.setXSeries(xSeries);
				lineSeries.setYSeries(ySeries);
				lineSeries.setSymbolType(ILineSeries.PlotSymbolType.CIRCLE);
				lineSeries.setSymbolSize(4);
				lineSeries.setSymbolColor(colors[colorIndex % colors.length]);
				lineSeries.setLineColor(colors[colorIndex % colors.length]); 
			}
			chart.getAxisSet().getXAxis(0).enableCategory(true);
			chart.getAxisSet().getXAxis(0).setCategorySeries(dateLabels);
			chart.getAxisSet().getXAxis(0).getTick().setFormat(new LabelFormat());
			colorIndex++;
		}

		chart.getAxisSet().getXAxis(0).adjustRange();
		chart.getAxisSet().getYAxis(0).adjustRange();
		chart.redraw();
	}


	public List<LabResult> getSelectedResults() {
		ViewerCell cell = focusCell.getFocusCell();
		return getSelectedResults(cell);
	}

	private List<LabResult> getSelectedResults(ViewerCell cell) {
		if (cell != null && cell.getColumnIndex() > 2) {
			TreeViewerColumn column = resultColumns.get(cell.getColumnIndex() - 3);
			TimeTool time = (TimeTool) column.getColumn().getData(COLUMN_DATE_KEY);
			if ((time != null) && (cell.getElement() instanceof LaborItemResults)) {
				LaborItemResults results = (LaborItemResults) cell.getElement();
				return results.getResult(time.toString(TimeTool.DATE_COMPACT));
			}
		}
		return null;
	}

	public void receiveSelectedResults(List<LaborItemResults> selectedResults) {
		this.Laborwert = new ArrayList<>(selectedResults);
		selectedItems.clear();
		if (selectedResults != null) {
			this.selectedItems.addAll(new ArrayList<>(Laborwert));
		}
		updateChartData(selectedItems);
	}

	public String[] getPrintHeaders() {
		ArrayList<String> ret = new ArrayList<String>();

		TreeColumn[] columns = viewer.getTree().getColumns();
		for (TreeColumn treeColumn : columns) {
			if (treeColumn != newColumn.getColumn()) {
				ret.add(treeColumn.getText());
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	public TreeItem[] getPrintRows() {
		ArrayList<TreeItem> ret = new ArrayList<TreeItem>();
		getAllItems(viewer.getTree(), ret);
		return ret.toArray(new TreeItem[ret.size()]);
	}

	private void getAllItems(Tree tree, List<TreeItem> allItems) {
		for (TreeItem item : tree.getItems()) {
			getAllItems(item, allItems);
		}
	}

	private void getAllItems(TreeItem currentItem, List<TreeItem> allItems) {
		TreeItem[] children = currentItem.getItems();
		for (int i = 0; i < children.length; i++) {
			allItems.add(children[i]);
			getAllItems(children[i], allItems);
		}
	}

	public List<LaborItemResults> setselectedItems(List<LaborItemResults> selectedItems) {
		return selectedItems;
	}

	public List<LaborItemResults> getSelectedItems() {
		return selectedItems;
	}

	class DateValuePair implements Comparable<DateValuePair> {
		private LocalDate date;
		private double value;

		public DateValuePair(LocalDate date, double value) {
			this.date = date;
			this.value = value;
		}

		public LocalDate getDate() {
			return date;
		}

		public double getValue() {
			return value;
		}

		@Override
		public int compareTo(DateValuePair o) {
			return this.date.compareTo(o.getDate());
		}
	}

	class LabelFormat extends Format {
		@Override
		public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
			int index = ((Number) obj).intValue();
			String label = "";
			return toAppendTo.append(label);
		}

		@Override
		public Object parseObject(String source, ParsePosition pos) {
			return null;
		}
	}
}