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
 * Built on the FX thread via {@link Fx}.
 */
public class ParamDialog {
	private ComboBox<String> localeCombo = null;
	private TextField widthText = null;
	private TextField heightText = null;
	private ComboBox<String> simRateCombo = null;
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
		stage.initOwner(mainWindow.getStage());
		stage.initModality(Modality.WINDOW_MODAL);
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

	/**
	 * Sim-rate preset items: "10".."500" plus the uncapped entry. Index
	 * {@link Utils#SIM_RATE_PRESETS}.length is uncapped.
	 */
	private static java.util.List<String> simRateItems() {
		java.util.List<String> items = new java.util.ArrayList<>();
		for (int r : Utils.SIM_RATE_PRESETS)
			items.add(String.valueOf(r));
		items.add(Messages.getString("T_UNCAPPED")); //$NON-NLS-1$
		return items;
	}

	/** Combo index for a rate: preset position, or the trailing uncapped entry. */
	private static int simRateIndex(int rate) {
		for (int i = 0; i < Utils.SIM_RATE_PRESETS.length; i++) {
			if (Utils.SIM_RATE_PRESETS[i] == rate)
				return i;
		}
		return Utils.SIM_RATE_PRESETS.length;
	}

	private void selectSimRate(int rate) {
		if (simRateCombo != null)
			simRateCombo.getSelectionModel().select(simRateIndex(rate));
	}

	/** Rate behind the current combo selection, or uncapped for the trailing entry. */
	private int selectedSimRate() {
		int idx = simRateCombo.getSelectionModel().getSelectedIndex();
		if (idx >= 0 && idx < Utils.SIM_RATE_PRESETS.length)
			return Utils.SIM_RATE_PRESETS[idx];
		return Utils.SIM_RATE_UNCAPPED;
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
		selectSimRate(Utils.nearestSimRate(Utils.DEF_DELAY));
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
		// Simulation speed preset (ticks/sec, or uncapped)
		simRateCombo = new ComboBox<>();
		simRateCombo.getItems().addAll(simRateItems());
		simRateCombo.getSelectionModel().select(simRateIndex(Utils.nearestSimRate(Utils.DELAY)));
		HBox delay = new HBox(8,
				new Label(Messages.getString("T_SIM_RATE")), simRateCombo, //$NON-NLS-1$
				new Label(Messages.getString("T_TICKS_PER_SECOND"))); //$NON-NLS-1$
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
		int[] probs = {Utils.RED_PROB, Utils.GREEN_PROB, Utils.BLUE_PROB, Utils.CYAN_PROB,
				Utils.WHITE_PROB, Utils.GRAY_PROB, Utils.YELLOW_PROB};
		double[] costs = {Utils.RED_ENERGY_CONSUMPTION, Utils.GREEN_ENERGY_CONSUMPTION,
				Utils.BLUE_ENERGY_CONSUMPTION, Utils.CYAN_ENERGY_CONSUMPTION,
				Utils.WHITE_ENERGY_CONSUMPTION, Utils.GRAY_ENERGY_CONSUMPTION,
				Utils.YELLOW_ENERGY_CONSUMPTION};
		TextField[] probFields = new TextField[colors.length];
		TextField[] costFields = new TextField[colors.length];
		for (int i = 0; i < colors.length; i++) {
			Label name = new Label(Messages.getString("T_" + colors[i])); //$NON-NLS-1$
			name.setMaxWidth(Double.MAX_VALUE);
			name.setAlignment(Pos.CENTER);
			probFields[i] = numField(Integer.toString(probs[i]));
			costFields[i] = numField(Double.toString(costs[i]));
			genes.addRow(i + 1, name, probFields[i], costFields[i]);
		}
		redprobText = probFields[0]; redcostText = costFields[0];
		greenprobText = probFields[1]; greencostText = costFields[1];
		blueprobText = probFields[2]; bluecostText = costFields[2];
		cyanprobText = probFields[3]; cyancostText = costFields[3];
		whiteprobText = probFields[4]; whitecostText = costFields[4];
		grayprobText = probFields[5]; graycostText = costFields[5];
		yellowprobText = probFields[6]; yellowcostText = costFields[6];
		return genes;
	}

	void checkParams() {
		if (Messages.getLocaleIndex() != localeCombo.getSelectionModel().getSelectedIndex()) {
			Messages.setLocale(localeCombo.getSelectionModel().getSelectedIndex());
			mainWindow.changeLocale();
		}
		Fx.parseIntOrKeep(widthText, v -> v > 0, v -> Utils.WORLD_WIDTH = v);
		Fx.parseIntOrKeep(heightText, v -> v > 0, v -> Utils.WORLD_HEIGHT = v);
		int selectedRate = selectedSimRate();
		int newDelay = Utils.delayForSimRate(selectedRate);
		if (newDelay != Utils.DELAY) {
			Utils.DELAY = newDelay;
			mainWindow.applySimDelay();
		}
		Fx.parseDoubleOrKeep(initialO2Text, v -> v >= 0, v -> Utils.INITIAL_O2 = v);
		Fx.parseDoubleOrKeep(initialCO2Text, v -> v >= 0, v -> Utils.INITIAL_CO2 = v);
		Fx.parseDoubleOrKeep(frictionText, v -> v >= 0 && v <= 1, v -> Utils.FRICTION = v);
		Fx.parseDoubleOrKeep(elasticityText, v -> v >= 0 && v <= 1, v -> Utils.ELASTICITY = v);
		Fx.parseIntOrKeep(initialnumberText, v -> v >= 0, v -> Utils.INITIAL_ORGANISMS = v);
		Fx.parseIntOrKeep(initialenergyText, v -> v > 0, v -> Utils.INITIAL_ENERGY = v);
		Fx.parseDoubleOrKeep(stressresistanceText, v -> v > 0, v -> Utils.STRESS_RESISTANCE = v);
		Fx.parseDoubleOrKeep(majormutationrateText, v -> v >= 0, v -> Utils.MAJOR_MUTATION_RATE = v);
		Fx.parseDoubleOrKeep(minormutationrateText, v -> v >= 0, v -> Utils.MINOR_MUTATION_RATE = v);
		Fx.parseDoubleOrKeep(minormutationamplitudeText, v -> v >= 0 && v <= 1, v -> Utils.MINOR_MUTATION_AMPLITUDE = v);
		Fx.parseDoubleOrKeep(spontaneousmutationrateText, v -> v >= 0, v -> Utils.SPONTANEOUS_MUTATION_RATE = v);
		Fx.parseDoubleOrKeep(redcostText, v -> v >= 0, v -> Utils.RED_ENERGY_CONSUMPTION = v);
		Fx.parseDoubleOrKeep(greencostText, v -> v >= 0, v -> Utils.GREEN_ENERGY_CONSUMPTION = v);
		Fx.parseDoubleOrKeep(bluecostText, v -> v >= 0, v -> Utils.BLUE_ENERGY_CONSUMPTION = v);
		Fx.parseDoubleOrKeep(cyancostText, v -> v >= 0, v -> Utils.CYAN_ENERGY_CONSUMPTION = v);
		Fx.parseDoubleOrKeep(whitecostText, v -> v >= 0, v -> Utils.WHITE_ENERGY_CONSUMPTION = v);
		Fx.parseDoubleOrKeep(graycostText, v -> v >= 0, v -> Utils.GRAY_ENERGY_CONSUMPTION = v);
		Fx.parseDoubleOrKeep(yellowcostText, v -> v >= 0, v -> Utils.YELLOW_ENERGY_CONSUMPTION = v);
		Fx.parseIntOrKeep(redprobText, v -> v >= 0, v -> Utils.RED_PROB = v);
		Fx.parseIntOrKeep(greenprobText, v -> v > 0, v -> Utils.GREEN_PROB = v);
		Fx.parseIntOrKeep(blueprobText, v -> v >= 0, v -> Utils.BLUE_PROB = v);
		Fx.parseIntOrKeep(cyanprobText, v -> v >= 0, v -> Utils.CYAN_PROB = v);
		Fx.parseIntOrKeep(whiteprobText, v -> v >= 0, v -> Utils.WHITE_PROB = v);
		Fx.parseIntOrKeep(grayprobText, v -> v >= 0, v -> Utils.GRAY_PROB = v);
		Fx.parseIntOrKeep(yellowprobText, v -> v >= 0, v -> Utils.YELLOW_PROB = v);
		Fx.parseIntOrKeep(segmentcostText, v -> v > 0, v -> Utils.SEGMENT_COST_DIVISOR = v);
		Fx.parseIntOrKeep(drainText, v -> v > 0, v -> Utils.DRAIN_SUBS_DIVISOR = v);

		Fx.parseIntOrKeep(greenenergyText, v -> v > 0, v -> Utils.GREEN_OBTAINED_ENERGY_DIVISOR = v);
		Fx.parseDoubleOrKeep(organicenergyText, v -> v >= 0, v -> Utils.ORGANIC_OBTAINED_ENERGY = v);
		Fx.parseDoubleOrKeep(organicsubsText, v -> v >= 0 && v <= 1, v -> Utils.ORGANIC_SUBS_PRODUCED = v);
		Fx.parseDoubleOrKeep(decayenergyText, v -> v >= 0, v -> Utils.DECAY_ENERGY = v);
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
