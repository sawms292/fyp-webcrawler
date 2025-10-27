package org.saw.webcrawler;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.saw.webcrawler.fxfeatures.DatabaseConnection;
import org.saw.webcrawler.corefeatures.DbReadWrite;
import org.saw.webcrawler.fxfeatures.ThemeChecking;
import org.saw.webcrawler.corefeatures.SearchAndDisplayDatabase;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class DatabasePage implements Initializable {
    private Scene scene;
    private Stage stage;
    private FXMLLoader fxmlLoader;
    private ObservableList<SearchAndDisplayDatabase> SearchRecords;
    private DatabaseConnection databaseConnection = new DatabaseConnection();
    private DbReadWrite dbReadWrite = new DbReadWrite();
    @FXML
    private TableView<SearchAndDisplayDatabase> dbTable;

    @FXML
    private TableColumn<SearchAndDisplayDatabase, String> colDate;

    @FXML
    private TableColumn<SearchAndDisplayDatabase, String> colFound;

    @FXML
    private TableColumn<SearchAndDisplayDatabase, String> colKeyword;

    @FXML
    private TableColumn<SearchAndDisplayDatabase, Number> colNo;

    @FXML
    private TableColumn<SearchAndDisplayDatabase, String> colLink;

    @FXML
    private TableColumn<SearchAndDisplayDatabase, Number> colTimes;
    @FXML
    private TextField dbSearchFld;
    @FXML
    private Button dbSearchBtn;
    @FXML
    private ComboBox<String> monthCb;
    @FXML
    private TextArea dbResultArea;
    ObservableList<String> months = FXCollections.observableArrayList("","January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December");

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        monthCb.setItems(months);
        SearchRecords = FXCollections.observableArrayList();
        colKeyword.setCellValueFactory(new PropertyValueFactory<>("searchKeyword"));
        colTimes.setCellValueFactory(new PropertyValueFactory<>("searchTimes"));
        colLink.setCellValueFactory(new PropertyValueFactory<>("searchLink"));
        colFound.setCellValueFactory(new PropertyValueFactory<>("searchFound"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("searchCrawlTime"));
        colNo.setCellFactory(no -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                } else {
                    setText(String.valueOf(getIndex() + 1));
                }
            }
        });

        dbTable.setItems(SearchRecords);
        dbTable.getColumns().forEach(col -> col.setResizable(false));
        dbTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNo.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.05));
        colKeyword.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.20));
        colTimes.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.10));
        colLink.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.35));
        colFound.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.10));
        colDate.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.20));

        dbSearchFld.textProperty().addListener((obs, oldText, newText) -> {
            String keyword = newText.trim();
            String month = monthCb.getValue() == null ? "" : monthCb.getValue();
            loadData(keyword, month);
        });

        monthCb.setOnAction(event -> {
            String keyword = dbSearchFld.getText().trim();
            String month = monthCb.getValue() == null ? "" : monthCb.getValue();
            loadData(keyword, month);
        });

        loadData("", "");
    }

    @FXML
    private void searchDatabase(ActionEvent event) {
        String keyword = dbSearchFld.getText().trim();
        String month = monthCb.getValue() == null ? "" : monthCb.getValue();
        loadData(keyword, month);
    }

private void loadData(String keyword, String month) {
    // Run DB query in background
    new Thread(() -> {
        ObservableList<SearchAndDisplayDatabase> tempRecords = FXCollections.observableArrayList();

        try (ResultSet rs = dbReadWrite.searchDatabase(keyword, month)) {
            while (rs != null && rs.next()) {
                tempRecords.add(new SearchAndDisplayDatabase(
                        rs.getString("keyword"),
                        rs.getInt("times"),
                        rs.getString("link"),
                        rs.getBoolean("found") ? "YES" : "NO",
                        rs.getTimestamp("crawl_time").toString()
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Update TableView safely on JavaFX thread
        javafx.application.Platform.runLater(() -> {
            SearchRecords.clear();
            SearchRecords.addAll(tempRecords);
        });
    }).start();
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
//        FXMLLoader fxmlLoader = new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/SettingsPage.fxml"));
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




}

