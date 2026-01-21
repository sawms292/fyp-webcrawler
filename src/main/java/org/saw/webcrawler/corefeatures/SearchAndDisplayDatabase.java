package org.saw.webcrawler.corefeatures;

import javafx.beans.property.SimpleStringProperty;

/**
 * Search and display records in database pages
 */
public class SearchAndDisplayDatabase {

    /**
     * Search keyword was insert by user
     */
    private final SimpleStringProperty searchKeyword;

    /**
     * Number of times the keyword was found
     */
    private final SimpleStringProperty searchTimes;

    /**
     * Associated link for the search result
     */
    private final SimpleStringProperty searchLink;

    /**
     * Whether the keyword was found
     */
    private final SimpleStringProperty searchFound;

    /**
     * Crawl timestamp
     */
    private final SimpleStringProperty searchCrawlTime;

    /**
     * Constructs a new search record for UI display.
     *
     * @param searchKeyword   the search keyword
     * @param searchTimes     number of search attempts or occurrences
     * @param searchLink      associated result link
     * @param searchFound     whether the keyword was found
     * @param searchCrawlTime crawl timestamp
     */
    public SearchAndDisplayDatabase(String searchKeyword, String searchTimes, String searchLink, String searchFound, String searchCrawlTime) {
        this.searchKeyword = new SimpleStringProperty(searchKeyword);
        this.searchTimes = new SimpleStringProperty(searchTimes);
        this.searchLink = new SimpleStringProperty(searchLink);
        this.searchFound = new SimpleStringProperty(searchFound);
        this.searchCrawlTime = new SimpleStringProperty(searchCrawlTime);
    }

    /**
     * Returns the search keyword
     * @return search keyword
     */
    public String getSearchKeyword() {
        return searchKeyword.get();
    }

    /**
     * Returns the search count or times value
     * @return total keyword was found
     */
    public String getSearchTimes() {
        return searchTimes.get();
    }

    /**
     * Returns the associated search link
     * @return search link
     */
    public String getSearchLink() {
        return searchLink.get();
    }

    /**
     * Returns whether the keyword was found
     * @return true or false(Yes or No)
     */
    public String getSearchFound() {
        return searchFound.get();
    }

    /**
     * Returns the crawl timestamp
     * @return crawl time
     */
    public String getSearchCrawlTime() {
        return searchCrawlTime.get();
    }
}
