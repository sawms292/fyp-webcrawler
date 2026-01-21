package org.saw.webcrawler;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.saw.webcrawler.fxfeatures.ThemeChecking;

import java.io.IOException;

/**
 * Main entry point for the Web Crawler JavaFX application
 */
public class MainMenu extends Application {
    private static Scene MainPageScene;
    private static Scene DatabasePageScene;
    private static Scene SettingsPageScene;

    public static Scene getMainPageScene() {
        return MainPageScene;
    }

    public static Scene getDatabasePageScene() {
        return DatabasePageScene;
    }

    public static Scene getSettingsPageScene() {
        return SettingsPageScene;
    }

    /**
     * @param stage the primary stage for this application
     * @throws IOException if an FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        MainPageScene = new Scene(new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/MainPage.fxml")).load());
        DatabasePageScene = new Scene(new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/DatabasePage.fxml")).load());
        SettingsPageScene = new Scene(new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/SettingsPage.fxml")).load());
        stage.setTitle("Web Crawler");
        stage.getIcons().add(new javafx.scene.image.Image(getClass().getResourceAsStream("/icon/favicon-32x32.png")));
        boolean wasMaximized = stage.isMaximized();
        stage.setScene(MainPageScene);
        stage.setMaximized(wasMaximized);
        stage.show();
        ThemeChecking.applyTheme(MainPageScene);
    }

    /**
     *  Launches the JavaFX application
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        launch();
    }
}
