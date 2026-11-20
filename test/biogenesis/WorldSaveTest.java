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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

/**
 * .bgw save/load round-trip through the deserialization allow-list.
 * The collision index must not ship inside saves (transient) and must
 * be rebuilt on load.
 */
public class WorldSaveTest {
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
	public void testSaveLoadRoundTrip() throws Exception {
		Utils.random.setSeed(12345L);
		World world = new World(new StubView());
		world.genesis();
		for (int i = 0; i < 100; i++)
			world.time();
		File f = File.createTempFile("world", ".bgw");
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
			out.writeObject(world);
		}
		World back;
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
			in.setObjectInputFilter(Utils.SERIAL_FILTER);
			back = (World) in.readObject();
		} finally {
			f.delete();
		}
		back.init(new StubView());
		assertEquals(world.getPopulation(), back.getPopulation());
		assertEquals(world.getTime(), back.getTime());
		assertEquals(world.getO2(), back.getO2(), 0.0);
		assertEquals(world.getCO2(), back.getCO2(), 0.0);
		assertNotNull(back.colDetTree);
	}

	@Test
	public void testFilterRejectsUnknownClass() throws Exception {
		Utils.random.setSeed(7L);
		World world = new World(new StubView());
		world.genesis();
		File f = File.createTempFile("world", ".bgw");
		try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(f))) {
			out.writeObject(world);
		}
		try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
			in.setObjectInputFilter(info -> ObjectInputFilter.Status.REJECTED);
			try {
				in.readObject();
				fail("expected InvalidClassException from reject-all filter");
			} catch (java.io.InvalidClassException expected) {
				// The filter trips before any gadget can load.
			}
		} finally {
			f.delete();
		}
	}
}
