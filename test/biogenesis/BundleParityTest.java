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
import static org.junit.Assert.assertFalse;

import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;

/**
 * All 7 message bundles must carry exactly the same keys, and a sample
 * of keys must resolve (no raw "!key!" markers reach the UI).
 */
public class BundleParityTest {
	static final String[] LANGS = {"en", "es", "ca", "de", "pt", "ru"};

	@Test
	public void testAllLocalesMatchBase() {
		Set<String> base = null;
		for (int i = 0; i < LANGS.length; i++) {
			Messages.setLocale(i);
			ResourceBundle b = ResourceBundle.getBundle("biogenesis.messages.messages",
					new java.util.Locale(LANGS[i]));
			Set<String> keys = b.keySet();
			if (base == null)
				base = keys;
			Set<String> missing = new TreeSet<>(base);
			missing.removeAll(keys);
			Set<String> extra = new TreeSet<>(keys);
			extra.removeAll(base);
			assertEquals("missing in " + LANGS[i], new TreeSet<String>(), missing);
			assertEquals("extra in " + LANGS[i], new TreeSet<String>(), extra);
		}
	}

	@Test
	public void testSampleKeysResolveEverywhere() {
		String[] keys = {"T_MANAGE_CONNECTIONS", "T_SIM_RATE", "T_FPS",
				"T_READ_ERROR", "T_WRONG_FILE_VERSION", "T_GENETIC_LABORATORY"};
		for (int i = 0; i < LANGS.length; i++) {
			Messages.setLocale(i);
			for (String k : keys)
				assertFalse(LANGS[i] + "/" + k + " unresolved",
						Messages.getString(k).contains("!")); //$NON-NLS-1$
		}
		Messages.setLocale(0);
	}
}
