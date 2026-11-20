package biogenesis;

import javafx.application.Platform;

/**
 * Temporary Swing-to-JavaFX bridge. Ensures the FX toolkit is started and
 * routes UI work onto the FX Application Thread. Deleted once MainWindow
 * itself is JavaFX (then everything already runs on the FX thread).
 */
public final class Fx {
	private Fx() {}
	private static volatile boolean started = false;

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
}
