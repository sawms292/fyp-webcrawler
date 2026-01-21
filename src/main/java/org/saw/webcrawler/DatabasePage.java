package org.saw.webcrawler;

import javafx.application.Platform;
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
import org.saw.webcrawler.corefeatures.DbReadWrite;
import org.saw.webcrawler.fxfeatures.ThemeChecking;
import org.saw.webcrawler.corefeatures.SearchAndDisplayDatabase;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

/**
 * database page for history checking
 */
public class DatabasePage implements Initializable {
    /**
     *  Observable list
     */
    private ObservableList<SearchAndDisplayDatabase> SearchRecords;
    /**
     * Database service used to read crawler data.
     */
    private DbReadWrite dbReadWrite = new DbReadWrite();

    // all about fx for data column(tables), search field, combobox, and button
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
    private TableColumn<SearchAndDisplayDatabase, String> colTimes;

    @FXML
    private TextField dbSearchFld;
    @FXML
    private Button dbClearBtn;
    @FXML
    private ComboBox<String> monthCb;


    /**
     * List of month names used for month filtering
     */
    ObservableList<String> months = FXCollections.observableArrayList(
            "", "January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"
    );

    /**
     * @param url the location used to resolve relative paths for the root object (unused)
     * @param resourceBundle resources used to localize the root object (unused)
     */
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
                setText(empty ? null : String.valueOf(getIndex() + 1));
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

    /**
     * @param keyword keyword filter (partial match); use empty string for no filter
     * @param month month name filter (e.g., {@code January}); use empty string for no filter
     */
    private void loadData(String keyword, String month) {
        // Run DB query in background
        new Thread(() -> {
            ObservableList<SearchAndDisplayDatabase> tempRecords = FXCollections.observableArrayList();

            try (ResultSet rs = dbReadWrite.searchDatabase(keyword, month)) {
                while (rs != null && rs.next()) {
                    tempRecords.add(new SearchAndDisplayDatabase(
                            rs.getString("keyword"),
                            rs.getString("times"),
                            rs.getString("link"),
                            rs.getBoolean("found") ? "YES" : "NO",
                            rs.getTimestamp("crawl_time").toString()
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }

            // safety update TableView
            javafx.application.Platform.runLater(() -> {
                SearchRecords.clear();
                SearchRecords.addAll(tempRecords);
            });
        }).start();
    }

    /**
     * @param mouseEvent navigation click event
     * @throws IOException  if the target scene cannot be loaded
     */
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

    /**
     * @param mouseEvent  navigation click event
     * @throws IOException  if the target scene cannot be loaded
     */
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

    /**
     * @param mouseEvent  navigation click event
     * @throws IOException if the target scene cannot be loaded
     */
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
     * @param actionEvent clear + refresh button
     */
    public void clearFilters(ActionEvent actionEvent) {
        dbSearchFld.clear();
        monthCb.getSelectionModel().clearSelection();
        loadData("", "");
    }
}
