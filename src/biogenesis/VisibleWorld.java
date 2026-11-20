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


import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.xml.sax.SAXException;

import javafx.geometry.Bounds;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.Pane;
/**
 * This class is associated with a {@link World} and represents its visible
 * part: it's the place where organisms are drawn and is in charge of
 * the context menus management.
 *
 * <p>FX port: a plain {@link Pane} hosting a {@link Canvas}, placed in
 * MainWindow's FX ScrollPane. Rendering still goes through the AWT
 * {@link World#draw} into a BufferedImage converted with ImageFx
 * (manual PixelWriter copy); the renderers move to
 * GraphicsContext later. Redraw requests coalesce through a dirty flag
 * so a burst of sim-thread repaints costs a single frame render.
 */
public class VisibleWorld extends Pane implements WorldView {
	/**
	 * The version of this class
	 */
	private static final long serialVersionUID = Utils.FILE_VERSION;
	/**
	 * A reference to the {@link MainWindow} where the VisibleWorld is.
	 */
	protected MainWindow _mainWindow;
	/**
	 * Efficiency: zoom factor (1.0 = 100%). Mirrors Utils.ZOOM_FACTOR.
	 */
	private double zoomFactor = 1.0;
	/**
	 * Zoom limits (100% = 1.0) and wheel sensitivity. Wheel factor per event
	 * is {@code exp(deltaY * WHEEL_ZOOM_SENSITIVITY)}: ~7% per classic wheel
	 * notch (deltaY=40), sub-percent per trackpad tick for smooth feel.
	 */
	static final double MIN_ZOOM = 0.25;
	static final double MAX_ZOOM = 4.0;
	private static final double WHEEL_ZOOM_SENSITIVITY = 0.0018;
	/**
	 * FX canvas. Built on the FX thread in the constructor.
	 */
	private Canvas canvas;
	/**
	 * The context menu showed when right clicking on an alive organism.
	 */
	protected ContextMenu popupAlive;
	/** First item of {@link #popupAlive}; its text follows the track toggle. */
	private MenuItem trackMenuItem;
	/**
	 * Menu option in {@link popupAlive} used to keep an organism on the center of the view.
	 */
	protected TrackAction trackAction;
	protected StdAction feedAction;
	protected StdAction weakenAction;
	protected StdAction killAction;
	protected StdAction copyAction;
	protected StdAction saveImageAction;
	protected StdAction reviveAction;
	protected StdAction disperseAction;
	/**
	 * Menu option in {@link popupAlive} used to give extra energy to an organism.
	 */
	/**
	 * Menu option in {@link popupAlive} used to force the reproduction of the organism,
	 * even if it dies.
	 */
	protected StdAction reproduceAction;
	/**
	 * Menu option in {@link popupAlive} used to kill an organism.
	 */
	/**
	 * Menu option in {@link popupAlive} used to set the age counter of the organism back to 0.
	 */
	protected StdAction rejuvenateAction;
	/**
	 * Menu option in {@link popupAlive} used to remove energy from an organism.
	 */
	/**
	 * Menu option in {@link popupAlive} used to copy an organism genetic code
	 * to use it to create new organisms or to edit it in the genetic lab.
	 */
	/**
	 * Menu option in {@link popupAlive} used to save the genetic code of
	 * an organism to a file.
	 */
	protected StdAction exportAction;
	/**
	 * Menu option in {@link popupAlive} used to save an image of an organism
	 * to a jpg file.
	 */
	/**
	 * The context menu showed when right clicking on a dead organism.
	 */
	protected ContextMenu popupDead;
	/**
	 * Menu option in {@link popupDead} used to give live to a dead organism.
	 */
	/**
	 * Menu option in {@link popupDead} used to return all its carbony back to
	 * the atmosphere, in form of CO2, and make the corpse disappear.
	 */
	/**
	 * The context menu showed when right clicking on a void plave in the world.
	 */
	protected ContextMenu popupVoid;
	/**
	 * Menu option in {@link popupVoid} used to create a new organism with the
	 * genetic code previously copies with the Copy option.
	 */
	protected StdAction pasteAction;
	/**
	 * Menu option in {@link popupVoid} used to create a new organism randomly.
	 */
	protected StdAction randomCreateAction;
	/**
	 * Menu option in {@link popupVoid} used to create a new organism with a
	 * genetic code obtained from a file.
	 */
	protected StdAction importAction;
	/**
	 * This is the selected organism. It is drawn with an orange bounding rectangle
	 * and, if there is an {@link InfoWindow}, it shows information about this organism.
	 */
	protected Organism _selectedOrganism = null;
	/**
	 * This is the last genetic code obtained using a Copy option. It is used when
	 * pasting new organisms or in the genetic lab.
	 */
	protected GeneticCode clippedGeneticCode = null;
	/**
	 * X coordinate of the mouse pointer when the user clicks or right clicks on the
	 * visible world, in world coordinates.
	 */
	protected int mouseX;
	/**
	 * Y coordinate of the mouse pointer when the user clicks or right clicks on the
	 * visible world, in world coordinates.
	 */
	protected int mouseY;
	/**
	 * Redraw coalescing: sim-thread repaint requests set the dirty flag; at most
	 * one frame render is ever queued on the FX thread.
	 */
	private final Object redrawLock = new Object();
	private boolean dirty = false;
	private boolean redrawPending = false;
	/**
	 * Reused frame buffers. Reallocated only when the canvas size changes,
	 * so the steady-state render loop allocates nothing per frame.
	 */
	private BufferedImage frameBuffer;
	private WritableImage fxFrame;
	/**
	 * A reference to the {@link InfoWindow}, that is created from this class.
	 */
	//transient protected InfoWindow _infoWindow = null;

	class TrackAction extends StdAction {
		private static final long serialVersionUID = 1L;
		protected String name_key2;
		protected String desc_key2;

		public TrackAction(String text_key, String text_key2, String icon_path, String desc, String desc2) {
			super(text_key, icon_path, desc);
			name_key2 = text_key2;
			desc_key2 = desc2;
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				if (_mainWindow._trackedOrganism == b)
					_mainWindow.setTrackedOrganism(null);
				else
					_mainWindow.setTrackedOrganism(b);
			}
		}

		public void setTracking(boolean isTracking) {
			if (isTracking) {
				name = Messages.getString(name_key2);
				description = Messages.getString(desc_key2);
				mnemonic = Messages.getMnemonic(name_key2);
			} else {
				name = Messages.getString(name_key);
				description = Messages.getString(desc_key);
				mnemonic = Messages.getMnemonic(name_key);
			}
		}
	}

	class FeedAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public FeedAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				double q = Math.min(10, _mainWindow.getWorld().getCO2());
				_mainWindow.getWorld().addCO2(q);
				b._energy += q;
				_mainWindow.getWorld().addO2(q);
			}
		}
	}

	class WeakenAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public WeakenAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				b.useEnergy(b.getEnergy()/2);
			}
		}
	}

	class KillAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public KillAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				b.die(null);
			}
		}
	}

	class CopyAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public CopyAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				clippedGeneticCode = b.getGeneticCode();
			}
		}
	}

	class SaveImageAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public SaveImageAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				boolean processState = _mainWindow._isProcessActive;
				// Stop time while asking for a file name
				_mainWindow._isProcessActive = false;
				// Get the image to save
				BufferedImage image = b.getImage();
				try {
					// Ask for file name
					File f = Fx.showSaveFile("T_SAVE_IMAGE", "T_PNG_IMAGE_FILES", "png", false); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					if (f != null) {
						boolean canWrite = true;
						// Check if file already exists and ask for confirmation
						if (f.exists()) {
							canWrite = Fx.confirmYesNo(Messages.getString("T_FILE_EXISTS"), Messages.getString("T_CONFIRM_FILE_OVERRIDE")); //$NON-NLS-1$ //$NON-NLS-2$
						}
						if (canWrite) {
							// Write image to file
							try {
								ImageIO.write(image,"PNG",f); //$NON-NLS-1$
							} catch (FileNotFoundException ex) {
								System.err.println(ex.getMessage());
							} catch (IOException ex) {
								System.err.println(ex.getMessage());
							}
						}
					}
				} catch (SecurityException ex) {
					System.err.println(ex.getMessage());
					Fx.error(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				_mainWindow._isProcessActive = processState;
			}
		}
	}

	class ReviveAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public ReviveAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && !b.isAlive()) {
				for (int i = 0; i < b._segments; i++)
					b._segColor[i] = b.getGeneticCode().getGene(i%b.getGeneticCode().getNGenes()).getColor();
				b.alive = true;
				b.hasMoved = true;
				b._age = 0;
				_mainWindow.getWorld().increasePopulation();
				showAliveToolbar();
			}
		}
	}

	class DisperseAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public DisperseAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			if (getSelectedOrganism() != null && !getSelectedOrganism().isAlive()) {
				Organism b = getSelectedOrganism();
				b.useEnergy(b.getEnergy());
			}
		}
	}

	class ReproduceAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public ReproduceAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				b.reproduce();
			}
		}
	}

	class RejuvenateAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public RejuvenateAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				b._age = 0;
			}
		}
	}

	class ExportAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public ExportAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			Organism b = getSelectedOrganism();
			if (b != null && b.isAlive()) {
				_mainWindow.saveObjectAs(b.getGeneticCode());
			}
		}
	}

	class PasteAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public PasteAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			if (clippedGeneticCode != null) {
				pasteGeneticCode(clippedGeneticCode, mouseX, mouseY);
			}
		}
	}

	class RandomCreateAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public RandomCreateAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			GeneticCode g = new GeneticCode();
			Organism newBiot = new Organism(_mainWindow.getWorld(), g);
			if (newBiot.pasteOrganism(mouseX, mouseY))
				_mainWindow.getWorld().addOrganism(newBiot, null);
		}
	}

	class ImportAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public ImportAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			GeneticCode g;
			Organism newBiot;
			boolean processState = _mainWindow._isProcessActive;
			// Stop time
    		_mainWindow._isProcessActive = false;
    		try {
    			File f = Fx.showOpenFile("T_IMPORT", "T_BIOGENESIS_GENETIC_CODE_FILES", "bgg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    			if (f != null) {
    				try {
    					// Read XML code from file
    					BioXMLParser parser = new BioXMLParser();
						g = parser.parseGeneticCode(f);
						// Create organism
    					newBiot = new Organism(_mainWindow.getWorld(), g);
    					if (newBiot.pasteOrganism(mouseX, mouseY))
    						_mainWindow.getWorld().addOrganism(newBiot, null);
    				} catch (SAXException ex) {
    					System.err.println(ex.getMessage());
    					Fx.error(Messages.getString("T_READ_ERROR"), Messages.getString("T_WRONG_FILE_VERSION")); //$NON-NLS-1$ //$NON-NLS-2$
    				} catch (IOException ex) {
    					System.err.println(ex.getMessage());
    					Fx.error(Messages.getString("T_READ_ERROR"), Messages.getString("T_CANT_READ_FILE")); //$NON-NLS-1$ //$NON-NLS-2$
					}
    			}
    		} catch (SecurityException ex) {
    			System.err.println(ex.getMessage());
    			Fx.error(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
    		}
    		_mainWindow._isProcessActive = processState;
		}
	}

	/**
	 * Sets an organism as the selected organism. If required, it creates an
	 * {@link InfoWindow} with information of this organism.
	 *
	 * @param b  The new selected organism
	 * @param showInfo  true if an InfoWindow should be created
	 */
	public void setSelectedOrganism(Organism b) {
		Organism lastSelectedOrganism = _selectedOrganism;
		_selectedOrganism = b;
		if (lastSelectedOrganism != null)
			requestRedraw();
		_mainWindow.getInfoPanel().setSelectedOrganism(b);
		if (_selectedOrganism != null)
			requestRedraw();
		// Make sure to don't create the tool bar twice when starting the program
		// because this causes spurious exceptions.
		if (_selectedOrganism != lastSelectedOrganism) {
			if (_selectedOrganism != null) {
				if (_selectedOrganism.isAlive())
					showAliveToolbar();
				else
					showDeadToolbar();
			}
			else
				_mainWindow.createToolBar();
		}
	}
	/**
	 * Return the selected organism.
	 *
	 * @return  The selected organism, if any.
	 */
	public Organism getSelectedOrganism() {
		return _selectedOrganism;
	}
	// WorldView: view-side seam for the simulation (headless/test/JavaFX use
	// provides its own implementation; behavior here is unchanged).
	@Override
	public void repaintRegion(java.awt.Rectangle region) {
		requestRedraw();
	}
	@Override
	public void clearSelection() {
		setSelectedOrganism(null);
	}
	@Override
	public void worldStatisticsChanged() {
		_mainWindow.getInfoPanel().recalculate();
	}
	@Override
	public void organismReproduced() {
		_mainWindow.getInfoPanel().changeNChildren();
	}
	@Override
	public void organismKilled() {
		_mainWindow.getInfoPanel().changeNKills();
	}
	@Override
	public void organismInfected() {
		_mainWindow.getInfoPanel().changeNInfected();
	}
	@Override
	public void worldResized(int width, int height) {
		updateSizes(width, height);
	}
	// showDeadToolbar() below already satisfies WorldView.
	/**
	 * Set a genetic code as the clipped genetic code, that will be used when
	 * pasting a new organism or in the genetic lab as the staring genetic code.
	 *
	 * @param gc  The clipped genetic code
	 */
	public void setClippedGeneticCode(GeneticCode gc) {
		if (gc != null)
			clippedGeneticCode = gc;
	}
	/**
	 * Creates a new VisibleWorld associated with a {@link MainWindow}.
	 * Creates the canvas, the menus and the mouse handlers. The FX scene is
	 * built synchronously (blocking the caller briefly, like InfoToolbar).
	 *
	 * @param mainWindow  The MainWindow associated with this VisibleWorld.
	 */
	public VisibleWorld(MainWindow mainWindow) {
		_mainWindow = mainWindow;
		createActions();
		Fx.ask(() -> {
			buildScene();
			return null;
		});
		setZoomFactor(Utils.ZOOM_FACTOR);
	}
	/**
	 * Builds the canvas and menus. Runs on the FX thread.
	 */
	private void buildScene() {
		World w = _mainWindow.getWorld();
		int ww = (w == null) ? Utils.WORLD_WIDTH : w.getWidth();
		int wh = (w == null) ? Utils.WORLD_HEIGHT : w.getHeight();
		canvas = new Canvas((int) (ww * zoomFactor), (int) (wh * zoomFactor));
		getChildren().add(canvas);
		setPrefSize(canvas.getWidth(), canvas.getHeight());
		canvas.setOnMouseClicked(e -> {
			if (e.getButton() == MouseButton.PRIMARY) {
				setSelectedOrganism(findOrganismFromPosition(
						toWorldCoord((int) e.getX()), toWorldCoord((int) e.getY())));
			}
		}
		);
		canvas.setOnContextMenuRequested(e -> {
			int worldX = toWorldCoord((int) e.getX());
			int worldY = toWorldCoord((int) e.getY());
			maybeShowPopupMenu(worldX, worldY, e.getScreenX(), e.getScreenY());
		});
		// Smooth Ctrl/Cmd+wheel zoom to cursor; plain wheel still pans.
		canvas.setOnScroll(e -> {
			if (!e.isControlDown() && !e.isMetaDown())
				return;
			if (e.getDeltaY() == 0)
				return;
			zoomAt(e.getX(), e.getY(), Math.exp(e.getDeltaY() * WHEEL_ZOOM_SENSITIVITY));
			e.consume();
		});
		// Trackpad pinch zoom to cursor.
		canvas.setOnZoom(e -> {
			zoomAt(e.getX(), e.getY(), e.getZoomFactor());
			e.consume();
		});
		createPopupMenu();
	}
	/**
	 * Explicit content refresh for the two MainWindow call sites that used
	 * repaint(). The canvas redraw is coalesced; the JFXPanel repaints itself
	 * when the scene changes.
	 */
	public void refresh() {
		requestRedraw();
	}
	/**
	 * Queue a frame render on the FX thread, coalescing bursts.
	 * Safe to call from any thread (sim, EDT, FX).
	 *
	 * <p>Note: when already on the FX thread the render MUST go through
	 * {@code runLater}, not run inline. The sim loop calls this once per
	 * moved organism per tick; inline execution would do N full renders
	 * per tick instead of coalescing them into one.
	 */
	private void requestRedraw() {
		synchronized (redrawLock) {
			dirty = true;
			if (redrawPending)
				return;
			redrawPending = true;
		}
		javafx.application.Platform.runLater(this::doRedraw);
	}
	/**
	 * Renders the world through the AWT pipeline into a reused BufferedImage
	 * and blits it to the canvas. Runs on the FX thread. Frame buffers are
	 * reused across ticks; only a canvas resize reallocates. In efficiency
	 * mode only the scrolled-visible region is drawn (the old paintComponent
	 * culling path); otherwise the full world is drawn.
	 */
	private void doRedraw() {
		synchronized (redrawLock) {
			redrawPending = false;
			if (!dirty)
				return;
			dirty = false;
		}
		if (canvas == null)
			return;
		int w = (int) canvas.getWidth();
		int h = (int) canvas.getHeight();
		if (w <= 0 || h <= 0)
			return;
		double zf = zoomFactor;
		Organism selected = _selectedOrganism;
		World world = _mainWindow.getWorld();
		if (frameBuffer == null || frameBuffer.getWidth() != w || frameBuffer.getHeight() != h) {
			frameBuffer = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			fxFrame = null;
		}
		java.awt.Rectangle visible = visibleWorldRect(w, h, zf);
		java.awt.Graphics2D g = frameBuffer.createGraphics();
		try {
			g.setBackground(Color.BLACK);
			g.clearRect(0, 0, w, h);
			if (world != null) {
				g.scale(zf, zf);
				if (visible != null)
					world.draw(g, visible);
				else
					world.draw(g);
				if (selected != null) {
					g.setColor(Color.ORANGE);
					g.drawRect(selected.x, selected.y,
							selected.width - 1, selected.height - 1);
				}
			}
		} finally {
			g.dispose();
		}
		fxFrame = ImageFx.copyToFXImage(frameBuffer, fxFrame);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.drawImage(fxFrame, 0, 0);
	}
	/**
	 * Visible world region in world coordinates for efficiency-mode culling,
	 * derived from the ScrollPane viewport. Null means draw everything
	 * (efficiency off, viewport unknown, or content smaller than the view).
	 */
	private java.awt.Rectangle visibleWorldRect(int contentW, int contentH, double zf) {
		if (!Utils.EFFICIENCY_MODE || _mainWindow == null || _mainWindow.scrollPane == null)
			return null;
		Bounds vp = _mainWindow.scrollPane.getViewportBounds();
		double vw = vp.getWidth();
		double vh = vp.getHeight();
		if (vw <= 0 || vh <= 0 || contentW <= vw && contentH <= vh)
			return null;
		double x0 = contentW > vw ? _mainWindow.scrollPane.getHvalue() * (contentW - vw) : 0;
		double y0 = contentH > vh ? _mainWindow.scrollPane.getVvalue() * (contentH - vh) : 0;
		double x1 = Math.min(x0 + vw, contentW);
		double y1 = Math.min(y0 + vh, contentH);
		int wx = (int) (x0 / zf);
		int wy = (int) (y0 / zf);
		int ww = Math.max(1, (int) (x1 / zf) - wx + 1);
		int wh = Math.max(1, (int) (y1 / zf) - wy + 1);
		return new java.awt.Rectangle(wx, wy, ww, wh);
	}
	/**
	 * Finds an organism that has the given coordinates inside its bounding box and
	 * returns a reference to it. If more than on organism satisfies this condition,
	 * if possible, an alive organism is returned. If non organism satisfies this
	 * condition, this method returns null.
	 *
	 * @param x  X coordinate
	 * @param y  Y coordinate
	 * @return  An organism with the point (x,y) inside its bounding box, or null
	 * if such organism doesn't exist.
	 */
	Organism findOrganismFromPosition(int x, int y) {
		return _mainWindow.getWorld().findOrganismFromPosition(x, y);
	}

	/**
	 * Efficiency: set zoom factor, resize viewport, persist to Utils.
	 */
	public void setZoomFactor(double zf) {
		if (zf <= 0)
			zf = 1.0;
		zoomFactor = zf;
		Utils.ZOOM_FACTOR = zf;
		World w = (_mainWindow == null) ? null : _mainWindow.getWorld();
		int ww = (w == null) ? Utils.WORLD_WIDTH : w.getWidth();
		int wh = (w == null) ? Utils.WORLD_HEIGHT : w.getHeight();
		updateSizes((int) (ww * zf), (int) (wh * zf));
	}

	public double getZoomFactor() {
		return zoomFactor;
	}

	public int toWorldCoord(int visibleCoord) {
		return (int) (visibleCoord / zoomFactor);
	}

	public void zoomIn() {
		zoomCentered(1.25);
	}

	public void zoomOut() {
		zoomCentered(1.0 / 1.25);
	}

	/**
	 * Continuous zoom keeping the content point under the cursor fixed.
	 * Runs on the FX thread (scroll/pinch handlers, menu actions).
	 *
	 * @param anchorX  cursor X in content pixels ({@code ScrollEvent.getX()})
	 * @param anchorY  cursor Y in content pixels
	 * @param factor   multiplicative zoom factor (&gt;0)
	 */
	public void zoomAt(double anchorX, double anchorY, double factor) {
		if (factor <= 0 || !Double.isFinite(factor) || canvas == null)
			return;
		double oldZoom = zoomFactor;
		double newZoom = Utils.between(oldZoom * factor, MIN_ZOOM, MAX_ZOOM);
		if (newZoom == oldZoom)
			return;
		var sp = (_mainWindow == null) ? null : _mainWindow.scrollPane;
		double oldCW = canvas.getWidth();
		double oldCH = canvas.getHeight();
		double h = 0, v = 0, vw = 0, vh = 0;
		boolean hasScroll = sp != null && oldCW > 0 && oldCH > 0;
		if (hasScroll) {
			vw = sp.getViewportBounds().getWidth();
			vh = sp.getViewportBounds().getHeight();
			h = sp.getHvalue();
			v = sp.getVvalue();
		}
		setZoomFactor(newZoom);
		if (hasScroll) {
			double newCW = canvas.getWidth();
			double newCH = canvas.getHeight();
			if (newCW > 0 && newCH > 0) {
				sp.setHvalue(anchoredScroll(anchorX, oldCW, newCW, vw, h));
				sp.setVvalue(anchoredScroll(anchorY, oldCH, newCH, vh, v));
			}
		}
	}

	/**
	 * Stepped menu/keyboard zoom anchored at the viewport center.
	 */
	private void zoomCentered(double factor) {
		if (canvas == null) {
			setZoomFactor(Utils.between(zoomFactor * factor, MIN_ZOOM, MAX_ZOOM));
			return;
		}
		var sp = (_mainWindow == null) ? null : _mainWindow.scrollPane;
		if (sp == null) {
			setZoomFactor(Utils.between(zoomFactor * factor, MIN_ZOOM, MAX_ZOOM));
			return;
		}
		double cw = canvas.getWidth();
		double ch = canvas.getHeight();
		double vw = sp.getViewportBounds().getWidth();
		double vh = sp.getViewportBounds().getHeight();
		double x0 = cw > vw ? sp.getHvalue() * (cw - vw) : 0;
		double y0 = ch > vh ? sp.getVvalue() * (ch - vh) : 0;
		zoomAt(x0 + vw / 2, y0 + vh / 2, factor);
	}

	/**
	 * Scroll fraction keeping the anchor's viewport-relative position fixed
	 * across a content resize. Pure math, headless-testable.
	 */
	static double anchoredScroll(double anchorContent, double oldContent, double newContent,
			double viewport, double oldValue) {
		if (newContent <= viewport || oldContent <= 0 || newContent <= 0)
			return 0;
		double x0 = oldContent > viewport ? oldValue * (oldContent - viewport) : 0;
		double anchorView = anchorContent - x0;
		double newAnchor = anchorContent * (newContent / oldContent);
		return Utils.between((newAnchor - anchorView) / (newContent - viewport), 0, 1);
	}
	/**
	 * Resizes the canvas and this pane. Runs on the FX thread.
	 */
	private void updateSizes(int w, int h) {
		if (w <= 0 || h <= 0)
			return;
		Fx.run(() -> {
			if (canvas != null) {
				canvas.setWidth(w);
				canvas.setHeight(h);
			}
			setPrefSize(w, h);
			requestLayout();
			requestRedraw();
		});
	}

	private void createActions() {
		trackAction = new TrackAction("T_TRACK", "T_ABORT_TRACKING", "images/menu_track.png", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				"T_TRACK_ORGANISM", "T_ABORT_TRACKING_ORGANISM"); //$NON-NLS-1$ //$NON-NLS-2$
		feedAction = new FeedAction("T_FEED", "images/menu_feed.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_FEED_ORGANISM"); //$NON-NLS-1$
		weakenAction = new WeakenAction("T_WEAKEN", "images/menu_weaken.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_WEAKEN_ORGANISM"); //$NON-NLS-1$
		killAction = new KillAction("T_KILL", "images/menu_kill.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_KILL_ORGANISM"); //$NON-NLS-1$
		copyAction = new CopyAction("T_COPY", "images/menu_copy.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_COPY_GENETIC_CODE"); //$NON-NLS-1$
		saveImageAction = new SaveImageAction("T_SAVE_IMAGE", "images/menu_save_image.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_SAVE_IMAGE"); //$NON-NLS-1$
		reviveAction = new ReviveAction("T_REVIVE", "images/menu_revive.png", //$NON-NLS-1$ //$NON-NLS-2$
				"T_REVIVE_ORGANISM"); //$NON-NLS-1$
		disperseAction = new DisperseAction("T_DISPERSE", "images/menu_disperse.png", //$NON-NLS-1$ //$NON-NLS-2$
				"T_DISPERSE_ORGANISM"); //$NON-NLS-1$
		reproduceAction = new ReproduceAction("T_FORCE_REPRODUCTION", null, "T_FORCE_REPRODUCTION"); //$NON-NLS-1$ //$NON-NLS-2$
		rejuvenateAction = new RejuvenateAction("T_REJUVENATE", null, "T_REJUVENATE"); //$NON-NLS-1$ //$NON-NLS-2$
		exportAction = new ExportAction("T_EXPORT", null, "T_EXPORT_GENETIC_CODE"); //$NON-NLS-1$ //$NON-NLS-2$
		pasteAction = new PasteAction("T_PASTE", null, "T_PASTE_GENETIC_CODE"); //$NON-NLS-1$ //$NON-NLS-2$
		randomCreateAction = new RandomCreateAction("T_CREATE_RANDOMLY", null, "T_CREATE_RANDOMLY"); //$NON-NLS-1$ //$NON-NLS-2$
		importAction = new ImportAction("T_IMPORT", null, "T_IMPORT_GENETIC_CODE"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void showAliveToolbar() {
		// Called from any thread (selection changes); inline on the FX thread.
		Fx.run(() -> {
			javafx.scene.control.ToolBar toolBar = _mainWindow.toolBar;
			toolBar.getItems().clear();
			toolBar.getItems().add(_mainWindow.toolButtonFor(feedAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(weakenAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(killAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(copyAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(saveImageAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(trackAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(_mainWindow.abortTrackingAction));
		});
	}

	@Override
	public void showDeadToolbar() {
		Fx.run(() -> {
			javafx.scene.control.ToolBar toolBar = _mainWindow.toolBar;
			toolBar.getItems().clear();
			toolBar.getItems().add(_mainWindow.toolButtonFor(reviveAction));
			toolBar.getItems().add(_mainWindow.toolButtonFor(disperseAction));
		});
	}

	/**
	 * Creates all popup menus. Runs on the FX thread; menu items wrap the
	 * StdAction commands and read their live text/enabled state at build time.
	 */
	private void createPopupMenu() {
		popupAlive = new ContextMenu();
		trackMenuItem = itemFor(trackAction);
		popupAlive.getItems().add(trackMenuItem);
		popupAlive.getItems().add(itemFor(feedAction));
		popupAlive.getItems().add(itemFor(weakenAction));
		popupAlive.getItems().add(itemFor(reproduceAction));
		popupAlive.getItems().add(itemFor(rejuvenateAction));
		popupAlive.getItems().add(itemFor(killAction));
		popupAlive.getItems().add(itemFor(copyAction));
		popupAlive.getItems().add(itemFor(exportAction));
		popupAlive.getItems().add(itemFor(saveImageAction));
	    popupDead = new ContextMenu();
	    popupDead.getItems().add(itemFor(reviveAction));
	    popupDead.getItems().add(itemFor(disperseAction));
	    popupVoid = new ContextMenu();
	    popupVoid.getItems().add(itemFor(pasteAction));
	    popupVoid.getItems().add(itemFor(randomCreateAction));
	    popupVoid.getItems().add(itemFor(importAction));
	    // Only enable file management menu options if at least there is
		//permission to read user's home directory
		// (Java 17: SecurityManager is deprecated for removal, probe property directly)
		try {
			System.getProperty("user.home"); //$NON-NLS-1$
		} catch (SecurityException ex) {
			exportAction.setEnabled(false);
			importAction.setEnabled(false);
			saveImageAction.setEnabled(false);
		}
	}
	/**
	 * Wraps a command in an FX menu item, snapshotting its live text and
	 * enabled state at build time.
	 */
	private MenuItem itemFor(StdAction action) {
		MenuItem item = new MenuItem(action.getName());
		item.setDisable(!action.isEnabled());
		item.setOnAction(evt -> action.perform());
		return item;
	}
	/**
	 * Called from MainWindow when the locale is changed in order to update the menu
	 * entries to the new language.
	 */
	public void changeLocale() {
		trackAction.changeLocale();
	    feedAction.changeLocale();
	    weakenAction.changeLocale();
	    reproduceAction.changeLocale();
	    rejuvenateAction.changeLocale();
	    killAction.changeLocale();
	    copyAction.changeLocale();
	    exportAction.changeLocale();
	    saveImageAction.changeLocale();
	    reviveAction.changeLocale();
	    disperseAction.changeLocale();
	    pasteAction.changeLocale();
	    randomCreateAction.changeLocale();
	    importAction.changeLocale();
	    Fx.run(this::createPopupMenu);
	}
	/**
	 * Creates a new organism with the given genetic code and puts it in the world,
	 * at the specified position.
	 *
	 * @param gc  The genetic code for the new organism
	 * @param x  X coordinate
	 * @param y  Y coordinate
	 * @return  true if the organism has been created (if there is space for it), false otherwise
	 */
	public boolean pasteGeneticCode(GeneticCode gc, int x, int y) {
		Organism newOrganism = new Organism(_mainWindow.getWorld(), gc);
		if (newOrganism.pasteOrganism(x, y)) {
			_mainWindow.getWorld().addOrganism(newOrganism, null);
			return true;
		}
		return false;
	}
	/**
	 * Shows the popup menu for a world position. Coordinates are converted
	 * by the caller; screen coordinates position the menu.
	 */
	void maybeShowPopupMenu(int worldX, int worldY, double screenX, double screenY) {
		// FX-only (ContextMenu.show); inline when already on the FX thread.
		Fx.run(() -> {
			mouseX = worldX;
			mouseY = worldY;
			Organism b = findOrganismFromPosition(mouseX, mouseY);
			if (b != null) {
				setSelectedOrganism(b);
				if (b.isAlive()) {
					trackAction.setTracking(_mainWindow._trackedOrganism == b);
					trackMenuItem.setText(trackAction.getName());
					popupAlive.show(canvas, screenX, screenY);
				}
				else
					popupDead.show(canvas, screenX, screenY);
			} else
				popupVoid.show(canvas, screenX, screenY);
		});
	}
}
