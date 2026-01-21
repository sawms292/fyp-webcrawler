package org.saw.webcrawler.fxfeatures;

import com.jthemedetecor.OsThemeDetector;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Apply and manage theme changes based on OS theme settings
 * Light and Dark mode only
 */
public class ThemeChecking {

    /**
     * Applies the appropriate theme to the given JavaFX scene based on the OS theme settings
     * @param scene the JavaFX scene to which the theme will be applied
     */
    public static void applyTheme(Scene scene) {
        //Os them detector instance
        OsThemeDetector detector = OsThemeDetector.getDetector();
            //Clear any themes change event occurred
            scene.getStylesheets().clear();
            //Apply dark theme CSS
            if (isDarkMode()) {
                scene.getStylesheets().add(ThemeChecking.class.getResource("/css/dark.css").toExternalForm());
                readInfo.logs.add(readInfo.timestamp() + "ThemeChecking init: Dark\n");
            } else {
            //Apply light themeCSS
                scene.getStylesheets().add(ThemeChecking.class.getResource("/css/light.css").toExternalForm());
                readInfo.logs.add(readInfo.timestamp() + "ThemeChecking init: White\n");
            }

        /*
         * Update the window title bar appearance if the scene
         * is already attached to a stage.
         */
        Stage stage = (Stage) scene.getWindow();
        if (stage != null) WindowsTitleBar.apply(stage, isDarkMode());
        detector.registerListener(isDark ->
                // Ensure UI updates occur on the JavaFX Application Thread
                Platform.runLater(() -> {
                    //Clear any themes change event occurred
                    scene.getStylesheets().clear();
                    if (isDark) {
                        scene.getStylesheets().add(ThemeChecking.class.getResource("/css/dark.css").toExternalForm());
                        readInfo.logs.add(readInfo.timestamp() + "ThemeChecking: Dark\n");
                    } else {
                        scene.getStylesheets().add(ThemeChecking.class.getResource("/css/light.css").toExternalForm());
                        readInfo.logs.add(readInfo.timestamp() + "ThemeChecking: White\n");
                    }
                    Stage win = (Stage) scene.getWindow();
                    if (win != null) WindowsTitleBar.apply(win, isDark);
                })
        );
    }

    /**
     * Determines whether the operating system is currently using dark mode
     * @return true if the OS is in dark mode, else false
     */
    private static boolean isDarkMode() {
        final OsThemeDetector detector = OsThemeDetector.getDetector();
        return detector.isDark();
    }
}
