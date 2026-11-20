/* Copyright (C) 2006-2010  Joan Queralt Molina
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package biogenesis;


import java.awt.image.BufferedImage;

import javafx.scene.canvas.Canvas;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;


/**
 * Shows a genetic code as a pie chart with a "copy" context menu.
 * Renders through the AWT {@link GeneticCode#draw} into a BufferedImage,
 * converted with ImageFx (same bridge as LabWindow's preview; port the
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
		WritableImage fxImage = ImageFx.toFXImage(image);
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
