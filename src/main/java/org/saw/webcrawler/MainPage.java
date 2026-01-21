package org.saw.webcrawler;

import com.google.common.net.InternetDomainName;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import org.saw.webcrawler.corefeatures.*;
import org.saw.webcrawler.fxfeatures.*;

import java.io.IOException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;

/**
 * The main pages that run the crawler
 */
public class MainPage implements Initializable {
    public AnchorPane rootPane;
    private InputCheck crawler;
    private HttpCheck httpCheck;
    private AutoShutDown autoShutDown;
    private KeyMatch keyMatch;
    private HashSet<String> crawledUrls;
    private Queue<String> queue;
    private volatile boolean stopRequested = false;
    private Thread crawlThread;
    private Task<Void> crawlTask;
    private ObservableList<DisplayCrawlerResult> DisplayRecords;
    private DbReadWrite dbReadWrite;
    private final DatabaseConnection databaseConnection = new DatabaseConnection();
    private Timestamp currentRunStart;
    private int pagesSavedThisRun = 0;

    //    getter
    public HashSet<String> getCrawledUrls() {
        return crawledUrls;
    }

    @FXML
    private ImageView home;
    @FXML
    private ImageView database;
    @FXML
    private ImageView settings;
    @FXML
    private TextField keywordFld;
    @FXML
    private TextField urlFld;
    @FXML
    private Label statusLbl;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Button stopBtn;
    @FXML
    private Button clearBtn;
    @FXML
    private Button startBtn;
    @FXML
    private Spinner<Integer> numberSp;
    @FXML
    private TableView<DisplayCrawlerResult> dbTable;
    @FXML
    private TableColumn<DisplayCrawlerResult, String> colDate;
    @FXML
    private TableColumn<DisplayCrawlerResult, String> colKeyword;
    @FXML
    private TableColumn<DisplayCrawlerResult, Number> colNo;
    @FXML
    private TableColumn<DisplayCrawlerResult, String> colLink;
    @FXML
    private TableColumn<DisplayCrawlerResult, String> colTimes;

    /**
     * @param url unused
     * @param resourceBundle unused
     */
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1);
        valueFactory.setConverter(new javafx.util.StringConverter<Integer>() {
            @Override
            public String toString(Integer value) {
                return value == null ? "" : value.toString();
            }

            @Override
            public Integer fromString(String text) {
                if (text == null || text.trim().isEmpty()) {
                    return valueFactory.getValue();
                }

                try {
                    int v = Integer.parseInt(text.trim());

                    if (v <= 0) {
                        return valueFactory.getValue();
                    }

                    return v;

                } catch (NumberFormatException e) {
                    return valueFactory.getValue();
                }
            }
        });

        numberSp.setValueFactory(valueFactory);
        numberSp.setEditable(true);
        crawler = new InputCheck();
        crawledUrls = new HashSet<>();
        httpCheck = new HttpCheck();
        autoShutDown = new AutoShutDown();
        keyMatch = new KeyMatch();
        queue = new LinkedList<>();

        DisplayRecords = FXCollections.observableArrayList();
        colKeyword.setCellValueFactory(new PropertyValueFactory<>("displayKeyword"));
        colTimes.setCellValueFactory(new PropertyValueFactory<>("displayTimes"));
        colLink.setCellValueFactory(new PropertyValueFactory<>("displayLink"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("displayCrawlTime"));
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
        dbTable.setItems(DisplayRecords);
        dbTable.getColumns().forEach(col -> col.setResizable(false));
        dbTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        colNo.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.05));
        colKeyword.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.20));
        colTimes.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.10));
        colLink.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.45));
        colDate.prefWidthProperty().bind(dbTable.widthProperty().multiply(0.20));

        disableAllInputs(true);

        Task<Void> monitorTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int retry = 0;

                setStatus("Checking connection…", "orange"); // STATUS A1
                updateProgress(-1, 1);

                // Database readiness loop
                boolean checkDatabase = false;
                while (!checkDatabase) {
                    retry++;
                    try {
                        setStatus("Checking database… (try " + retry + ")", "orange");
//                        DatabaseConnection dbConn = new DatabaseConnection();
//                        checkDatabase = dbConn.checkingUser();
                        checkDatabase = databaseConnection.checkingUser();
                        if (checkDatabase) {
                            setStatus("Database ready", "green");
                            updateProgress(1, 1);

                            try {
                                dbReadWrite = new DbReadWrite();
                            } catch (Exception e) {
                                setStatus("Database init failed", "red");
                                return null;
                            }
                            disableAllInputs(false);

                        } else {
                            setStatus("Database not ready… retrying (" + retry + ")", "orange");
                            updateProgress(0.5, 1);
                            Thread.sleep(1000);
                        }
                    } catch (Exception e) {
                        setStatus("Database error… retrying (" + retry + ")", "red");
                        Thread.sleep(1000);
                    }
                }
                return null;
            }
        };
        progressBar.progressProperty().bind(monitorTask.progressProperty());
        Thread t = new Thread(monitorTask, "Tunnel-Monitor");
        t.setDaemon(true);
        t.start();
    }

    /**
     * @param keyword keyword filter used during the current run
     * @param url base URL filter used during the current run
     */
    @FXML
    private void readDatabase(String keyword, String url) {
        loadData(keyword, url, currentRunStart);
    }

    /**
     * @param keyword keyword filter; may be empty for no filter
     * @param url URL filter applied to {@code link_default}; may be empty for no filter
     * @param runStart optional earliest crawl time; may be null to include all rows
     */
    private void loadData(String keyword, String url, Timestamp runStart) {
        DisplayRecords.clear();

        try (ResultSet rs = dbReadWrite.readDatabase(keyword, url, runStart)) {
            while (rs != null && rs.next()) {
                DisplayRecords.add(new DisplayCrawlerResult(
                        rs.getString("keyword"),
                        rs.getString("times"),
                        rs.getString("link"),
                        rs.getTimestamp("crawl_time").toString()
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param disable if db have issues will try to prevent it using
     */
    private void disableAllInputs(boolean disable) {
        keywordFld.setDisable(disable);
        urlFld.setDisable(disable);
        numberSp.setDisable(disable);
        startBtn.setDisable(disable);
        clearBtn.setDisable(disable);
        dbTable.setDisable(disable);
        home.setDisable(disable);
        database.setDisable(disable);
        settings.setDisable(disable);
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
            Platform.runLater(() -> stage.setMaximized(true));
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
            Platform.runLater(() -> stage.setMaximized(true));
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
            Platform.runLater(() -> stage.setMaximized(true));
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }

    }

    /**
     * @param num input string
     * @return if true is a positive integer, else is negative and can't run
     */
    public boolean isInteger(String num) {
       Integer numChecking;
        try{
           numChecking = Integer.parseInt(String.valueOf(num));
            if(numChecking <= 0) {return false;}
            if(numChecking == null){return false;}
            return true;
        } catch (NumberFormatException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Shows a warning alert to inform the user that the crawl number is invalid
     */
    private void showInvalidNumberAlert() {
        Platform.runLater(() -> {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Number");
        alert.setHeaderText("Notification");
        alert.setContentText("Crawling number must be a positive value (greater than zero)!");
        alert.showAndWait();
        });
    }


    /**
     * @param actionEvent start crawler web pages
     */
    public void startCrawlingBtn(ActionEvent actionEvent) {
        String keyword = keywordFld.getText();
        String protocolUrl = urlFld.getText();
        String url;
        if (protocolUrl.startsWith("http://") || protocolUrl.startsWith("https://")) {
            url = protocolUrl;
        } else {
            url = "http://" + protocolUrl;
        }

        String rawValue = numberSp.getEditor().getText();

        if(isInteger(rawValue) == false) {
            showInvalidNumberAlert();
            return;
        }
        int number = Integer.parseInt(rawValue.trim());

        String savedEmail = databaseConnection.fetchEmail();
        // optional: show a tiny confirmation
        if (savedEmail == null || savedEmail.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Input Required");
                alert.setHeaderText("Notification");
                alert.setContentText("Please go to Settings Page insert your email and click saved email button!");
                alert.showAndWait();
                return;
        }

        if(url.trim().contains(" ")) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Invalid URL");
            alert.setHeaderText("Notification");
            alert.setContentText("URL should not contain spaces and only one URL!");
            alert.showAndWait();
            return;
        }

        if (!crawler.isValidInput(keyword, url, number)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Input Required");
            alert.setHeaderText("Notification");
            alert.setContentText("Crawling number, URL and Keyword Input Required!");
            alert.showAndWait();
            return;
        }

        if (crawler.isValidInput(keyword, url, number)) {
            try {
                boolean checkUrl = httpCheck.checkAccess(url);

                if (checkUrl) {
                    URL uri = new URL(url);
                    String host = uri.getHost();
                    InternetDomainName domainName = InternetDomainName.from(host).topPrivateDomain();
                    HTagRemove.baseUrl = domainName.toString();
                    String urlDomain = domainName.toString();
                    stopBtn.setDisable(false);
                    clearBtn.setDisable(true);
                    startBtn.setDisable(true);
                    progressBar.progressProperty().unbind();
                    setStatus("Status: Crawling", "blue");

                    stopRequested = false;
                    currentRunStart = new Timestamp(System.currentTimeMillis());
                    pagesSavedThisRun = 0;

                    crawlTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            try {
                                updateProgress(0, 100);
                                setStatus("Starting keyword analysis…", "blue");
                                queue.clear();
                                crawledUrls.clear();
                                queue.add(url);

                                int limit = number;
                                int count = 0;

                                while (!queue.isEmpty() && count < limit && !isCancelled() && !stopRequested) {
                                    String currentUrl = queue.poll();
                                    if (crawledUrls.contains(currentUrl)) {
                                        continue;
                                    }
                                    crawledUrls.add(currentUrl);
                                    setStatus("Crawling: " + currentUrl, "blue");


                                    KeyMatch.KeywordResult result = keyMatch.checkWebsiteForKeyword(currentUrl, keyword);

                                    setStatus("Saving to database…", "yellow");
                                    dbReadWrite.saveDatabase(
                                            keyword,
                                            result.getTimesJson(),
                                            currentUrl,
                                            urlDomain,
                                            url,
                                            result.isFound());
                                    pagesSavedThisRun++;

                                    for (String newLink : HTagRemove.getFoundLinks()) {
                                        if (!crawledUrls.contains(newLink)) {
                                            queue.add(newLink);
                                        }
                                    }

                                    count++;
                                    updateProgress(count, limit);

                                    setStatus("Saved. Discovering new links…", "yellow");

                                    try {
                                        Thread.sleep(100);
                                    } catch (InterruptedException e) {
                                        if (isCancelled() || stopRequested) {
                                            progressBar.progressProperty().unbind();
                                            setStatus("Crawling Stopped", "red");
                                            break;
                                        }
                                    }
                                }
                            return null;
                            } finally {
                                if (!isCancelled() && !stopRequested) {
                                    HTagRemove.getFoundLinks().clear();
                                    HTagRemove.closeSharedDriver();
                                }
                            }
                        }

                        @Override
                        protected void succeeded() {
                            super.succeeded();
                            setStatus("✓ Keyword Analysis Complete", "green");
                            stopBtn.setDisable(true);
                            clearBtn.setDisable(false);
                            startBtn.setDisable(false);
                            readDatabase(keyword, url);
                            HTagRemove.getFoundLinks().clear();
                            HTagRemove.closeSharedDriver();


                            if (autoShutDown.isAutoShutdownEnabled()) {
                                new Thread(() -> {
                                    try {                                        Thread.sleep(3000);
                                        autoShutDown.shutdown();
                                    } catch (InterruptedException | IOException e) {
                                        e.printStackTrace();
                                    }
                                }).start();
                            }
                        }

                        @Override
                        protected void failed() {
                            super.failed();
                            setStatus("Crawling Failed", "red");
                        }

                        @Override
                        protected void cancelled() {
                            super.cancelled();
                            setStatus("Crawling Cancelled", "red");

                            stopBtn.setDisable(true);
                            startBtn.setDisable(false);
                            clearBtn.setDisable(false);

                            if (pagesSavedThisRun > 0) {
                                readDatabase(keyword, url);
                            } else {
                                DisplayRecords.clear();
                            }

                            HTagRemove.getFoundLinks().clear();
                            HTagRemove.closeSharedDriver();
                        }
                    };

                    progressBar.progressProperty().bind(crawlTask.progressProperty());

                    crawlThread = new Thread(crawlTask, "Crawler-Thread");
                    crawlThread.setDaemon(true);
                    crawlThread.start();

                } else {
                    String convertCheckUrl = checkUrl ? "True" : "False";
                    setStatus("URL access failed", "red");
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("URL/Database Problem");
                    alert.setHeaderText("Notification");
                    alert.setContentText("Status For URL Access: " + convertCheckUrl);
                    alert.showAndWait();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * @param actionEvent to manually stop the crawler
     */
    public void stopCrawlingBtn(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Stop Crawling");
        alert.setHeaderText("Notification");
        alert.setContentText("Progress will be stop it and cant resume!\nAre you sure you want to stop Crawling?");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == ButtonType.OK) {
            stopRequested = true;
            if (crawlTask != null && crawlTask.isRunning()) {
                crawlTask.cancel();
            }
            if (crawlThread != null && crawlThread.isAlive()) {
                crawlThread.interrupt();
            }
            clearBtn.setDisable(false);
            startBtn.setDisable(false);
            stopBtn.setDisable(true);
//            clearTextBtn(actionEvent);
//            progressBar.progressProperty().unbind();
            setStatus("Crawling Stopped", "red");
        }
    }

    /**
     * @param actionEvent clear the main page input, url, results
     */
    public void clearTextBtn(ActionEvent actionEvent) {
        stopRequested = true;
        if (crawlTask != null && crawlTask.isRunning()) {
            crawlTask.cancel();
        }
        if (crawlThread != null && crawlThread.isAlive()) {
            crawlThread.interrupt();
        }
        crawlThread = null;
        crawlTask = null;
        queue.clear();
        crawledUrls.clear();
        DisplayRecords.clear();
        currentRunStart = null;
        pagesSavedThisRun = 0;
        keywordFld.clear();
        urlFld.clear();
        setStatus("Status: Clear", "red");
        new Thread(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            Platform.runLater(() -> {
                if ("Status: Clear".equals(statusLbl.getText())) {
                    setStatus("Status: Waiting for input", "green");
                }
            });
        }).start();

        numberSp.getValueFactory().setValue(1);
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1);
        numberSp.setValueFactory(valueFactory);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0);
    }

    /**
     * @param text  status message to display
     * @param color difference color difference situation
     */
    private void setStatus(String text, String color) {
        Platform.runLater(() -> {
            statusLbl.textProperty().unbind();
            statusLbl.setText(text);
            statusLbl.setStyle("-fx-text-fill: " + color + ";");
            progressBar.setStyle("-fx-accent: " + color + ";");
        });
    }

}
