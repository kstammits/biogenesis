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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;


/**
 * JavaFX port of the genetic laboratory. Gene table, combo selectors and
 * spinners are JavaFX controls; the organism preview still renders through
 * {@link GeneticCode#draw} into a BufferedImage, converted with
 * ImageFx (manual PixelWriter copy). See {@link NetConfigWindow} for the
 * sim-thread to FX-thread handoff.
 */
public class LabWindow {
	protected MainWindow mainWindow;
	protected List<Gene> genesList;
	protected Button cancelButton;
	protected Button okButton;
	protected GridPane genesGrid;
	protected Canvas drawCanvas;
	protected ScrollPane genesScroll;
	protected Label energyLabel;
	protected Label segmentsLabel;
	protected ComboBox<String> symmetryCombo;
	protected ComboBox<String> mirrorCombo;
	protected ComboBox<String> disperseCombo;

	protected int symmetry=2;
	protected int energy=40;
	protected int mirror=0;
	protected boolean disperseChildren = false;

	private Stage stage;

	public LabWindow(MainWindow v, GeneticCode gc) {
		mainWindow = v;
		Fx.run(() -> init(gc));
	}

	public LabWindow(MainWindow v) {
		mainWindow = v;
		Fx.run(() -> init(v.getVisibleWorld().clippedGeneticCode));
	}

	private void init(GeneticCode gc) {
		genesList = new ArrayList<Gene>();
		if (gc != null)
			importGeneticCode(gc);

		stage = new Stage();
		stage.initOwner(mainWindow.getStage());
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setTitle(Messages.getString("T_GENETIC_LABORATORY")); //$NON-NLS-1$

		setComponents();

		okButton.setOnAction(evt -> {
			if (genesList.size() > 0)
				mainWindow.getVisibleWorld().setClippedGeneticCode(new GeneticCode(genesList,symmetry, mirror, disperseChildren));
			stage.close();
		});
		cancelButton.setOnAction(evt -> stage.close());

		stage.sizeToScene();
		stage.show();
	}

	/* Initialize the variables of this dialog using a previously existing genetic code */
	private void importGeneticCode(GeneticCode g) {
		energy = g.getReproduceEnergy();
		disperseChildren = g.getDisperseChildren();
		mirror = g.getMirror();
		symmetry = g.getSymmetry();
		for (int i=0; i<g.getNGenes(); i++)
			genesList.add((Gene)g.getGene(i).clone());
	}

	/* Initialize all the components of the dialog */
	protected void setComponents() {
		GridPane general = new GridPane();
		general.setHgap(8);
		general.setVgap(6);
		general.setPadding(new Insets(6));
		general.add(new Label(Messages.getString("T_SEGMENTS")), 0, 0); //$NON-NLS-1$
		segmentsLabel = new Label(Integer.toString(genesList.size() * symmetry));
		general.add(segmentsLabel, 1, 0);
		general.add(new Label(Messages.getString("T_ENERGY_TO_REPRODUCE")), 2, 0); //$NON-NLS-1$
		energyLabel = new Label(Integer.toString(energy));
		general.add(energyLabel, 3, 0);
		general.add(new Label(Messages.getString("T_STRESS_RESISTANCE")), 4, 0); //$NON-NLS-1$
		general.add(new Label(Double.toString(Utils.STRESS_RESISTANCE)), 5, 0);
		general.add(new Label(Messages.getString("T_SYMMETRY")), 0, 1); //$NON-NLS-1$
		symmetryCombo = new ComboBox<>();
		symmetryCombo.getItems().addAll("1","2","3","4","5","6","7","8");  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$
		symmetryCombo.getSelectionModel().select(Integer.toString(symmetry));
		symmetryCombo.setOnAction(evt -> {
			symmetry = Integer.parseInt(symmetryCombo.getSelectionModel().getSelectedItem());
			energy = 40 + 3 * symmetry * genesList.size();
			energyLabel.setText(Integer.toString(energy));
			segmentsLabel.setText(Integer.toString(genesList.size() * symmetry));
			redraw();
		});
		general.add(symmetryCombo, 1, 1);
		general.add(new Label(Messages.getString("T_MIRROR")), 2, 1); //$NON-NLS-1$
		String[] noyesValues = {Messages.getString("T_NO"), Messages.getString("T_YES")}; //$NON-NLS-1$ //$NON-NLS-2$
		mirrorCombo = new ComboBox<>();
		mirrorCombo.getItems().addAll(noyesValues);
		mirrorCombo.getSelectionModel().select(mirror);
		mirrorCombo.setOnAction(evt -> {
			mirror = mirrorCombo.getSelectionModel().getSelectedIndex();
			redraw();
		});
		general.add(mirrorCombo, 3, 1);
		general.add(new Label(Messages.getString("T_DISPERSE_CHILDREN")), 4, 1); //$NON-NLS-1$
		disperseCombo = new ComboBox<>();
		disperseCombo.getItems().addAll(noyesValues);
		disperseCombo.getSelectionModel().select(disperseChildren ? 1 : 0);
		disperseCombo.setOnAction(evt ->
			disperseChildren = disperseCombo.getSelectionModel().getSelectedIndex() != 0);
		general.add(disperseCombo, 5, 1);

		genesGrid = new GridPane();
		genesGrid.setHgap(8);
		genesGrid.setVgap(4);
		genesGrid.setPadding(new Insets(6));
		drawCanvas = new Canvas(200, 200);
		refreshGenesPanel();
		genesScroll = new ScrollPane(genesGrid);
		genesScroll.setPrefSize(440, 300);

		HBox middle = new HBox(8, genesScroll, drawCanvas);

		okButton = new Button(Messages.getString("T_COPY_TO_CLIPBOARD")); //$NON-NLS-1$
		okButton.setDefaultButton(true);
		cancelButton = new Button(Messages.getString("T_CANCEL")); //$NON-NLS-1$
		cancelButton.setCancelButton(true);
		Button clearButton = new Button(Messages.getString("T_CLEAR")); //$NON-NLS-1$
		clearButton.setOnAction(evt -> {
			genesList.clear();
			refreshGenesPanel();
		});
		Button importButton = new Button(Messages.getString("T_IMPORT")); //$NON-NLS-1$
		importButton.setOnAction(evt -> onImport());
		HBox buttons = new HBox(8, okButton, cancelButton, clearButton, importButton);

		VBox root = new VBox(8, general, middle, buttons);
		root.setPadding(new Insets(10));
		stage.setScene(new Scene(root));
	}

	private void onImport() {
		try {
			File f = Fx.showOpenFile("T_IMPORT", "T_BIOGENESIS_GENETIC_CODE_FILES", "bgg"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			if (f != null) {
				try {
					// Read XML code from file
					BioXMLParser parser = new BioXMLParser();
					GeneticCode g = parser.parseGeneticCode(f);
					genesList.clear();
					importGeneticCode(g);
					refreshGenesPanel();
				} catch (SAXParseException ex) {
					// Malformed XML or DTD mismatch: not a genetic-code file.
					System.err.println(ex.getMessage());
					errorAlert(Messages.getString("T_READ_ERROR"), Messages.getString("T_WRONG_FILE_VERSION")); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (SAXException ex) {
					// Well-formed XML but bad content (numbers, colors...).
					System.err.println(ex.getMessage());
					errorAlert(Messages.getString("T_READ_ERROR"), Messages.getString("T_CANT_READ_FILE")); //$NON-NLS-1$ //$NON-NLS-2$
				} catch (IOException ex) {
					System.err.println(ex.getMessage());
					errorAlert(Messages.getString("T_READ_ERROR"), Messages.getString("T_CANT_READ_FILE")); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		} catch (SecurityException ex) {
			System.err.println(ex.getMessage());
			errorAlert(Messages.getString("T_PERMISSION_DENIED"), Messages.getString("T_PERMISSION_DENIED")); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void errorAlert(String title, String message) {
		javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
				javafx.scene.control.Alert.AlertType.ERROR, message,
				javafx.scene.control.ButtonType.OK);
		alert.initOwner(stage);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.showAndWait();
	}

	private void onColorChanged(int index, ColorComboBox combo) {
		genesList.get(index).setColor(combo.getSelectedColor());
		redraw();
	}

	private void onAddGene() {
		Gene gene = new Gene(2.0,0.0,Color.GREEN);
		genesList.add(gene);
		refreshGenesPanel();
	}

	private void onDeleteGene(int index) {
		genesList.remove(index);
		refreshGenesPanel();
	}

	private void onInsertGene(int index) {
		Gene gene = new Gene(2.0,0.0,Color.GREEN);
		genesList.add(index, gene);
		refreshGenesPanel();
	}

	protected void refreshGenesPanel() {
		genesGrid.getChildren().clear();
		genesGrid.add(new Label(Messages.getString("T_GENE")), 0, 0); //$NON-NLS-1$
		genesGrid.add(new Label(Messages.getString("T_LENGTH")), 1, 0); //$NON-NLS-1$
		genesGrid.add(new Label(Messages.getString("T_ROTATION")), 2, 0); //$NON-NLS-1$
		genesGrid.add(new Label(Messages.getString("T_COLOR2")), 3, 0); //$NON-NLS-1$
		for (int i = 0; i < genesList.size(); i++) {
			final Gene gene = genesList.get(i);
			final int index = i;
			genesGrid.add(new Label(i + ": "), 0, i + 1); //$NON-NLS-1$

			Spinner<Double> lengthSpinner = new Spinner<>(
					new SpinnerValueFactory.DoubleSpinnerValueFactory(2.0, 18.0, gene.getLength(), 0.1));
			lengthSpinner.setEditable(true);
			lengthSpinner.setPrefWidth(80);
			lengthSpinner.valueProperty().addListener((obs, was, is) -> {
				gene.setLength(is);
				redraw();
			});
			genesGrid.add(lengthSpinner, 1, i + 1);

			int initialTheta = (int) Math.round(gene.getTheta() * 180.0 / Math.PI);
			while (initialTheta < 0) initialTheta += 360;
			while (initialTheta > 359) initialTheta -= 360;
			SpinnerValueFactory.IntegerSpinnerValueFactory thetaFactory =
					new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 359, initialTheta, 1);
			thetaFactory.setWrapAround(true);
			Spinner<Integer> thetaSpinner = new Spinner<>(thetaFactory);
			thetaSpinner.setEditable(true);
			thetaSpinner.setPrefWidth(70);
			thetaSpinner.valueProperty().addListener((obs, was, is) -> {
				gene.setTheta(is * Math.PI / 180.0);
				redraw();
			});
			genesGrid.add(thetaSpinner, 2, i + 1);

			ColorComboBox colorCombo = new ColorComboBox(gene.getColor());
			colorCombo.setOnAction(evt -> onColorChanged(index, colorCombo));
			genesGrid.add(colorCombo, 3, i + 1);

			Button insertButton = new Button(Messages.getString("T_INSERT")); //$NON-NLS-1$
			insertButton.setOnAction(evt -> onInsertGene(index));
			genesGrid.add(insertButton, 4, i + 1);

			Button deleteButton = new Button(Messages.getString("T_DELETE")); //$NON-NLS-1$
			deleteButton.setOnAction(evt -> onDeleteGene(index));
			genesGrid.add(deleteButton, 5, i + 1);
		}
		Button addButton = new Button(Messages.getString("T_ADD")); //$NON-NLS-1$
		addButton.setOnAction(evt -> onAddGene());
		genesGrid.add(addButton, 0, genesList.size() + 1);

		energy = 40 + 3 * symmetry * genesList.size();
		energyLabel.setText(Integer.toString(energy));
		segmentsLabel.setText(Integer.toString(genesList.size() * symmetry));
		redraw();
	}

	/** Preview the current gene list through the AWT renderer, converted for FX. */
	protected void redraw() {
		if (drawCanvas == null)
			return; // still under construction
		int w = (int) drawCanvas.getWidth();
		int h = (int) drawCanvas.getHeight();
		BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
		java.awt.Graphics2D g = image.createGraphics();
		try {
			g.setBackground(java.awt.Color.BLACK);
			g.clearRect(0, 0, w, h);
			new GeneticCode(genesList, symmetry, mirror, false).draw(g, w, h);
		} finally {
			g.dispose();
		}
		WritableImage fxImage = ImageFx.toFXImage(image);
		drawCanvas.getGraphicsContext2D().drawImage(fxImage, 0, 0);
	}
}
