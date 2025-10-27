
package org.saw.webcrawler.corefeatures;

import org.saw.webcrawler.MainPage;
import org.saw.webcrawler.fxfeatures.readInfo;
import org.saw.webcrawler.corefeatures.HTagRemove.fetchResult;
//https://stackoverflow.com/questions/3322152/is-there-a-way-to-get-rid-of-accents-and-convert-a-whole-string-to-regular-lette
import java.text.Normalizer;



public class KeyMatch {
    private HTagRemove hTagRemove = new HTagRemove();
    private MainPage mainPage;
    
    public KeyMatch() {
    }

    /**
     * Check if website contains keyword using simple detection
     * @param url The website URL to check
     * @param keyword The keyword to search for
     * @return KeywordResult containing match count and execution time
     */
    public KeywordResult checkWebsiteForKeyword(String url, String keyword) {
        try{
            fetchResult result = hTagRemove.fetchWebsiteContent(url);
            if(result == null){
                return new KeywordResult(false, 0, "Failed to fetch website content");
            }

//  Called countKeywordMatches()and pass the html and keyword
            int countStatic = countKeywordMatches(result.getStaticHtmlWithoutSelenium(), keyword);
            int countJs = countKeywordMatches(result.getSeleniumJsHtml(), keyword);
            int countBody = countKeywordMatches(result.getSeleniumNonJsHtml(), keyword);
//  initialize int for finalcount
            int finalCount;
//  check which one is same else using countJs
            if (countJs == countBody) {
                finalCount = countJs;
            } else if (countJs == countStatic) {
                finalCount = countJs;
            } else if (countBody == countStatic) {
                finalCount = countBody;
            } else {
                finalCount = countJs;
            }

            boolean found = finalCount > 0;
            String message = found
                    ? String.format("Found %d matches for '%s'", finalCount, keyword)
                    : String.format("No matches found for '%s'", keyword);

            return new KeywordResult(found, finalCount, message);

        }catch (Exception e){
            readInfo.logs.add(readInfo.timestamp() + " KeyMatch file-checkWebsiteForKeyword: " + e.getMessage());
            return new KeywordResult(false, 0, "Error: " + e.getMessage());
        }
    }
    
    /**
     * Simple keyword check without fetching website content
     * @param textContent The text content to search
     * @param keyword The keyword to search for
     * @return true if keyword is found
     */
    public boolean containsKeyword(String textContent, String keyword) {
        if (textContent == null || keyword == null) return false;
        return textContent.toLowerCase().contains(keyword.toLowerCase());
    }
    
    /**
     * Count how many times a keyword appears in text
     * @param text The text to search in
     * @param keyword The keyword to count
     * @return Number of matches found
     */
    private int countKeywordMatches(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) return 0;

        String textFix = replaceText(text);
        String lowerKeyword = replaceText(keyword);
        int count = 0;
        int index = 0;
        
        while ((index = textFix.indexOf(lowerKeyword, index)) != -1) {
            count++;
            index += lowerKeyword.length();
        }
        
        return count;
    }

    /**
     * Result class to hold keyword search results
     */
    public static class KeywordResult {
        private boolean found;
        private int matchCount;
        private String message;

        public KeywordResult(boolean found, int matchCount, String message) {
            this.found = found;
            this.matchCount = matchCount;
            this.message = message;
        }

        public boolean isFound() { return found; }
        public int getMatchCount() { return matchCount; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return message;
        }
    }

    private String replaceText(String textAndKeyword){
        if (textAndKeyword == null || textAndKeyword.isEmpty()) return "";
        String lowerText = textAndKeyword.replace("’", "'")
                .replace("‘", "'")
                .replace("“", "\"")
                .replace("”", "\"")
                .replace("–", "-")
                .replace("—", "-")
                .replace("\\u00A0", " ")
                .replace("\\u200B", "");
        System.out.println("Replacing Finish");
        lowerText = Normalizer.normalize(lowerText, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        System.out.println("Normalized finish");
        return lowerText.toLowerCase().trim();
    }


}