package biogenesis;


import java.awt.image.BufferedImage;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;


/**
 * Shows a genetic code as a pie chart with a "copy" context menu.
 * Renders through the AWT {@link GeneticCode#draw} into a BufferedImage,
 * converted with SwingFXUtils (same bridge as LabWindow's preview; port the
 * renderer to GraphicsContext only after VisibleWorld/MainWindow are done).
 */
public class GeneticCodePanel extends Pane {
	protected GeneticCode code;
	protected VisibleWorld visible;
	protected Canvas canvas;
	protected ContextMenu popup;

	public GeneticCodePanel(GeneticCode geneticCode, VisibleWorld visibleWorld) {
		code = geneticCode;
		visible = visibleWorld;
		canvas = new Canvas(100, 100);
		getChildren().add(canvas);
		setPrefSize(100, 100);
		popup = new ContextMenu();
	    MenuItem menuCopy = new MenuItem(Messages.getString("T_COPY")); //$NON-NLS-1$
	    menuCopy.setOnAction(e -> visible.setClippedGeneticCode(code));
	    popup.getItems().add(menuCopy);
		canvas.setOnContextMenuRequested(e -> popup.show(canvas, e.getScreenX(), e.getScreenY()));
		widthProperty().addListener((obs, oldV, newV) -> resizeCanvas());
		heightProperty().addListener((obs, oldV, newV) -> resizeCanvas());
		redraw();
	}

	public void setGeneticCode(GeneticCode geneticCode) {
		code = geneticCode;
		redraw();
	}

	protected void redraw() {
		int w = (int) canvas.getWidth();
		int h = (int) canvas.getHeight();
		if (w <= 0 || h <= 0)
			return;
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		try {
			g.setBackground(java.awt.Color.BLACK);
			g.clearRect(0, 0, w, h);
			if (code != null)
				code.draw(g, w, h);
		} finally {
			g.dispose();
		}
		WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
		canvas.getGraphicsContext2D().drawImage(fxImage, 0, 0);
	}

	private void resizeCanvas() {
		int w = Math.max(1, (int) getWidth());
		int h = Math.max(1, (int) getHeight());
		canvas.setWidth(w);
		canvas.setHeight(h);
		redraw();
	}
}
