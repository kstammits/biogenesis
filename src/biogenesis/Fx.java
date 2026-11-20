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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.FileChooser;

/**
 * FX toolkit helper. Ensures the FX toolkit is started and routes UI work
 * onto the FX Application Thread (the sim thread drives UI off-thread).
 */
public final class Fx {
	private Fx() {}
	private static volatile boolean started = false;
	/** Last directory used by the file choosers. */
	private static File lastDir = new File(System.getProperty("user.dir")); //$NON-NLS-1$

	public static void ensureStarted() {
		if (!started) {
			synchronized (Fx.class) {
				if (!started) {
					try {
						Platform.startup(() -> {});
					} catch (IllegalStateException e) {
						// toolkit already started
					}
					started = true;
				}
			}
		}
	}

	/** Run UI work on the FX Application Thread, starting the toolkit if needed. */
	public static void run(Runnable r) {
		ensureStarted();
		if (Platform.isFxApplicationThread())
			r.run();
		else
			Platform.runLater(r);
	}

	/**
	 * Parses a dialog field, applying the value only when it parses and the
	 * validator accepts it. Otherwise the old value is silently kept (the
	 * ParamDialog/NetConfigWindow contract).
	 */
	public static void parseIntOrKeep(TextField field,
			java.util.function.IntPredicate valid, java.util.function.IntConsumer apply) {
		try {
			int v = Integer.parseInt(field.getText());
			if (valid.test(v))
				apply.accept(v);
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
	}

	/** Double-valued variant of {@link #parseIntOrKeep}. */
	public static void parseDoubleOrKeep(TextField field,
			java.util.function.DoublePredicate valid, java.util.function.DoubleConsumer apply) {
		try {
			double v = Double.parseDouble(field.getText());
			if (valid.test(v))
				apply.accept(v);
		} catch (NumberFormatException ex) {
			// Keep old value if there is a problem
		}
	}

	/**
	 * Run a dialog on the FX Application Thread and block the caller for the
	 * result, so sim-thread code can use modal FX dialogs with unchanged flow.
	 */
	public static <T> T ask(Callable<T> dialog) {
		ensureStarted();
		if (Platform.isFxApplicationThread()) {
			try {
				return dialog.call();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		FutureTask<T> task = new FutureTask<>(dialog);
		Platform.runLater(task);
		try {
			return task.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException)
				throw (RuntimeException) cause;
			throw new RuntimeException(cause);
		}
	}

	/** Modal error alert. Null title shows no title. */
	public static void error(String title, String message) {
		ask(() -> {
			Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
			if (title != null)
				alert.setTitle(title);
			alert.setHeaderText(null);
			alert.showAndWait();
			return null;
		});
	}

	/**
	 * Standard file-load failure dialog (title {@code T_READ_ERROR}).
	 * For owned contexts (dialogs with a stage) use an owned alert instead —
	 * this one is unowned, like {@link #error}.
	 */
	public static void readError(String messageKey) {
		error(Messages.getString("T_READ_ERROR"), Messages.getString(messageKey)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	/** Modal info alert, returned after dismissal. */
	public static void info(String title, String message) {
		ask(() -> {
			Alert alert = new Alert(Alert.AlertType.INFORMATION, message, ButtonType.OK);
			alert.setTitle(title);
			alert.setHeaderText(null);
			alert.showAndWait();
			return null;
		});
	}

	public enum Answer { YES, NO, CANCEL }

	/** Modal yes/no/cancel question. */
	public static Answer confirmYesNoCancel(String title, String message) {
		return ask(() -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
					ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
			alert.setTitle(title);
			alert.setHeaderText(null);
			ButtonType result = alert.showAndWait().orElse(ButtonType.CANCEL);
			if (result.getButtonData() == ButtonData.YES)
				return Answer.YES;
			if (result.getButtonData() == ButtonData.NO)
				return Answer.NO;
			return Answer.CANCEL;
		});
	}

	/** Modal yes/no question. */
	public static boolean confirmYesNo(String title, String message) {
		return ask(() -> {
			Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
					ButtonType.YES, ButtonType.NO);
			alert.setTitle(title);
			alert.setHeaderText(null);
			return alert.showAndWait().orElse(ButtonType.NO).getButtonData() == ButtonData.YES;
		});
	}

	private static FileChooser chooser(String title, String descKey, String ext) {
		FileChooser chooser = new FileChooser();
		chooser.setTitle(Messages.getString(title));
		chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
				Messages.getString(descKey) + " (*." + ext + ")", "*." + ext)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		if (lastDir.isDirectory())
			chooser.setInitialDirectory(lastDir);
		return chooser;
	}

	/**
	 * Pure extension rule for save dialogs, extracted for headless testing:
	 * appends ".ext" when missing unless {@code autoAppendExt} is false
	 * (e.g. PNG snapshots keep the name as typed). Null in, null out.
	 */
	static File withAppendedExt(File f, String ext, boolean autoAppendExt) {
		if (f == null)
			return null;
		if (autoAppendExt && f.getName().lastIndexOf('.') == -1)
			return new File(f.getAbsolutePath() + "." + ext); //$NON-NLS-1$
		return f;
	}

	/** Modal open-file dialog with a single extension filter. Null if cancelled. */
	public static File showOpenFile(String titleKey, String descKey, String ext) {
		return ask(() -> {
			File f = chooser(titleKey, descKey, ext).showOpenDialog(null);
			if (f != null && f.getParentFile() != null)
				lastDir = f.getParentFile();
			return f;
		});
	}

	/**
	 * Modal save-file dialog with a single extension filter. Appends the
	 * extension when missing (unless {@code autoAppendExt} is false, e.g.
	 * for PNG snapshots where the name is kept as typed).
	 * Null if cancelled.
	 */
	public static File showSaveFile(String titleKey, String descKey, String ext, boolean autoAppendExt) {
		return ask(() -> {
			File f = chooser(titleKey, descKey, ext).showSaveDialog(null);
			if (f == null)
				return null;
			f = withAppendedExt(f, ext, autoAppendExt);
			if (f.getParentFile() != null)
				lastDir = f.getParentFile();
			return f;
		});
	}
}
