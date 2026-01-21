package org.saw.webcrawler.corefeatures;

import javafx.beans.property.SimpleStringProperty;

/**
 * crawler result for display in Main Page
 */
public class DisplayCrawlerResult {
    /**
     * Keyword used during crawling
     */
    private final SimpleStringProperty displayKeyword;

    /**
     * Number of times the keyword was processed or matched
     */
    private final SimpleStringProperty displayTimes;

    /**
     * URL associated with the crawler result
     */
    private final SimpleStringProperty displayLink;

    /**
     * Timestamp indicating when the crawl occurred
     */
    private final SimpleStringProperty displayCrawlTime;

    /**
     * Constructs a new crawler result model for UI display.
     *
     * @param displayKeyword   the keyword used during crawling
     * @param displayTimes     number of crawl attempts or matches
     * @param displayLink      associated result URL
     * @param displayCrawlTime crawl timestamp
     */
    public DisplayCrawlerResult(String displayKeyword, String displayTimes, String displayLink, String displayCrawlTime){
        this.displayKeyword = new SimpleStringProperty(displayKeyword);
        this.displayTimes = new SimpleStringProperty(displayTimes);
        this.displayLink = new SimpleStringProperty(displayLink);
        this.displayCrawlTime = new SimpleStringProperty(displayCrawlTime);
    }

    /**
     * Returns the crawler keyword
     * @return crawler keyword
     */
    public String getDisplayKeyword() {
        return displayKeyword.get();
    }

    /**
     * Returns the search count or times value
     * @return total keyword was found
     */
    public String getDisplayTimes() {
        return displayTimes.get();
    }

    /**
     * Returns the crawler link
     * @return crawler link
     */
    public String getDisplayLink() {
        return displayLink.get();
    }

    /**
     * Returns the crawl timestamp
     * @return crawl time
     */
    public String getDisplayCrawlTime() {
        return displayCrawlTime.get();
    }
}
