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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Headless unit tests for the pure pacing/config math. No UI, no sim.
 */
public class SimMathTest {
	@Test
	public void testBetweenClamps() {
		assertEquals(5, Utils.between(5, 1, 10));
		assertEquals(1, Utils.between(-3, 1, 10));
		assertEquals(10, Utils.between(99, 1, 10));
		assertEquals(1.5, Utils.between(1.5, 0.0, 2.0), 0.0);
		assertEquals(0.0, Utils.between(-1.0, 0.0, 2.0), 0.0);
		assertEquals(2.0, Utils.between(9.0, 0.0, 2.0), 0.0);
	}

	@Test
	public void testSimRateRoundTrip() {
		for (int rate : Utils.SIM_RATE_PRESETS) {
			if (rate <= 0)
				continue; // uncapped has no delay
			assertEquals(rate, Utils.nearestSimRate(Utils.delayForSimRate(rate)));
		}
		// Legacy raw delays snap to the nearest preset rate.
		assertEquals(30, Utils.nearestSimRate(33));
		assertEquals(60, Utils.nearestSimRate(17));
	}

	@Test
	public void testBatchSizeForDelay() {
		assertEquals(1, MainWindow.batchSizeForDelay(100));
		assertEquals(1, MainWindow.batchSizeForDelay(25));
		assertEquals(2, MainWindow.batchSizeForDelay(12));
		assertEquals(16, MainWindow.batchSizeForDelay(1));
		assertEquals(1, MainWindow.batchSizeForDelay(0));
		assertEquals(1, MainWindow.batchSizeForDelay(-5));
	}

	@Test
	public void testEffectiveRate() {
		// At target the preset rate itself paces (exact, not 1000/delay).
		assertEquals(120.0, MainWindow.effectiveRate(8, 8), 0.0);
		assertEquals(60.0, MainWindow.effectiveRate(17, 17), 0.0);
		// Uncapped has no target rate.
		assertTrue(Double.isInfinite(MainWindow.effectiveRate(1, 1)));
		// Under governor backoff the delay's own rate paces.
		assertEquals(1000.0 / 50, MainWindow.effectiveRate(50, 8), 1e-9);
	}

	@Test
	public void testNominalPeriod() {
		// One fire carries batch ticks: period = batch * 1000/rate.
		double p = MainWindow.nominalPeriodMs(8, 8);
		assertTrue(p > 0);
		assertEquals(MainWindow.batchSizeForDelay(8) * 1000.0 / 120, p, 1e-9);
	}
}
