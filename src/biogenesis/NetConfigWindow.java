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


import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


/**
 * JavaFX port of the network configuration dialog.
 * The window is built on the FX Application Thread via {@link Fx} (both call
 * sites are fire-and-forget, so modal blocking is irrelevant).
 */
public class NetConfigWindow {
	protected CheckBox acceptConnectionsCheck;
	protected boolean acceptConnections;
	protected TextField localPortText;
	protected TextField maxConnectionsText;
	protected Button cancelButton;
	protected Button okButton;

	protected MainWindow mainWindow;
	private Stage stage;

	public NetConfigWindow(MainWindow parent) {
		mainWindow = parent;
		acceptConnections = mainWindow.isAcceptingConnections();
		Fx.run(this::buildAndShow);
	}

	private void buildAndShow() {
		stage = new Stage();
		stage.initOwner(mainWindow.getStage());
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setTitle(Messages.getString("T_NETWORK_CONFIGURATION")); //$NON-NLS-1$
		stage.setResizable(false);

		acceptConnectionsCheck = new CheckBox(Messages.getString("T_ALLOW_CONNECTIONS_FROM_OTHER_USERS")); //$NON-NLS-1$
		acceptConnectionsCheck.setSelected(acceptConnections);
		acceptConnectionsCheck.selectedProperty().addListener(
			(obs, oldValue, newValue) -> acceptConnections = newValue);

		GridPane grid = new GridPane();
		grid.setHgap(8);
		grid.setVgap(8);
		grid.add(new Label(Messages.getString("T_MAXIMUM_NUMBER_OF_ALLOWED_CONNECTIONS")), 0, 0); //$NON-NLS-1$
		maxConnectionsText = new TextField(String.valueOf(Utils.MAX_CONNECTIONS));
		maxConnectionsText.setPrefColumnCount(4);
		grid.add(maxConnectionsText, 1, 0);
		grid.add(new Label(Messages.getString("T_LOCAL_PORT_TO_RECEIVE_CONNECTIONS")), 0, 1); //$NON-NLS-1$
		localPortText = new TextField(String.valueOf(Utils.LOCAL_PORT));
		localPortText.setPrefColumnCount(6);
		grid.add(localPortText, 1, 1);

		okButton = new Button(Messages.getString("T_OK")); //$NON-NLS-1$
		okButton.setDefaultButton(true);
		okButton.setOnAction(evt -> onOk());
		cancelButton = new Button(Messages.getString("T_CANCEL")); //$NON-NLS-1$
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(evt -> stage.close());
		HBox buttons = new HBox(8, okButton, cancelButton);

		VBox root = new VBox(10, acceptConnectionsCheck, grid, buttons);
		root.setPadding(new Insets(10));
		stage.setScene(new Scene(root));
		stage.sizeToScene();
		stage.show();
	}

	private void onOk() {
		int oldPort = Utils.LOCAL_PORT;
		checkParams();
		Utils.savePreferences();
		if (oldPort != Utils.LOCAL_PORT) {
			mainWindow.stopServer();
			if (mainWindow.isAcceptingConnections())
				mainWindow.startServer();
		}
		stage.close();
	}

	void checkParams() {
		int i;
		mainWindow.setAcceptConnections(acceptConnections);

		try {
			i = Integer.parseInt(localPortText.getText());
			if (i>=0) Utils.LOCAL_PORT = i;
		} catch (NumberFormatException e) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(maxConnectionsText.getText());
			if (i>=0) Utils.MAX_CONNECTIONS = i;
		} catch (NumberFormatException e) {
			// Keep old value if there is a problem
		}
	}
}
