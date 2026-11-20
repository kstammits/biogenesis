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

/**
 * View-side seam for the simulation.
 *
 * Implemented by the Swing {@link VisibleWorld} now and by the JavaFX canvas
 * view later. Lets {@link World} and {@link Organism} run headless (tests,
 * server use) and replaces direct component reach-through such as
 * {@code _visibleWorld._mainWindow.getInfoPanel()...}.
 *
 * <p>Direction matters: the simulation calls <em>out</em> through this
 * interface (repaint requests, selection queries, statistics events);
 * the view calls <em>in</em> to {@link World#draw} and {@link World#time}.
 *
 * <p>TODO(javafx): move selection ownership into World. The view should push
 * selection changes in rather than the sim polling
 * {@link #getSelectedOrganism()}.
 */
public interface WorldView {
	/**
	 * Request repaint of a world region. Organisms and corridors are
	 * {@link java.awt.Rectangle}s, so a single method covers both.
	 */
	void repaintRegion(java.awt.Rectangle region);
	/** The currently tracked organism, or null if none. */
	Organism getSelectedOrganism();
	/** Clear any tracked-organism selection. */
	void clearSelection();
	/** Switch the info toolbar to dead-organism mode. */
	void showDeadToolbar();
	/** Periodic statistics refresh (every 20 frames). */
	void worldStatisticsChanged();
	/** A birth was attributed to the selected organism. */
	void organismReproduced();
	/** A kill was attributed to the selected organism. */
	void organismKilled();
	/** An infection was attributed to the selected organism. */
	void organismInfected();
	/** World (re)sized: the view should adjust its preferred size. */
	void worldResized(int width, int height);
}
