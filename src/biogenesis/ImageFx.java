package biogenesis;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;

/**
 * BufferedImage → FX image bridge via manual PixelWriter copy:
 * direct INT_RGB raster + alpha-fill into a reused buffer, then bulk
 * setPixels with IntArgb format.
 */
public final class ImageFx {
	private ImageFx() {}

	/**
	 * Manual conversion: direct INT_RGB raster + alpha-fill into a reused
	 * buffer, then bulk setPixels with IntArgb format. Pixel-exact, A/B-benched
	 * ~20% faster at full-world size than the previous converter.
	 * Must run on the FX thread; scratch buffer is FX-thread-confined.
	 */
	public static WritableImage toFXImage(BufferedImage image) {
		return copyToFXImage(image, null);
	}

	/**
	 * Blit a BufferedImage into a (possibly reused) WritableImage. When
	 * {@code reuse} matches the image size it is overwritten in place and
	 * returned, avoiding one WritableImage allocation per frame. Otherwise a
	 * correctly sized image is allocated. Must run on the FX thread.
	 */
	public static WritableImage copyToFXImage(BufferedImage image, WritableImage reuse) {
		int w = image.getWidth();
		int h = image.getHeight();
		int n = w * h;
		int[] argb = scratch(n);
		if (image.getType() == BufferedImage.TYPE_INT_RGB
				&& image.getRaster().getDataBuffer() instanceof DataBufferInt) {
			int[] raster = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			if (raster.length >= n) {
				for (int i = 0; i < n; i++)
					argb[i] = raster[i] | 0xFF000000;
			} else {
				image.getRGB(0, 0, w, h, argb, 0, w);
			}
		} else {
			image.getRGB(0, 0, w, h, argb, 0, w);
		}
		WritableImage out = reuse;
		if (out == null || (int) out.getWidth() != w || (int) out.getHeight() != h)
			out = new WritableImage(w, h);
		out.getPixelWriter().setPixels(0, 0, w, h,
				PixelFormat.getIntArgbInstance(), argb, 0, w);
		return out;
	}

	/** Pure pixel copy, headless-testable (no FX toolkit needed). */
	public static int[] toArgb(BufferedImage image) {
		int w = image.getWidth();
		int h = image.getHeight();
		return image.getRGB(0, 0, w, h, null, 0, w);
	}

	/**
	 * FX-thread-confined scratch buffer (all callers run via Fx.run/doRedraw
	 * on the FX thread). Grows to the largest frame, never shrinks.
	 */
	private static int[] scratch;

	private static int[] scratch(int n) {
		if (scratch == null || scratch.length < n)
			scratch = new int[n];
		return scratch;
	}

}
