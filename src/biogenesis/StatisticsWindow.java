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
	public void close() {
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
		VBox popStatsPanel = new VBox(
			stat("T_AVERAGE_POPULATION", nf.format(worldStatistics.getAveragePopulation())), //$NON-NLS-1$
			stat("T_AVERAGE_BIRTH_RATE", nf.format(worldStatistics.getAverageBirths())), //$NON-NLS-1$
			stat("T_AVERAGE_MORTALITY_RATE", nf.format(worldStatistics.getAverageDeaths())), //$NON-NLS-1$
			stat("T_AVERAGE_INFECTIONS_RATE", nf.format(worldStatistics.getAverageInfections())), //$NON-NLS-1$
			stat("T_GENERATED_ORGANISMS", nf.format(worldStatistics.getCreatedOrganisms())), //$NON-NLS-1$
			stat("T_MAXIMUM_POPULATION", nf.format(worldStatistics.getMaxPopulation())), //$NON-NLS-1$
			stat("T_AT_TIME", nf.format(worldStatistics.getMaxPopulationTime())), //$NON-NLS-1$
			stat("T_MINIMUM_POPULATION", nf.format(worldStatistics.getMinPopulation())), //$NON-NLS-1$
			stat("T_AT_TIME", nf.format(worldStatistics.getMinPopulationTime())), //$NON-NLS-1$
			stat("T_MASS_EXTINTIONS", nf.format(worldStatistics.getMassExtintions()))); //$NON-NLS-1$

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
		VBox atmosphereStatsPanel = new VBox(
			stat("T_MAXIMUM_CARBON_DIOXIDE", nf.format(worldStatistics.getMaxCarbonDioxide())), //$NON-NLS-1$
			stat("T_AT_TIME", nf.format(worldStatistics.getMaxCarbonDioxideTime())), //$NON-NLS-1$
			stat("T_MINIMUM_CARBON_DIOXIDE", nf.format(worldStatistics.getMinCarbonDioxide())), //$NON-NLS-1$
			stat("T_AT_TIME", nf.format(worldStatistics.getMinCarbonDioxideTime())), //$NON-NLS-1$
			stat("T_MAXIMUM_OXYGEN", nf.format(worldStatistics.getMaxOxygen())), //$NON-NLS-1$
			stat("T_AT_TIME", nf.format(worldStatistics.getMaxOxygenTime())), //$NON-NLS-1$
			stat("T_MINIMUM_OXYGEN", nf.format(worldStatistics.getMinOxygen())), //$NON-NLS-1$
			stat("T_AT_TIME", nf.format(worldStatistics.getMinOxygenTime()))); //$NON-NLS-1$

		VBox atmospherePanel = new VBox(atmosphereGraphPanel, atmosphereStatsPanel);
		TitledPane atmospherePane = titled(Messages.getString("T_ATMOSPHERE"), atmospherePanel); //$NON-NLS-1$

		// World history: population + atmosphere
		VBox worldHistoryPanel = new VBox(populationPane, atmospherePane);
		TitledPane worldHistoryPane = titled(Messages.getString("T_WORLD_HISTORY"), worldHistoryPanel); //$NON-NLS-1$

		// Current state
		GridPane currentStatePanel = new GridPane();
		currentStatePanel.setHgap(8);
		currentStatePanel.add(stat("T_TIME", Long.toString(world.getTime())), 0, 0); //$NON-NLS-1$
		currentStatePanel.add(stat("T_OXYGEN2", nf.format(world.getO2())), 1, 0); //$NON-NLS-1$
		currentStatePanel.add(stat("T_POPULATION2", Integer.toString(world.getPopulation())), 0, 1); //$NON-NLS-1$
		currentStatePanel.add(stat("T_CARBON_DIOXIDE2", nf.format(world.getCO2())), 1, 1); //$NON-NLS-1$
		currentStatePanel.add(stat("T_REMAINS_OF_BEINGS", Integer.toString(world.getNCorpses())), 0, 2); //$NON-NLS-1$
		currentStatePanel.add(new Label(Messages.getString("T_COLOR_PROPORTION")), 0, 3); //$NON-NLS-1$
		ColorPanel colorPanel = createColorPanel();
		currentStatePanel.add(colorPanel, 1, 3);
		TitledPane currentStatePane = titled(Messages.getString("T_CURRENT_STATE"), currentStatePanel); //$NON-NLS-1$

		// Notable beings
		worldStatistics.findBestAliveBeings(organisms);
		GridPane notableBeingsPanel = new GridPane();
		notableBeingsPanel.setHgap(8);
		addNotablePair(notableBeingsPanel, 0,
				"T_ALIVE_BEING_HAVING_THE_MOST_CHILDREN", "T_BEING_HAVING_THE_MOST_CHILDREN", //$NON-NLS-1$ //$NON-NLS-2$
				worldStatistics.getAliveBeingMostChildren(), worldStatistics.getBeingMostChildren(),
				"T_NUMBER_OF_CHILDREN", worldStatistics.getAliveBeingMostChildrenNumber(), //$NON-NLS-1$
				worldStatistics.getBeingMostChildrenNumber(), worldStatistics.getBeingMostChildrenTime());
		addNotablePair(notableBeingsPanel, 4,
				"T_ALIVE_BEING_HAVING_THE_MOST_VICTIMS", "T_BEING_HAVING_THE_MOST_VICTIMS", //$NON-NLS-1$ //$NON-NLS-2$
				worldStatistics.getAliveBeingMostKills(), worldStatistics.getBeingMostKills(),
				"T_NUMBER_OF_VICTIMS", worldStatistics.getAliveBeingMostKillsNumber(), //$NON-NLS-1$
				worldStatistics.getBeingMostKillsNumber(), worldStatistics.getBeingMostKillsTime());
		addNotablePair(notableBeingsPanel, 8,
				"T_ALIVE_BEING_HAVING_THE_MOST_INFECTED", "T_BEING_HAVING_THE_MOST_INFECTED", //$NON-NLS-1$ //$NON-NLS-2$
				worldStatistics.getAliveBeingMostInfections(), worldStatistics.getBeingMostInfections(),
				"T_NUMBER_OF_INFECTED", worldStatistics.getAliveBeingMostInfectionsNumber(), //$NON-NLS-1$
				worldStatistics.getBeingMostInfectionsNumber(), worldStatistics.getBeingMostInfectionsTime());
		TitledPane notableBeingsPane = titled(Messages.getString("T_REMARKABLE_ORGANISMS"), notableBeingsPanel); //$NON-NLS-1$


		// Buttons
		Button updateButton = new Button(Messages.getString("T_UPDATE")); //$NON-NLS-1$
		Button closeButton = new Button(Messages.getString("T_CLOSE")); //$NON-NLS-1$
		updateButton.setDefaultButton(true);
		closeButton.setCancelButton(true);
		updateButton.setOnAction(evt -> {
			stage.setScene(new Scene(buildRoot()));
			stage.sizeToScene();
		});
		closeButton.setOnAction(evt -> stage.close());
		HBox buttonsPanel = new HBox(8, updateButton, closeButton);
		buttonsPanel.setAlignment(Pos.CENTER);

		VBox leftPanel = new VBox(8, currentStatePane, notableBeingsPane);
		VBox rightPanel = new VBox(8, worldHistoryPane, buttonsPanel);

		HBox root = new HBox(10, leftPanel, rightPanel);
		root.setPadding(new Insets(10));
		return root;
	}

	private static TitledPane titled(String title, Parent content) {
		TitledPane pane = new TitledPane(title, content);
		pane.setCollapsible(false);
		return pane;
	}

	/** One "prefix: value" stat line. */
	private static Label stat(String key, String value) {
		return new Label(Messages.getString(key) + value);
	}

	/** One alive/all-time notable pair: headers, gene pies, counts, time. */
	private void addNotablePair(GridPane grid, int row,
			String aliveTitleKey, String allTimeTitleKey,
			GeneticCode alive, GeneticCode allTime,
			String countKey, int aliveCount, int allTimeCount, long allTimeTime) {
		grid.add(new Label(Messages.getString(aliveTitleKey)), 0, row);
		grid.add(new Label(Messages.getString(allTimeTitleKey)), 1, row);
		grid.add(new GeneticCodePanel(alive, visibleWorld), 0, row + 1);
		grid.add(new GeneticCodePanel(allTime, visibleWorld), 1, row + 1);
		grid.add(new Label(Messages.getString(countKey) + aliveCount), 0, row + 2);
		grid.add(new Label(Messages.getString(countKey) + allTimeCount), 1, row + 2);
		grid.add(new Label(Messages.getString("T_TIME") + allTimeTime), 1, row + 3); //$NON-NLS-1$
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
