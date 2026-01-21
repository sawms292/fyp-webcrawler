package org.saw.webcrawler.fxfeatures;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HWND;
import com.sun.jna.ptr.IntByReference;
import javafx.stage.Stage;

/**
 * This utility uses JNA to call the Windows Desktop Window Manager (DWM)
 * API to enable or disable immersive dark mode for the window title bar
 */
public final class WindowsTitleBar {
    /**
     * Prevents instantiation of this utility class
     */
    private WindowsTitleBar() {}

    /**
     * DWM window attribute flag for enabling immersive dark mode
     */
    private static final int DWMWA_USE_IMMERSIVE_DARK_MODE = 20;

    /**
     * JNA mapping for the Windows {@code dwmapi.dll} function used to set
     * window attributes.
     */
    interface Dwmapi extends com.sun.jna.Library {
        /**
         * Singleton instance for calling functions
         */
        Dwmapi INSTANCE = Native.load("dwmapi", Dwmapi.class);

        /**
         * Sets a Desktop Window Manager attribute for a given window
         *
         * @param hwnd  native window handle
         * @param attr  attribute identifier
         * @param value attribute value
         * @param size  size of {@code value} in bytes
         * @return an HRESULT-like status code (0 indicates success)
         */
        int DwmSetWindowAttribute(HWND hwnd, int attr, IntByReference value, int size);
    }

    /**
     *  Applies Windows immersive dark mode styling to the title bar of the given stage
     * @param stage the JavaFX stage whose title bar will updated
     * @param dark true to enable dark mode, false to disable
     */
    public static void apply(Stage stage, boolean dark) {
        try {
            if (!System.getProperty("os.name").toLowerCase().contains("win")) return;

            HWND hwnd = User32.INSTANCE.FindWindow(null, stage.getTitle());
            if (hwnd == null) return;

            IntByReference on = new IntByReference(dark ? 1 : 0);
            Dwmapi.INSTANCE.DwmSetWindowAttribute(hwnd, DWMWA_USE_IMMERSIVE_DARK_MODE, on, Integer.BYTES);
            readInfo.logs.add(readInfo.timestamp() + "WindowsTitleBar: Success checking\n");
        } catch (Throwable ignore) {
            readInfo.logs.add(readInfo.timestamp() + "WindowsTitleBar: Error\n");
        }
    }
}
