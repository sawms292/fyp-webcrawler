
package org.saw.webcrawler.corefeatures;

import org.saw.webcrawler.fxfeatures.readInfo;
import org.saw.webcrawler.corefeatures.HTagRemove.fetchResult;
//https://stackoverflow.com/questions/3322152/is-there-a-way-to-get-rid-of-accents-and-convert-a-whole-string-to-regular-lette
import java.text.Normalizer;


/**
 * Checks whether a given website contains a specified keyword and counts matches
 */
public class KeyMatch {
    private HTagRemove hTagRemove = new HTagRemove();
    
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
                int status = hTagRemove.getLastHttpStatus();
                String reason;
                if (status == 403 || status == 429) {
                    reason = "restricted";
                } else if (status >= 400 && status <= 599) {
                    reason = "error http";
                } else {
                    reason = "error fetching";
                }
                String timesJson = "[0,\"" + reason + "\"]";
                return new KeywordResult(false, 0, "Failed to fetch website content",reason, timesJson);
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

            String before = simpleUrl(url);
            String after  = simpleUrl(hTagRemove.getLastSeleniumFinalUrl());
            int status    = hTagRemove.getLastHttpStatus();

            String reason;
            if (!before.isEmpty() && !after.isEmpty() && !before.equals(after)) {
                reason = "redirect";
            } else if ((status >= 400 && status <= 599)
                    || looksLikeCaptcha(result.getSeleniumJsHtml())
                    || looksLikeCaptcha(result.getSeleniumNonJsHtml())) {
                reason = "restricted";
            } else {
                reason = "none";
            }

            String timesJson = "[" + finalCount + ",\"" + reason + "\"]";


            return new KeywordResult(found, finalCount, message, reason, timesJson);

        }catch (Exception e){
            readInfo.logs.add(readInfo.timestamp() + " KeyMatch file-checkWebsiteForKeyword: " + e.getMessage());
            return new KeywordResult(false, 0, "Error: " + e.getMessage());
        }
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
        private String reason;
        private String timesJson;

        public KeywordResult(boolean found, int matchCount, String message) {
            this(found, matchCount, message, "none", "[" + matchCount + ",\"none\"]");
        }

        public KeywordResult(boolean found, int matchCount, String message, String reason, String timesJson) {
            this.found = found;
            this.matchCount = matchCount;
            this.message = message;
            this.reason = reason;
            this.timesJson = timesJson;
        }

        public boolean isFound() { return found; }
        public int getMatchCount() { return matchCount; }
        public String getMessage() { return message; }
        public String getReason() { return reason; }
        public String getTimesJson() { return timesJson; }

        @Override
        public String toString() {
            return message;
        }
    }

    /**
     * @param textAndKeyword input text to normalize
     * @return normalized lowercase text
     */
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


    /**
     * @param u input URL
     * @return simplified URL for comparison
     */
    private String simpleUrl(String u) {
        if (u == null) return "";
        String url = u.trim();

        if (url.startsWith("http://")) {
            url = "https://" + url.substring(7);
        }
        if (url.startsWith("https://www.")) {
            url = "https://" + url.substring("https://www.".length());
        }
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }

        return url;
    }


    /**
     * @param text visible text to inspect
     * @return true if text indicates a CAPTCHA or human verification page
     */
    private boolean looksLikeCaptcha(String text) {
        if (text == null) return false;
        String t = text.toLowerCase();
        return t.contains("captcha")
                || t.contains("verify you are human")
                || (t.contains("cloudflare") && t.contains("checking your browser"));
    }


}