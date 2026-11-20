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

import java.text.NumberFormat;
import java.util.*;

import javafx.scene.Scene;
import javafx.scene.control.Button;
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
import javafx.scene.layout.VBox;
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
	protected Label _statusLabel;
	protected ToolBar toolBar;

	protected InfoToolbar infoToolbar;

	protected Stage stage;

	private String statusMessage=""; //$NON-NLS-1$
	private StringBuilder statusLabelText = new StringBuilder(100);
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
	 * Wraps a command in an FX toolbar button, snapshotting its live text,
	 * icon, tooltip and enabled state. Chrome rebuilds via refreshChrome().
	 */
	Button toolButtonFor(StdAction action) {
		Button button = new Button(action.getName(), iconView(action));
		String desc = action.getDescription();
		if (desc != null)
			button.setTooltip(new Tooltip(desc));
		button.setDisable(!action.isEnabled());
		button.setOnAction(evt -> action.perform());
		return button;
	}

	/**
	 * Wraps a command in an FX menu item, keeping the icon policy of the
	 * old menu (icons dropped where the actions nulled them).
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
		private static final long serialVersionUID = 1L;
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
		private static final long serialVersionUID = 1L;
		protected String name2;
		protected String description2;
		protected int mnemonic2;
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
			mnemonic2 = Messages.getMnemonic(text2);
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
			int auxmnemonic = mnemonic;
			mnemonic = mnemonic2;
			mnemonic2 = auxmnemonic;
			active = !active;
		}

		@Override
		public void changeLocale() {
			super.changeLocale();
			name2 = Messages.getString(name2_key);
			description2 = Messages.getString(desc2_key);
			mnemonic2 = Messages.getMnemonic(name2_key);
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
		private static final long serialVersionUID = 1L;
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
		private static final long serialVersionUID = 1L;
		public IncreaseCO2Action(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.addCO2(500);
		}
	}

	class DecreaseCO2Action extends StdAction {
		private static final long serialVersionUID = 1L;
		public DecreaseCO2Action(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.decreaseCO2(500);
		}
	}

	class ManageConnectionsAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public ManageConnectionsAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			netConnectionsWindow();
		}
	}

	class AbortTrackingAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public AbortTrackingAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			setTrackedOrganism(null);
		}
	}

	class OpenGameAction extends StdAction {
		private static final long serialVersionUID = 1L;
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
						_statisticsWindow.dispose();
						_statisticsWindow = null;
					}
					//_world.clean();
					ObjectInputStream inputStream;
					try {
						FileInputStream fileStream = new FileInputStream(f);
						inputStream = new ObjectInputStream(fileStream);
						_world = (World) inputStream.readObject();
						inputStream.close();
						_gameFile = f;
						_trackedOrganism = null;
						processState = true;
						setStatusMessage(Messages.getString("T_WORLD_LOADED_SUCCESSFULLY")); //$NON-NLS-1$
					} catch (InvalidClassException ex) {
						// Serial version mismatch (e.g. pre-800 save): report as
						// wrong version, not generic read failure.
						System.err.println(ex.getMessage());
						Fx.error(Messages.getString("T_READ_ERROR"), Messages.getString("T_WRONG_FILE_VERSION")); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (IOException ex) {
						System.err.println(ex.getMessage());
						Fx.error(Messages.getString("T_READ_ERROR"), Messages.getString("T_CANT_READ_FILE")); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (ClassNotFoundException ex) {
						System.err.println(ex.getMessage());
						Fx.error(Messages.getString("T_READ_ERROR"), Messages.getString("T_WRONG_FILE_TYPE")); //$NON-NLS-1$ //$NON-NLS-2$
					} catch (ClassCastException ex) {
						System.err.println(ex.getMessage());
						Fx.error(Messages.getString("T_READ_ERROR"), Messages.getString("T_WRONG_FILE_VERSION")); //$NON-NLS-1$ //$NON-NLS-2$
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
		private static final long serialVersionUID = 1L;
		public SaveGameAsAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			saveGameAs();
		}
	}

	class QuitAction extends StdAction {
		private static final long serialVersionUID = 1L;
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
		private static final long serialVersionUID = 1L;
		public StatisticsAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			if (_statisticsWindow != null)
				_statisticsWindow.dispose();
			_statisticsWindow = _world.createStatisticsWindow(MainWindow.this);
		}
	}

	class LabAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public LabAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			new LabWindow(MainWindow.this);
		}
	}

	class KillAllAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public KillAllAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.killAll();
		}
	}

	class DisperseAllAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public DisperseAllAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			_world.disperseAll();
		}
	}

	class ParametersAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public ParametersAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			paramDialog();
		}
	}

	class ManualAction extends StdAction {
		private static final long serialVersionUID = 1L;
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
		private static final long serialVersionUID = 1L;
		public CheckLastVersionAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			checkLastVersion();
		}
	}

	class AboutAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public AboutAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			about();
		}
	}

	class NetConfigAction extends StdAction {
		private static final long serialVersionUID = 1L;
		public NetConfigAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			netConfig();
		}
	}

	class ToggleEfficiencyModeAction extends StdAction {
		private static final long serialVersionUID = 1L;
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
		private static final long serialVersionUID = 1L;
		public ZoomInAction(String text, String icon_path, String desc) {
			super(text, icon_path, desc);
		}

		public void perform() {
			if (_visibleWorld != null)
				_visibleWorld.zoomIn();
		}
	}

	class ZoomOutAction extends StdAction {
		private static final long serialVersionUID = 1L;
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

		_statusLabel = new Label(" "); //$NON-NLS-1$
		_statusLabel.setStyle("-fx-border-color: grey; -fx-padding: 2;"); //$NON-NLS-1$
		_nf = NumberFormat.getInstance();
		_nf.setMaximumFractionDigits(1);

		BorderPane root = new BorderPane();
		VBox top = new VBox(menuBar, toolBar);
		root.setTop(top);
		root.setCenter(scrollPane);
		VBox bottom = new VBox(infoToolbar, _statusLabel);
		root.setBottom(bottom);
		stage.setScene(new Scene(root));
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
						try {
							BioXMLParser.writeGeneticCode(new PrintStream(f),(GeneticCode)obj);
							resultFile = f;
						} catch (FileNotFoundException ex) {
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
		ObjectOutputStream outputStream;
		try {
			FileOutputStream fileStream = new FileOutputStream(f);
			outputStream = new ObjectOutputStream(fileStream);
			outputStream.writeObject(obj);
			outputStream.close();
			setStatusMessage(Messages.getString("T_WRITING_COMPLETED")); //$NON-NLS-1$
			return true;
		} catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (SecurityException ex) {
			System.err.println(ex.getMessage());
			Fx.error(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return false;
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
			statusLabelText.setLength(0);
			statusLabelText.append(Messages.getString("T_FPS")); //$NON-NLS-1$
			statusLabelText.append(getFPS());
			statusLabelText.append("     "); //$NON-NLS-1$
			statusLabelText.append(Messages.getString("T_TIME")); //$NON-NLS-1$
			statusLabelText.append(_world.getTime());
			statusLabelText.append("     "); //$NON-NLS-1$
			statusLabelText.append(Messages.getString("T_CURRENT_POPULATION")); //$NON-NLS-1$
			statusLabelText.append(_world.getPopulation());
			statusLabelText.append("     "); //$NON-NLS-1$
			statusLabelText.append(Messages.getString("T_O2")); //$NON-NLS-1$
			statusLabelText.append(_nf.format(_world.getO2()));
			statusLabelText.append("     "); //$NON-NLS-1$
			statusLabelText.append(Messages.getString("T_CO2")); //$NON-NLS-1$
			statusLabelText.append(_nf.format(_world.getCO2()));
			statusLabelText.append("     "); //$NON-NLS-1$
			statusLabelText.append(getStatusMessage());
			_statusLabel.setText(statusLabelText.toString());
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
	 * Decoupled render target: sim ticks run every {@link #currentDelay} ms,
	 * repaints happen at most this often. Keeps a 200Hz sim from paying a
	 * full-world blit per tick.
	 */
	protected int renderIntervalMs = 33;
	protected long lastRenderMs = 0;
	/** Measured ticks/sec state, sampled on the FX thread in lifeProcess. */
	protected int measuredFps = -1;
	protected long fpsWindowStartMs = 0;
	protected int fpsWindowFrames = 0;

	final transient Runnable lifeProcess = new Runnable() {
	    public void run() {
	    	if (_isProcessActive) {
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
	    			if (elapsed > 0)
	    				measuredFps = (int) Math.round(fpsWindowFrames * 1000.0 / elapsed);
	    			fpsWindowStartMs = nowMs;
	    			fpsWindowFrames = 0;
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
	 * Number of milliseconds between two invocations to the lifeProcess invocation.
	 * It starts as the user's preference DELAY but is adapted to the current situation
	 * in the world and the machine speed.
	 */
	protected int currentDelay = Utils.DELAY;
	/**
	 * If positive, the consecutive number of frames that haven't accomplished the expected time contract.
	 * If negative, the consecutive number of frame that have accomplished the expected time contract.
	 * Used to adapt the program's speed.
	 */
	protected int failedTime=0;
	/**
	 * The moment when the last painting of this visual world started. Used to control
	 * the program's speed.
	 */
	protected long lastPaintTime;
	/**
	 * Settle-down ticks after a (re)start before the governor may adapt the
	 * delay again. Covers cold-start JIT/warmup (where the first deltas are
	 * meaningless — lastPaintTime starts at 0) and damps backoff oscillation.
	 */
	protected int delayCooldown = 0;
	public void startLifeProcess(int delay) {
		if (updateTask != null)
			updateTask.cancel();
		delayCooldown = 40;
		updateTask = new TimerTask() {
		    @Override
			public void run() {
		    	try {
					Fx.ask(() -> {
						lifeProcess.run();
						return null;
					});
					/**
					 * Checks the actual drawing speed and increases or decreases the speed
					 * of the program in order to keep it running smoothly.
					 * Skipped while delayCooldown runs (startup + after each
					 * adaptation) so one slow frame can't cascade the delay up.
					 */
					long actualTime = System.currentTimeMillis();
					if (delayCooldown > 0) {
						delayCooldown--;
						failedTime = 0;
					} else if (actualTime - lastPaintTime > currentDelay*1.5 || currentDelay < Utils.DELAY) {
		    			// We can't run so fast
		    			failedTime=Math.max(failedTime+1, 0);
		    			if (failedTime >= 2) {
		    				failedTime = 0;
		    				currentDelay*=1.5;
		    				startLifeProcess(currentDelay);
		    			}
					} else {
		    			// Symmetric 1.5x hysteresis for both directions: with
		    			// FX overhead the interval sits a little above the
		    			// delay, and a tighter recover threshold wedges the
		    			// governor one step up (cooldown + resets never clear).
		    			if (actualTime - lastPaintTime < currentDelay*1.5 && currentDelay > Utils.DELAY) {
		    				// We can run faster
		    				failedTime=Math.min(failedTime-1, 0);
		    				if (failedTime <= -10) {
		    					currentDelay = Math.max(Utils.DELAY, currentDelay-1);
		    					startLifeProcess(currentDelay);
		    				}
		    			} else
		    				// Normal situation: we run at the expected speed
		    				failedTime = 0;
		    		}
		    		if (currentDelay > 1000) {
		    			pauseGame();
		    		}
		    		lastPaintTime = actualTime;
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
		    }
		};
		_timer.schedule(updateTask, delay, delay);
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
	}
}
