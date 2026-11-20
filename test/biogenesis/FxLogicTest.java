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

import java.io.File;

/**
 * Headless unit test for the save-dialog filename rules in {@link Fx}.
 * Covers the pure extension logic without opening any native dialog.
 * Run via {@code ./build.sh test}. Never shipped in the jar.
 */
public class FxLogicTest {
	static int checks = 0;

	static void check(boolean cond, String name) {
		checks++;
		System.out.println((cond ? "PASS " : "FAIL ") + name);
		if (!cond)
			throw new AssertionError("FxLogicTest failed: " + name);
	}

	public static void main(String[] args) {
		// null in -> null out (cancelled dialog)
		check(Fx.withAppendedExt(null, "bgw", true) == null, "null stays null");
		// world save: missing ext gets appended
		check(new File("/tmp/world.bgw").equals(Fx.withAppendedExt(new File("/tmp/world"), "bgw", true)),
				"auto-append adds .bgw");
		// already has an extension: untouched
		check(new File("/tmp/world.bgw").equals(Fx.withAppendedExt(new File("/tmp/world.bgw"), "bgw", true)),
				"existing ext untouched");
		// PNG snapshot rule: name kept as typed
		check(new File("/tmp/shot").equals(Fx.withAppendedExt(new File("/tmp/shot"), "png", false)),
				"no-auto keeps bare name");
		check(new File("/tmp/shot.png").equals(Fx.withAppendedExt(new File("/tmp/shot.png"), "png", false)),
				"no-auto keeps .png name");

		System.out.println("FXLOGIC OK checks=" + checks);
	}
}
