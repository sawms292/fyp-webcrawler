package org.saw.webcrawler.fxfeatures;

import com.jthemedetecor.OsThemeDetector;
import javafx.scene.Scene;

public class ThemeChecking {
    public static void applyTheme(Scene scene) {
            scene.getStylesheets().clear();
            if (isDarkMode()) {
                scene.getStylesheets().add(ThemeChecking.class.getResource("/css/dark.css").toExternalForm());
            } else {
                scene.getStylesheets().add(ThemeChecking.class.getResource("/css/light.css").toExternalForm());
            }

    }

    private static boolean isDarkMode() {
        final OsThemeDetector detector = OsThemeDetector.getDetector();
        return detector.isDark();
    }
}
