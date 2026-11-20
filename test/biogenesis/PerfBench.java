/* Throwaway-grade benchmark, kept in test/ (never shipped in the jar).
 *
 * Headless scaling benchmark for the sim core + AWT render path.
 * Runs a matrix of (seeds x population sizes x world sizes x collide on/off),
 * warming up before measuring, and prints one CSV row per run to stdout.
 *
 * Run via {@code ./build.sh bench [--ns 50,100,200] [--worlds 1000x560]
 *   [--seeds 3] [--ticks 600] [--warmup 100] [--collide both|on|off]
 *   [--render on|off] [--out results.csv]}.
 * FX texture upload can't run headless, so render_ms covers AWT draw + pixel
 * copy only (the upload half is measured headful via /tmp/dbg FxBench).
 */
package biogenesis;

import java.awt.Rectangle;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.Arrays;

public class PerfBench {
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

	static class BenchWorld extends World {
		static boolean noCollide = false;
		BenchWorld(WorldView view) { super(view); }
		@Override
		public Organism checkHit(Organism org1) {
			if (noCollide)
				return null;
			return super.checkHit(org1);
		}
	}

	static long gcMs() {
		long ms = 0;
		for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans())
			ms += gc.getCollectionTime();
		return ms;
	}

	static double percentile(double[] sorted, double p) {
		if (sorted.length == 0)
			return Double.NaN;
		int idx = Math.min(sorted.length - 1, (int) Math.ceil(p * sorted.length) - 1);
		return sorted[Math.max(0, idx)];
	}

	static class Cfg {
		int[] ns = {50, 100, 200, 400, 800};
		int[][] worlds = {{1000, 560}};
		int seeds = 3;
		int ticks = 600;
		int warmup = 100;
		String collide = "both"; // on|off|both
		boolean render = true;
		String baseSeed = "12345";
		double initCO2 = Double.NaN; // NaN = leave Utils default
		double initO2 = Double.NaN;
		int rticks = 100;
	}

	public static void main(String[] args) throws Exception {
		Cfg cfg = new Cfg();
		String outPath = null;
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
			case "--ns":
				cfg.ns = Arrays.stream(args[++i].split(",")).mapToInt(Integer::parseInt).toArray();
				break;
			case "--worlds": {
				String[] parts = args[++i].split(",");
				cfg.worlds = new int[parts.length][];
				for (int k = 0; k < parts.length; k++) {
					String[] wh = parts[k].split("x");
					cfg.worlds[k] = new int[]{Integer.parseInt(wh[0]), Integer.parseInt(wh[1])};
				}
				break;
			}
			case "--seeds":
				cfg.seeds = Integer.parseInt(args[++i]);
				break;
			case "--ticks":
				cfg.ticks = Integer.parseInt(args[++i]);
				break;
			case "--warmup":
				cfg.warmup = Integer.parseInt(args[++i]);
				break;
			case "--collide":
				cfg.collide = args[++i];
				break;
			case "--render":
				cfg.render = args[++i].equalsIgnoreCase("on");
				break;
			case "--base-seed":
				cfg.baseSeed = args[++i];
				break;
			case "--co2":
				cfg.initCO2 = Double.parseDouble(args[++i]);
				break;
			case "--o2":
				cfg.initO2 = Double.parseDouble(args[++i]);
				break;
			case "--rticks":
				cfg.rticks = Integer.parseInt(args[++i]);
				break;
			case "--out":
				outPath = args[++i];
				break;
			default:
				System.err.println("unknown arg: " + args[i]);
				System.err.println("usage: PerfBench [--ns 50,100,...] [--worlds WxH,...] [--seeds N]"
						+ " [--ticks N] [--warmup N] [--collide both|on|off] [--render on|off]"
						+ " [--base-seed N] [--co2 N] [--o2 N] [--rticks N] [--out file.csv]");
				System.exit(2);
			}
		}

		String header = "run_ts,host,os,arch,java,seed,world_w,world_h,initial_n,placed_n,"
				+ "pop_warm,pop_end,ticks,warmup,collide,sim_ms_mean,sim_ms_p95,sim_ms_max,"
				+ "render_ms_mean,avg_seg,max_seg,gc_ms,init_co2,init_o2,rticks,notes";
		StringBuilder csv = new StringBuilder(header).append('\n');
		String runTs = Instant.now().toString();
		String os = System.getProperty("os.name") + " " + System.getProperty("os.version");
		String arch = System.getProperty("os.arch");
		String javaVer = System.getProperty("java.version");
		String host = System.getenv().getOrDefault("HOSTNAME",
				System.getProperty("user.name") + "@" + arch);

		boolean[] collideModes = cfg.collide.equals("both")
				? new boolean[]{false, true}
				: new boolean[]{cfg.collide.equals("off")};
		long baseSeed = Long.parseLong(cfg.baseSeed);

		for (int[] wh : cfg.worlds) {
			for (int n : cfg.ns) {
				for (int s = 0; s < cfg.seeds; s++) {
					for (boolean noCollide : collideModes) {
						csv.append(runOne(runTs, host, os, arch, javaVer, baseSeed + s,
								wh[0], wh[1], n, cfg, noCollide));
					}
				}
			}
		}

		if (outPath != null) {
			java.nio.file.Files.writeString(java.nio.file.Path.of(outPath), csv.toString());
			System.err.println("wrote " + outPath);
		} else {
			System.out.print(csv.toString());
		}
	}

	static String runOne(String runTs, String host, String os, String arch, String javaVer,
			long seed, int ww, int wh, int n, Cfg cfg, boolean noCollide) {
		Utils.random.setSeed(seed);
		Utils.WORLD_WIDTH = ww;
		Utils.WORLD_HEIGHT = wh;
		Utils.INITIAL_ORGANISMS = n;
		if (!Double.isNaN(cfg.initCO2))
			Utils.INITIAL_CO2 = cfg.initCO2;
		if (!Double.isNaN(cfg.initO2))
			Utils.INITIAL_O2 = cfg.initO2;
		BenchWorld.noCollide = noCollide;
		BenchWorld world = new BenchWorld(new StubView());
		world.genesis();
		int placed = world.getPopulation();
		String notes = placed < n ? "placement-shortfall" : "";

		for (int i = 0; i < cfg.warmup; i++)
			world.time();
		int popWarm = world.getPopulation();

		double[] tickMs = new double[cfg.ticks];
		long gc0 = gcMs();
		for (int i = 0; i < cfg.ticks; i++) {
			long t0 = System.nanoTime();
			world.time();
			tickMs[i] = (System.nanoTime() - t0) / 1e6;
		}
		long gc1 = gcMs();
		int popEnd = world.getPopulation();

		double sum = 0, max = 0;
		for (double v : tickMs) {
			sum += v;
			max = Math.max(max, v);
		}
		double[] sorted = tickMs.clone();
		Arrays.sort(sorted);

		// organism size stats
		long segSum = 0;
		int segMax = 0, count = 0;
		synchronized (world._organisms) {
			for (Organism o : world._organisms) {
				segSum += o._segments;
				segMax = Math.max(segMax, o._segments);
				count++;
			}
		}
		double avgSeg = count == 0 ? 0 : (double) segSum / count;

		// render phase: full-world AWT draw into a reused buffer + pixel copy
		double renderMean = Double.NaN;
		if (cfg.render) {
			java.awt.image.BufferedImage buf = new java.awt.image.BufferedImage(
					world.getWidth(), world.getHeight(),
					java.awt.image.BufferedImage.TYPE_INT_RGB);
			int w = buf.getWidth(), h = buf.getHeight();
			int[] argb = new int[w * h];
			int rwarm = Math.min(20, cfg.rticks);
			for (int i = 0; i < rwarm; i++) {
				java.awt.Graphics2D g = buf.createGraphics();
				g.setBackground(java.awt.Color.BLACK);
				g.clearRect(0, 0, w, h);
				world.draw(g);
				g.dispose();
				buf.getRGB(0, 0, w, h, argb, 0, w);
			}
			int rticks = cfg.rticks;
			long r0 = System.nanoTime();
			for (int i = 0; i < rticks; i++) {
				java.awt.Graphics2D g = buf.createGraphics();
				g.setBackground(java.awt.Color.BLACK);
				g.clearRect(0, 0, w, h);
				world.draw(g);
				g.dispose();
				buf.getRGB(0, 0, w, h, argb, 0, w);
			}
			renderMean = (System.nanoTime() - r0) / 1e6 / rticks;
		}

		return String.format(java.util.Locale.ROOT,
				"%s,%s,%s,%s,%s,%d,%d,%d,%d,%d,%d,%d,%d,%d,%s,%.4f,%.4f,%.4f,%s,%.1f,%d,%d,%s,%s,%d,%s%n",
				runTs, host, os, arch, javaVer, seed, ww, wh, n, placed,
				popWarm, popEnd, cfg.ticks, cfg.warmup, noCollide ? "off" : "on",
				sum / cfg.ticks, percentile(sorted, 0.95), max,
				Double.isNaN(renderMean) ? "" : String.format(java.util.Locale.ROOT, "%.4f", renderMean),
				avgSeg, segMax, gc1 - gc0,
				Double.isNaN(cfg.initCO2) ? "" : String.format(java.util.Locale.ROOT, "%.0f", cfg.initCO2),
				Double.isNaN(cfg.initO2) ? "" : String.format(java.util.Locale.ROOT, "%.0f", cfg.initO2),
				cfg.rticks,
				notes);
	}
}
