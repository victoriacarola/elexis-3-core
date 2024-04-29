package ch.elexis.core.ui.laboratory.controls;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
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
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;

import ch.elexis.core.data.interfaces.ILabItem;
import ch.elexis.core.ui.UiDesk;
import ch.elexis.core.ui.laboratory.actions.LaborParameterEditAction;
import ch.elexis.core.ui.laboratory.actions.LaborResultEditDetailAction;
import ch.elexis.core.ui.laboratory.actions.LaborResultOrderDeleteAction;
import ch.elexis.core.ui.laboratory.actions.LaborResultSetNonPathologicAction;
import ch.elexis.core.ui.laboratory.actions.LaborResultSetPathologicAction;
import ch.elexis.core.ui.laboratory.controls.model.LaborItemResults;
import ch.elexis.core.ui.laboratory.controls.util.ChangeNewDateSelection;
import ch.elexis.core.ui.laboratory.controls.util.ChangeResultsDateSelection;
import ch.elexis.core.ui.laboratory.controls.util.LaborResultsLabelProvider;
import ch.elexis.data.LabResult;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.rgw.tools.TimeTool;

public class LaborCompareComposite extends Composite {

	public static final String COLUMN_DATE_KEY = "labresult.date"; //$NON-NLS-1$
	{
	}

	public static final String ID = "ch.elexis.LaborCompareComposite"; //$NON-NLS-1$
	private Button updateChartsButton;
	private TreeViewer viewer;
	private final FormToolkit tk = UiDesk.getToolkit();
	private Form form;
	private DateTime fromDate;
	private DateTime toDate;
	private Patient actPatient;
	private TreeViewerFocusCellManager focusCell;
	private List<LaborItemResults> selectedItems = new ArrayList<>();

	private TreeViewerColumn newColumn;
	private int newColumnIndex;

	private List<TreeViewerColumn> resultColumns = new ArrayList<TreeViewerColumn>();

	private LaborResultsContentProvider contentProvider2 = new LaborResultsContentProvider();

	private int columnOffset = 0;

	private boolean reloadPending;

	private List<LaborItemResults> test66;
	private Chart chart;

	private static final int COLUMNS_PER_PAGE = 7;

	public LaborCompareComposite(Composite parent, int style) {
		super(parent, style);
		createContent();

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
					// If fromDate is after toDate, reset fromDate to toDate
					setDate(fromDate, to);
					showMessage("Das Startdatum kann nicht später als das Enddatum sein.");
					System.out.println("test1");

				}
				if (to.isAfter(currentDate)) {
					setDate(toDate, currentDate);
					showMessage("Das Enddatum kann nicht in der Zukunft liegen.");
					System.out.println("test2");
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
					System.out.println("test1");
				}

				if (to.isAfter(currentDate)) {
					setDate(toDate, currentDate);
					showMessage("Das Enddatum kann nicht in der Zukunft liegen.");
					System.out.println("test2");
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
		updateChartsButton.setText("Diagramme aktualisieren");
		GridData gridData = new GridData(SWT.END, SWT.CENTER, false, false);
		gridData.widthHint = 150;
		gridData.heightHint = 24;
		updateChartsButton.setLayoutData(gridData);

		updateChartsButton.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				e.gc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK)); // Setzen Sie die Linienfarbe
																							// auf Schwarz
				e.gc.setLineWidth(1);
				e.gc.drawRectangle(0, 0, updateChartsButton.getSize().x - 1, updateChartsButton.getSize().y - 1); // Zeichnen
																													// Sie
																													// das
																													// Rechteck
																													// als
																													// Rahmen
			}
		});

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

		chart.getTitle().setText("Linien- Histogramm");
		chart.getAxisSet().getXAxis(0).getTitle().setText("Zeit");
		chart.getAxisSet().getYAxis(0).getTitle().setText("Wert");

		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chart.addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				updateChartData(selectedItems);
				System.out.println("hallo");
			}
		});
		chart.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				if (e.button == SWT.BUTTON3) {
					System.out.println("Rechtsklick im LaborCompareComposite erkannt.");
				}
			}
		});
		this.layout();
	}

	public void updateChartData(List<LaborItemResults> selectedItems2) {
		System.out.println("test3");
		System.out.println("chart " + chart);
		System.out.println("selectedItems " + selectedItems);
		if (chart != null && !selectedItems.isEmpty()) {
			System.out.println("test4");
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

			if (series instanceof ILineSeries) {
				seriesSet.deleteSeries(series.getId());
			}
		}
		int colorIndex = 0;
		for (LaborItemResults itemResult : selectedItems) {
			List<DateValuePair> pairs = new ArrayList<>();
			for (LabResult result : itemResult.getAllResults()) {
				LocalDate date = LocalDate.parse(result.getDate(), formatter);
				if (date.isAfter(fromDate) && date.isBefore(toDate)) {
					try {
						double value = Double.parseDouble(result.getResult());
						pairs.add(new DateValuePair(date, value));
					} catch (NumberFormatException e) {
						System.out.println("Fehler beim Parsen des Wertes: " + result.getResult());
					}
				}
			}

			Collections.sort(pairs);

			double[] xSeries = pairs.stream().mapToDouble(p -> p.getDate().toEpochDay()).toArray();
			double[] ySeries = pairs.stream().mapToDouble(DateValuePair::getValue).toArray();

			ILineSeries lineSeries = (ILineSeries) seriesSet.createSeries(SeriesType.LINE,
					"Serie " + itemResult.getItem());
			lineSeries.setXSeries(xSeries);
			lineSeries.setYSeries(ySeries);

			lineSeries.setLineWidth(2);

			Color color = colors[colorIndex % colors.length];
			lineSeries.setLineColor(color);
			colorIndex++;
		}

		chart.getAxisSet().getXAxis(0).adjustRange();
		chart.getAxisSet().getYAxis(0).adjustRange();

		chart.redraw();
	}

//	Tabelle---------------------------------------------------------------------------------------------->

	private void createContent() {
		setLayout(new GridLayout());
		form = tk.createForm(this);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite body = form.getBody();
		body.setLayout(new GridLayout());

		viewer = new TreeViewer(body, SWT.FULL_SELECTION | SWT.LEFT | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		viewer.getTree().setHeaderVisible(true);
		viewer.getTree().setLinesVisible(true);

		LaborCompareContentProvider contentProvider = new LaborCompareContentProvider();
		viewer.setContentProvider(contentProvider);

		ColumnViewerToolTipSupport.enableFor(viewer, ToolTip.NO_RECREATE);

		focusCell = new TreeViewerFocusCellManager(viewer, new FocusCellOwnerDrawHighlighter(viewer));

		final MenuManager mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				List<LabResult> results = getSelectedResults();
				if (results != null) {
					mgr.add(new LaborResultSetPathologicAction(results, viewer));
					mgr.add(new LaborResultSetNonPathologicAction(results, viewer));
					mgr.add(new LaborResultEditDetailAction(results, viewer));
					mgr.add(new LaborResultOrderDeleteAction(results, viewer));
					mgr.add(new Separator());
					mgr.add(new LaborParameterEditAction(results, viewer));
				}
			}
		});

		viewer.getControl().setMenu(mgr.createContextMenu(viewer.getControl()));

//		PARAMETER COLLUMN ----------------------------------------------------------------------------------------------------------------
		TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(200);
		column.getColumn().setText(Messages.Core_Parameter);
		column.setLabelProvider(new ColumnLabelProvider() {
			private StringBuilder sb = new StringBuilder();

//			LaborItemResults kommen hier rein---------------------------->

			@Override
			public String getText(Object selectedItems) {
				if (selectedItems instanceof String) {
					String groupName = (String) selectedItems;
					if ((groupName.length() > 2) && groupName.charAt(1) == ' ') {
						groupName = groupName.substring(2);
					}
					return groupName;
				} else if (selectedItems instanceof LaborItemResults) {
					sb.setLength(0);
					ILabItem item = ((LaborItemResults) selectedItems).getFirstResult().getItem();
					sb.append(item.getKuerzel()).append(" - ").append(item.getName()).append(" [") //$NON-NLS-1$ //$NON-NLS-2$
							.append(item.getUnit()).append("]"); //$NON-NLS-1$
					return sb.toString();
				}
				return StringUtils.EMPTY;
			}
		});

//		REFERENZ COLLUMN ----------------------------------------------------------------------------------------------------------------
		column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(100);
		column.getColumn().setText(Messages.Core_Reference);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element instanceof LaborItemResults) {
					if (actPatient.getGeschlecht().equals(Person.MALE)) {
						return ((LaborItemResults) element).getFirstResult().getRefMale();
					} else {
						return ((LaborItemResults) element).getFirstResult().getRefFemale();
					}
				}
				return StringUtils.EMPTY;
			}
		});

//		NEU COLLUMN ----------------------------------------------------------------------------------------------------------------
		newColumn = new TreeViewerColumn(viewer, SWT.NONE);
		newColumn.getColumn().setWidth(5);
		TimeTool now = new TimeTool();
		newColumn.getColumn().setText("Neu (" + now.toString(TimeTool.DATE_GER) + ")");
		newColumn.getColumn().setData(COLUMN_DATE_KEY, now);
		newColumn.getColumn().addSelectionListener(new ChangeNewDateSelection(newColumn, this));
		newColumn.setLabelProvider(new LaborResultsLabelProvider(newColumn));
		newColumnIndex = 2;

//		DATUM COLLUMN ----------------------------------------------------------------------------------------------------------------
		for (int i = 0; i < COLUMNS_PER_PAGE; i++) {
			column = new TreeViewerColumn(viewer, SWT.NONE);
			column.getColumn().setWidth(75);
			column.getColumn().setText(StringUtils.EMPTY);
			column.setLabelProvider(new LaborResultsLabelProvider(column));
			column.getColumn().addSelectionListener(new ChangeResultsDateSelection(column, this));
			resultColumns.add(column);
		}

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

	private List<LaborItemResults> addSelectedItemToList(Object selectedItem) {
		return selectedItems;
	}

	public void receiveSelectedResults(List<LaborItemResults> selectedResults) {
		this.test66 = new ArrayList<>(selectedResults);
		selectedItems.clear();
		if (selectedResults != null) {
			this.selectedItems.addAll(new ArrayList<>(test66));
		}
		System.out.println("Nach dem Hinzufügen: " + this.selectedItems);

		viewer.refresh();
		updateChartData(selectedItems);
	}

	private void updateView() {
		viewer.setInput(this.selectedItems);
		viewer.refresh();
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

	/**
	 * Reload all content and update the Viewer
	 */
	public void reload() {
		if (selectedItems == null) {
			selectedItems = new ArrayList<>();
		}

		List<LabResult> selectedResults = new ArrayList<>();
		for (LaborItemResults selectedItem : selectedItems) {
			selectedResults.addAll(selectedItem.getResults());
		}

		viewer.setInput(selectedResults);


		viewer.refresh();
	}

	/**
	 * Reload all content and update the Viewer with the data of the Patient.
	 */
	public void selectPatient(Patient patient) {
		actPatient = patient;
		if (!isVisible()) {
			reloadPending = true;
			return;
		}
		setRedraw(false);
		viewer.setInput(LabResult.getGrouped(actPatient));

		TimeTool now = new TimeTool();
		newColumn.getColumn().setData(COLUMN_DATE_KEY, now);
		newColumn.getColumn().setText("Neu (" + now.toString(TimeTool.DATE_GER) + ")");

		for (int i = 0; i < resultColumns.size(); i++) {
			resultColumns.get(i).getColumn().setData(COLUMN_DATE_KEY, null);
			resultColumns.get(i).getColumn().setText(StringUtils.EMPTY);
		}

		viewer.expandAll();
		setRedraw(true);
	}

	@Override
	public boolean setFocus() {
		if (reloadPending) {
			selectPatient(actPatient);
			reloadPending = false;
		}
		return super.setFocus();
	}

	public Patient getPatient() {
		return actPatient;
	}

	public void expandAll() {
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.expandAll();
		}
	}

	public void collapseAll() {
		if (viewer != null && !viewer.getControl().isDisposed()) {
			viewer.collapseAll();
		}
	}

	public void toggleNewColumn() {
		if ((newColumn.getColumn().getWidth() > 10)) {
			newColumn.getColumn().setWidth(5);
		} else {
			newColumn.getColumn().setWidth(100);
		}
		viewer.refresh();
	}

	public int[] getSkipIndex() {
		int[] ret = new int[1];
		ret[0] = newColumnIndex;
		return ret;
	}

	class LaborCompareContentProvider implements ITreeContentProvider {
		private List<LaborItemResults> items = new ArrayList<>();

		public void setItems(List<LaborItemResults> items) {
			this.items = items;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return items.toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
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

}