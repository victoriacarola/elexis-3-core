package ch.elexis.core.ui.laboratory.controls;

import java.text.FieldPosition;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.FocusCellOwnerDrawHighlighter;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerFocusCellManager;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ILineSeries;
import org.eclipse.swtchart.ISeries.SeriesType;
import org.eclipse.swtchart.LineStyle;
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
import ch.elexis.core.ui.laboratory.controls.util.DisplayDoubleClickListener;
import ch.elexis.core.ui.laboratory.controls.util.LabResultEditingSupport;
import ch.elexis.core.ui.laboratory.controls.util.LaborResultsLabelProvider;
import ch.elexis.core.ui.laboratory.views.LaborView;
import ch.elexis.data.LabResult;
import ch.elexis.data.Patient;
import ch.elexis.data.Person;
import ch.rgw.tools.TimeTool;

public class LaborResultsComposite extends Composite {

	public static final String ID = "ch.elexis.LaborResultsComposite"; //$NON-NLS-1$

	private CheckboxTreeViewer checkboxViewer;
	private final FormToolkit tk = UiDesk.getToolkit();
	private Form form;
	private Patient actPatient;
	private List<Object> selectedItems = new ArrayList<>();
	private TreeViewer viewer;
	private TreeViewerFocusCellManager focusCell;
	private LinkedList<TimeTool> lastSixDates = new LinkedList<>();

	private TreeViewerColumn newColumn;
	private int newColumnIndex;

	public final static String COLUMN_DATE_KEY = "labresult.date"; //$NON-NLS-1$
	private List<TreeViewerColumn> resultColumns = new ArrayList<TreeViewerColumn>();

	private LaborResultsContentProvider contentProvider = new LaborResultsContentProvider();
	private Shell chartPopup;

	private int columnOffset = 0;

	private boolean reloadPending;
	private static final int COLUMNS_PER_PAGE = 7;
	private List<LaborItemResults> selectedResults = new ArrayList<>();
	private List<LaborItemResults> items;
	private AtomicInteger selectedCount;
	private LaborView parentLaborView;
	private boolean showCheckboxes = false;

	public void setItems(List<LaborItemResults> items) {
		this.items = items;
	}

	public Object[] getElements(Object inputElement) {
		return items.toArray();
	}

	public LaborResultsComposite(Composite parent, int style, LaborView parentLaborView) {
		super(parent, style);
		this.parentLaborView = parentLaborView;
		createContent();
	}

	public void addNewDate(TimeTool newDate) {
		if (lastSixDates.size() == 6) {
			lastSixDates.removeFirst();
		}
		lastSixDates.add(newDate);
		Collections.sort(lastSixDates);
	}

	protected String generateChartContent() {
		// TODO Auto-generated method stub
		return null;
	}

	public int getColumnOffset() {
		return columnOffset;
	}

	public void setColumnOffset(int newOffset) {
		List<TimeTool> dates = contentProvider.getDates();
		if (dates.size() <= COLUMNS_PER_PAGE) {
			columnOffset = 0;
		} else {
			if ((newOffset + COLUMNS_PER_PAGE <= dates.size()) && (newOffset >= 0)) {
				columnOffset = newOffset;
			}
		}
	}

	private void setInitialColumnOffset() {
		List<TimeTool> dates = contentProvider.getDates();
		int offset = dates.size() - COLUMNS_PER_PAGE;
		if (offset > 0) {
			setColumnOffset(offset);
		} else {
			setColumnOffset(0);
		}
	}

	private void createContent() {
		setLayout(new GridLayout());
		form = tk.createForm(this);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite body = form.getBody();
		body.setLayout(new GridLayout());

		CheckboxTreeViewer checkboxViewer = new CheckboxTreeViewer(body,
				SWT.FULL_SELECTION | SWT.LEFT | SWT.V_SCROLL | SWT.H_SCROLL);
		viewer = checkboxViewer;
		checkboxViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
		checkboxViewer.getTree().setHeaderVisible(true);
		checkboxViewer.getTree().setLinesVisible(true);

		AtomicInteger selectedCount = new AtomicInteger(0);

		checkboxViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				boolean hasChildren = ((ITreeContentProvider) checkboxViewer.getContentProvider()).hasChildren(element);

				if (hasChildren) {
					checkboxViewer.setChecked(element, false);
				}
				if (event.getChecked()) {
					selectedCount.incrementAndGet();
					if (selectedCount.get() > 5) {
						selectedCount.decrementAndGet();

						event.getCheckable().setChecked(event.getElement(), false);

						MessageDialog.openInformation(getShell(), "Checkbox Limit",
								"Es dürfen maximal 5 Parameter gleichzeitig verglichen werden");
					} else {
						selectedItems.add(event.getElement());

						sendSelectedResultsToLaborComparComposite();
					}
				} else {
					selectedCount.decrementAndGet();
					selectedItems.remove(event.getElement());

					sendSelectedResultsToLaborComparComposite();
				}
			}
		});

		checkboxViewer.setContentProvider(contentProvider);

		focusCell = new TreeViewerFocusCellManager(checkboxViewer, new FocusCellOwnerDrawHighlighter(checkboxViewer));
		checkboxViewer.addDoubleClickListener(new DisplayDoubleClickListener(this));

		final MenuManager mgr = new MenuManager();
		mgr.setRemoveAllWhenShown(true);
		mgr.addMenuListener(new IMenuListener() {
			@Override
			public void menuAboutToShow(IMenuManager manager) {
				List<LabResult> results = getSelectedResults();
				if (results != null) {
					mgr.add(new LaborResultSetPathologicAction(results, checkboxViewer));
					mgr.add(new LaborResultSetNonPathologicAction(results, checkboxViewer));
					mgr.add(new LaborResultEditDetailAction(results, checkboxViewer));
					mgr.add(new LaborResultOrderDeleteAction(results, checkboxViewer));
					mgr.add(new Separator());
					mgr.add(new LaborParameterEditAction(results, checkboxViewer));
				}
			}
		});

		checkboxViewer.getControl().setMenu(mgr.createContextMenu(checkboxViewer.getControl()));

		TreeViewerColumn column = new TreeViewerColumn(viewer, SWT.NONE);
		column.getColumn().setWidth(200);
		column.getColumn().setText(Messages.Core_Parameter);
		column.setLabelProvider(new ColumnLabelProvider() {
			private StringBuilder sb = new StringBuilder();

			@Override
			public String getText(Object element) {
				if (element instanceof String) {
					String groupName = (String) element;
					if ((groupName.length() > 2) && groupName.charAt(1) == ' ') {
						groupName = groupName.substring(2);
					}
					return groupName;
				} else if (element instanceof LaborItemResults) {
					sb.setLength(0);
					ILabItem item = ((LaborItemResults) element).getFirstResult().getItem();
					sb.append(item.getKuerzel()).append(" - ").append(item.getName()).append(" [") //$NON-NLS-1$ //$NON-NLS-2$
							.append(item.getUnit()).append("]"); //$NON-NLS-1$
					return sb.toString();
				}
				return StringUtils.EMPTY;
			}
		});
		addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				resetCheckboxSelection();
			}
		});
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

		newColumn = new TreeViewerColumn(viewer, SWT.NONE);
		newColumn.getColumn().setWidth(5);
		TimeTool now = new TimeTool();
		newColumn.getColumn().setData(COLUMN_DATE_KEY, now);
		newColumn.getColumn().setText("Neu (" + now.toString(TimeTool.DATE_GER) + ")");
		newColumn.getColumn().addSelectionListener(new ChangeNewDateSelection(newColumn, this));
		newColumn.setLabelProvider(new LaborResultsLabelProvider(newColumn));
		newColumn.setEditingSupport(new LabResultEditingSupport(this, viewer, newColumn));
		newColumnIndex = 2;

		for (int i = 0; i < COLUMNS_PER_PAGE; i++) {
			column = new TreeViewerColumn(viewer, SWT.NONE);
			column.getColumn().setWidth(75);
			column.getColumn().setText(StringUtils.EMPTY);
			column.setLabelProvider(new LaborResultsLabelProvider(column));
			column.getColumn().addSelectionListener(new ChangeResultsDateSelection(column, this));
			resultColumns.add(column);
		}
		addTreeMouseTrackListener();
		setupTreeViewer();
		addTreeMouseTrackListener();
	}

	private void setupTreeViewer() {
	}

	private void addTreeMouseTrackListener() {
		viewer.getTree().addMouseTrackListener(new MouseTrackAdapter() {
			@Override
			public void mouseHover(MouseEvent e) {
				Point point = new Point(e.x, e.y);
				TreeItem item = viewer.getTree().getItem(point);
				if (item != null && item.getData() instanceof LaborItemResults) {
					createChartPopup(item, e);
				}
			}

			@Override
			public void mouseExit(MouseEvent e) {
				if (chartPopup != null && !chartPopup.isDisposed()) {
					chartPopup.dispose();
					chartPopup = null;
				}
			}
		});
	}

	private void createChartPopup(TreeItem item, MouseEvent e) {
		if (chartPopup != null && !chartPopup.isDisposed()) {
			chartPopup.dispose();
		}

		Shell parentShell = viewer.getControl().getShell();
		chartPopup = new Shell(parentShell, SWT.NO_TRIM | SWT.TOOL | SWT.ON_TOP);

		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		chartPopup.setLayout(gridLayout);
		Chart chart = new Chart(chartPopup, SWT.NONE);
		chart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		chart.getTitle().setText("7 zuletzt getestete Werte");
		chart.getAxisSet().getXAxis(0).getTitle().setText("Datum");
		chart.getAxisSet().getYAxis(0).getTitle().setText("Wert");
		LaborItemResults laborItemResults = (LaborItemResults) item.getData();
		List<LabResult> labResults = new ArrayList<>(laborItemResults.getAllResults());
		configureChart(chart, labResults);

		chartPopup.setSize(400, 300); // Passen Sie diese Werte an Ihre Bedürfnisse an

		Point location = parentShell.getDisplay().map(viewer.getControl(), null, e.x, e.y);
		chartPopup.setLocation(location.x + 20, location.y + 20); // Anpassen der Position des Popups

		chartPopup.open();
	}

	private void configureChart(Chart chart, List<LabResult> labResults) {
		labResults.sort(Comparator.comparing(LabResult::getDate));
		int startIndex = Math.max(0, labResults.size() - 7);
		List<LabResult> lastSevenResults = labResults.subList(startIndex, labResults.size());
		System.out.println(labResults);
		List<Date> xSeriesList = new ArrayList<>();
		List<Double> ySeriesList = new ArrayList<>();
		List<String> rSeriesList = new ArrayList<>();

		SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
		for (LabResult result : lastSevenResults) {

			try {
				Date date = dateFormat.parse(result.getDate());
				xSeriesList.add(date);
				ySeriesList.add(Double.parseDouble(result.getResult()));
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		}

		chart.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {

				GC gc = e.gc;
				double referenzwert = getReferenzwertFürAktuellenParameter();
				int yKoordinate = berechneYKoordinateAusReferenzwert(referenzwert, chart);
				gc.setForeground(e.display.getSystemColor(SWT.COLOR_RED));
			}
		});
		double[] xSeries = xSeriesList.stream().mapToDouble(Date::getTime).toArray();
		double[] ySeries = ySeriesList.stream().mapToDouble(Double::doubleValue).toArray();

		double[] xSeries2 = { xSeries[0], xSeries[xSeries.length - 1] };
		double[] ySeries2 = { 85, 85 };

		ILineSeries lineSeries2 = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Referenzwert");
		lineSeries2.setYSeries(ySeries2);
		lineSeries2.setXSeries(xSeries2);
		lineSeries2.setLineStyle(LineStyle.SOLID);
		lineSeries2.setLineColor(new Color(255, 0, 0, 50));
		lineSeries2.setLineWidth(200);
		ILineSeries lineSeries = (ILineSeries) chart.getSeriesSet().createSeries(SeriesType.LINE, "Parameterwert");
		lineSeries.setYSeries(ySeries);
		lineSeries.setXSeries(xSeries);
		lineSeries.setLineStyle(LineStyle.SOLID);

		chart.getAxisSet().getXAxis(0).getTick().setFormat(new Format() {
			@Override
			public StringBuffer format(Object obj, StringBuffer toAppendTo, FieldPosition pos) {
				long value = ((Number) obj).longValue();
				Date date = new Date(value);
				return dateFormat.format(date, toAppendTo, pos);
			}

			@Override
			public Object parseObject(String source, ParsePosition pos) {
				return null;
			}
		});

		chart.getAxisSet().adjustRange();

	}

	protected int berechneYKoordinateAusReferenzwert(double referenzwert, Chart chart) {
		// TODO Auto-generated method stub
		return 0;
	}

	protected double getReferenzwertFürAktuellenParameter() {
		// TODO Auto-generated method stub
		return 0;
	}

	public List<LabResult> getSelectedResults() {
		ViewerCell cell = focusCell.getFocusCell();
		return getSelectedResults(cell);
	}

	public List<LaborItemResults> getSelectedResult() {
		return selectedResults;
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

	private void sendSelectedResultsToLaborComparComposite() {
		updateSelectedResults();
		if (parentLaborView != null) {
			parentLaborView.receiveSelectedResultsFromResultsComposite(selectedResults);
		}
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
		viewer.getContentProvider().inputChanged(viewer, null, LabResult.getGrouped(actPatient));

		for (int i = 0; i < resultColumns.size(); i++) {
			resultColumns.get(i).getColumn().setData(COLUMN_DATE_KEY, null);
			resultColumns.get(i).getColumn().setText(StringUtils.EMPTY);
		}

		List<TimeTool> dates = contentProvider.getDates();
		for (int i = 0; i < dates.size() && i < resultColumns.size() && ((i + columnOffset) < dates.size()); i++) {
			resultColumns.get(i).getColumn().setText(dates.get(i + columnOffset).toString(TimeTool.DATE_GER));
			resultColumns.get(i).getColumn().setData(COLUMN_DATE_KEY, dates.get(i + columnOffset));
		}
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

		setInitialColumnOffset();
		for (int i = 0; i < resultColumns.size(); i++) {
			resultColumns.get(i).getColumn().setData(COLUMN_DATE_KEY, null);
			resultColumns.get(i).getColumn().setText(StringUtils.EMPTY);
		}

		List<TimeTool> dates = contentProvider.getDates();
		for (int i = 0; i < dates.size() && i < resultColumns.size(); i++) {
			resultColumns.get(i).getColumn().setText(dates.get(i + columnOffset).toString(TimeTool.DATE_GER));
			resultColumns.get(i).getColumn().setData(COLUMN_DATE_KEY, dates.get(i + columnOffset));
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

	private void updateSelectedResults() {
		selectedResults.clear();
		for (Object item : selectedItems) {
			if (item instanceof LaborItemResults) {
				selectedResults.add((LaborItemResults) item);
			}
		}
	}

	private void resetCheckboxSelection() {
		if (viewer instanceof CheckboxTreeViewer) {
			CheckboxTreeViewer checkboxViewer = (CheckboxTreeViewer) viewer;
			checkboxViewer.setAllChecked(false);

			selectedCount.set(0);
			selectedItems.clear();
			viewer.refresh();
		}

//	shell popup/Mousehover----------------------------------------------------------------------------------------------->

		viewer.getTree().addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseHover(MouseEvent e) {
				Point point = new Point(e.x, e.y);
				TreeItem item = viewer.getTree().getItem(point);
				if (item != null) {
					// Popup mit Chart anzeigen, basierend auf den Daten des ausgewählten TreeItem
					createChartPopup(item, e);
				}
			}

		});

		chartPopup.addFocusListener(new FocusAdapter() {
			@Override
			public void focusLost(FocusEvent e) {
				if (chartPopup != null && !chartPopup.isDisposed()) {
					chartPopup.dispose();
				}
			}
		});
	}

}