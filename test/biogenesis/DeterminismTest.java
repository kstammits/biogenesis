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

import static org.junit.Assert.assertEquals;

import java.awt.Rectangle;

import org.junit.Test;

/**
 * Deterministic sim trace: seed 12345 must reproduce the exact population,
 * time and atmosphere of the reference run. Catches silent physics drift.
 */
public class DeterminismTest {
	static class StubView implements WorldView {
		public void repaintRegion(Rectangle r) {}
		public Organism getSelectedOrganism() { return null; }
		public void clearSelection() {}
		public void showDeadToolbar() {}
		public void worldStatisticsChanged() {}
		public void organismReproduced() {}
		public void organismKilled() {}
		public void organismInfected() {}
		public void worldResized(int w, int h) {}
	}

	@Test
	public void testSeed12345Trace() {
		Utils.random.setSeed(12345L);
		World world = new World(new StubView());
		world.genesis();
		for (int i = 0; i < 500; i++)
			world.time();
		assertEquals(1, world.getTime());
		assertEquals(41, world.getPopulation());
		assertEquals(1678.5532538909933, world.getO2(), 1e-9);
		assertEquals(5321.446746108239, world.getCO2(), 1e-9);
		if (world.getPopulation() <= 0)
			throw new AssertionError("population died out");
	}
}
