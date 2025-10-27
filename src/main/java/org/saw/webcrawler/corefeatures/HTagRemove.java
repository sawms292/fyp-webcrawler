package org.saw.webcrawler.corefeatures;
// import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.jsoup.select.Elements;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.saw.webcrawler.fxfeatures.readInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.jsoup.*;


public class HTagRemove {
//    https://www.youtube.com/watch?v=X0vgIO10Cds
    public static String baseUrl;
    private static HashSet<String> foundLinks = new HashSet<>();


// Getter for MainPage can copy the links
public static HashSet<String> getFoundLinks() {
    return foundLinks;
}


    public String getTextContent(String htmlContent) {
        return null;
    }
//use for get the html code become clean text
    public static class fetchResult {
        private final String staticHtmlWithoutSelenium;
        private final String seleniumJsHtml;
        private final String seleniumNonJsHtml;
//  Constractor
        public fetchResult(String staticHtmlWithoutSelenium, String seleniumJsHtml, String seleniumNonJsHtml) {
            this.staticHtmlWithoutSelenium = staticHtmlWithoutSelenium;
            this.seleniumJsHtml = seleniumJsHtml;
            this.seleniumNonJsHtml = seleniumNonJsHtml;
        }
//  Getter
        public String getStaticHtmlWithoutSelenium() {
            return staticHtmlWithoutSelenium;
        }
        public String getSeleniumJsHtml() {
            return seleniumJsHtml;
        }
        public String getSeleniumNonJsHtml() {
            return seleniumNonJsHtml;
        }
    }


    /**
     * Fetch content from a website URL
     * @param urlString The URL to fetch
     * @return The website content as string
     */
    public fetchResult fetchWebsiteContent(String urlString) {
        try{
            String staticHtml = getHtmlUseStringBuilder(urlString);
            String seleniumHtmlJs = getHtmlUseSeleniumJs(urlString, true);
            String seleniumHtmlNonJs = getHtmlUseSeleniumJs(urlString, false);
            return new fetchResult(staticHtml,seleniumHtmlJs, seleniumHtmlNonJs);
        }catch(Exception e){
            readInfo.logs.add(readInfo.timestamp() + " HTagRemove file-fetchWebsiteContent: " + e.getMessage());
            System.err.println(readInfo.timestamp() + " HTagRemove file-fetchWebsiteContent: " + e.getMessage());
//            return new fetchResult("", "", "");
            return null;
        }
    }

    private String getHtmlUseStringBuilder (String staticHtmlUrl) {
        StringBuilder html = new StringBuilder();
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    new URL(staticHtmlUrl).openStream()));
            String line;
            while ((line = in.readLine()) != null) {
                html.append(line).append("\n");
            }
            in.close();
            System.out.println(readInfo.timestamp() + " Static HTML-fetched successfully");
            return cleanHtmlContent(html.toString());
        }catch (IOException e){
            readInfo.logs.add(readInfo.timestamp() + "HTageRemove file-getHtmlUseStringBuilder: " + e.getMessage());
            System.err.println(readInfo.timestamp() + "HTageRemove file-getHtmlUseStringBuilder: " + e.getMessage());
            return null;
        }
    }

    private String getHtmlUseSeleniumJs(String url, boolean useSeleniumJs) {
        String staticHtml;
        Map<String, Object> profile = new HashMap<>();
        Map< String, Object > prefs = new HashMap< String, Object >();
        Map<String, Object> contentSettings = new HashMap<>();
        ChromeOptions options = new ChromeOptions();
//  Javascript
        prefs.put("profile.managed_default_content_settings.javascript", 1);
//  Cookies
        contentSettings.put("profile.default_content_setting_values.cookies", 1);
        profile.put("managed_default_content_settings", contentSettings);
        prefs.put("profile", profile);
//  Maximized windows
        options.addArguments("--start-maximized");
//  Pretend to be a normal Chrome
        options.addArguments("--disable-blink-features=AutomationControlled");
//  Use real user profile
        options.addArguments("user-data-dir=/tmp/chrome/profile");
//  Disable automation flags
        options.setExperimentalOption("excludeSwitches", new String[]{"enable-automation"});
//  turn off userAutomationExtension
        options.setExperimentalOption("useAutomationExtension", false);
//  Setting user-agent
        options.addArguments("user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36");
//  Loading as normal
        options.setPageLoadStrategy(PageLoadStrategy.EAGER);
        options.setExperimentalOption("prefs", prefs);
        WebDriver driver = new ChromeDriver(options);
        //  Reduce risk to detect selenium
        ((JavascriptExecutor) driver).executeScript(
                "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
        );
        String visibleText = "";
        if (useSeleniumJs) {
            try{
                driver.get(url);
                Thread.sleep(5000);
                JavascriptExecutor js = (JavascriptExecutor) driver;
                long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
                while (true) {
                    // Scroll to bottom
                    js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                    long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                    if (newHeight == lastHeight) {
                        break; // reached the bottom, no more content
                    }
                    lastHeight = newHeight;
                }
                visibleText = (String)((JavascriptExecutor)driver)
                        .executeScript("return document.body.innerText");
                String renderedHtml = driver.getPageSource();
                reserveHref(renderedHtml);
                driver.quit();
                System.out.println(readInfo.timestamp() + " HTagRemove file-Selenium Js HTML-Fetched website content successfully");
            }catch (InterruptedException e){
                readInfo.logs.add(readInfo.timestamp() + " HTagRemove file-SeleniumJs: " + e.getMessage());
                System.err.println(readInfo.timestamp() + " HTagRemove file-SeleniumJs: " + e.getMessage());
                return null;
            }
        }else{
            try{
                driver.get(url);
                Thread.sleep(5000);
                WebElement body = driver.findElement(By.tagName("body"));
                visibleText = body.getText();  // respects visibility like Ctrl+F
                String renderedHtml = driver.getPageSource();
                reserveHref(renderedHtml);
                driver.quit();
                System.out.println(readInfo.timestamp() + "Selenium Non-Js HTML-Fetched website content successfully");
            }catch (InterruptedException e){
                e.printStackTrace();
                readInfo.logs.add(readInfo.timestamp() + " HTagRemove file-SeleniumNonJs: " + e.getMessage());
                System.err.println(readInfo.timestamp() + " HTagRemove file-SeleniumNonJs: " + e.getMessage());
                return null;
            }
        }
        return visibleText;
    }


    /**
     * Clean HTML content by removing tags and scripts
     * @param htmlContent Raw HTML content
     * @return Cleaned text content
     */
    private String cleanHtmlContent(String htmlContent) {
        System.out.println(baseUrl);
        Document doc = Jsoup.parse(htmlContent);
        //title and other supervisor webpage
//        doc.select("div.hide").remove();
//        doc.select("title").remove();
//        doc.select("script").remove();
//        doc.select("meta").remove();
//        doc.select("div.w3-hide-medium").remove();
//        doc.select("div.w3-hide-small").remove();
        doc.select("hide").remove();
        String cleanedHtml = doc.text();
        String cleanedWithoutA = doc.html();
//        Cleantext for without any tag
        String cleanText = Jsoup.clean(cleanedHtml, Safelist.none());

//        Cleantextwithouta reserver tag a href
        String cleanTextWithoutA = Jsoup.clean(cleanedWithoutA, Safelist.none().addTags("a").addAttributes("a", "href"));
        Document cleanedDoc = Jsoup.parse(cleanTextWithoutA, "http://" +baseUrl);

        Elements links = cleanedDoc.select("a[href]");
        for (Element link : links) {
            String absoluteUrl = link.absUrl("href");
            if (!absoluteUrl.isEmpty()
//                    && HttpCheck.checkAccess(absoluteUrl)
                    && (absoluteUrl.startsWith("http://" + baseUrl)
                    || absoluteUrl.startsWith("https://" + baseUrl))
                    && !absoluteUrl.contains("#")
                    && !absoluteUrl.contains(".pdf")) {
//                System.out.println(absoluteUrl);
                foundLinks.add(absoluteUrl);
            }
        }
        return cleanText;
    }

    private void reserveHref(String href) {
        System.out.println("HTagRemove-reserveHref");
        Document doc = Jsoup.parse(href);
        String cleanedWithoutA = doc.html();
        String cleanText = Jsoup.clean(cleanedWithoutA, Safelist.none().addTags("a").addAttributes("a", "href"));
        Document cleanedDoc = Jsoup.parse(cleanText, "http://" +baseUrl);

        Elements links = cleanedDoc.select("a[href]");
        for (Element link : links) {
//            String aHref = link.attr("href");
            String absoluteUrl = link.absUrl("href");
//            if (!absoluteUrl.isEmpty()
////                    && HttpCheck.checkAccess(absoluteUrl)
//                    && (absoluteUrl.startsWith("http://" + baseUrl)
//                    || absoluteUrl.startsWith("https://" + baseUrl))
//            && !absoluteUrl.contains("#")
//            && !absoluteUrl.contains(".pdf")) {
//                foundLinks.add(absoluteUrl);
//                System.out.println("Unique links in foundLinks: " + foundLinks.size());
//                for (String test : foundLinks) {
//                    System.out.println(test);
//                }
//            }
            if (!absoluteUrl.isEmpty()
                    && (absoluteUrl.startsWith("http://" + baseUrl)
                    || absoluteUrl.startsWith("https://" + baseUrl))
                    && !absoluteUrl.contains("#")
                    && !absoluteUrl.contains(".pdf")) {

                // Split if multiple http/https URLs are concatenated
                String[] possibleUrls = absoluteUrl.split("(?=(https?://))");

                for (String rawUrl : possibleUrls) {
                    String trimmedUrl = rawUrl.trim();

                    // Check again after split
                    if (!trimmedUrl.isEmpty()
                            && (trimmedUrl.startsWith("http://" + baseUrl)
                            || trimmedUrl.startsWith("https://" + baseUrl))
                            && !trimmedUrl.contains("#")
                            && !trimmedUrl.contains(".pdf")
                            && !trimmedUrl.contains(".jpg")
                            && !trimmedUrl.contains(".jpeg")
                            && !trimmedUrl.contains(".png")
                            && !trimmedUrl.contains(".gif")
                            && !trimmedUrl.contains(".svg")
                            && !trimmedUrl.contains(".bmp")
                            && !trimmedUrl.contains(".ico")
                            && !trimmedUrl.contains(".mp3")
                            && !trimmedUrl.contains(".wav")
                            && !trimmedUrl.contains(".mp4")
                            && !trimmedUrl.contains(".avi")
                            && !trimmedUrl.contains(".zip")
                            && !trimmedUrl.contains(".rar")
                            && !trimmedUrl.contains(".gz")
                            && !trimmedUrl.contains(".exe")
                            && !trimmedUrl.contains(".msi")
                            && !trimmedUrl.contains(".doc")
                            && !trimmedUrl.contains(".docx")
                            && !trimmedUrl.contains(".xls")
                            && !trimmedUrl.contains(".xlsx")
                            && !trimmedUrl.contains(".ppt")
                            && !trimmedUrl.contains(".pptx")
                            && !trimmedUrl.contains(".txt")
                            && !trimmedUrl.contains(".csv")
                            && !trimmedUrl.contains(".json")
                            && !trimmedUrl.contains(".xml")
                            && !trimmedUrl.contains(".js")
                            && !trimmedUrl.contains(".css")
                            && !trimmedUrl.contains(".woff")
                            && !trimmedUrl.contains(".woff2")
                            && !trimmedUrl.contains(".ttf")){


                        // Avoid duplicates
                        if (!foundLinks.contains(trimmedUrl)) {
                            foundLinks.add(trimmedUrl);
                            System.out.println("Added: " + trimmedUrl);
                        }
                    }
                }
            }

        }

    }

}
