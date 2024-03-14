package ch.elexis.core.pdfbox.ui.parts;

import java.awt.image.BufferedImage;
import java.awt.image.DirectColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.slf4j.LoggerFactory;

import ch.elexis.core.l10n.Messages;
import ch.elexis.core.pdfbox.ui.parts.handlers.PDFTextHighlighter;

public class PdfPreviewPartLoadHandler {

	private static ExecutorService loader = Executors.newSingleThreadExecutor();

	private final ScrolledComposite scrolledComposite;
	private final Composite previewComposite;

	private float scalingFactor;

	private Label headLabel;
	private int numberOfPages;

	private Image[] images;

	private PDDocument pdDocument;

	private String searchText;

	private static int currentPageNo;

	public PdfPreviewPartLoadHandler(InputStream pdfInputStream, Float scalingFactor, Composite previewComposite,
			ScrolledComposite scrolledComposite) {

		this.previewComposite = previewComposite;
		this.scrolledComposite = scrolledComposite;
		this.scalingFactor = scalingFactor != null ? scalingFactor : 1f;

		loader.submit(new LoaderRunnable(pdfInputStream));
	}

	protected void unloadDocument() throws IOException {
		if (pdDocument != null) {
			pdDocument.close();
			pdDocument = null;
		}
	}

	private class LoaderRunnable implements Runnable {

		private InputStream pdfInputStream;

		public LoaderRunnable(InputStream pdfInputStream) {
			this.pdfInputStream = pdfInputStream;
		}

		@Override
		public void run() {
			try {

				// cleanup existing controls, show user feedback
				previewComposite.getDisplay().syncExec(() -> {
					headLabel = new Label(previewComposite, SWT.None);
					headLabel.setText(Messages.PdfPreview_NoPDFSelected);
					previewComposite.layout(true, true);
					scrolledComposite.layout(true, true);
					scrolledComposite.setMinSize(previewComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));

					Control[] children = previewComposite.getChildren();
					for (Control control : children) {
						if (headLabel.equals(control)) {
							continue;
						}
						control.dispose();
					}
					previewComposite.layout(true, true);

				});

				// load pdf document if not already loaded
				if (pdDocument == null) {
					if (pdfInputStream != null) {
						pdDocument = PDDocument.load(pdfInputStream);
						pdfInputStream.close();
						if (StringUtils.isNotBlank(searchText)) {
							PDFTextHighlighter highlighter = new PDFTextHighlighter(pdDocument);
							highlighter.highlightSearchTextInPDF(searchText.toLowerCase());
						}
						numberOfPages = pdDocument.getNumberOfPages();
						images = new Image[numberOfPages];
					} else if (pdfInputStream == null && pdDocument == null) {
						return;
					}
				}
				// render pages and display
				PDFRenderer renderer = new PDFRenderer(pdDocument);
				for (int i = 0; i < numberOfPages; i++) {
					final int j = i;
					BufferedImage bufferedImage = renderer.renderImage(i, scalingFactor);
					ImageData imageData = convertToSWT(bufferedImage);
					images[j] = new Image(previewComposite.getDisplay(), imageData);

					previewComposite.getDisplay().asyncExec(() -> {
						if (j == 0) {
							// reuse initialLabel
							headLabel.setText(StringUtils.EMPTY);
							headLabel.setImage(images[j]);
							headLabel.addDisposeListener(dl -> images[j].dispose());
						} else {
							Label label = new Label(previewComposite, SWT.None);
							label.setImage(images[j]);
							label.addDisposeListener(dl -> images[j].dispose());
						}

						previewComposite.layout(true, true);
						scrolledComposite.layout(true, true);
						scrolledComposite.setMinSize(previewComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
						if (StringUtils.isNotBlank(searchText)) {
							centerContentHorizontally();
							centerContentOnPage(currentPageNo);
						}
					});
				}

			} catch (IOException e) {
				previewComposite.getDisplay().asyncExec(() -> {
					if (headLabel != null) {
						headLabel.dispose();
					}
					headLabel = new Label(previewComposite, SWT.None);
					headLabel.setText(e.getMessage());
					previewComposite.layout(true, true);
					scrolledComposite.layout(true, true);
					scrolledComposite.setMinSize(previewComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT));
				});

			}
		}
	}

	// Zoom
	public void changeScalingFactor(Float scalingFactor) {
		this.scalingFactor = scalingFactor;
		loader.submit(new LoaderRunnable(null));
	}

	public void close() {
		if (pdDocument != null) {
			try {
				pdDocument.close();
			} catch (IOException e) {
				LoggerFactory.getLogger(getClass()).warn("Excepton closing PDDocument", e);
			}
		}
	}

	@Override
	protected void finalize() throws Throwable {
		close();
	}

	/**
	 * Convert AWT BufferedImage to SWT ImageData
	 *
	 * @param bufferedImage
	 * @return
	 * @see https://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet156.java
	 */
	private ImageData convertToSWT(BufferedImage bufferedImage) {
		if (bufferedImage.getColorModel() instanceof DirectColorModel) {
			DirectColorModel colorModel = (DirectColorModel) bufferedImage.getColorModel();
			PaletteData palette = new PaletteData(colorModel.getRedMask(), colorModel.getGreenMask(),
					colorModel.getBlueMask());
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					int rgb = bufferedImage.getRGB(x, y);
					int pixel = palette.getPixel(new RGB((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF));
					data.setPixel(x, y, pixel);
					if (colorModel.hasAlpha()) {
						data.setAlpha(x, y, (rgb >> 24) & 0xFF);
					}
				}
			}
			return data;
		} else if (bufferedImage.getColorModel() instanceof IndexColorModel) {
			IndexColorModel colorModel = (IndexColorModel) bufferedImage.getColorModel();
			int size = colorModel.getMapSize();
			byte[] reds = new byte[size];
			byte[] greens = new byte[size];
			byte[] blues = new byte[size];
			colorModel.getReds(reds);
			colorModel.getGreens(greens);
			colorModel.getBlues(blues);
			RGB[] rgbs = new RGB[size];
			for (int i = 0; i < rgbs.length; i++) {
				rgbs[i] = new RGB(reds[i] & 0xFF, greens[i] & 0xFF, blues[i] & 0xFF);
			}
			PaletteData palette = new PaletteData(rgbs);
			ImageData data = new ImageData(bufferedImage.getWidth(), bufferedImage.getHeight(),
					colorModel.getPixelSize(), palette);
			data.transparentPixel = colorModel.getTransparentPixel();
			WritableRaster raster = bufferedImage.getRaster();
			int[] pixelArray = new int[1];
			for (int y = 0; y < data.height; y++) {
				for (int x = 0; x < data.width; x++) {
					raster.getPixel(x, y, pixelArray);
					data.setPixel(x, y, pixelArray[0]);
				}
			}
			return data;
		}
		return null;
	}

	/**
	 * Centers the content horizontally within the {@link ScrolledComposite}. This
	 * method calculates the horizontal position needed to center the content of
	 * {@code previewComposite} within {@code scrolledComposite} and sets the
	 * horizontal scroll position accordingly. It's executed asynchronously to
	 * ensure that it runs on the UI thread without blocking the caller. This method
	 * does nothing if either the scrolledComposite or previewComposite is disposed
	 * at the time of execution.
	 */
	private void centerContentHorizontally() {
		previewComposite.getDisplay().asyncExec(() -> {
			if (!scrolledComposite.isDisposed() && !previewComposite.isDisposed()) {
				int clientWidth = scrolledComposite.getClientArea().width;
				int contentWidth = previewComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int hScrollPos = (contentWidth - clientWidth) / 2;
				if (hScrollPos > 0) {
					scrolledComposite.getHorizontalBar().setSelection(hScrollPos);
				}
				scrolledComposite.setOrigin(hScrollPos, scrolledComposite.getOrigin().y);
			}
		});
	}

	/**
	 * Centers the content on a specific page number within the
	 * {@link ScrolledComposite}. This method is similar to
	 * {@link #centerContentHorizontally()} but also adjusts the vertical scroll
	 * position to bring the target page into view. The target page is specified by
	 * the {@code targetPage} parameter. The method calculates the necessary
	 * horizontal and vertical scroll positions to center the content horizontally
	 * and position it at the beginning of the specified page. It's executed
	 * asynchronously to ensure that it runs on the UI thread without blocking the
	 * caller. This method does nothing if either the scrolledComposite or
	 * previewComposite is disposed at the time of execution.
	 *
	 * @param targetPage The page number to center content on. This should be a
	 *                   positive integer, where 1 corresponds to the first page.
	 */
	private void centerContentOnPage(int targetPage) {
		previewComposite.getDisplay().asyncExec(() -> {
			if (!scrolledComposite.isDisposed() && !previewComposite.isDisposed()) {
				int clientWidth = scrolledComposite.getClientArea().width;
				int contentWidth = previewComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
				int hScrollPos = (contentWidth - clientWidth) / 6;
				if (hScrollPos > 0) {
					scrolledComposite.getHorizontalBar().setSelection(hScrollPos);
				}
				scrolledComposite.setOrigin(hScrollPos, scrolledComposite.getOrigin().y);
				int yOffset = (targetPage - 1) * scrolledComposite.getClientArea().height;
				scrolledComposite.setOrigin(scrolledComposite.getOrigin().x, yOffset);
			}
		});
	}

	public static void setCurrentPageNo(int pageNo) {
		currentPageNo = pageNo;
	}

	public void setSearchText(String searchText) {
		this.searchText = searchText;
	}
}