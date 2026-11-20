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

import javafx.scene.control.ComboBox;


/** JavaFX port: color names with an index-to-awt.Color mapping. */
public class ColorComboBox extends ComboBox<String> {
	private static final String[] colorValues = {Messages.getString("T_RED"),Messages.getString("T_GREEN"),  //$NON-NLS-1$//$NON-NLS-2$
		Messages.getString("T_BLUE"),Messages.getString("T_CYAN"),Messages.getString("T_WHITE"),  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		Messages.getString("T_GRAY"),Messages.getString("T_YELLOW")};  //$NON-NLS-1$//$NON-NLS-2$

	public ColorComboBox(Color c) {
		getItems().addAll(colorValues);
		setSelectedColor(c);
	}

	public Color getSelectedColor() {
		switch (getSelectionModel().getSelectedIndex()) {
		case 0: return Color.RED;
		case 1: return Color.GREEN;
		case 2: return Color.BLUE;
		case 3: return Color.CYAN;
		case 4: return Color.WHITE;
		case 5: return Color.GRAY;
		case 6: return Color.YELLOW;
		default: return Utils.ColorBROWN;
		}
	}

	public void setSelectedColor(Color c) {
		if (c.equals(Color.RED)) getSelectionModel().select(0);
		if (c.equals(Color.GREEN)) getSelectionModel().select(1);
		if (c.equals(Color.BLUE)) getSelectionModel().select(2);
		if (c.equals(Color.CYAN)) getSelectionModel().select(3);
		if (c.equals(Color.WHITE)) getSelectionModel().select(4);
		if (c.equals(Color.GRAY)) getSelectionModel().select(5);
		if (c.equals(Color.YELLOW)) getSelectionModel().select(6);
	}
}
