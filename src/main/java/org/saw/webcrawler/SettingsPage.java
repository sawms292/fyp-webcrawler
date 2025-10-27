package org.saw.webcrawler;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextArea;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.saw.webcrawler.fxfeatures.HttpCheck;
import org.saw.webcrawler.fxfeatures.SaveSettings;
import org.saw.webcrawler.fxfeatures.ThemeChecking;
import org.saw.webcrawler.fxfeatures.readInfo;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;

public class SettingsPage implements Initializable {
    private Scene scene;
    private Stage stage;
    private FXMLLoader fxmlLoader;
    private HttpCheck httpCheck = new HttpCheck();
    private readInfo readiInfo = new readInfo();
    private SaveSettings saveSettings = new SaveSettings();
    @FXML
    private RadioButton onAutoShutDownBtn;
    @FXML
    private RadioButton offAutoShutDownBtn;
    @FXML
    private TextArea settingsArea;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        boolean readAutoShutDown = saveSettings.loadAutoShutDown();
        onAutoShutDownBtn.setSelected(readAutoShutDown);
        offAutoShutDownBtn.setSelected(!readAutoShutDown);
        read();
    }

    public void homeClick(MouseEvent mouseEvent) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/MainPage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        ThemeChecking.applyTheme(scene);
//        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
//        stage.setScene(MainMenu.getMainPageScene());
        boolean wasMaximized = stage.isMaximized();
        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        Scene scene = MainMenu.getMainPageScene();
        ThemeChecking.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        if (wasMaximized) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }
//        stage.show();
    }

    public void databaseClick(MouseEvent mouseEvent) throws IOException {
//        FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/DatabasePage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        ThemeChecking.applyTheme(scene);
//        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
//        stage.setScene(MainMenu.getDatabasePageScene());
        boolean wasMaximized = stage.isMaximized();
        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        Scene scene = MainMenu.getDatabasePageScene();
        ThemeChecking.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        if (wasMaximized) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }
//        stage.show();
    }

    public void settingsClick(MouseEvent mouseEvent) throws IOException {
//        fxmlLoader = new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/SettingsPage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        ThemeChecking.applyTheme(scene);
//        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
//        stage.setScene(MainMenu.getSettingsPageScene());
        boolean wasMaximized = stage.isMaximized();
        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        Scene scene = MainMenu.getSettingsPageScene();
        ThemeChecking.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        if (wasMaximized) {
            stage.setMaximized(true);
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }
//        stage.show();
    }

    public void settingsTestConnectionBtn(ActionEvent actionEvent) {
        if (httpCheck.testConnection()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connection Test");
            alert.setHeaderText("RESULT");
            alert.setContentText("Connection Test Successfully (Code MY)");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connection Test");
            alert.setHeaderText("RESULT");
            alert.setContentText("Connection Test Not Successfully (Code MY)");
            alert.showAndWait();

        }
    }

    public void autoOffCheck(ActionEvent actionEvent) {
        if (onAutoShutDownBtn.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Auto Shut Down");
            alert.setHeaderText("Check Auto Shut Down");
            alert.setContentText("Are you sure you want to auto shut down?");
            Optional<ButtonType> result = alert.showAndWait();

            if (result.get() == ButtonType.OK) {
                saveSettings.saveAutoShutDown(true);
            } else {
                onAutoShutDownBtn.setSelected(false);
                offAutoShutDownBtn.setSelected(true);
                saveSettings.saveAutoShutDown(false);
            }
        } else {
            saveSettings.saveAutoShutDown(false);
        }

        read();
    }


    public void read() {
        if (onAutoShutDownBtn.isSelected()) {
            settingsArea.setText("Auto ShutDown has been selected");
        } else {
            settingsArea.setText("Auto ShutDown has unselected");
        }
    }

    public void settingsBugBtn(ActionEvent actionEvent) {
        ZonedDateTime currentTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z VV");
        String formattedDate = currentTime.format(formatter);
        String generatedDate = "<h3>This report generate by " + formattedDate + "</h3>\n";
        if (readiInfo.sendMessage(generatedDate) == true) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Send Bug");
            alert.setHeaderText("Send Bug");
            alert.setContentText("Send log to check bug Successfully");
            alert.showAndWait();
        }else{
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Send Bug");
            alert.setHeaderText("Send Bug");
            alert.setContentText("Send log to check bug Failed");
            alert.showAndWait();
        }

    }
}