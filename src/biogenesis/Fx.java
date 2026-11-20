package biogenesis;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.stage.FileChooser;

/**
 * Temporary Swing-to-JavaFX bridge. Ensures the FX toolkit is started and
 * routes UI work onto the FX Application Thread. Deleted once MainWindow
 * itself is JavaFX (then everything already runs on the FX thread).
 */
public final class Fx {
	private Fx() {}
	private static volatile boolean started = false;
	/** Last directory used, mirroring the old JFileChooser behavior. */
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
	 * Run a dialog on the FX Application Thread and block the caller for the
	 * result. Lets Swing-EDT code use modal FX dialogs with unchanged flow.
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

	/** Modal error alert. Null title shows no title, like the old calls. */
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

	/** Modal yes/no/cancel question, like JOptionPane YES_NO_CANCEL_OPTION. */
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

	/** Modal yes/no question, like JOptionPane YES_NO_OPTION. */
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
	 * for PNG snapshots where the old code kept the name as typed).
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
