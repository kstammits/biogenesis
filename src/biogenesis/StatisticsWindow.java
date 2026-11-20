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
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;


public class StatisticsWindow {
	private Button updateButton;
	private Button closeButton;
	private Stage stage;

	private World world;
	private VisibleWorld visibleWorld;
	private WorldStatistics worldStatistics;
	private List<Organism> organisms;
	private Stage ownerStage;

	public StatisticsWindow(MainWindow w, WorldStatistics ws, List<Organism> os) {
		world = w.getWorld();
		visibleWorld = w.getVisibleWorld();
		ownerStage = w.getStage();
		worldStatistics = ws;
		organisms = os;
		Fx.run(this::buildAndShow);
	}

	/** Close the window, e.g. when MainWindow opens a fresh one. */
	public void dispose() {
		Fx.run(() -> {
			if (stage != null)
				stage.close();
		});
	}

	private void buildAndShow() {
		stage = new Stage();
		stage.initOwner(ownerStage);
		stage.initModality(Modality.WINDOW_MODAL);
		stage.setTitle(Messages.getString("T_STATISTICS")); //$NON-NLS-1$
		stage.setResizable(false);
		stage.setScene(new Scene(buildRoot()));
		stage.sizeToScene();
		stage.show();
	}

	private Parent buildRoot() {
		// Prepare number format
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(1);

		// Population graphic
		GraphPanel populationGraphPanel = new GraphPanel(100, 80);
		populationGraphPanel.addGraph(worldStatistics.getDeathList(), Math.max(worldStatistics.getAveragePopulation(), worldStatistics.getMaxDeaths()),
				0, Color.RED, Messages.getString("T_DEATHS")); //$NON-NLS-1$
		populationGraphPanel.addGraph(worldStatistics.getBirthList(), Math.max(worldStatistics.getAveragePopulation(), worldStatistics.getMaxBirth()),
				0, Color.GREEN, Messages.getString("T_BIRTHS")); //$NON-NLS-1$
		populationGraphPanel.addGraph(worldStatistics.getPopulationList(), worldStatistics.getMaxPopulation(),
				0, Color.WHITE, Messages.getString("T_POPULATION")); //$NON-NLS-1$
		populationGraphPanel.updateLegend();


		// Population statistics
		VBox popStatsPanel = new VBox();
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_AVERAGE_POPULATION")+ //$NON-NLS-1$
				nf.format(worldStatistics.getAveragePopulation())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_AVERAGE_BIRTH_RATE")+ //$NON-NLS-1$
				nf.format(worldStatistics.getAverageBirths())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_AVERAGE_MORTALITY_RATE")+ //$NON-NLS-1$
				nf.format(worldStatistics.getAverageDeaths())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_AVERAGE_INFECTIONS_RATE")+ //$NON-NLS-1$
				nf.format(worldStatistics.getAverageInfections())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_GENERATED_ORGANISMS")+ //$NON-NLS-1$
				nf.format(worldStatistics.getCreatedOrganisms())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_MAXIMUM_POPULATION")+ //$NON-NLS-1$
				nf.format(worldStatistics.getMaxPopulation())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_AT_TIME")+ //$NON-NLS-1$
				nf.format(worldStatistics.getMaxPopulationTime())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_MINIMUM_POPULATION")+ //$NON-NLS-1$
				nf.format(worldStatistics.getMinPopulation())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_AT_TIME")+ //$NON-NLS-1$
				nf.format(worldStatistics.getMinPopulationTime())));
		popStatsPanel.getChildren().add(new Label(Messages.getString("T_MASS_EXTINTIONS")+ //$NON-NLS-1$
				nf.format(worldStatistics.getMassExtintions())));

		// Population = population graph + population stats
		VBox populationPanel = new VBox(populationGraphPanel, popStatsPanel);
		TitledPane populationPane = titled(Messages.getString("T_POPULATION"), populationPanel); //$NON-NLS-1$

		// Atmosphere graphic
		GraphPanel atmosphereGraphPanel = new GraphPanel(100,80);
		atmosphereGraphPanel.addGraph(worldStatistics.getOxygenList(), worldStatistics.getMaxOxygen(),
				worldStatistics.getMinOxygen(), Color.BLUE, Messages.getString("T_OXYGEN")); //$NON-NLS-1$
		atmosphereGraphPanel.addGraph(worldStatistics.getCarbonDioxideList(), worldStatistics.getMaxCarbonDioxide(),
				worldStatistics.getMinCarbonDioxide(), Color.WHITE, Messages.getString("T_CARBON_DIOXIDE")); //$NON-NLS-1$
		atmosphereGraphPanel.updateLegend();

		//Atmosphere statistics
		VBox atmosphereStatsPanel = new VBox();
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_MAXIMUM_CARBON_DIOXIDE")+nf.format(worldStatistics.getMaxCarbonDioxide()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_AT_TIME")+nf.format(worldStatistics.getMaxCarbonDioxideTime()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_MINIMUM_CARBON_DIOXIDE")+nf.format(worldStatistics.getMinCarbonDioxide()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_AT_TIME")+nf.format(worldStatistics.getMinCarbonDioxideTime()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_MAXIMUM_OXYGEN")+nf.format(worldStatistics.getMaxOxygen()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_AT_TIME")+nf.format(worldStatistics.getMaxOxygenTime()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_MINIMUM_OXYGEN")+nf.format(worldStatistics.getMinOxygenTime()))); //$NON-NLS-1$
		atmosphereStatsPanel.getChildren().add(new Label(Messages.getString("T_AT_TIME")+nf.format(worldStatistics.getMinOxygenTime()))); //$NON-NLS-1$

//		 Population = population graph + population stats
		VBox atmospherePanel = new VBox(atmosphereGraphPanel, atmosphereStatsPanel);
		TitledPane atmospherePane = titled(Messages.getString("T_ATMOSPHERE"), atmospherePanel); //$NON-NLS-1$

		// World history: population + atmosphere
		VBox worldHistoryPanel = new VBox(populationPane, atmospherePane);
		TitledPane worldHistoryPane = titled(Messages.getString("T_WORLD_HISTORY"), worldHistoryPanel); //$NON-NLS-1$

		// Current state
		GridPane currentStatePanel = new GridPane();
		currentStatePanel.setHgap(8);
		currentStatePanel.add(new Label(Messages.getString("T_TIME")+world.getTime()), 0, 0); //$NON-NLS-1$
		currentStatePanel.add(new Label(Messages.getString("T_OXYGEN2")+nf.format(world.getO2())), 1, 0); //$NON-NLS-1$
		currentStatePanel.add(new Label(Messages.getString("T_POPULATION2")+world.getPopulation()), 0, 1); //$NON-NLS-1$
		currentStatePanel.add(new Label(Messages.getString("T_CARBON_DIOXIDE2")+nf.format(world.getCO2())), 1, 1); //$NON-NLS-1$
		currentStatePanel.add(new Label(Messages.getString("T_REMAINS_OF_BEINGS")+world.getNCorpses()), 0, 2); //$NON-NLS-1$
		currentStatePanel.add(new Label(Messages.getString("T_COLOR_PROPORTION")), 0, 3); //$NON-NLS-1$
		ColorPanel colorPanel = createColorPanel();
		currentStatePanel.add(colorPanel, 1, 3);
		TitledPane currentStatePane = titled(Messages.getString("T_CURRENT_STATE"), currentStatePanel); //$NON-NLS-1$

		// Notable beings
		worldStatistics.findBestAliveBeings(organisms);
		GridPane notableBeingsPanel = new GridPane();
		notableBeingsPanel.setHgap(8);
		notableBeingsPanel.add(new Label(Messages.getString("T_ALIVE_BEING_HAVING_THE_MOST_CHILDREN")), 0, 0); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_BEING_HAVING_THE_MOST_CHILDREN")), 1, 0); //$NON-NLS-1$
		GeneticCodePanel aliveMostChildrenPanel = new GeneticCodePanel(worldStatistics.getAliveBeingMostChildren(),
				visibleWorld);
		notableBeingsPanel.add(aliveMostChildrenPanel, 0, 1);
		GeneticCodePanel mostChildrenPanel = new GeneticCodePanel(worldStatistics.getBeingMostChildren(),
				visibleWorld);
		notableBeingsPanel.add(mostChildrenPanel, 1, 1);
		notableBeingsPanel.add(new Label(Messages.getString("T_NUMBER_OF_CHILDREN")+worldStatistics.getAliveBeingMostChildrenNumber()), 0, 2); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_NUMBER_OF_CHILDREN")+worldStatistics.getBeingMostChildrenNumber()), 1, 2); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_TIME")+worldStatistics.getBeingMostChildrenTime()), 1, 3); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_ALIVE_BEING_HAVING_THE_MOST_VICTIMS")), 0, 4); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_BEING_HAVING_THE_MOST_VICTIMS")), 1, 4); //$NON-NLS-1$
		GeneticCodePanel aliveMostKillsPanel = new GeneticCodePanel(worldStatistics.getAliveBeingMostKills(),
				visibleWorld);
		notableBeingsPanel.add(aliveMostKillsPanel, 0, 5);
		GeneticCodePanel mostKillsPanel = new GeneticCodePanel(worldStatistics.getBeingMostKills(),
				visibleWorld);
		notableBeingsPanel.add(mostKillsPanel, 1, 5);
		notableBeingsPanel.add(new Label(Messages.getString("T_NUMBER_OF_VICTIMS")+worldStatistics.getAliveBeingMostKillsNumber()), 0, 6);		 //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_NUMBER_OF_VICTIMS")+worldStatistics.getBeingMostKillsNumber()), 1, 6); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_TIME")+worldStatistics.getBeingMostKillsTime()), 1, 7);		 //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_ALIVE_BEING_HAVING_THE_MOST_INFECTED")), 0, 8); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_BEING_HAVING_THE_MOST_INFECTED")), 1, 8); //$NON-NLS-1$
		GeneticCodePanel aliveMostInfectionsPanel = new GeneticCodePanel(worldStatistics.getAliveBeingMostInfections(),
				visibleWorld);
		notableBeingsPanel.add(aliveMostInfectionsPanel, 0, 9);
		GeneticCodePanel mostInfectionsPanel = new GeneticCodePanel(worldStatistics.getBeingMostInfections(),
				visibleWorld);
		notableBeingsPanel.add(mostInfectionsPanel, 1, 9);
		notableBeingsPanel.add(new Label(Messages.getString("T_NUMBER_OF_INFECTED")+worldStatistics.getAliveBeingMostInfectionsNumber()), 0, 10);		 //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_NUMBER_OF_INFECTED")+worldStatistics.getBeingMostInfectionsNumber()), 1, 10); //$NON-NLS-1$
		notableBeingsPanel.add(new Label(Messages.getString("T_TIME")+worldStatistics.getBeingMostInfectionsTime()), 1, 11); //$NON-NLS-1$
		TitledPane notableBeingsPane = titled(Messages.getString("T_REMARKABLE_ORGANISMS"), notableBeingsPanel); //$NON-NLS-1$


		// Buttons
		updateButton = new Button(Messages.getString("T_UPDATE")); //$NON-NLS-1$
		closeButton = new Button(Messages.getString("T_CLOSE")); //$NON-NLS-1$
		updateButton.setDefaultButton(true);
		closeButton.setCancelButton(true);
		updateButton.setOnAction(evt -> {
			stage.setScene(new Scene(buildRoot()));
			stage.sizeToScene();
		});
		closeButton.setOnAction(evt -> stage.close());
		HBox buttonsPanel = new HBox(8, updateButton, closeButton);
		buttonsPanel.setAlignment(Pos.CENTER);

		// Add all components to the content pane
		VBox leftPanel = new VBox(8, currentStatePane, notableBeingsPane);
		VBox rightPanel = new VBox(8, worldHistoryPane, buttonsPanel);

		HBox root = new HBox(10, leftPanel, rightPanel);
		root.setPadding(new Insets(10));
		return root;
	}

	private static TitledPane titled(String title, Parent content) {
		TitledPane pane = new TitledPane(title, content);
		pane.setCollapsible(false);
		pane.setAnimated(false);
		return pane;
	}

	private ColorPanel createColorPanel() {
		ColorPanel colorPanel = new ColorPanel();
		colorPanel.setPrefSize(100, 20);
		GeneticCode gc;
		InfoAndColor[] colorCounter = new InfoAndColor[7];
		colorCounter[0] = new InfoAndColor(0, Color.GREEN);
		colorCounter[1] = new InfoAndColor(0, Color.RED);
		colorCounter[2] = new InfoAndColor(0, Color.BLUE);
		colorCounter[3] = new InfoAndColor(0, Color.CYAN);
		colorCounter[4] = new InfoAndColor(0, Color.WHITE);
		colorCounter[5] = new InfoAndColor(0, Color.GRAY);
		colorCounter[6] = new InfoAndColor(0, Color.YELLOW);

		int i,j;
		Color c;
		synchronized(organisms) {
			for (Iterator<Organism> it = organisms.iterator(); it.hasNext();) {
				gc = it.next().getGeneticCode();
				for (i=0; i<gc.getNGenes(); i++) {
					c = gc.getGene(i).getColor();
					for (j=0; j<7; j++) {
						if (c.equals(colorCounter[j].color))
							colorCounter[j].info++;
					}
				}
			}
		}
		Arrays.sort(colorCounter);
		for (j=6; j>=0; j--)
			colorPanel.addColor(colorCounter[j].info, colorCounter[j].color);
		colorPanel.redraw();

		return colorPanel;
	}

	static javafx.scene.paint.Color toFx(Color c) {
		return javafx.scene.paint.Color.rgb(c.getRed(), c.getGreen(), c.getBlue());
	}
}

class ColorPanel extends Pane {
	private List<InfoAndColor> infoList = new ArrayList<InfoAndColor>();
	private int total=0;
	private Canvas canvas;

	public ColorPanel() {
		canvas = new Canvas(100, 20);
		getChildren().add(canvas);
		setPrefSize(100, 20);
		widthProperty().addListener((obs, oldV, newV) -> redraw());
		heightProperty().addListener((obs, oldV, newV) -> redraw());
	}

	public void addColor(int info, Color color) {
		infoList.add(new InfoAndColor(info, color));
		total += info;
	}

	public void redraw() {
		int width = Math.max(1, (int) getWidth());
		int height = Math.max(1, (int) getHeight());
		canvas.setWidth(width);
		canvas.setHeight(height);
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.setFill(javafx.scene.paint.Color.BLACK);
		g.fillRect(0, 0, width, height);
		int lastX=0;
		InfoAndColor infoAndColor;
		if (total > 0) {
		    for (Iterator<InfoAndColor> it = infoList.iterator(); it.hasNext();) {
			     infoAndColor = it.next();
			     int x = width * infoAndColor.info / total;
			     g.setFill(StatisticsWindow.toFx(infoAndColor.color));
			     g.fillRect(lastX, 0, x, height);
			     lastX += x;
		    }
		}
	}
}

class InfoAndColor implements Comparable<InfoAndColor> {
	public int info;
	public Color color;

	public InfoAndColor(int i, Color c) {
		info = i;
		color = c;
	}

	public int compareTo(InfoAndColor o) {
		if (info < o.info) return -1;
		if (info > o.info) return 1;
		return 0;
	}
}

class GraphPanel extends VBox {
	private List<GraphInfo> graphList = new ArrayList<GraphInfo>();
	private int width;
	private int height;
	private Canvas canvas;
	private VBox legendBox = new VBox();

	public void addGraph(List<Double> info, double max, double min, Color color, String name) {
		graphList.add(new GraphInfo(info, max, min, width, height, color, name));
	}

	public void clear() {
		graphList.clear();
	}

	public void updateLegend() {
		legendBox.getChildren().clear();
		legendBox.setStyle("-fx-background-color: black;"); //$NON-NLS-1$
		GraphInfo graph;
		for (Iterator<GraphInfo> it = graphList.iterator(); it.hasNext();) {
			graph = it.next();
			Label label = new Label(graph.name);
			label.setTextFill(StatisticsWindow.toFx(graph.color));
			legendBox.getChildren().add(label);
		}
		redraw();
	}

	public GraphPanel(int w, int h) {
		width = w;
		height = h;
		canvas = new Canvas(w, h);
		HBox top = new HBox(canvas, legendBox);
		HBox.setHgrow(legendBox, Priority.ALWAYS);
		legendBox.setStyle("-fx-background-color: black;"); //$NON-NLS-1$

		Label zero = new Label("0"); //$NON-NLS-1$
		Label hundred = new Label("100"); //$NON-NLS-1$
		Region spacer = new Region();
		HBox.setHgrow(spacer, Priority.ALWAYS);
		HBox southPanel = new HBox(zero, spacer, hundred);
		southPanel.setPrefWidth(w);

		getChildren().addAll(top, southPanel);
		redraw();
	}

	private void redraw() {
		GraphicsContext g = canvas.getGraphicsContext2D();
		g.setFill(javafx.scene.paint.Color.BLACK);
		g.fillRect(0, 0, width, height);
		GraphInfo graph;
		for (Iterator<GraphInfo> it = graphList.iterator(); it.hasNext();) {
			graph = it.next();
			graph.draw(g);
		}
	}
}

class GraphInfo {
	public Color color;
	public String name;
	public List<Double> info;
	public double max;
	public double min;

	private double[] xPoints;
	private double[] yPoints;
	private int nPoints;

	public void draw(GraphicsContext g) {
		g.setStroke(StatisticsWindow.toFx(color));
		g.strokePolyline(xPoints, yPoints, nPoints);
	}

	public GraphInfo(List<Double> datum, double maxValue, double minValue, int width, int height, Color graphColor, String graphName) {
		info = datum;
		max = maxValue;
		min = minValue;
		if (max == min)
			max = min + 1; // flat series: keep the polyline defined
		color = graphColor;
		name = graphName;
		int x = 0;
		double y;
		nPoints = Math.min(info.size(), width);
		xPoints = new double[nPoints];
		yPoints = new double[nPoints];
		for (Iterator<Double> it = info.iterator(); it.hasNext() && x < width; x++) {
			y = height-(it.next().doubleValue() - min)*height/(max-min);
			xPoints[x] = x;
			yPoints[x] = y;
		}

	}
}
