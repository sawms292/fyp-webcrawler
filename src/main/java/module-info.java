module org.saw.webcrawler.webcrawler {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jdk.management;
    requires javax.mail;
    requires mysql.connector.j;
    requires org.seleniumhq.selenium.chrome_driver;
    requires org.seleniumhq.selenium.devtools_v137;
    requires org.jsoup;
    requires java.net.http;
    requires java.desktop;
    requires java.prefs;
    requires com.google.common;
    requires com.jthemedetector;


    opens org.saw.webcrawler to javafx.fxml;
    opens org.saw.webcrawler.corefeatures to javafx.base;
    exports org.saw.webcrawler;
}