package org.saw.webcrawler.corefeatures;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class SearchAndDisplayDatabase {
    private final SimpleStringProperty searchKeyword;
    private final SimpleIntegerProperty searchTimes;
    private final SimpleStringProperty searchLink;
    private final SimpleStringProperty searchFound;
    private final SimpleStringProperty searchCrawlTime;

    public SearchAndDisplayDatabase(String searchKeyword, int searchTimes, String searchLink, String searchFound, String searchCrawlTime) {
        this.searchKeyword = new SimpleStringProperty(searchKeyword);
        this.searchTimes = new SimpleIntegerProperty(searchTimes);
        this.searchLink = new SimpleStringProperty(searchLink);
        this.searchFound = new SimpleStringProperty(searchFound);
        this.searchCrawlTime = new SimpleStringProperty(searchCrawlTime);
    }

    public String getSearchKeyword() {
        return searchKeyword.get();
    }

    public int getSearchTimes() {
        return searchTimes.get();
    }

    public String getSearchLink() {
        return searchLink.get();
    }

    public String getSearchFound() {
        return searchFound.get();
    }

    public String getSearchCrawlTime() {
        return searchCrawlTime.get();
    }
}
