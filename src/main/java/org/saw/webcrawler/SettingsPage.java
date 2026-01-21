package org.saw.webcrawler;

import javafx.application.Platform;
import javafx.concurrent.Task;
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
import org.saw.webcrawler.fxfeatures.*;
import javafx.scene.control.TextField;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
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
    private TextArea feedbackArea;
    @FXML
    private TextField emailField;
    @FXML
    private TextArea getCodeArea;


    private final DatabaseConnection databaseConnection = new DatabaseConnection();

    /**
     * Ensure it can read write
     */
    public void DbReadWrite() {
//         Ensure this instance has URL/user/pass populated
        boolean ok = databaseConnection.checkingUser();
        if (!ok) {
            throw new IllegalStateException("Database not ready: checkingUser() returned false");
        }
    }


    /**
     * @param url unused
     * @param resourceBundle unused
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        boolean readAutoShutDown = saveSettings.loadAutoShutDown();
        onAutoShutDownBtn.setSelected(readAutoShutDown);
        offAutoShutDownBtn.setSelected(!readAutoShutDown);
        DbReadWrite();
        String userEmail = databaseConnection.fetchEmail();
        if (userEmail != null && !userEmail.isBlank()) {
            emailField.setText(userEmail);
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage email: Check success\n");
            System.out.println("SettingsPage email: Check success");
        }
    }

    public void homeClick(MouseEvent mouseEvent) throws IOException {
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        boolean wasMaximized = stage.isMaximized();
        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        Scene scene = MainMenu.getMainPageScene();
        ThemeChecking.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        if (wasMaximized) {
            Platform.runLater(() -> stage.setMaximized(true));  // re-apply on next pulse
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }

    }

    public void databaseClick(MouseEvent mouseEvent) throws IOException {
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        boolean wasMaximized = stage.isMaximized();
        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        Scene scene = MainMenu.getDatabasePageScene();
        ThemeChecking.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        if (wasMaximized) {
            Platform.runLater(() -> stage.setMaximized(true));  // re-apply on next pulse
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }

    }

    public void settingsClick(MouseEvent mouseEvent) throws IOException {
        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
        boolean wasMaximized = stage.isMaximized();
        double oldWidth = stage.getWidth();
        double oldHeight = stage.getHeight();
        Scene scene = MainMenu.getSettingsPageScene();
        ThemeChecking.applyTheme(scene);
        stage.setScene(scene);
        stage.setMinWidth(900);
        stage.setMinHeight(600);
        if (wasMaximized) {
            Platform.runLater(() -> stage.setMaximized(true));  // re-apply on next pulse
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }

    }

    /**
     * @param actionEvent test the connection
     */
    public void settingsTestConnectionBtn(ActionEvent actionEvent) {
        if (httpCheck.testConnection()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connection Test");
            alert.setHeaderText("RESULT");
            alert.setContentText("Connection Test Successfully (Code MY)");
            alert.showAndWait();
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage TestConnection: Success.\n");
            System.out.println("SettingsPage TestConnection: Success.");
        } else {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Connection Test");
            alert.setHeaderText("RESULT");
            alert.setContentText("Connection Test Not Successfully (Code MY)");
            alert.showAndWait();
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage TestConnection: Failed.\n");
            System.out.println("SettingsPage TestConnection: Failed.");

        }
    }

    /**
     * @param actionEvent enable or disable shutdown features
     */
    public void autoOffCheck(ActionEvent actionEvent) {
        if (onAutoShutDownBtn.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Auto Shut Down");
            alert.setHeaderText("Check Auto Shut Down");
            alert.setContentText("Are you sure you want to auto shut down?");
            Optional<ButtonType> result = alert.showAndWait();

            if (result.get() == ButtonType.OK) {
                saveSettings.saveAutoShutDown(true);
                readInfo.logs.add(readInfo.timestamp() + "SettingsPage AutoShutDown: On.\n");
            } else {
                onAutoShutDownBtn.setSelected(false);
                offAutoShutDownBtn.setSelected(true);
                saveSettings.saveAutoShutDown(false);
                readInfo.logs.add(readInfo.timestamp() + "SettingsPage AutoShutDown: Off.\n");
            }
        } else {
            saveSettings.saveAutoShutDown(false);
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage AutoShutDown: Off.\n");
        }
    }

    /**
     * @param actionEvent Send the bug / feedback
     */
    public void settingsBugBtn(ActionEvent actionEvent) {
        Task<Void> sendBugTask = new Task<>() {
            @Override
            protected Void call() throws Exception {

        ZonedDateTime currentTime = ZonedDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z VV");
        String formattedDate = currentTime.format(formatter);
        String generatedDate = "<h3>This report generate by " + formattedDate + "</h3>\n";
        String feedback = (feedbackArea != null) ? feedbackArea.getText() : "No feedback provided!";
        String feedbackHtml = "<br><hr>" + "<h3>User Feedback: </h3><p>" + feedback + "</p>" + "<hr><br>";
        String messages = generatedDate + feedbackHtml;

        if (readiInfo.sendMessage(messages)) {
            Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Send Bug");
            alert.setHeaderText("Send Bug");
            alert.setContentText("Send log to check bug Successfully");
            alert.showAndWait();
                if (feedbackArea != null) {
                    feedbackArea.clear();
                }
            });
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage SendBug: Success.\n");
            System.out.println("SettingsPage SendBug: Success.");
        }else{
            Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Send Bug");
            alert.setHeaderText("Send Bug");
            alert.setContentText("Send log to check bug Failed");
            alert.showAndWait();
            });
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage SendBug: Failed.\n");
            System.out.println("SettingsPage SendBug: Failed.");
        }

        return null;
    }
};
Thread sendBugThread = new Thread(sendBugTask);
        sendBugThread.setDaemon(true);
        sendBugThread.start();

    }

    /**
     * @param actionEvent get login webpage info
     */
    @FXML
    public void getLatestInfo(ActionEvent actionEvent) {
        Runnable getInfoTask = () -> {
            String user = databaseConnection.getDatabaseUser();
            String pass = databaseConnection.getDatabasePassword();
            String db = databaseConnection.getStoreDatabaseName();
            String userEmail = databaseConnection.fetchEmail();

            String latestUser = (user != null) ? user : "N/A";
            String latestPass = (pass != null) ? pass : "N/A";
            String latestDb = (db != null) ? db : "N/A";
            String latestEmail = (userEmail != null) ? userEmail : "N/A";

            String latestInfo =
                    "Below is your latest database information:\n" +
                    "Latest Database User: " + latestUser + "\n" +
                            "Latest Database Password: " + latestPass + "\n" +
                            "Latest Database Name: " + latestDb + "\n" +
                            "Latest User Email: " + latestEmail + "\n" +
                            "End of latest database information" + "\n" +
                            "WebPage url: https://crawlerdb.shenguum.com:3000/index.php?database=" + latestDb + "&username=" + latestUser + "&password=" + latestPass + "\n";
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage Get Latest Information.\n");
            Platform.runLater(() -> getCodeArea.setText(latestInfo));
        };

        Thread t = new Thread(getInfoTask);
        t.setDaemon(true);
        t.start();
    }


    /**
     * @param actionEvent auto fillup the input using get from webpages
     */
    public void webpageLogin(ActionEvent actionEvent) {
        Runnable urlTask = () -> {
            String user = databaseConnection.getDatabaseUser();
            String pass = databaseConnection.getDatabasePassword();
            String db = databaseConnection.getStoreDatabaseName();

            String url = "https://crawlerdb.shenguum.com:3000/index.php?database=" + db + "&username=" + user + "&password=" + pass;
            try{
//                https://youtu.be/1FXjK717Aes?si=wA0wEhoIjO1dx7Sn
                if(!Desktop.isDesktopSupported()){
                    readInfo.logs.add(readInfo.timestamp() + "SettingsPage webpageLogin Error: Desktop is not supported. Cannot open webpage.\n");
                    System.out.println("SettingsPage webpageLogin Error: Desktop is not supported. Cannot open webpage.");
                    return;
                }
                Desktop desktop = Desktop.getDesktop();
                desktop.browse(java.net.URI.create(url));
                readInfo.logs.add(readInfo.timestamp() + "SettingsPage webpageLogin : Desktop is supported. Open webpage.\n");
                System.out.println("SettingsPage webpageLogin : Desktop is supported. Open webpage.");

            }catch (Exception e){
                readInfo.logs.add(readInfo.timestamp() + "SettingsPage webpageLogin Error: " + e.getMessage() + "\n");
                System.out.println("SettingsPage webpageLogin Error: " + e.getMessage());
            }
        };
        Thread webpageLoginThread = new Thread(urlTask);
        webpageLoginThread.setDaemon(true);
        webpageLoginThread.start();
    }

    /**
     * @param actionEvent let user can login webpage using email
     */
    public void saveEmail(ActionEvent actionEvent) {
        String newEmail = emailField.getText().trim();

        if (!InputCheck.emailValidInput(newEmail)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Input Required");
                alert.setHeaderText("Notification");
                alert.setContentText("New email must be inserted!");
                alert.showAndWait();
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail Error: New email is empty\n");
            System.out.println("SettingsPage saveEmail Error: New email is empty");
            return;
        }

        String currentEmail = databaseConnection.fetchEmail(); // may return null
        if (currentEmail != null && currentEmail.equalsIgnoreCase(newEmail)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("No Changes");
            alert.setHeaderText("Notification");
            alert.setContentText("This email is already saved. No update needed.");
            alert.showAndWait();
            readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail: skipped (same email)\n");
            System.out.println("SettingsPage saveEmail: skipped (same email)");
            return;
        }

        if (!(newEmail.contains("gmail.com") ||
                newEmail.contains("hotmail.com") ||
                newEmail.contains("outlook.com") ||
                newEmail.contains("yahoo.com") ||
                newEmail.contains("icloud.com"))) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Verify Email domain");
            alert.setHeaderText("Notification");
            alert.setContentText("Only gmail.com, hotmail.com, outlook.com, yahoo.com, icloud.com are allowed!");
            alert.showAndWait();
            return;
        }

        Task<Void> saveEmailTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try {
                    final String ROOT_DATABASE_URL = ""; //Your DB URL
                    final String ROOT_USER = ""; //Your DB User
                    final String ROOT_PASSWORD = ""; //Your DB Password

                    String user = databaseConnection.getDatabaseUser();
                    String pass = databaseConnection.getDatabasePassword();
                    if ((user == null || user.isBlank()) && (pass == null || pass.isBlank())){
                        readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail Error: user and pass is empty\n");
                        System.out.println("SettingsPage saveEmail Error: user and pass is empty");
                        throw new IllegalStateException("Database user or pass is empty.");

                    }

                    Connection con = DriverManager.getConnection(ROOT_DATABASE_URL, ROOT_USER, ROOT_PASSWORD);
                    String sql = "UPDATE available SET email = ? WHERE username = ? AND password = ?";
                    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                        pstmt.setString(1, newEmail);
                        pstmt.setString(2, user);
                        pstmt.setString(3, pass);

                        int rowsUpdated = pstmt.executeUpdate();

                        if (rowsUpdated > 0) {
                            Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Input Successfully");
                            alert.setHeaderText("Notification");
                            alert.setContentText("New email has been updated!");
                            alert.showAndWait();
                            });
                            readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail Success: If statement\n");
                            System.out.println("SettingsPage saveEmail Success: If statement");
                        } else {
                            Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.WARNING);
                            alert.setTitle("Input Failed");
                            alert.setHeaderText("Notification");
                            alert.setContentText("New email not successfully updated!");
                            alert.showAndWait();
                            });
                            readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail Error: Else statement\n");
                            System.out.println("SettingsPage saveEmail Error: Else statement");
                        }
                    } catch (Exception e) {
                        Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("Error");
                        alert.setHeaderText("Notification");
                        alert.setContentText("updated email error! Try to send feedback to author");
                        alert.showAndWait();
                        });
                        readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail Error: " + e.getMessage() + "\n");
                        System.out.println("SettingsPage saveEmail Error: " + e.getMessage());
                    }

                } catch (SQLException e) {
                    readInfo.logs.add(readInfo.timestamp() + "SettingsPage saveEmail Error: " + e.getMessage() + "\n");
                    System.out.println("SettingsPage saveEmail Error: " + e.getMessage());
                }
                return null;
            }
        };
        Thread saveEmailThread = new Thread(saveEmailTask);
        saveEmailThread.setDaemon(true);
        saveEmailThread.start();
    }

}