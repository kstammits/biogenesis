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

import javafx.embed.swing.SwingFXUtils;
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
 * {@link World#draw} into a BufferedImage converted with SwingFXUtils
 * (same bridge as the preview canvases); the renderers move to
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
	 */
	private void requestRedraw() {
		synchronized (redrawLock) {
			dirty = true;
			if (redrawPending)
				return;
			redrawPending = true;
		}
		Fx.run(this::doRedraw);
	}
	/**
	 * Renders the world through the AWT pipeline into a BufferedImage and
	 * blits it to the canvas. Runs on the FX thread.
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
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		try {
			g.setBackground(Color.BLACK);
			g.clearRect(0, 0, w, h);
			if (world != null) {
				// Zoom-aware full-world draw, like the non-efficiency
				// paintComponent path. Viewport culling (the efficiency path)
				// returns when MainWindow itself is FX and the viewport is
				// native; until then the cost matches the classic path.
				g.scale(zf, zf);
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
		WritableImage fxImage = SwingFXUtils.toFXImage(image, null);
		GraphicsContext gc = canvas.getGraphicsContext2D();
		gc.drawImage(fxImage, 0, 0);
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
		setZoomFactor(Utils.between(zoomFactor * 1.25, 0.25, 4.0));
	}

	public void zoomOut() {
		setZoomFactor(Utils.between(zoomFactor / 1.25, 0.25, 4.0));
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
