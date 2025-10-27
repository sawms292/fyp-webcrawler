package org.saw.webcrawler;

import com.jthemedetecor.OsThemeDetector;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.saw.webcrawler.fxfeatures.AutoShutDown;
import org.saw.webcrawler.fxfeatures.CloudflaredConnection;
import org.saw.webcrawler.fxfeatures.ThemeChecking;


import java.io.IOException;

public class MainMenu extends Application {
    private static Scene MainPageScene;
    private static Scene DatabasePageScene;
    private static Scene SettingsPageScene;

    public static Scene getMainPageScene(){
        return MainPageScene;
    }

    public static Scene getDatabasePageScene(){
        return DatabasePageScene;
    }

    public static Scene getSettingsPageScene(){
        return SettingsPageScene;
    }


    @Override
    public void start(Stage stage) throws IOException, InterruptedException {
        MainPageScene = new Scene(new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/MainPage.fxml")).load());
        DatabasePageScene =new Scene(new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/DatabasePage.fxml")).load());
        SettingsPageScene =new Scene(new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/SettingsPage.fxml")).load());
//        FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource("MainPage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
        ThemeChecking.applyTheme(MainPageScene);
        boolean wasMaximized = stage.isMaximized();
        stage.setScene(MainPageScene);
        stage.setMaximized(wasMaximized);
        stage.show();

        if(!CloudflaredConnection.tunnelExists()){
            CloudflaredConnection.getTunnelDirectoryAndRun();
        }

//        https://www.youtube.com/watch?v=TdqI-hbuWx4
        stage.setOnCloseRequest(event -> {
            new Thread(() -> {
                CloudflaredConnection.turnOffProcess();
                System.out.println("MainMenu file: Closing");
            }, "Tunnel-Shutdown-Thread").start();
        });
//         JVM shutdown hook (handles Ctrl+C, system shutdown, or crash)
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            CloudflaredConnection.turnOffProcess();
            System.out.println("MainMenu file: Closing (via JVM shutdown hook)");
        }, "Tunnel-Shutdown-Hook"));
    }

    public static void main(String[] args) {
        launch();
    }
}