package org.saw.webcrawler.corefeatures;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class DisplayCrawlerResult {
    private final SimpleStringProperty displayKeyword;
    private final SimpleIntegerProperty displayTimes;
    private final SimpleStringProperty displayLink;
    private final SimpleStringProperty displayCrawlTime;

    public DisplayCrawlerResult(String displayKeyword, int displayTimes, String displayLink, String displayCrawlTime){
        this.displayKeyword = new SimpleStringProperty(displayKeyword);
        this.displayTimes = new SimpleIntegerProperty(displayTimes);
        this.displayLink = new SimpleStringProperty(displayLink);
        this.displayCrawlTime = new SimpleStringProperty(displayCrawlTime);
    }

    public String getDisplayKeyword() {
        return displayKeyword.get();
    }

    public int getDisplayTimes() {
        return displayTimes.get();
    }

    public String getDisplayLink() {
        return displayLink.get();
    }

    public String getDisplayCrawlTime() {
        return displayCrawlTime.get();
    }
}
