package ch.elexis.core.ui.laboratory.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.ISeries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import ch.elexis.core.ui.laboratory.controls.LaborResultsComposite;
import ch.elexis.core.ui.laboratory.controls.model.LaborItemResults;

public class LaborResultsCompositeTest {

	@InjectMocks
	private LaborResultsComposite composite;

	@Mock
	private TreeItem treeItem;

	@Mock
	private Shell shell;

	@Mock
	private Chart chart;

	@BeforeEach
	public void setup() {
		composite.init();
		when(treeItem.getData()).thenReturn(new LaborItemResults());
		when(composite.viewer.getControl().getShell()).thenReturn(shell);
	}

	@AfterEach
	public void tearDown() {
		reset(treeItem, shell, chart);

	}

	@Test
	public void testCreateChartPopupOnMouseHover() {
		MouseEvent mockEvent = mock(MouseEvent.class);
		mockEvent.x = 10;
		mockEvent.y = 20;

		composite.mouseHover(mockEvent);

		verify(composite, times(1)).createChartPopup(eq(treeItem), any());

		verify(composite, times(1)).configureChart(any(Chart.class), anyList());

		ArgumentCaptor<ISeries> seriesCaptor = ArgumentCaptor.forClass(ISeries.class);
		verify(chart).getSeriesSet().createSeries(eq(ISeries.SeriesType.LINE), eq("Parameterwert"));
		ISeries series = seriesCaptor.getValue();
		assertNotNull(series, "The series should not be null");
	}
}
