module org.saw.webcrawler.webcrawler {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires jdk.management;
    requires javax.mail;
    requires mysql.connector.j;
    requires org.seleniumhq.selenium.chrome_driver;
    requires org.jsoup;
    requires java.net.http;
    requires java.prefs;
    requires com.jthemedetector;
    requires com.sun.jna;
    requires com.sun.jna.platform;
    requires org.seleniumhq.selenium.support;
    requires com.google.common;


    opens org.saw.webcrawler to javafx.fxml;
    opens org.saw.webcrawler.corefeatures to javafx.base;
    exports org.saw.webcrawler;
}