package biogenesis;

////////////////////////////////////////////////////////
//Bare Bones Browser Launch                          //
//Version 1.5                                        //
//December 10, 2005                                  //
//Supports: Mac OS X, GNU/Linux, Unix, Windows XP    //
//Example Usage:                                     //
// String url = "http://www.centerkey.com/";       //
// BareBonesBrowserLaunch.openURL(url);            //
//Public Domain Software -- Free to Use as You Like  //
/////////////////////////////////////////////////////////

import java.awt.Desktop;
import java.net.URI;

public class BareBonesBrowserLaunch {

	private static final String errMsg = "Error attempting to launch web browser"; //$NON-NLS-1$

	/**
	 * Opens a URL in the system browser via {@link Desktop}. The 2005
	 * fallback ladder (com.apple.eio, rundll32, which-loop) was removed:
	 * Desktop browsing has been reliable on all platforms since Java 6.
	 */
	public static void openURL(String url) {
		try {
			if (Desktop.isDesktopSupported()) {
				Desktop desktop = Desktop.getDesktop();
				if (desktop.isSupported(Desktop.Action.BROWSE)) {
					desktop.browse(new URI(url));
					return;
				}
			}
			throw new UnsupportedOperationException("Desktop browsing is not supported"); //$NON-NLS-1$
		} catch (Exception e) {
			Fx.error(null, errMsg
					+ ":\n" + e.getLocalizedMessage()); //$NON-NLS-1$
		}
	}

}