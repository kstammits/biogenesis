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


import java.io.*;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.NumberFormat;
import java.util.*;

import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

public class MainWindow {
	protected VisibleWorld _visibleWorld;
	protected World _world;
	protected boolean _isProcessActive=false;
	protected transient java.util.Timer _timer;
	protected transient TimerTask updateTask = null;
	protected File _gameFile = null;

	protected ScrollPane scrollPane;
	protected StdAction newGameAction;
	protected StartStopAction startStopAction;
	protected StdAction saveGameAction;
	protected StdAction increaseCO2Action;
	protected StdAction decreaseCO2Action;
	protected StdAction manageConnectionsAction;
	protected StdAction abortTrackingAction;
	protected StdAction openGameAction;
	protected StdAction saveGameAsAction;
	protected StdAction quitAction;
	protected StdAction statisticsAction;
	protected StdAction labAction;
	protected StdAction killAllAction;
	protected StdAction disperseAllAction;
	protected StdAction parametersAction;
	protected StdAction aboutAction;
	protected StdAction manualAction;
	protected StdAction checkLastVersionAction;
	protected StdAction netConfigAction;
	protected StdAction toggleEfficiencyAction;
	protected StdAction zoomInAction;
	protected StdAction zoomOutAction;

	protected MenuBar menuBar;
	protected MenuItem _menuStartStopGame;
	protected Menu _menuGame;
	protected Menu _menuWorld;
	protected Menu _menuHelp;
	protected Menu _menuNet;
	protected NumberFormat _nf;
	/** Locked-width status slots + trailing message, replacing the old
	 * single concatenated label whose every segment shuffled on each tick. */
	protected HBox _statusBar;
	protected Label _fpsSlot, _timeSlot, _popSlot, _o2Slot, _co2Slot;
	protected Label _statusMessageLabel;
	protected ToolBar toolBar;

	protected InfoToolbar infoToolbar;

	protected Stage stage;

	private String statusMessage=""; //$NON-NLS-1$
	protected Organism _trackedOrganism = null;

	protected transient NetServerThread serverThread = null;


	protected StatisticsWindow _statisticsWindow = null;
//	 Comptador de frames, per saber quan actualitzar la finestra d'informaci�
	protected long nFrames=0;

	public void setStatusMessage(String str) {
		Fx.run(() -> {
			statusMessage = str;
			updateStatusLabel();
		});
	}
	public String getStatusMessage() {
		return statusMessage;
	}

	public World getWorld() {
		return _world;
	}

	public VisibleWorld getVisibleWorld() {
        return _visibleWorld;
    }

	/** Owner for owned modal dialogs so they composite over fullscreen. */
	public Stage getStage() {
		return stage;
	}

	public boolean isProcessActive() {
		return _isProcessActive;
	}

	public InfoToolbar getInfoPanel() {
		return infoToolbar;
	}

	/**
	 * Builds the whole shell. Must run on the FX thread (like InfoToolbar);
	 * use {@code Fx.ask(() -> new MainWindow())} from anywhere else.
	 */
	public MainWindow() {
		createActions();
		toolBar = new ToolBar();
		menuBar = new MenuBar();
		setControls();
		refreshChrome();
		configureApp();
		_world = new World(_visibleWorld);
		startApp();
		_world.genesis();
		scrollPane.setContent(_visibleWorld);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if (args.length > 1) {
			System.err.println("java -jar biogenesis.jar [random seed]");
		} else if (args.length == 1) {
			try {
				long seed = Long.parseLong(args[0]);
				Utils.random.setSeed(seed);
			} catch (NumberFormatException e) {
				System.err.println("java -jar biogenesis.jar [random seed]");
			}
		}
		Utils.readPreferences();
		Fx.ask(() -> new MainWindow());
	}

	private void createActions() {
		newGameAction = new NewGameAction("T_NEW", "images/menu_new.png", //$NON-NLS-1$ //$NON-NLS-2$
				"T_NEW_WORLD"); //$NON-NLS-1$
		startStopAction = new StartStopAction("T_RESUME","T_PAUSE","images/menu_start.png",  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				"images/menu_stop.png","T_RESUME_PROCESS","T_PAUSE_PROCESS"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		saveGameAction = new SaveGameAction("T_SAVE", "images/menu_save.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_SAVE_WORLD"); //$NON-NLS-1$
		increaseCO2Action = new IncreaseCO2Action("T_INCREASE_CO2", "images/menu_increase_co2.png", //$NON-NLS-1$ //$NON-NLS-2$
				"T_INCREASE_CO2"); //$NON-NLS-1$
		decreaseCO2Action = new DecreaseCO2Action("T_DECREASE_CO2", "images/menu_decrease_co2.png", //$NON-NLS-1$ //$NON-NLS-2$
				"T_DECREASE_CO2"); //$NON-NLS-1$
		manageConnectionsAction = new ManageConnectionsAction("T_MANAGE_CONNECTIONS", "images/menu_manage_connections.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_MANAGE_CONNECTIONS"); //$NON-NLS-1$
		abortTrackingAction = new AbortTrackingAction("T_ABORT_TRACKING", "images/menu_stop_tracking.png",  //$NON-NLS-1$//$NON-NLS-2$
				"T_ABORT_TRACKING"); //$NON-NLS-1$
		openGameAction = new OpenGameAction("T_OPEN", null, "T_OPEN_WORLD");  //$NON-NLS-1$//$NON-NLS-2$
		saveGameAsAction = new SaveGameAsAction("T_SAVE_AS", null, "T_SAVE_WORLD_AS");  //$NON-NLS-1$//$NON-NLS-2$
		quitAction = new QuitAction("T_QUIT", null, "T_QUIT_PROGRAM"); //$NON-NLS-1$ //$NON-NLS-2$
		statisticsAction = new StatisticsAction("T_STATISTICS", null, "T_WORLD_STATISTICS"); //$NON-NLS-1$ //$NON-NLS-2$
		labAction = new LabAction("T_GENETIC_LABORATORY", null, "T_GENETIC_LABORATORY"); //$NON-NLS-1$ //$NON-NLS-2$
		killAllAction = new KillAllAction("T_KILL_ALL", null, "T_KILL_ALL_ORGANISMS"); //$NON-NLS-1$ //$NON-NLS-2$
		disperseAllAction = new DisperseAllAction("T_DISPERSE_ALL", null, "T_DISPERSE_ALL_DEAD_ORGANISMS"); //$NON-NLS-1$ //$NON-NLS-2$
		parametersAction = new ParametersAction("T_PARAMETERS", null, "T_EDIT_PARAMETERS"); //$NON-NLS-1$ //$NON-NLS-2$
		aboutAction = new AboutAction("T_ABOUT", null, "T_ABOUT");  //$NON-NLS-1$ //$NON-NLS-2$
		manualAction = new ManualAction("T_USER_MANUAL", null, "T_USER_MANUAL");  //$NON-NLS-1$ //$NON-NLS-2$
		checkLastVersionAction = new CheckLastVersionAction("T_CHECK_LAST_VERSION", null, "T_CHECK_LAST_VERSION"); //$NON-NLS-1$ //$NON-NLS-2$
		netConfigAction = new NetConfigAction("T_CONFIGURE_NETWORK", null, "T_CONFIGURE_NETWORK"); //$NON-NLS-1$ //$NON-NLS-2$
		toggleEfficiencyAction = new ToggleEfficiencyModeAction("T_EFFICIENCY_MODE", null, "T_EFFICIENCY_MODE_DESC"); //$NON-NLS-1$ //$NON-NLS-2$
		zoomInAction = new ZoomInAction("T_ZOOM_IN", null, "T_ZOOM_IN"); //$NON-NLS-1$ //$NON-NLS-2$
		zoomOutAction = new ZoomOutAction("T_ZOOM_OUT", null, "T_ZOOM_OUT"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/**
	 * Loads an action's icon for FX controls. Null when the action has none.
	 */
	private static ImageView iconView(StdAction action) {
		if (action.iconPath == null)
			return null;
		try (InputStream in = StdAction.class.getResourceAsStream(action.iconPath)) {
			if (in == null)
				return null;
			return new ImageView(new Image(in));
		} catch (IOException e) {
			return null;
		}
	}

	/**
	 * Fixed label width inside toolbar buttons, so labels wrap under the icon
	 * at word boundaries (e.g. "Manage" / "connections"). Must be min+prefs
	 * +max: a wrapping Label measured at its min width inside a VBox reports
	 * a huge pref height and blows the toolbar row out.
	 */
	private static final double TOOLBAR_LABEL_WIDTH = 88;

	/**
	 * Wraps a command in an FX toolbar button, snapshotting its live text,
	 * icon, tooltip and enabled state. Chrome rebuilds via refreshChrome().
	 * Icon above wrapped text (like the old toolbar) to keep the row
	 * condensed. The label lives in a VBox graphic because Button's own text
	 * layout single-lines (and ellipsizes) under a ToolBar.
	 */
	Button toolButtonFor(StdAction action) {
		Button button = new Button();
		button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		Label label = new Label(action.getName());
		label.setWrapText(true);
		label.setMinWidth(TOOLBAR_LABEL_WIDTH);
		label.setPrefWidth(TOOLBAR_LABEL_WIDTH);
		label.setMaxWidth(TOOLBAR_LABEL_WIDTH);
		label.setAlignment(Pos.CENTER);
		label.setTextAlignment(TextAlignment.CENTER);
		ImageView icon = iconView(action);
		VBox graphic = icon == null ? new VBox(label) : new VBox(icon, label);
		graphic.setAlignment(Pos.CENTER);
		graphic.setSpacing(button.getGraphicTextGap());
		button.setGraphic(graphic);
		button.setAlignment(Pos.CENTER);
		String desc = action.getDescription();
		if (desc != null)
			button.setTooltip(new Tooltip(desc));
		button.setDisable(!action.isEnabled());
		button.setOnAction(evt -> action.perform());
		return button;
	}

	/**
	 * Wraps a command in an FX menu item (icons dropped where the actions
	 * nulled them).
	 */
	private static MenuItem menuItemFor(StdAction action, boolean keepIcon) {
		MenuItem item = new MenuItem(action.getName());
		if (keepIcon)
			item.setGraphic(iconView(action));
		item.setDisable(!action.isEnabled());
		KeyCombination acc = action.getAccelerator();
		if (acc != null)
			item.setAccelerator(acc);
		item.setOnAction(evt -> action.perform());
		return item;
	}

	public void createToolBar() {
		abortTrackingAction.setEnabled(_trackedOrganism != null);
		toolBar.getItems().setAll(
			toolButtonFor(newGameAction),
			toolButtonFor(startStopAction),
			toolButtonFor(saveGameAction),
			toolButtonFor(increaseCO2Action),
			toolButtonFor(decreaseCO2Action),
			toolButtonFor(manageConnectionsAction),
			toolButtonFor(abortTrackingAction));
	}

	/**
	 * Rebuilds menus and toolbar from the actions' live text/icons/enabled
	 * state. Called after anything that mutates them (toggles, selection,
	 * locale change) instead of syncing individual controls. Runs on the FX
	 * thread (inline when already there).
	 */
	private void refreshChrome() {
		Fx.run(() -> {
			menuBar.getMenus().setAll(
				buildGameMenu(), buildWorldMenu(), buildNetMenu(), buildHelpMenu());
			createToolBar();
		});
	}

	private Menu buildGameMenu() {
		_menuGame = new Menu(Messages.getString("T_GAME")); //$NON-NLS-1$
		_menuGame.getItems().add(menuItemFor(newGameAction, false));
		_menuStartStopGame = menuItemFor(startStopAction, false);
		_menuGame.getItems().add(_menuStartStopGame);
		_menuGame.getItems().add(menuItemFor(openGameAction, true));
		_menuGame.getItems().add(menuItemFor(saveGameAction, false));
		_menuGame.getItems().add(menuItemFor(saveGameAsAction, true));
		_menuGame.getItems().add(menuItemFor(quitAction, true));
		return _menuGame;
	}

	private Menu buildWorldMenu() {
		_menuWorld = new Menu(Messages.getString("T_WORLD")); //$NON-NLS-1$
		_menuWorld.getItems().add(menuItemFor(statisticsAction, true));
		_menuWorld.getItems().add(menuItemFor(labAction, true));
		_menuWorld.getItems().add(menuItemFor(increaseCO2Action, false));
		_menuWorld.getItems().add(menuItemFor(decreaseCO2Action, false));
		_menuWorld.getItems().add(menuItemFor(killAllAction, true));
		_menuWorld.getItems().add(menuItemFor(disperseAllAction, true));
		_menuWorld.getItems().add(menuItemFor(parametersAction, true));
		_menuWorld.getItems().add(new SeparatorMenuItem());
		_menuWorld.getItems().add(menuItemFor(toggleEfficiencyAction, true));
		_menuWorld.getItems().add(menuItemFor(zoomInAction, true));
		_menuWorld.getItems().add(menuItemFor(zoomOutAction, true));
		return _menuWorld;
	}

	private Menu buildNetMenu() {
		_menuNet = new Menu(Messages.getString("T_NETWORK")); //$NON-NLS-1$
		_menuNet.getItems().add(menuItemFor(netConfigAction, true));
		_menuNet.getItems().add(menuItemFor(manageConnectionsAction, false));
		// Only enable file management menu options if at least there is
		//permission to read user's home directory
		// (Java 17: SecurityManager is deprecated for removal, probe property directly)
		try {
			System.getProperty("user.home"); //$NON-NLS-1$
		} catch (SecurityException ex) {
			openGameAction.setEnabled(false);
			saveGameAsAction.setEnabled(false);
			saveGameAction.setEnabled(false);
			manualAction.setEnabled(false);
			netConfigAction.setEnabled(false);
			manageConnectionsAction.setEnabled(false);
		}
		return _menuNet;
	}

	private Menu buildHelpMenu() {
		_menuHelp = new Menu(Messages.getString("T_HELP")); //$NON-NLS-1$
		_menuHelp.getItems().add(menuItemFor(manualAction, true));
		_menuHelp.getItems().add(menuItemFor(checkLastVersionAction, true));
		_menuHelp.getItems().add(menuItemFor(aboutAction, true));
		return _menuHelp;
	}

	protected void checkLastVersion() {
		CheckVersionThread thread = new CheckVersionThread();
		thread.start();
	}

	protected void netConfig() {
		new NetConfigWindow(this);
	}

	public void setAcceptConnections(boolean newAcceptConnections) {
		if (newAcceptConnections != Utils.ACCEPT_CONNECTIONS) {
			Utils.ACCEPT_CONNECTIONS = newAcceptConnections;
			if (newAcceptConnections)
				startServer();
			else
				if (serverThread.getConnections().isEmpty())
					stopServer();
		}
	}
	public boolean isAcceptingConnections() {
		return Utils.ACCEPT_CONNECTIONS;
	}

	public NetServerThread getNetServer() {
		return serverThread;
	}

	public NetServerThread startServer() {
		if (serverThread == null || !serverThread.isActive()) {
			serverThread = new NetServerThread(this);
			serverThread.start();
		}
		return serverThread;
	}

	public void stopServer() {
		if (serverThread != null) {
			serverThread.closeServer();
		}
	}

	class NewGameAction extends StdAction {
		public NewGameAction(String text_key, String icon_path, String desc) {
			super(text_key, icon_path, desc);
		}

		public void perform() {
			_trackedOrganism = null;
			_world.genesis();
			scrollPane.setContent(_visibleWorld);
			_isProcessActive = true;
			startStopAction.setActive(true);
			setStatusMessage(Messages.getString("T_NEW_WORLD_CREATED")); //$NON-NLS-1$
		}
	}

	class StartStopAction extends StdAction {
		protected String name2;
		protected String description2;
		protected String iconPath2;

		protected String name2_key;
		protected String desc2_key;

		protected boolean active;

		public StartStopAction(String text1, String text2, String icon_path1,
				String icon_path2, String desc1, String desc2) {
			super(text1, icon_path1, desc1);
			name2 = Messages.getString(text2);
			description2 = Messages.getString(desc2);
			iconPath2 = icon_path2;
			name2_key = text2;
			desc2_key = desc2;
			active = false;

			accelerator = keyCombinationFor(Messages.getString("T_PAUSE_ACCELERATOR"));
		}

		public void perform() {
			if (_isProcessActive)
				pauseGame();
			else
				playGame();
		}

		public void toogle() {
			String aux;
			aux = name;
			name = name2;
			name2 = aux;
			aux = description;
			description = description2;
			description2 = aux;
			aux = iconPath;
			iconPath = iconPath2;
			iconPath2 = aux;
			active = !active;
		}

		@Override
		public void changeLocale() {
			super.changeLocale();
			name2 = Messages.getString(name2_key);
			description2 = Messages.getString(desc2_key);
			accelerator = keyCombinationFor(Messages.getString("T_PAUSE_ACCELERATOR"));
		}

		public void setActive(boolean newState) {
			if (newState != active) {
				toogle();
				active = newState;
			}
		}

		public boolean isActive() {
			return active;
		}
	}

	class SaveGameAction extends StdAction {
		public SaveGameAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
			accelerator = keyCombinationFor(Messages.getString("T_SAVE_ACCELERATOR"));
		}

		public void perform() {
			if (_gameFile != null)
				saveObject(_world, _gameFile);
			else
				saveGameAs();
		}

		@Override
		public void changeLocale() {
			super.changeLocale();
			accelerator = keyCombinationFor(Messages.getString("T_SAVE_ACCELERATOR"));
		}
	}

	class IncreaseCO2Action extends StdAction {
		public IncreaseCO2Action(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.addCO2(500);
		}
	}

	class DecreaseCO2Action extends StdAction {
		public DecreaseCO2Action(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.decreaseCO2(500);
		}
	}

	class ManageConnectionsAction extends StdAction {
		public ManageConnectionsAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			netConnectionsWindow();
		}
	}

	class AbortTrackingAction extends StdAction {
		public AbortTrackingAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			setTrackedOrganism(null);
		}
	}

	class OpenGameAction extends StdAction {
		public OpenGameAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			boolean processState = _isProcessActive;
			_isProcessActive = false;
			try {
				File f = Fx.showOpenFile("T_OPEN_WORLD", "T_BIOGENESIS_WORLD_FILES", "bgw"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				if (f != null) {
					// Elimina les finestres antigues
					if (_statisticsWindow != null) {
						_statisticsWindow.close();
						_statisticsWindow = null;
					}
					//_world.clean();
					try (FileInputStream fileStream = new FileInputStream(f);
							ObjectInputStream inputStream = new ObjectInputStream(fileStream)) {
						inputStream.setObjectInputFilter(Utils.SERIAL_FILTER);
						_world = (World) inputStream.readObject();
						_gameFile = f;
						_trackedOrganism = null;
						processState = true;
						setStatusMessage(Messages.getString("T_WORLD_LOADED_SUCCESSFULLY")); //$NON-NLS-1$
					} catch (InvalidClassException ex) {
						// Serial version mismatch (e.g. pre-800 save): report as
						// wrong version, not generic read failure.
						System.err.println(ex.getMessage());
						Fx.readError("T_WRONG_FILE_VERSION"); //$NON-NLS-1$
					} catch (IOException ex) {
						System.err.println(ex.getMessage());
						Fx.readError("T_CANT_READ_FILE"); //$NON-NLS-1$
					} catch (ClassNotFoundException ex) {
						System.err.println(ex.getMessage());
						Fx.readError("T_WRONG_FILE_TYPE"); //$NON-NLS-1$
					} catch (ClassCastException ex) {
						System.err.println(ex.getMessage());
						Fx.readError("T_WRONG_FILE_VERSION"); //$NON-NLS-1$
					}
					// Torna a assignar els valors dels camps no guardats a l'objecte world
					_world.init(_visibleWorld);
					scrollPane.setContent(_visibleWorld);
				// Assegurem que s'ha dibuixat el m�n
				_visibleWorld.refresh();
				}
			} catch (SecurityException ex) {
				System.err.println(ex.getMessage());
				Fx.error(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			_isProcessActive = processState;
		}
	}

	class SaveGameAsAction extends StdAction {
		public SaveGameAsAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			saveGameAs();
		}
	}

	class QuitAction extends StdAction {
		public QuitAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			quit();
		}
	}

	public void quit() {
		Fx.Answer save = Fx.confirmYesNoCancel(Messages.getString("T_SAVE_WORLD"), Messages.getString("T_SAVE_BEFORE_QUIT")); //$NON-NLS-1$ //$NON-NLS-2$

		if (save != Fx.Answer.CANCEL) {
			if (save == Fx.Answer.YES) {
				if (_gameFile != null)
					saveObject(_world, _gameFile);
				else
					if (saveGameAs() == null)
						return;
			}
			Utils.quitProgram(this);
			if (serverThread != null)
				serverThread.closeServer();
			try {
				System.exit(0);
			} catch (SecurityException ex) {
				stage.close();
			}
		}
	}

	/** Geometry for Utils.quitProgram (prefs), read off the stage. */
	public int getX() {
		return (int) stage.getX();
	}
	public int getY() {
		return (int) stage.getY();
	}
	public int getWidth() {
		return (int) stage.getWidth();
	}
	public int getHeight() {
		return (int) stage.getHeight();
	}
	public int getExtendedState() {
		return stage.isMaximized() ? Utils.WINDOW_STATE_MAXIMIZED : Utils.WINDOW_STATE_NORMAL;
	}

	class StatisticsAction extends StdAction {
		public StatisticsAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			if (_statisticsWindow != null)
				_statisticsWindow.close();
			_statisticsWindow = _world.createStatisticsWindow(MainWindow.this);
		}
	}

	class LabAction extends StdAction {
		public LabAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			new LabWindow(MainWindow.this);
		}
	}

	class KillAllAction extends StdAction {
		public KillAllAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.killAll();
		}
	}

	class DisperseAllAction extends StdAction {
		public DisperseAllAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.disperseAll();
		}
	}

	class ParametersAction extends StdAction {
		public ParametersAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			paramDialog();
		}
	}

	class ManualAction extends StdAction {
		public ManualAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
		String DirNameManual = "/usr/share/doc/biogenesis/biogenesis_manual";
		if ((new File(DirNameManual+"/manual_"+Messages.getLanguage()+".html")).exists()) {
			BareBonesBrowserLaunch.openURL(DirNameManual+"/manual_"+Messages.getLanguage()+".html");
		} else {
			if ((new File(DirNameManual+"/manual_en.html")).exists()) {
				BareBonesBrowserLaunch.openURL(DirNameManual+"/manual_en.html");
			} else {
				if ((Messages.getLanguage().equals("ca")) || (Messages.getLanguage().equals("es")) || (Messages.getLanguage().equals("en"))) {
					BareBonesBrowserLaunch.openURL("http://biogenesis.sourceforge.net/manual."+Messages.getLanguage()+".php"); //$NON-NLS-1$
				} else {
					BareBonesBrowserLaunch.openURL("http://biogenesis.sourceforge.net/manual.en.php"); //$NON-NLS-1$
				}
			}
		}
		}
	}

	class CheckLastVersionAction extends StdAction {
		public CheckLastVersionAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			checkLastVersion();
		}
	}

	class AboutAction extends StdAction {
		public AboutAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			about();
		}
	}

	class NetConfigAction extends StdAction {
		public NetConfigAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			netConfig();
		}
	}

	class ToggleEfficiencyModeAction extends StdAction {
		public ToggleEfficiencyModeAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			boolean on = Utils.toggleEFFICIENCY_MODE();
			Utils.savePreferences();
			setStatusMessage(on ? "Efficiency mode ON" : "Efficiency mode OFF"); //$NON-NLS-1$ //$NON-NLS-2$
			if (_visibleWorld != null)
				_visibleWorld.refresh();
		}
	}

	class ZoomInAction extends StdAction {
		public ZoomInAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			if (_visibleWorld != null)
				_visibleWorld.zoomIn();
		}
	}

	class ZoomOutAction extends StdAction {
		public ZoomOutAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			if (_visibleWorld != null)
				_visibleWorld.zoomOut();
		}
	}

	protected void paramDialog() {
		new ParamDialog(this);
	}

	public void setTrackedOrganism(Organism org) {
		_trackedOrganism = org;
		abortTrackingAction.setEnabled(_trackedOrganism != null);
		refreshChrome();
	}

	protected void netConnectionsWindow() {
		new NetConnectionsWindow(this);
	}

	public void pauseGame() {
		Fx.run(() -> {
			_isProcessActive = false;
			startStopAction.toogle();
			refreshChrome();
			setStatusMessage(Messages.getString("T_GAME_PAUSED")); //$NON-NLS-1$
		});
	}

	public void playGame() {
		Fx.run(() -> {
			_isProcessActive = true;
			startStopAction.toogle();
			refreshChrome();
			setStatusMessage(Messages.getString("T_GAME_RESUMED")); //$NON-NLS-1$
		});
	}

	protected File saveGameAs() {
		File savedFile = saveObjectAs(_world);
		if (savedFile != null)
			_gameFile = savedFile;
		return savedFile;
	}

	protected void about() {
		String aboutString = Messages.getString("T_PROGRAM_NAME")+"http://biogenesis.sourceforge.net/"+"\n"  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
		+Messages.getString("T_VERSION_B")+" "+CheckVersionThread.translateVersion(String.valueOf(Utils.VERSION))+"\n"+ //$NON-NLS-1$//$NON-NLS-2$
				Messages.getString("T_VANILLA_COPYRIGHT")+"joanq@users.sourceforge.net\n"+ //$NON-NLS-1$ //$NON-NLS-2$
				Messages.getString("T_ARTWORK_BY")+" Ananda Daydream, Florian Haag (http://toolbaricons.sourceforge.net/)";  //$NON-NLS-1$ //$NON-NLS-2$
		Fx.ask(() -> {
			javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
					javafx.scene.control.Alert.AlertType.INFORMATION, aboutString,
					javafx.scene.control.ButtonType.OK);
			alert.setTitle(Messages.getString("T_ABOUT")); //$NON-NLS-1$
			alert.setHeaderText(null);
			try {
				alert.setGraphic(new javafx.scene.image.ImageView(new javafx.scene.image.Image(
						getClass().getResourceAsStream("images/bullet.jpg")))); //$NON-NLS-1$
			} catch (Exception e) {
				// icon is decorative; show the dialog without it
			}
			alert.showAndWait();
			return null;
		});
	}

	private void setControls () {
		stage = new Stage();
		stage.setTitle(Messages.getString("T_BIOGENESIS")); //$NON-NLS-1$
		try (InputStream iconStream = getClass().getResourceAsStream("images/bullet.jpg")) { //$NON-NLS-1$
			if (iconStream != null)
				stage.getIcons().add(new Image(iconStream));
		} catch (IOException e) {
			// icon is decorative; run without it
		}
		stage.setX(Utils.WINDOW_X);
		stage.setY(Utils.WINDOW_Y);

		_visibleWorld = new VisibleWorld(this);
		scrollPane = new ScrollPane(_visibleWorld);
		// Letterbox behind the world canvas: dark slate so the black world
		// floor keeps a visible edge when the window is bigger than the world.
		// -fx-background is inherited by .viewport, so one rule covers both.
		scrollPane.setStyle("-fx-background: #3a3f44;"); //$NON-NLS-1$

		infoToolbar = new InfoToolbar(null, this);

		_fpsSlot = statusSlot(Messages.getString("T_FPS"), "8888"); //$NON-NLS-1$ //$NON-NLS-2$
		_timeSlot = statusSlot(Messages.getString("T_TIME"), "888888"); //$NON-NLS-1$ //$NON-NLS-2$
		_popSlot = statusSlot(Messages.getString("T_CURRENT_POPULATION"), "88888"); //$NON-NLS-1$ //$NON-NLS-2$
		_o2Slot = statusSlot(Messages.getString("T_O2"), "88888.8"); //$NON-NLS-1$ //$NON-NLS-2$
		_co2Slot = statusSlot(Messages.getString("T_CO2"), "88888.8"); //$NON-NLS-1$ //$NON-NLS-2$
		_statusMessageLabel = new Label(" "); //$NON-NLS-1$
		_statusMessageLabel.setMaxWidth(Double.MAX_VALUE);
		_statusBar = new HBox(8, _fpsSlot, _timeSlot, _popSlot, _o2Slot, _co2Slot, _statusMessageLabel);
		HBox.setHgrow(_statusMessageLabel, Priority.ALWAYS);
		_statusBar.setStyle("-fx-border-color: grey; -fx-padding: 2;"); //$NON-NLS-1$
		_nf = NumberFormat.getInstance();
		_nf.setMaximumFractionDigits(1);

		BorderPane root = new BorderPane();
		VBox top = new VBox(menuBar, toolBar);
		root.setTop(top);
		root.setCenter(scrollPane);
		VBox bottom = new VBox(infoToolbar, _statusBar);
		root.setBottom(bottom);
		stage.setScene(new Scene(root));
    }

	/**
	 * One locked-width numeric slot of the status bar. Min+pref are sized
	 * to the widest expected sample (right-aligned), so per-tick number
	 * changes repaint inside the slot instead of shoving every downstream
	 * segment sideways. Label text starts at a zero value; the first
	 * {@link #updateStatusLabel} fills in live numbers.
	 */
	private static Label statusSlot(String prefix, String widestValue) {
		Label slot = new Label(prefix + "0"); //$NON-NLS-1$
		slot.setAlignment(Pos.CENTER_RIGHT);
		sizeSlot(slot, prefix, widestValue);
		return slot;
	}

	/** Re-locks a slot's width for the current locale's prefix. */
	private static void sizeSlot(Label slot, String prefix, String widestValue) {
		double w = new Text(prefix + widestValue).getLayoutBounds().getWidth() + 6;
		slot.setMinWidth(w);
		slot.setPrefWidth(w);
	}

	/** Re-locks all status slots for the current locale (prefixes changed). */
	private void sizeStatusSlots() {
		sizeSlot(_fpsSlot, Messages.getString("T_FPS"), "8888"); //$NON-NLS-1$ //$NON-NLS-2$
		sizeSlot(_timeSlot, Messages.getString("T_TIME"), "888888"); //$NON-NLS-1$ //$NON-NLS-2$
		sizeSlot(_popSlot, Messages.getString("T_CURRENT_POPULATION"), "88888"); //$NON-NLS-1$ //$NON-NLS-2$
		sizeSlot(_o2Slot, Messages.getString("T_O2"), "88888.8"); //$NON-NLS-1$ //$NON-NLS-2$
		sizeSlot(_co2Slot, Messages.getString("T_CO2"), "88888.8"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public File saveObjectAs(Object obj) {
		File resultFile = null;
		boolean processState = _isProcessActive;
		_isProcessActive = false;

		try {
			File f;
			if (obj instanceof GeneticCode)
				f = Fx.showSaveFile("T_EXPORT", "T_BIOGENESIS_GENETIC_CODE_FILES", "bgg", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			else
				f = Fx.showSaveFile("T_SAVE_WORLD_AS", "T_BIOGENESIS_WORLD_FILES", "bgw", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (f != null) {
				boolean canWrite = true;
				if (f.exists()) {
					canWrite = Fx.confirmYesNo(Messages.getString("T_FILE_EXISTS"), Messages.getString("T_CONFIRM_FILE_OVERRIDE")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				if (canWrite) {
					if (obj instanceof GeneticCode) {
						try (PrintStream ps = new PrintStream(f, "UTF-8")) { //$NON-NLS-1$
							BioXMLParser.writeGeneticCode(ps, (GeneticCode)obj);
							resultFile = f;
						} catch (FileNotFoundException ex) {
							System.err.println(ex.getLocalizedMessage());
						} catch (java.io.UnsupportedEncodingException ex) {
							System.err.println(ex.getLocalizedMessage());
						}
					} else
						if (saveObject(obj, f))
							resultFile = f;
				}
			}
		} catch (SecurityException ex) {
			System.err.println(ex.getMessage());
			Fx.error(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	    _isProcessActive = processState;
	    return resultFile;
	}

	public boolean saveObject(Object obj, File f) {
		// Atomic write: serialize to a temp sibling, then move it over the
		// target — a crash mid-write must not destroy the only good copy.
		File tmp = new File(f.getParent(), f.getName() + ".tmp"); //$NON-NLS-1$
		try (FileOutputStream fileStream = new FileOutputStream(tmp);
				ObjectOutputStream outputStream = new ObjectOutputStream(fileStream)) {
			outputStream.writeObject(obj);
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
			return false;
		} catch (IOException e) {
			System.err.println(e.getMessage());
			return false;
		} catch (SecurityException ex) {
			System.err.println(ex.getMessage());
			Fx.error(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
			return false;
		}
		try {
			try {
				Files.move(tmp.toPath(), f.toPath(),
						StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp.toPath(), f.toPath(), StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			tmp.delete();
			return false;
		}
		setStatusMessage(Messages.getString("T_WRITING_COMPLETED")); //$NON-NLS-1$
		return true;
	}

	public void configureApp() {
		stage.setTitle(Messages.getString("T_BIOGENESIS")); //$NON-NLS-1$
		stage.setOnCloseRequest(e -> {
			e.consume();
			quit();
		});
	}

	public void updateStatusLabel() {
		Fx.run(() -> {
			_fpsSlot.setText(Messages.getString("T_FPS") + getFPS()); //$NON-NLS-1$
			_timeSlot.setText(Messages.getString("T_TIME") + _world.getTime()); //$NON-NLS-1$
			_popSlot.setText(Messages.getString("T_CURRENT_POPULATION") + _world.getPopulation()); //$NON-NLS-1$
			_o2Slot.setText(Messages.getString("T_O2") + _nf.format(_world.getO2())); //$NON-NLS-1$
			_co2Slot.setText(Messages.getString("T_CO2") + _nf.format(_world.getCO2())); //$NON-NLS-1$
			_statusMessageLabel.setText(getStatusMessage());
		});
	}
	/**
	 * Returns the measured sim ticks per second, sampled over the last
	 * 20-tick window. Falls back to the 1000/currentDelay target before
	 * the first sample (e.g. right after launch).
	 *
	 * @return  The actual FPS.
	 */
	public int getFPS() {
		if (measuredFps > 0)
			return measuredFps;
		return 1000/currentDelay;
	}

	/**
	 * Render cap: the app never paints above this, whatever the sim rate.
	 * Sim ticks run every {@link #currentDelay} ms, repaints happen at most
	 * this often. Keeps a 500Hz sim from paying a full-world blit per tick.
	 */
	protected static final int MAX_RENDER_FPS = 30;
	protected int renderIntervalMs = 1000 / MAX_RENDER_FPS;
	protected long lastRenderMs = 0;
	/** Measured ticks/sec state, sampled on the FX thread in lifeProcess. */
	protected int measuredFps = -1;
	protected long fpsWindowStartMs = 0;
	protected int fpsWindowFrames = 0;

	final transient Runnable lifeProcess = new Runnable() {
	    public void run() {
	    	// Batched ticks: one FX round-trip runs several sim ticks at fast
	    	// rates (slow rates stay 1:1). Pause still bites mid-batch.
	    	int batch = batchSizeForDelay(currentDelay);
	    	for (int t = 0; t < batch && _isProcessActive; t++) {
	    		// executa un torn
	    		_world.time();
	    		nFrames++;
	    		long nowMs = System.currentTimeMillis();
	    		if (fpsWindowStartMs == 0)
	    			fpsWindowStartMs = nowMs;
	    		fpsWindowFrames++;
	    		if (nFrames % 20 == 0) {
	    			//if (_statisticsWindow != null)
	    			//	_statisticsWindow.recalculate();
	    			long elapsed = nowMs - fpsWindowStartMs;
	    			// Compute over >=250ms so batched fires and ms-clock
	    			// quantization average out; status text still refreshes
	    			// every 20 ticks.
	    			if (elapsed >= 250) {
	    				// Smooth the readout: per-window instants still
	    				// jitter a few percent at high rates.
	    				int instant = (int) Math.round(fpsWindowFrames * 1000.0 / elapsed);
	    				measuredFps = measuredFps <= 0 ? instant
	    					: (int) Math.round(0.3 * instant + 0.7 * measuredFps);
	    				fpsWindowStartMs = nowMs;
	    				fpsWindowFrames = 0;
	    			}
	    			updateStatusLabel();
	    		}
	    		// dibuixa de nou si cal — throttled: sim runs free, paint
	    		// at most every renderIntervalMs (explicit refresh() calls
	    		// from selection/zoom/menus still repaint immediately).
	    		if (nowMs - lastRenderMs >= renderIntervalMs || lastRenderMs == 0) {
	    			lastRenderMs = nowMs;
	    			_world.setPaintingRegion();
	    		}
	    		// tracking
	    		if (_trackedOrganism != null) {
	    			if (!_trackedOrganism.isAlive()) {
	    				_trackedOrganism = null;
	    				abortTrackingAction.setEnabled(false);
	    				refreshChrome();
	    			}
	    			else {
	    				double vw = scrollPane.getViewportBounds().getWidth();
	    				double vh = scrollPane.getViewportBounds().getHeight();
	    				scrollPane.setHvalue(scrollTo(_trackedOrganism._centerX,
	    						_visibleWorld.getWidth(), vw));
	    				scrollPane.setVvalue(scrollTo(_trackedOrganism._centerY,
	    						_visibleWorld.getHeight(), vh));
	    			}
	    		}
	    	}
	    }
	};

	/**
	 * Ticks per timer fire for a per-tick delay in ms. Slow rates stay 1:1
	 * (behavior unchanged); 25ms is the anchor because 120/240/500 all
	 * divide it exactly. Pure math, headless-testable.
	 */
	static int batchSizeForDelay(int delay) {
		if (delay <= 0)
			return 1;
		return Utils.between((int) Math.round(25.0 / delay), 1, 16);
	}

	/**
	 * Sim ticks/sec the timer paces to: the preset rate itself at target
	 * (exact — 120.0, not 1000/8), the delay's own rate under governor
	 * backoff, infinity for uncapped (no pacing). Pure math.
	 */
	static double effectiveRate(int currentDelay, int targetDelay) {
		if (currentDelay != targetDelay)
			return 1000.0 / currentDelay;
		int preset = Utils.nearestSimRate(targetDelay);
		return preset <= 0 ? Double.POSITIVE_INFINITY : preset;
	}

	/**
	 * Nominal ms between fire starts: batch ticks at the effective rate.
	 * For uncapped this is just batch × delay (governor bands only —
	 * pacing itself is as-fast-as-possible). Pure math.
	 */
	static double nominalPeriodMs(int currentDelay, int targetDelay) {
		int batch = batchSizeForDelay(currentDelay);
		double rate = effectiveRate(currentDelay, targetDelay);
		if (Double.isInfinite(rate))
			return (double) batch * currentDelay;
		return batch * 1000.0 / rate;
	}

	/**
	 * Maps a content pixel offset to a 0..1 scroll fraction.
	 */
	private static double scrollTo(double center, double content, double viewport) {
		if (content <= viewport)
			return 0;
		return Utils.between((center - viewport / 2) / (content - viewport), 0, 1);
	}

	public void startApp() {
		stage.setResizable(true);
		stage.setWidth(Utils.WINDOW_WIDTH);
		stage.setHeight(Utils.WINDOW_HEIGHT);
		if (Utils.WINDOW_STATE == Utils.WINDOW_STATE_MAXIMIZED)
			stage.setMaximized(true);
		stage.show();

		_timer = new java.util.Timer();
		startLifeProcess(Utils.DELAY);
		if (isAcceptingConnections())
			startServer();
	}
	/**
	 * Number of milliseconds between two sim ticks. It starts as the user's
	 * sim-rate preset (Utils.DELAY) but the governor adapts it under load.
	 * The timer fires every batchSizeForDelay(currentDelay) ticks.
	 */
	protected int currentDelay = Utils.DELAY;
	/**
	 * If positive, the consecutive number of frames that haven't accomplished the expected time contract.
	 * If negative, the consecutive number of frame that have accomplished the expected time contract.
	 * Used to adapt the program's speed.
	 */
	protected int failedTime=0;
	/**
	 * The moment of the last timer fire, in nanoTime. Used both for the
	 * governor's interval check and the pacing EWMA. Zero until the first
	 * fire after a (re)start, which is treated as exactly on schedule.
	 */
	protected long lastFireNanos;
	/**
	 * Settle-down fires after a (re)start before the governor may adapt the
	 * delay again. Covers cold-start JIT/warmup (where the first deltas are
	 * meaningless) and damps backoff oscillation.
	 */
	protected int delayCooldown = 0;
	/**
	 * Timer generation: guards the self-rearming chain — a fire only
	 * schedules its successor while its generation is still current, so a
	 * governor/preset restart mid-fire can't fork a duplicate chain.
	 * Volatile: bumped on the FX thread (preset change), read on the
	 * timer thread (re-arm check).
	 */
	protected volatile long timerGen = 0;
	/**
	 * Smoothed per-fire overhead in ms (Fx.ask handoff + timer slack beyond
	 * the nominal period), subtracted from the next wait. Reset on every
	 * (re)start; frozen while paused, cooling down, or adapting.
	 */
	protected double smoothedCostMs = 0;
	/**
	 * Retarget a running sim at the current {@link Utils#DELAY} (e.g. after
	 * the parameters dialog changes the sim-rate preset). Restarts the
	 * timer with the settle-down cooldown so the governor doesn't trip on
	 * the transition. No-op when already at the target delay.
	 */
	public void applySimDelay() {
		if (currentDelay != Utils.DELAY) {
			currentDelay = Utils.DELAY;
			failedTime = 0;
			startLifeProcess(currentDelay);
		}
	}
	/**
	 * (Re)starts the sim timer. Synchronized: the governor calls it from
	 * the timer thread while preset changes call it from the FX thread —
	 * cancel/schedule must not interleave.
	 */
	public synchronized void startLifeProcess(int delay) {
		if (updateTask != null)
			updateTask.cancel();
		timerGen++;
		delayCooldown = 40;
		smoothedCostMs = 0;
		lastFireNanos = 0;
		scheduleNext(delay, timerGen);
	}
	/**
	 * One link of the self-rearming timer chain: waits, fires a batch,
	 * folds the fire's overhead into the pacing EWMA, and schedules the
	 * next wait from nominal-minus-cost (uncapped: nominal as-is, i.e.
	 * as-fast-as-possible). Re-arms only while its generation is current.
	 */
	private synchronized void scheduleNext(long waitMs, long gen) {
		updateTask = new TimerTask() {
		    @Override
			public void run() {
				try {
					doFire(waitMs);
				} catch (RuntimeException e) {
					e.printStackTrace();
				} finally {
					if (gen == timerGen)
						scheduleNext(computeWaitMs(), gen);
				}
		    }
		};
		_timer.schedule(updateTask, Math.max(0, waitMs));
	}
	/**
	 * Next wait in ms: nominal fire period minus smoothed overhead (never
	 * below 1ms; uncapped skips compensation — there is no target to hold).
	 */
	private long computeWaitMs() {
		if (Double.isInfinite(effectiveRate(currentDelay, Utils.DELAY)))
			return (long) batchSizeForDelay(currentDelay) * currentDelay;
		double nominal = nominalPeriodMs(currentDelay, Utils.DELAY);
		return Math.max(1L, Math.round(nominal - smoothedCostMs));
	}
	/**
	 * Runs one batched fire (on the FX thread via Fx.ask), adapts the
	 * governor delay on sustained over/underruns, and folds this fire's
	 * overhead into the pacing EWMA (steady state only — frozen while
	 * paused, cooling down, or adapting so the two loops can't fight).
	 *
	 * @param askedMs  the wait that preceded this fire, for the EWMA.
	 */
	private void doFire(long askedMs) {
		Fx.ask(() -> {
			lifeProcess.run();
			return null;
		});
		/**
		 * Checks the actual drawing speed and increases or decreases the speed
		 * of the program in order to keep it running smoothly. Compares
		 * against the nominal fire interval (rate-derived, fractional).
		 * Skipped while delayCooldown runs (startup + after each
		 * adaptation) so one slow frame can't cascade the delay up.
		 */
		long fireEnd = System.nanoTime();
		double expectedMs = nominalPeriodMs(currentDelay, Utils.DELAY);
		double actualMs = lastFireNanos == 0 ? expectedMs : (fireEnd - lastFireNanos) / 1e6;
		if (delayCooldown > 0) {
			delayCooldown--;
			failedTime = 0;
		} else if (actualMs > expectedMs*1.5 || currentDelay < Utils.DELAY) {
			// We can't run so fast
			failedTime=Math.max(failedTime+1, 0);
			if (failedTime >= 2) {
				failedTime = 0;
				// +1 floor guarantees progress at 1-2ms delays
				// (uncapped/500fps), where int-truncated ×1.5
				// would spin restarting the timer at the same delay.
				currentDelay = Math.max(currentDelay + 1, (int) (currentDelay * 1.5));
				startLifeProcess(currentDelay);
				return;
			}
		} else {
			// Symmetric 1.5x hysteresis for both directions: with
			// FX overhead the interval sits a little above the
			// delay, and a tighter recover threshold wedges the
			// governor one step up (cooldown + resets never clear).
			if (actualMs < expectedMs*1.5 && currentDelay > Utils.DELAY) {
				// We can run faster
				failedTime=Math.min(failedTime-1, 0);
				if (failedTime <= -10) {
					currentDelay = Math.max(Utils.DELAY, currentDelay-1);
					startLifeProcess(currentDelay);
					return;
				}
			} else
				// Normal situation: we run at the expected speed
				failedTime = 0;
		}
		if (currentDelay > 1000) {
			pauseGame();
		}
		if (lastFireNanos != 0 && delayCooldown == 0 && failedTime == 0 && _isProcessActive
				&& !Double.isInfinite(effectiveRate(currentDelay, Utils.DELAY))) {
			double cost = actualMs - askedMs;
			smoothedCostMs += 0.1 * (cost - smoothedCostMs);
			if (smoothedCostMs < 0)
				smoothedCostMs = 0;
		}
		lastFireNanos = fireEnd;
	}

	public void changeLocale() {
		newGameAction.changeLocale();
		startStopAction.changeLocale();
		openGameAction.changeLocale();
		saveGameAction.changeLocale();
		saveGameAsAction.changeLocale();
		quitAction.changeLocale();
		statisticsAction.changeLocale();
		increaseCO2Action.changeLocale();
		decreaseCO2Action.changeLocale();
		parametersAction.changeLocale();
		labAction.changeLocale();
		killAllAction.changeLocale();
		disperseAllAction.changeLocale();
		manualAction.changeLocale();
		checkLastVersionAction.changeLocale();
		aboutAction.changeLocale();
		netConfigAction.changeLocale();
		manageConnectionsAction.changeLocale();
		stage.setTitle(Messages.getString("T_BIOGENESIS")); //$NON-NLS-1$
		refreshChrome();
		_visibleWorld.changeLocale();
		infoToolbar.changeLocale();
		Fx.run(() -> {
			sizeStatusSlots();
			updateStatusLabel();
		});
	}
}
