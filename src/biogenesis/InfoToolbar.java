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


import java.text.NumberFormat;

import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;


/**
 * Status bar under the world view: genetic-code pie plus the selected
 * organism's live stats. A plain {@link GridPane} (9x3 grid),
 * embedded directly in MainWindow's FX layout.
 *
 * <p>Must be constructed on the FX thread. All mutators route through
 * {@link Fx#run} because the sim thread drives them off-thread.
 */
public class InfoToolbar extends GridPane {
	protected Organism _selOrganism;
	protected Button _buttonGenes;
	protected GeneticCodePanel _geneticCodePanel;
	static private NumberFormat _nf = NumberFormat.getInstance();
	protected MainWindow _mainWindow;

	/** One row pair per stat: title key, grid position, value slot. */
	private static final String[] TITLE_KEYS = {"T_ID", "T_GENERATION", "T_AGE", "T_ENERGY", //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			"T_CHILDREN", "T_VICTIMS", "T_INFECTED", "T_MASS", "T_REPRODUCTION"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
	/** Title columns; each value sits one column right. Row 0: ID/Gen/Age/Energy, row 1: Children/Victims/Infected/Mass, row 2: Reproduction. */
	private static final int[] TITLE_COLS = {1, 3, 5, 7, 1, 3, 5, 7, 3};
	private static final int[] TITLE_ROWS = {0, 0, 0, 0, 1, 1, 1, 1, 2};
	private static final int I_ID = 0, I_GENERATION = 1, I_AGE = 2, I_ENERGY = 3,
			I_CHILDREN = 4, I_KILLS = 5, I_INFECTED = 6, I_MASS = 7, I_REPRODUCE_ENERGY = 8;
	private final Label[] titleLabels = new Label[TITLE_KEYS.length];
	private final Label[] valueLabels = new Label[TITLE_KEYS.length];

	public void setSelectedOrganism(Organism selectedOrganism) {
		Fx.run(() -> {
			_selOrganism = selectedOrganism;
			valueLabels[I_ID].setText(_selOrganism!=null?_nf.format(_selOrganism.getID()):"-1");
			valueLabels[I_GENERATION].setText(_selOrganism!=null?_nf.format(_selOrganism.getGeneration()):"0");
			valueLabels[I_REPRODUCE_ENERGY].setText(_selOrganism!=null?_nf.format(_selOrganism.getGeneticCode().getReproduceEnergy()):"0");
			recalculateInner();
			changeNChildrenInner();
			changeNKillsInner();
			changeNInfectedInner();
			_buttonGenes.setDisable(_selOrganism == null);
			_geneticCodePanel.setGeneticCode(_selOrganism!=null?_selOrganism.getGeneticCode():null);
			setVisible(_selOrganism != null);
		});
	}

	// Recalculate continuously changing parameters
	public void recalculate() {
		Fx.run(this::recalculateInner);
	}

	private void recalculateInner() {
		valueLabels[I_ENERGY].setText(_selOrganism!=null?_nf.format(_selOrganism.getEnergy()):"0"); //$NON-NLS-1$
		valueLabels[I_AGE].setText(_selOrganism!=null?_nf.format(_selOrganism.getAge()>>8):"0"); //$NON-NLS-1$
		valueLabels[I_MASS].setText(_selOrganism!=null?_nf.format(_selOrganism.getMass()):"0"); //$NON-NLS-1$
	}

	// Notify panel of important events
	public void changeNChildren() {
		Fx.run(this::changeNChildrenInner);
	}

	private void changeNChildrenInner() {
		valueLabels[I_CHILDREN].setText(_selOrganism!=null?_nf.format(_selOrganism.getTotalChildren()):"0"); //$NON-NLS-1$
	}

	public void changeNKills() {
		Fx.run(this::changeNKillsInner);
	}

	private void changeNKillsInner() {
		valueLabels[I_KILLS].setText(_selOrganism!=null?_nf.format(_selOrganism.getTotalKills()):"0"); //$NON-NLS-1$
	}

	public void changeNInfected() {
		Fx.run(this::changeNInfectedInner);
	}

	private void changeNInfectedInner() {
		valueLabels[I_INFECTED].setText(_selOrganism!=null?_nf.format(_selOrganism.getTotalInfected()):"0"); //$NON-NLS-1$
	}

	public InfoToolbar(Organism selectedOrganism, MainWindow mainWindow) {
		_selOrganism = selectedOrganism;
		_mainWindow = mainWindow;
		// Collapse layout space when hidden: an invisible-but-managed node
		// still reserves its rows in MainWindow's bottom VBox, leaving a
		// blank gap that makes the world scrollbar look floating.
		managedProperty().bind(visibleProperty());
		// Prepare number format
		_nf.setMaximumFractionDigits(1);
		setAlignment(Pos.CENTER);
		setHgap(4);
		setVgap(2);
		for (int i = 0; i < 9; i++) {
			ColumnConstraints cc = new ColumnConstraints();
			cc.setHalignment(HPos.CENTER);
			if (i > 0) {
				cc.setPrefWidth(60);
				cc.setHgrow(Priority.SOMETIMES);
			}
			getColumnConstraints().add(cc);
		}
		// ID
	    _geneticCodePanel = new GeneticCodePanel(_selOrganism!=null?_selOrganism.getGeneticCode():null, _mainWindow.getVisibleWorld());
	    _geneticCodePanel.setPrefSize(50, 50);
	    add(_geneticCodePanel, 0, 0);
	    GridPane.setRowSpan(_geneticCodePanel, 3);

	    for (int i = 0; i < TITLE_KEYS.length; i++) {
			titleLabels[i] = centeredLabel(Messages.getString(TITLE_KEYS[i]));
			valueLabels[i] = centeredLabel(i == I_ID ? "-1" : "0"); //$NON-NLS-1$ //$NON-NLS-2$
			add(titleLabels[i], TITLE_COLS[i], TITLE_ROWS[i]);
			add(valueLabels[i], TITLE_COLS[i] + 1, TITLE_ROWS[i]);
	    }
		// Button to view genes
		_buttonGenes = new Button(Messages.getString("T_EXAMINE_GENES")); //$NON-NLS-1$
		_buttonGenes.setOnAction(evt -> new LabWindow(_mainWindow, _selOrganism.getGeneticCode()));
		_buttonGenes.setDisable(_selOrganism == null);
		add(_buttonGenes, 1, 2);
		GridPane.setColumnSpan(_buttonGenes, 2);

		setVisible(_selOrganism != null);
	}

	private static Label centeredLabel(String text) {
		Label l = new Label(text);
		l.setAlignment(Pos.CENTER);
		l.setMaxWidth(Double.MAX_VALUE);
		return l;
	}

	public void changeLocale() {
		Fx.run(() -> {
			for (int i = 0; i < TITLE_KEYS.length; i++)
				titleLabels[i].setText(Messages.getString(TITLE_KEYS[i]));
			_buttonGenes.setText(Messages.getString("T_EXAMINE_GENES")); //$NON-NLS-1$
		});
	}
}
