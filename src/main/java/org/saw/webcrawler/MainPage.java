package org.saw.webcrawler;

import com.google.common.net.InternetDomainName;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
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
import java.util.*;

public class MainPage implements Initializable {
    public AnchorPane rootPane;
    private Scene scene;
    private Stage stage;
    private FXMLLoader fxmlLoader;
    private InputCheck crawler;
    private HttpCheck httpCheck;
    private AutoShutDown autoShutDown;
    private KeyMatch keyMatch; // Add KeyMatch for parallel keyword checking
    private HashSet<String> crawledUrls;
    private Queue<String> queue;
    private volatile boolean stopRequested = false;
    private Thread crawlThread;
    private Task<Void> crawlTask;
    private ObservableList<DisplayCrawlerResult> DisplayRecords;
    private DbReadWrite dbReadWrite = new DbReadWrite();

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
//    @FXML
//    private TextArea outputArea;
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
    private TableColumn<DisplayCrawlerResult, Number> colTimes;


    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1);
        numberSp.setValueFactory(valueFactory);
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
        loadData("", "");
        disableAllInputs(true);
        Task<Void> monitorTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                int retry = 0;
//              Wait for cloudflared tunnel
                updateMessage("Checking tunnel connection...");

//                https://www.youtube.com/watch?v=7OPvC6we5-o
                Platform.runLater(() ->{
                        statusLbl.setStyle("-fx-text-fill: orange;");
                        progressBar.lookup(".bar").setStyle("-fx-accent: orange;");
                });
                updateProgress(-1, 1);

                while (!CloudflaredConnection.tunnelExists()) {
                    retry++;
                    updateMessage(retry + " Try : Starting tunnel...");
                    Platform.runLater(() ->{
                        statusLbl.setStyle("-fx-text-fill: orange;");
                    });
                    Thread.sleep(2000);
                }

                if(CloudflaredConnection.tunnelExists()) {
                    updateMessage("Tunnel connection started");
                    updateProgress(0.5, 1);
                    Platform.runLater(() ->{
                            statusLbl.setStyle("-fx-text-fill: orange");
                            progressBar.lookup(".bar").setStyle("-fx-accent: orange;");
                    });
                    Thread.sleep(2000);
                    retry = 0;

//                    Check Database
                    boolean checkDatabase = false;
                    while (!checkDatabase) {
                        retry++;
                        try {
                            updateMessage(retry + " Try : Checking database connection & Create database...");
                            DatabaseConnection dbConn = new DatabaseConnection();
                            checkDatabase = dbConn.checkingUser();
                            if (checkDatabase) {
                                updateMessage("Database ready..");
                                updateProgress(1, 1);
                                Platform.runLater(() -> {
                                    statusLbl.setStyle("-fx-text-fill: green");
                                    progressBar.lookup(".bar").setStyle("-fx-accent: green;");
                                });
                                disableAllInputs(false);
                            } else {
                                updateMessage("Database not ready.. Try again... : " +retry);
                                updateProgress(0.5, 1);
                                Platform.runLater(() ->{
                                            statusLbl.setStyle("-fx-text-fill: orange");
                                    progressBar.lookup(".bar").setStyle("-fx-accent: orange;");
                                });
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            retry++;
                            updateMessage("Database error... Try again... : " + retry);
                            updateProgress(0.5, 1);
                            Platform.runLater(() ->{
                                statusLbl.setStyle("-fx-text-fill: red");
                                progressBar.lookup(".bar").setStyle("-fx-accent: red;");
                                    });
                            Thread.sleep(1000);
                        }
                    }
                }else{
                    updateMessage("Tunnel connection failed");
                    Platform.runLater(() ->{
                        statusLbl.setStyle("-fx-text-fill: red");
                    });
                }
                return null;
            }
        };
        statusLbl.textProperty().bind(monitorTask.messageProperty());
        progressBar.progressProperty().bind(monitorTask.progressProperty());
        Thread t = new Thread(monitorTask, "Tunnel-Monitor");
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void readDatabase(String keyword, String url) {
        loadData(keyword, url);
    }

    private void loadData(String keyword, String url) {
        DisplayRecords.clear();

        try (ResultSet rs = dbReadWrite.readDatabase(keyword, url)) {
            while (rs != null && rs.next()) {
                DisplayRecords.add(new DisplayCrawlerResult(
                        rs.getString("keyword"),
                        rs.getInt("times"),
                        rs.getString("link"),
                        rs.getTimestamp("crawl_time").toString()
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

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
//        fxmlLoader = new FXMLLoader(MainMenu.class.getResource("/org/saw/demo/MainPage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        ThemeChecking.applyTheme(scene);
//        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
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
            stage.setMaximized(true);
        } else {
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);
        }
//        stage.show();
    }

    public void databaseClick(MouseEvent mouseEvent) throws IOException {
//        fxmlLoader = new FXMLLoader(MainMenu.class.getResource("/org/saw/webcrawler/DatabasePage.fxml"));
//        Scene scene = new Scene(fxmlLoader.load());
//        ThemeChecking.applyTheme(scene);
//        Stage stage = (Stage) ((Node) mouseEvent.getSource()).getScene().getWindow();
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

    public void startCrawlingBtn(ActionEvent actionEvent) {
        String keyword = keywordFld.getText();
        String url = urlFld.getText();
        int number = (int) numberSp.getValue();

        if(!crawler.isValidInput(keyword, url, number)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Input Required");
                alert.setHeaderText("Notification");
                alert.setContentText("Crawling number, URL and Keyword Input Required!");
                alert.showAndWait();
        }

        if(!CloudflaredConnection.tunnelExists()){
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Tunnel Not Found");
            alert.setHeaderText("Tunnel Not Found");
            alert.setContentText("Tunnel Maybe still startup.. Please wait few seconds!");
        }

        if(crawler.isValidInput(keyword, url, number)) {
            try{
                boolean checkUrl = httpCheck.checkAccess(url);
//                boolean checkDatabase = new DatabaseConnection().checkingUser();

                if(checkUrl) {
                    URL uri = new URL(url);
                    String host = uri.getHost();
                    InternetDomainName domainName = InternetDomainName.from(host).topPrivateDomain();
                    HTagRemove.baseUrl = domainName.toString();
                    String urlDomain = domainName.toString();
                    stopBtn.setDisable(false);
                    clearBtn.setDisable(false);
                    startBtn.setDisable(true);
                    progressBar.progressProperty().unbind();
                    statusLbl.textProperty().unbind();
                    statusLbl.setText("Status: Crawling");

                    stopRequested = false;

                    crawlTask = new Task<>() {
                        @Override
                        protected Void call() throws Exception {
                            updateProgress(0, 100);
                            updateMessage("Starting keyword analysis...");
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
                                updateMessage("Crawling: " + currentUrl);
                                // Use your ForkJoin KeyMatch to check if the website contains the keyword
                                KeyMatch.KeywordResult result = keyMatch.checkWebsiteForKeyword(currentUrl, keyword);
                                DbReadWrite dbRW = new DbReadWrite();
                                dbRW.saveDatabase(keyword, result.getMatchCount(), currentUrl, urlDomain, url,null, result.isFound());
                                for(String newLink : HTagRemove.getFoundLinks()){
                                    if(!crawledUrls.contains(newLink)){
                                        queue.add(newLink);
                                    }
                                }
                                System.out.println("Unique links in mainpage crawledUrls: " + queue.size());
                                for (String test : queue) {
                                    System.out.println(test);
                                }
                                count++;
                                // Update progress
                                updateProgress(count, limit);

                                try {
                                    Thread.sleep(100);
                                } catch (InterruptedException e) {
                                    if (isCancelled() || stopRequested) break;
                                }
                            }

                            // Simulate additional processing time
                            for (int i = 50; i <= 100; i += 10) {
                                Thread.sleep(100);
                                updateProgress(i, 100);
                            }

                            return null;
                        }

                        @Override
                        protected void succeeded() {
                            super.succeeded();
                            statusLbl.setText("✓ Keyword Analysis Complete");
                            stopBtn.setDisable(true);
                            clearBtn.setDisable(false);
                            startBtn.setDisable(false);
                            readDatabase(keyword,url);
                            HTagRemove.getFoundLinks().clear();
//                            DbReadWrite dbRW = new DbReadWrite();
//                            String dbOutput = dbRW.readDatabase(keyword,urlDomain);
//                            outputArea.setText("--- Database Results ---\n" + dbOutput);

                            if (autoShutDown.isAutoShutdownEnabled()) {
                                new Thread(() -> {
                                    try {
                                        Thread.sleep(3000);
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
                            statusLbl.setText("Crawling Failed");
                        }

                        @Override
                        protected void cancelled() {
                            super.cancelled();
                            statusLbl.setText("Crawling Cancelled");
                        }
                    };

                    // Bind progress bar to task
                    progressBar.progressProperty().bind(crawlTask.progressProperty());

                    // Start task in a new thread
//                    new Thread(crawlTask).start();
                    crawlThread = new Thread(crawlTask, "Crawler-Thread");
                    crawlThread.setDaemon(true);
                    crawlThread.start();

                }else{
                    String convertCheckUrl = checkUrl ? "True" : "False";
//                    String convertCheckDatabase = checkDatabase ? "True" : "False";
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("URL/Database Problem");
                    alert.setHeaderText("Notification");
                    alert.setContentText("Status For URL Access: " + convertCheckUrl );
                    alert.showAndWait();
                }
            }catch(IOException e){
                e.printStackTrace();
            }
        }
    }

    public void stopCrawlingBtn(ActionEvent actionEvent) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Stop Crawling");
        alert.setHeaderText("Notification");
        alert.setContentText("Progress will be stop it and cant resume!\nAre you sure you want to stop Crawling?");
        Optional<ButtonType> result = alert.showAndWait();
        if(result.get() == ButtonType.OK){
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
            clearTextBtn(actionEvent);
            statusLbl.textProperty().unbind();
            statusLbl.setText("Crawling Stopped");
        }

    }


    public void clearTextBtn(ActionEvent actionEvent) {
//        stopRequested = false;
        if (crawlThread != null && crawlThread.isAlive()) {
            crawlThread.interrupt();
        }
        crawlThread = null;
        crawlTask = null;
        queue.clear();
        crawledUrls.clear();
        DisplayRecords.clear();
//        dbTable.getItems().clear();
//        dbTable.getColumns().clear();
        stopRequested = false;
        keywordFld.clear();
        urlFld.clear();
        statusLbl.textProperty().unbind();
        statusLbl.setStyle("-fx-text-fill: Red;");
        statusLbl.setText("Status: Clear");

        new Thread(() -> {
                    try {
            Thread.sleep(5000);
        }catch(InterruptedException e){
            e.printStackTrace();
        }
            Platform.runLater(() -> {
                if ("Status: Clear".equals(statusLbl.getText())) {
                    statusLbl.textProperty().unbind();
                    statusLbl.setStyle("-fx-text-fill: green;");
                    statusLbl.setText("Status: Waiting for input");
                }
            });
        }).start();
        numberSp.getValueFactory().setValue(1);
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10000, 1);
        numberSp.setValueFactory(valueFactory);
//        https://stackoverflow.com/questions/18517161/javafx-progress-cannot-be-to-solve-a-variable/18519587
        progressBar.progressProperty().unbind();
        progressBar.setProgress(0.0);

    }


}
