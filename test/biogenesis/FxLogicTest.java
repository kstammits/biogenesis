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
