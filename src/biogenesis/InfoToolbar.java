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
	protected Label _lEnergy, _lID, _lGeneration, _lAge, _lChildren, _lKills, _lInfected, _lMass, _lReproduceEnergy;
	protected Button _buttonGenes;
	protected GeneticCodePanel _geneticCodePanel;
	static private NumberFormat _nf = NumberFormat.getInstance();
	protected MainWindow _mainWindow;

	private Label _lT_REPRODUCTION;

	private Label _lT_ID;

	private Label _lT_GENERATION;

	private Label _lT_AGE;

	private Label _lT_ENERGY;

	private Label _lT_CHILDREN;

	private Label _lT_VICTIMS;

	private Label _lT_INFECTED;

	private Label _lT_MASS;

	public void setSelectedOrganism(Organism selectedOrganism) {
		Fx.run(() -> {
			_selOrganism = selectedOrganism;
			_lID.setText(_selOrganism!=null?_nf.format(_selOrganism.getID()):"-1");
			_lGeneration.setText(_selOrganism!=null?_nf.format(_selOrganism.getGeneration()):"0");
			_lReproduceEnergy.setText(_selOrganism!=null?_nf.format(_selOrganism.getGeneticCode().getReproduceEnergy()):"0");
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
		_lEnergy.setText(_selOrganism!=null?_nf.format(_selOrganism.getEnergy()):"0"); //$NON-NLS-1$
		_lAge.setText(_selOrganism!=null?_nf.format(_selOrganism.getAge()>>8):"0"); //$NON-NLS-1$
		_lMass.setText(_selOrganism!=null?_nf.format(_selOrganism.getMass()):"0"); //$NON-NLS-1$
	}

	// Notify panel of important events
	public void changeNChildren() {
		Fx.run(this::changeNChildrenInner);
	}

	private void changeNChildrenInner() {
		_lChildren.setText(_selOrganism!=null?_nf.format(_selOrganism.getTotalChildren()):"0"); //$NON-NLS-1$
	}

	public void changeNKills() {
		Fx.run(this::changeNKillsInner);
	}

	private void changeNKillsInner() {
		_lKills.setText(_selOrganism!=null?_nf.format(_selOrganism.getTotalKills()):"0"); //$NON-NLS-1$
	}

	public void changeNInfected() {
		Fx.run(this::changeNInfectedInner);
	}

	private void changeNInfectedInner() {
		_lInfected.setText(_selOrganism!=null?_nf.format(_selOrganism.getTotalInfected()):"0"); //$NON-NLS-1$
	}

	public InfoToolbar(Organism selectedOrganism, MainWindow mainWindow) {
		_selOrganism = selectedOrganism;
		_mainWindow = mainWindow;
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

	    _lT_ID = centeredLabel(Messages.getString("T_ID")); //$NON-NLS-1$
		add(_lT_ID, 1, 0);
		_lID = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getID()):"-1"); //$NON-NLS-1$
		add(_lID, 2, 0);
		// Generation
		_lT_GENERATION = centeredLabel(Messages.getString("T_GENERATION")); //$NON-NLS-1$
		add(_lT_GENERATION, 3, 0);
		_lGeneration = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getGeneration()):"0"); //$NON-NLS-1$
		add(_lGeneration, 4, 0);
		// Age
		_lT_AGE = centeredLabel(Messages.getString("T_AGE")); //$NON-NLS-1$
		add(_lT_AGE, 5, 0);
		_lAge = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getAge()>>8):"0"); //$NON-NLS-1$
		add(_lAge, 6, 0);
		// Energy
		_lT_ENERGY = centeredLabel(Messages.getString("T_ENERGY")); //$NON-NLS-1$
		add(_lT_ENERGY, 7, 0);
		_lEnergy = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getEnergy()):"0"); //$NON-NLS-1$
		add(_lEnergy, 8, 0);
		// Number of sons
		_lT_CHILDREN = centeredLabel(Messages.getString("T_CHILDREN")); //$NON-NLS-1$
		add(_lT_CHILDREN, 1, 1);
		_lChildren = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getTotalChildren()):"0"); //$NON-NLS-1$
		add(_lChildren, 2, 1);
		// Number of killed organisms
		_lT_VICTIMS = centeredLabel(Messages.getString("T_VICTIMS")); //$NON-NLS-1$
		add(_lT_VICTIMS, 3, 1);
		_lKills = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getTotalKills()):"0"); //$NON-NLS-1$
		add(_lKills, 4, 1);
		// Number of infected organisms
		_lT_INFECTED = centeredLabel(Messages.getString("T_INFECTED")); //$NON-NLS-1$
		add(_lT_INFECTED, 5, 1);
		_lInfected = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getTotalInfected()):"0"); //$NON-NLS-1$
		add(_lInfected, 6, 1);
		// Total mass
		_lT_MASS = centeredLabel(Messages.getString("T_MASS")); //$NON-NLS-1$
		add(_lT_MASS, 7, 1);
		_lMass = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getMass()):"0"); //$NON-NLS-1$
		add(_lMass, 8, 1);
		// Button to view genes
		_buttonGenes = new Button(Messages.getString("T_EXAMINE_GENES")); //$NON-NLS-1$
		_buttonGenes.setOnAction(evt -> new LabWindow(_mainWindow, _selOrganism.getGeneticCode()));
		_buttonGenes.setDisable(_selOrganism == null);
		add(_buttonGenes, 1, 2);
		GridPane.setColumnSpan(_buttonGenes, 2);
		// Reproduce energy
		_lT_REPRODUCTION = centeredLabel(Messages.getString("T_REPRODUCTION")); //$NON-NLS-1$
		add(_lT_REPRODUCTION, 3, 2);
		_lReproduceEnergy = centeredLabel(_selOrganism!=null?_nf.format(_selOrganism.getGeneticCode().getReproduceEnergy()):"0");
		add(_lReproduceEnergy, 4, 2);

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
			_lT_ID.setText(Messages.getString("T_ID")); //$NON-NLS-1$
			_lT_GENERATION.setText(Messages.getString("T_GENERATION")); //$NON-NLS-1$
			_lT_AGE.setText(Messages.getString("T_AGE"));  //$NON-NLS-1$
			_lT_ENERGY.setText(Messages.getString("T_ENERGY"));  //$NON-NLS-1$
			_lT_CHILDREN.setText(Messages.getString("T_CHILDREN"));  //$NON-NLS-1$
			_lT_VICTIMS.setText(Messages.getString("T_VICTIMS"));  //$NON-NLS-1$
			_lT_INFECTED.setText(Messages.getString("T_INFECTED"));  //$NON-NLS-1$
			_lT_MASS.setText(Messages.getString("T_MASS"));  //$NON-NLS-1$
			_buttonGenes.setText(Messages.getString("T_EXAMINE_GENES")); //$NON-NLS-1$
			_lT_REPRODUCTION.setText(Messages.getString("T_REPRODUCTION"));  //$NON-NLS-1$
		});
	}
}
