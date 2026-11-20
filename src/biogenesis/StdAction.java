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


import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

/**
 * Plain FX command object holding a menu/toolbar action's live state:
 * just data + an abstract {@link #perform()} body. FX wrappers ({@code toolButtonFor},
 * {@code Fx.menuItemFor}) read the getters when rebuilding
 * chrome via {@code refreshChrome()} / {@code createToolBar()}.
 */
public abstract class StdAction {
	protected String name_key;
	protected String desc_key;
	/**
	 * Icon resource path (e.g. "images/menu_track.png"), kept so FX menus and
	 * buttons can load the same artwork lazily. Plain string on purpose:
	 * loading an FX Image here would break headless tests.
	 */
	protected String iconPath;
	protected String name;
	protected String description;
	/** Messages key for a single-letter accelerator, re-derived on locale change. Null for none. */
	protected String acceleratorKey;
	protected KeyCombination accelerator;
	private boolean enabled = true;

	public void changeLocale() {
		name = Messages.getString(name_key);
		description = Messages.getString(desc_key);
		if (acceleratorKey != null)
			accelerator = keyCombinationFor(Messages.getString(acceleratorKey));
	}

	public StdAction(String key, String icon_path, String desc) {
		this(key, icon_path, desc, null);
	}

	public StdAction(String key, String icon_path, String desc, String acceleratorKey) {
		name_key = key;
		desc_key = desc;
		iconPath = icon_path;
		name = Messages.getString(key);
		description = Messages.getString(desc);
		this.acceleratorKey = acceleratorKey;
		if (acceleratorKey != null)
			accelerator = keyCombinationFor(Messages.getString(acceleratorKey));
	}

	/** One-shot command: no subclass needed for a plain body. */
	public static StdAction of(String key, String icon_path, String desc, Runnable body) {
		return new StdAction(key, icon_path, desc) {
			public void perform() {
				body.run();
			}
		};
	}

	/** One-shot command with a locale-sensitive accelerator. */
	public static StdAction of(String key, String icon_path, String desc, String acceleratorKey, Runnable body) {
		return new StdAction(key, icon_path, desc, acceleratorKey) {
			public void perform() {
				body.run();
			}
		};
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public KeyCombination getAccelerator() {
		return accelerator;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	/**
	 * Builds a plain-key accelerator from a single-letter message string
	 * (e.g. "P" from T_PAUSE_ACCELERATOR). Null when unparseable.
	 */
	protected static KeyCombination keyCombinationFor(String keyText) {
		if (keyText == null || keyText.isEmpty())
			return null;
		KeyCode code = KeyCode.getKeyCode(keyText.toUpperCase());
		if (code == null || code == KeyCode.UNDEFINED)
			return null;
		return new KeyCodeCombination(code);
	}

	/** Runs the action. Bodies never took the event, so no parameter. */
	public abstract void perform();
}
