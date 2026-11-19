import biogenesis.Organism;
import biogenesis.Utils;
import biogenesis.World;
import biogenesis.WorldView;
import java.awt.Rectangle;

/**
 * Headless simulation smoke test. Runs the world without any UI by
 * attaching a no-op {@link WorldView}, proving the sim core is decoupled
 * from Swing. Not shipped in the jar (lives in test/, compiled ad-hoc).
 */
public class SimSmoke {
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

	public static void main(String[] args) throws Exception {
		Utils.random.setSeed(12345L); // deterministic run
		World world = new World(new StubView());
		world.genesis();
		int ticks = args.length > 0 ? Integer.parseInt(args[0]) : 500;
		for (int i = 0; i < ticks; i++)
			world.time();
		System.out.println("ticks=" + ticks + " time=" + world.getTime()
			+ " population=" + world.getPopulation()
			+ " o2=" + world.getO2() + " co2=" + world.getCO2());
		if (world.getPopulation() <= 0)
			throw new AssertionError("population died out after " + ticks + " ticks");
		System.out.println("SIMSMOKE OK");
	}
}
