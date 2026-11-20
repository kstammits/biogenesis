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


import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


/**
 * JavaFX port of the network connections window.
 * Built on the FX thread via {@link Fx}.
 */
public class NetConnectionsWindow {

	protected MainWindow mainWindow;

	private TextField ipText, portText;
	private GridPane connectionsGrid;
	private Stage stage;

	public String getPortText() {
		return portText.getText();
	}
	public String getIPText() {
		return ipText.getText();
	}

	public NetConnectionsWindow(MainWindow parent) {
		mainWindow = parent;
		Fx.run(this::buildAndShow);
	}

	private void buildAndShow() {
		stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle(Messages.getString("T_NETWORK_CONNECTIONS")); //$NON-NLS-1$
		stage.setResizable(false);

		GridPane form = new GridPane();
		form.setHgap(8);
		form.setVgap(8);
		form.add(new Label(Messages.getString("T_IP")), 0, 0); //$NON-NLS-1$
		ipText = new TextField();
		ipText.setPrefColumnCount(15);
		form.add(ipText, 1, 0);
		form.add(new Label(Messages.getString("T_PORT")), 2, 0); //$NON-NLS-1$
		portText = new TextField();
		portText.setPrefColumnCount(5);
		form.add(portText, 3, 0);
		Button newConnectionButton = new Button(Messages.getString("T_NEW_CONNECTION")); //$NON-NLS-1$
		newConnectionButton.setDefaultButton(true);
		newConnectionButton.setOnAction(evt -> onNewConnection());
		form.add(newConnectionButton, 4, 0);

		connectionsGrid = new GridPane();
		connectionsGrid.setHgap(8);
		connectionsGrid.setVgap(4);
		refreshConnectionsPanel();
		ScrollPane scroll = new ScrollPane(connectionsGrid);
		scroll.setPrefSize(440, 300);

		VBox root = new VBox(10, form, scroll);
		root.setPadding(new Insets(10));
		stage.setScene(new Scene(root));
		stage.sizeToScene();
		stage.show();
	}

	private void onNewConnection() {
		try {
			int port = Integer.parseInt(getPortText());
			if (port > 0) {
				try {
					InetAddress address = InetAddress.getByName(getIPText());
					NetServerThread netServer = mainWindow.startServer();
					Connection connection = netServer.newConnection(address, port);
					if (connection != null)
						connection.connect();
					refreshConnectionsPanel();
				} catch (UnknownHostException e) {
					errorAlert(Messages.getString("T_CONNECTION_FAILED"), //$NON-NLS-1$
							Messages.getString("T_CANT_STABLISH_CONNECTION")+e.getLocalizedMessage()); //$NON-NLS-1$
				}
			}
		} catch (NumberFormatException e) {
			errorAlert(Messages.getString("T_CONNECTION_FAILED"), //$NON-NLS-1$
					Messages.getString("T_PORT_MUST_BE_A_NUMBER")); //$NON-NLS-1$
		}
	}

	private void errorAlert(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.showAndWait();
	}

	protected void refreshConnectionsPanel() {
		connectionsGrid.getChildren().clear();
		NetServerThread netServer = mainWindow.getNetServer();
		if (netServer != null) {
			List<Connection> connections = netServer.getConnections();
			synchronized(connections) {
				int i = 0;
				for (Connection c : connections) {
					connectionsGrid.add(new Label(c.getRemoteAddress().toString()), 0, i);
					connectionsGrid.add(new Label(Integer.toString(c.getRemotePort())), 1, i);
					Button button = new Button(Messages.getString("T_DISCONNECT")); //$NON-NLS-1$
					final Connection target = c;
					button.setOnAction(evt -> {
						target.send(NetServerThread.DISCONNECT);
						target.setState(Connection.STATE_DISCONNECTED);
						refreshConnectionsPanel();
					});
					connectionsGrid.add(button, 2, i);
					i++;
				}
			}
		}
	}
}
