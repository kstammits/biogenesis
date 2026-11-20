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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Modality;
import javafx.stage.Stage;


/**
 * JavaFX port of the parameters dialog. Same five tabs, same fields, same
 * validation semantics; layout uses GridPane rows instead of FlowLayout rows.
 * See {@link NetConfigWindow} for the Swing-EDT to FX-thread handoff.
 */
public class ParamDialog {
	private ComboBox<String> localeCombo = null;
	private TextField widthText = null;
	private TextField heightText = null;
	private TextField delayText = null;
	protected RadioButton hardwareNoneRadio = null;
	protected RadioButton hardwareOpenGLRadio = null;
	protected CheckBox hardwareFBObjectCheck = null;
	private TextField frictionText = null;
	private TextField elasticityText = null;
	private TextField initialnumberText = null;
	private TextField initialenergyText = null;
	private TextField initialO2Text = null;
	private TextField initialCO2Text = null;
	private TextField stressresistanceText = null;
	private TextField majormutationrateText = null;
	private TextField minormutationrateText = null;
	private TextField minormutationamplitudeText = null;
	private TextField spontaneousmutationrateText = null;
	private TextField redcostText = null;
	private TextField greencostText = null;
	private TextField bluecostText = null;
	private TextField cyancostText = null;
	private TextField whitecostText = null;
	private TextField graycostText = null;
	private TextField yellowcostText = null;
	private TextField segmentcostText = null;
	private TextField drainText = null;
	private TextField greenenergyText = null;
	private TextField organicenergyText = null;
	private TextField organicsubsText = null;
	private TextField redprobText = null;
	private TextField greenprobText = null;
	private TextField blueprobText = null;
	private TextField cyanprobText = null;
	private TextField whiteprobText = null;
	private TextField grayprobText = null;
	private TextField yellowprobText = null;
	private TextField decayenergyText = null;
	private Button OKButton = null;
	private Button cancelButton = null;
	private Button defaultButton = null;

	protected MainWindow mainWindow;
	private Stage stage;

	public ParamDialog(MainWindow parent) {
		mainWindow = parent;
		Fx.run(this::buildAndShow);
	}

	private void buildAndShow() {
		stage = new Stage();
		stage.initModality(Modality.APPLICATION_MODAL);
		stage.setTitle(Messages.getString("T_PARAMETERS_CONFIGURATION")); //$NON-NLS-1$
		stage.setResizable(false);

		OKButton = new Button(Messages.getString("T_OK")); //$NON-NLS-1$
		OKButton.setDefaultButton(true);
		OKButton.setOnAction(evt -> { checkParams(); Utils.savePreferences(); stage.close(); });
		cancelButton = new Button(Messages.getString("T_CANCEL")); //$NON-NLS-1$
		cancelButton.setCancelButton(true);
		cancelButton.setOnAction(evt -> stage.close());
		defaultButton = new Button(Messages.getString("T_DEFAULT_VALUES")); //$NON-NLS-1$
		defaultButton.setOnAction(evt -> defaultPreferences());
		HBox buttons = new HBox(8, OKButton, cancelButton, defaultButton);
		buttons.setAlignment(Pos.CENTER);

		TabPane tabs = new TabPane();
		tabs.getTabs().add(new Tab(Messages.getString("T_GENERAL"), setGeneralTab())); //$NON-NLS-1$
		tabs.getTabs().add(new Tab(Messages.getString("T_WORLD"), setWorldTab())); //$NON-NLS-1$
		tabs.getTabs().add(new Tab(Messages.getString("T_ORGANISMS"), setOrganismsTab())); //$NON-NLS-1$
		tabs.getTabs().add(new Tab(Messages.getString("T_METABOLISM"), setMetabolismTab())); //$NON-NLS-1$
		tabs.getTabs().add(new Tab(Messages.getString("T_GENES"), setGenesTab())); //$NON-NLS-1$
		tabs.getTabs().forEach(t -> t.setClosable(false));

		VBox root = new VBox(10, tabs, buttons);
		root.setPadding(new Insets(10));
		stage.setScene(new Scene(root));
		stage.sizeToScene();
		stage.show();
	}

	private static TextField numField(String initial) {
		TextField f = new TextField(initial);
		f.setPrefColumnCount(6);
		return f;
	}

	private static GridPane grid() {
		GridPane g = new GridPane();
		g.setHgap(8);
		g.setVgap(6);
		return g;
	}

	protected void defaultPreferences() {
		widthText.setText(String.valueOf(Utils.DEF_WORLD_WIDTH));
		heightText.setText(String.valueOf(Utils.DEF_WORLD_HEIGHT));
		delayText.setText(String.valueOf(Utils.DEF_DELAY));
		frictionText.setText(String.valueOf(Utils.DEF_FRICTION));
		elasticityText.setText(String.valueOf(Utils.DEF_ELASTICITY));
		initialnumberText.setText(String.valueOf(Utils.DEF_INITIAL_ORGANISMS));
		initialenergyText.setText(String.valueOf(Utils.DEF_INITIAL_ENERGY));
		initialO2Text.setText(String.valueOf(Utils.DEF_INITIAL_O2));
		initialCO2Text.setText(String.valueOf(Utils.DEF_INITIAL_CO2));
		stressresistanceText.setText(String.valueOf(Utils.DEF_STRESS_RESISTANCE));
		majormutationrateText.setText(String.valueOf(Utils.DEF_MAJOR_MUTATION_RATE));
		minormutationrateText.setText(String.valueOf(Utils.DEF_MINOR_MUTATION_RATE));
		minormutationamplitudeText.setText(String.valueOf(Utils.DEF_MINOR_MUTATION_AMPLITUDE));
		spontaneousmutationrateText.setText(String.valueOf(Utils.DEF_SPONTANEOUS_MUTATION_RATE));
		segmentcostText.setText(String.valueOf(Utils.DEF_SEGMENT_COST_DIVISOR));
		drainText.setText(String.valueOf(Utils.DEF_DRAIN_SUBS_DIVISOR));
		greenenergyText.setText(String.valueOf(Utils.DEF_GREEN_OBTAINED_ENERGY_DIVISOR));
		organicenergyText.setText(String.valueOf(Utils.DEF_ORGANIC_OBTAINED_ENERGY));
		organicsubsText.setText(String.valueOf(Utils.DEF_ORGANIC_SUBS_PRODUCED));
		redcostText.setText(String.valueOf(Utils.DEF_RED_ENERGY_CONSUMPTION));
		greencostText.setText(String.valueOf(Utils.DEF_GREEN_ENERGY_CONSUMPTION));
		bluecostText.setText(String.valueOf(Utils.DEF_BLUE_ENERGY_CONSUMPTION));
		cyancostText.setText(String.valueOf(Utils.DEF_CYAN_ENERGY_CONSUMPTION));
		whitecostText.setText(String.valueOf(Utils.DEF_WHITE_ENERGY_CONSUMPTION));
		graycostText.setText(String.valueOf(Utils.DEF_GRAY_ENERGY_CONSUMPTION));
		yellowcostText.setText(String.valueOf(Utils.DEF_YELLOW_ENERGY_CONSUMPTION));
		redprobText.setText(String.valueOf(Utils.DEF_RED_PROB));
		greenprobText.setText(String.valueOf(Utils.DEF_GREEN_PROB));
		blueprobText.setText(String.valueOf(Utils.DEF_BLUE_PROB));
		cyanprobText.setText(String.valueOf(Utils.DEF_CYAN_PROB));
		whiteprobText.setText(String.valueOf(Utils.DEF_WHITE_PROB));
		grayprobText.setText(String.valueOf(Utils.DEF_GRAY_PROB));
		yellowprobText.setText(String.valueOf(Utils.DEF_YELLOW_PROB));
		decayenergyText.setText(String.valueOf(Utils.DEF_DECAY_ENERGY));
		switch (Utils.DEF_HARDWARE_ACCELERATION) {
		case 0:
		case 2:
		case 5:
			hardwareNoneRadio.setSelected(true);
			hardwareFBObjectCheck.setSelected(false);
			hardwareFBObjectCheck.setDisable(true);
			break;
		case 1:
		case 3:
			hardwareOpenGLRadio.setSelected(true);
			hardwareFBObjectCheck.setSelected(false);
			break;
		case 6:
			hardwareOpenGLRadio.setSelected(true);
			hardwareFBObjectCheck.setSelected(true);
			break;
		}
	}

	protected VBox setGeneralTab() {
		VBox general = new VBox(8);
		general.setPadding(new Insets(6));
		// Language
		HBox lang = new HBox(8,
				new Label(Messages.getString("T_LANGUAGE")), //$NON-NLS-1$
				localeCombo = new ComboBox<>());
		localeCombo.getItems().addAll(Messages.getSupportedLocalesNames());
		localeCombo.getSelectionModel().select(Messages.getLocaleIndex());
		lang.setAlignment(Pos.CENTER_LEFT);
		general.getChildren().add(lang);
		// Frames per second
		delayText = numField(Integer.toString(Utils.DELAY));
		HBox delay = new HBox(8,
				new Label(Messages.getString("T_TIME_PER_FRAME")), delayText, //$NON-NLS-1$
				new Label(Messages.getString("T_MILLISECONDS"))); //$NON-NLS-1$
		delay.setAlignment(Pos.CENTER_LEFT);
		general.getChildren().add(delay);
		// OpenGL
		hardwareNoneRadio = new RadioButton(Messages.getString("T_NONE")); //$NON-NLS-1$
		hardwareOpenGLRadio = new RadioButton(Messages.getString("T_OPENGL")); //$NON-NLS-1$
		hardwareFBObjectCheck = new CheckBox(Messages.getString("T_DISABLE_FBOBJECT")); //$NON-NLS-1$
		ToggleGroup hardwareGroup = new ToggleGroup();
		hardwareNoneRadio.setToggleGroup(hardwareGroup);
		hardwareOpenGLRadio.setToggleGroup(hardwareGroup);
		switch (Utils.HARDWARE_ACCELERATION) {
		case 0:
			hardwareNoneRadio.setSelected(true);
			hardwareFBObjectCheck.setDisable(true);
			break;
		case 1:
		case 2:
		case 3:
			hardwareOpenGLRadio.setSelected(true);
			break;
		case 4:
		case 5:
		case 6:
			hardwareOpenGLRadio.setSelected(true);
			hardwareFBObjectCheck.setSelected(true);
			break;
		}
		hardwareNoneRadio.selectedProperty().addListener(
			(obs, was, is) -> hardwareFBObjectCheck.setDisable(is));
		VBox hw = new VBox(6, hardwareNoneRadio, hardwareOpenGLRadio, hardwareFBObjectCheck,
				new Label(Messages.getString("T_DIRECTX_IS_AUTOMATICALLY_DETECTED_AND_INITIALIZED")), //$NON-NLS-1$
				new Label(Messages.getString("T_APPLICATION_MUST_BE_RESTARTED_TO_APPLY_CHANGES"))); //$NON-NLS-1$
		hw.setPadding(new Insets(6));
		TitledPane hwPane = new TitledPane(Messages.getString("T_HARDWARE_ACCELERATION"), hw); //$NON-NLS-1$
		hwPane.setCollapsible(false);
		general.getChildren().add(hwPane);
		return general;
	}

	protected GridPane setWorldTab() {
		GridPane world = grid();
		world.setPadding(new Insets(6));
		widthText = numField(Integer.toString(Utils.WORLD_WIDTH));
		heightText = numField(Integer.toString(Utils.WORLD_HEIGHT));
		world.addRow(0, new Label(Messages.getString("T_WIDTH")), widthText, //$NON-NLS-1$
				new Label(Messages.getString("T_HEIGHT")), heightText); //$NON-NLS-1$
		initialO2Text = numField(Double.toString(Utils.INITIAL_O2));
		initialCO2Text = numField(Double.toString(Utils.INITIAL_CO2));
		world.addRow(1, new Label(Messages.getString("T_INITIAL_OXYGEN")), initialO2Text, //$NON-NLS-1$
				new Label(Messages.getString("T_INITIAL_CARBON_DIOXIDE")), initialCO2Text); //$NON-NLS-1$
		frictionText = numField(Double.toString(Utils.FRICTION));
		world.addRow(2, new Label(Messages.getString("T_FRICTION_COEFFICIENT")), frictionText); //$NON-NLS-1$
		elasticityText = numField(Double.toString(Utils.ELASTICITY));
		world.addRow(3, new Label(Messages.getString("T_ELASTICITY_COEFFICIENT")), elasticityText); //$NON-NLS-1$
		return world;
	}

	protected GridPane setOrganismsTab() {
		GridPane org = grid();
		org.setPadding(new Insets(6));
		initialnumberText = numField(Integer.toString(Utils.INITIAL_ORGANISMS));
		initialenergyText = numField(Integer.toString(Utils.INITIAL_ENERGY));
		org.addRow(0, new Label(Messages.getString("T_INITIAL_NUMBER")), initialnumberText, //$NON-NLS-1$
				new Label(Messages.getString("T_INITIAL_ENERGY")), initialenergyText); //$NON-NLS-1$
		// X-Mod: Stress resistance replaces life expectancy;
		// major mutation rate replaces vanilla mutation percentage.
		stressresistanceText = numField(Double.toString(Utils.STRESS_RESISTANCE));
		majormutationrateText = numField(Double.toString(Utils.MAJOR_MUTATION_RATE));
		org.addRow(1, new Label(Messages.getString("T_STRESS_RESISTANCE")), stressresistanceText, //$NON-NLS-1$
				new Label(Messages.getString("T_MAJOR_MUTATION_RATE")), majormutationrateText); //$NON-NLS-1$
		// X-Mod: Minor rate - Minor amplitude
		minormutationrateText = numField(Double.toString(Utils.MINOR_MUTATION_RATE));
		minormutationamplitudeText = numField(Double.toString(Utils.MINOR_MUTATION_AMPLITUDE));
		org.addRow(2, new Label(Messages.getString("T_MINOR_MUTATION_RATE")), minormutationrateText, //$NON-NLS-1$
				new Label(Messages.getString("T_MINOR_MUTATION_AMPLITUDE")), minormutationamplitudeText); //$NON-NLS-1$
		// X-Mod: Spontaneous mutation rate
		spontaneousmutationrateText = numField(Double.toString(Utils.SPONTANEOUS_MUTATION_RATE));
		org.addRow(3, new Label(Messages.getString("T_SPONTANEOUS_MUTATION_RATE")), spontaneousmutationrateText); //$NON-NLS-1$
		segmentcostText = numField(Integer.toString(Utils.SEGMENT_COST_DIVISOR));
		org.addRow(4, new Label(Messages.getString("T_UPKEEP_COST_DIVISOR")), segmentcostText); //$NON-NLS-1$
		drainText = numField(Integer.toString(Utils.DRAIN_SUBS_DIVISOR));
		org.addRow(5, new Label(Messages.getString("T_SUBSTANCE_DRAINAGE_DIVISOR")), drainText); //$NON-NLS-1$
		decayenergyText = numField(Double.toString(Utils.DECAY_ENERGY));
		org.addRow(6, new Label(Messages.getString("T_DECAY_ENERGY")), decayenergyText); //$NON-NLS-1$
		return org;
	}

	protected VBox setMetabolismTab() {
		VBox met = new VBox(8);
		met.setPadding(new Insets(6));
		Label photo = new Label(Messages.getString("T_PHOTOSYNTHETIC_METABOLISM")); //$NON-NLS-1$
		photo.setMaxWidth(Double.MAX_VALUE);
		photo.setAlignment(Pos.CENTER);
		met.getChildren().add(photo);
		greenenergyText = numField(Integer.toString(Utils.GREEN_OBTAINED_ENERGY_DIVISOR));
		HBox obtained = new HBox(8,
				new Label(Messages.getString("T_OBTAINED_ENERGY_DIVISOR")), greenenergyText); //$NON-NLS-1$
		obtained.setAlignment(Pos.CENTER_LEFT);
		met.getChildren().add(obtained);
		Label chemo = new Label(Messages.getString("T_CHEMOORGANOTROPHIC_METABOLISM")); //$NON-NLS-1$
		chemo.setMaxWidth(Double.MAX_VALUE);
		chemo.setAlignment(Pos.CENTER);
		met.getChildren().add(chemo);
		organicenergyText = numField(Double.toString(Utils.ORGANIC_OBTAINED_ENERGY));
		HBox organic = new HBox(8,
				new Label(Messages.getString("T_OBTAINED_ENERGY")), organicenergyText); //$NON-NLS-1$
		organic.setAlignment(Pos.CENTER_LEFT);
		met.getChildren().add(organic);
		organicsubsText = numField(Double.toString(Utils.ORGANIC_SUBS_PRODUCED));
		HBox released = new HBox(8,
				new Label(Messages.getString("T_RELEASED_ENERGY_PROPORTION")), organicsubsText); //$NON-NLS-1$
		released.setAlignment(Pos.CENTER_LEFT);
		met.getChildren().add(released);
		return met;
	}

	protected GridPane setGenesTab() {
		GridPane genes = grid();
		genes.setPadding(new Insets(6));
		Label header = new Label(Messages.getString("T_COLOR2")); //$NON-NLS-1$
		header.setMaxWidth(Double.MAX_VALUE);
		header.setAlignment(Pos.CENTER);
		Label prob = new Label(Messages.getString("T_PROBABILITY")); //$NON-NLS-1$
		prob.setMaxWidth(Double.MAX_VALUE);
		prob.setAlignment(Pos.CENTER);
		Label cost = new Label(Messages.getString("T_COST")); //$NON-NLS-1$
		cost.setMaxWidth(Double.MAX_VALUE);
		cost.setAlignment(Pos.CENTER);
		genes.addRow(0, header, prob, cost);
		String[] colors = {"RED", "GREEN", "BLUE", "CYAN", "WHITE", "GRAY", "YELLOW"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
		for (int i = 0; i < colors.length; i++) {
			Label name = new Label(Messages.getString("T_" + colors[i])); //$NON-NLS-1$
			name.setMaxWidth(Double.MAX_VALUE);
			name.setAlignment(Pos.CENTER);
			TextField probF = numField("");
			TextField costF = numField("");
			genes.addRow(i + 1, name, probF, costF);
			switch (colors[i]) {
			case "RED": //$NON-NLS-1$
				redprobText = probF; redcostText = costF;
				redprobText.setText(Integer.toString(Utils.RED_PROB));
				redcostText.setText(Double.toString(Utils.RED_ENERGY_CONSUMPTION));
				break;
			case "GREEN": //$NON-NLS-1$
				greenprobText = probF; greencostText = costF;
				greenprobText.setText(Integer.toString(Utils.GREEN_PROB));
				greencostText.setText(Double.toString(Utils.GREEN_ENERGY_CONSUMPTION));
				break;
			case "BLUE": //$NON-NLS-1$
				blueprobText = probF; bluecostText = costF;
				blueprobText.setText(Integer.toString(Utils.BLUE_PROB));
				bluecostText.setText(Double.toString(Utils.BLUE_ENERGY_CONSUMPTION));
				break;
			case "CYAN": //$NON-NLS-1$
				cyanprobText = probF; cyancostText = costF;
				cyanprobText.setText(Integer.toString(Utils.CYAN_PROB));
				cyancostText.setText(Double.toString(Utils.CYAN_ENERGY_CONSUMPTION));
				break;
			case "WHITE": //$NON-NLS-1$
				whiteprobText = probF; whitecostText = costF;
				whiteprobText.setText(Integer.toString(Utils.WHITE_PROB));
				whitecostText.setText(Double.toString(Utils.WHITE_ENERGY_CONSUMPTION));
				break;
			case "GRAY": //$NON-NLS-1$
				grayprobText = probF; graycostText = costF;
				grayprobText.setText(Integer.toString(Utils.GRAY_PROB));
				graycostText.setText(Double.toString(Utils.GRAY_ENERGY_CONSUMPTION));
				break;
			case "YELLOW": //$NON-NLS-1$
				yellowprobText = probF; yellowcostText = costF;
				yellowprobText.setText(Integer.toString(Utils.YELLOW_PROB));
				yellowcostText.setText(Double.toString(Utils.YELLOW_ENERGY_CONSUMPTION));
				break;
			}
		}
		return genes;
	}

	void checkParams() {
		int i;
		double d;
		if (Messages.getLocaleIndex() != localeCombo.getSelectionModel().getSelectedIndex()) {
			Messages.setLocale(localeCombo.getSelectionModel().getSelectedIndex());
			mainWindow.changeLocale();
		}
		try {
			i = Integer.parseInt(widthText.getText());
			if (i > 0) Utils.WORLD_WIDTH = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(heightText.getText());
			if (i > 0) Utils.WORLD_HEIGHT = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(delayText.getText());
			if (i > 0) {
				Utils.DELAY = i;
			}
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(initialO2Text.getText());
			if (d >= 0) Utils.INITIAL_O2 = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(initialCO2Text.getText());
			if (d >= 0) Utils.INITIAL_CO2 = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(frictionText.getText());
			if (d >= 0 && d <= 1) Utils.FRICTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(elasticityText.getText());
			if (d >= 0 && d <= 1) Utils.ELASTICITY = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(initialnumberText.getText());
			if (i >= 0) Utils.INITIAL_ORGANISMS = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(initialenergyText.getText());
			if (i > 0) Utils.INITIAL_ENERGY = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(stressresistanceText.getText());
			if (d > 0) Utils.STRESS_RESISTANCE = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(majormutationrateText.getText());
			if (d >= 0) Utils.MAJOR_MUTATION_RATE = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(minormutationrateText.getText());
			if (d >= 0) Utils.MINOR_MUTATION_RATE = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(minormutationamplitudeText.getText());
			if (d >= 0 && d <= 1) Utils.MINOR_MUTATION_AMPLITUDE = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(spontaneousmutationrateText.getText());
			if (d >= 0) Utils.SPONTANEOUS_MUTATION_RATE = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(redcostText.getText());
			if (d >= 0) Utils.RED_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(greencostText.getText());
			if (d >= 0) Utils.GREEN_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(bluecostText.getText());
			if (d >= 0) Utils.BLUE_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(cyancostText.getText());
			if (d >= 0) Utils.CYAN_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(whitecostText.getText());
			if (d >= 0) Utils.WHITE_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(graycostText.getText());
			if (d >= 0) Utils.GRAY_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(yellowcostText.getText());
			if (d >= 0) Utils.YELLOW_ENERGY_CONSUMPTION = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(redprobText.getText());
			if (i >= 0) Utils.RED_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(greenprobText.getText());
			if (i > 0) Utils.GREEN_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(blueprobText.getText());
			if (i >= 0) Utils.BLUE_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(cyanprobText.getText());
			if (i >= 0) Utils.CYAN_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(whiteprobText.getText());
			if (i >= 0) Utils.WHITE_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(grayprobText.getText());
			if (i >= 0) Utils.GRAY_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(yellowprobText.getText());
			if (i >= 0) Utils.YELLOW_PROB = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(segmentcostText.getText());
			if (i > 0) Utils.SEGMENT_COST_DIVISOR = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			i = Integer.parseInt(drainText.getText());
			if (i > 0) Utils.DRAIN_SUBS_DIVISOR = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}

		try {
			i = Integer.parseInt(greenenergyText.getText());
			if (i > 0) Utils.GREEN_OBTAINED_ENERGY_DIVISOR = i;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(organicenergyText.getText());
			if (d >= 0) Utils.ORGANIC_OBTAINED_ENERGY = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(organicsubsText.getText());
			if (d >= 0 && d <= 1) Utils.ORGANIC_SUBS_PRODUCED = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		try {
			d = Double.parseDouble(decayenergyText.getText());
			if (d >= 0) Utils.DECAY_ENERGY = d;
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
		if (hardwareNoneRadio.isSelected()) {
			Utils.setHardwareAcceleration(0);
		}
		if (hardwareOpenGLRadio.isSelected()) {
			if (hardwareFBObjectCheck.isSelected()) {
				if (Utils.HARDWARE_ACCELERATION < 4)
					Utils.setHardwareAcceleration(4);
			} else {
				if (Utils.HARDWARE_ACCELERATION != 0 && Utils.HARDWARE_ACCELERATION > 3)
					Utils.setHardwareAcceleration(1);
			}
		}
	}
}
