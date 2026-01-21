package org.saw.webcrawler.corefeatures;
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


/**
 * Extract HTML, remove tags and get clean text content used for keyword matching
 */
public class HTagRemove {
    //    https://www.youtube.com/watch?v=X0vgIO10Cds
    /**
     * Shared Selenium driver instance used to fetch dynamic content.
     */
    private static WebDriver usedriver = null;
    /**
     *  Determine whether a discovered link is internal
     */
    public static String baseUrl;

    /**
     * Set of internal links discovered during HTML extraction
     */
    private static HashSet<String> foundLinks = new HashSet<>();

    /**
     * final URL observed after Selenium navigation
     */
    private String lastSeleniumFinalUrl = null;

    /**
     * HTTP status code for the last fetch attempt
     */
    private int lastHttpStatus = -1;

    /**
     * @return the final URL observed after Selenium navigation
     */
    public String getLastSeleniumFinalUrl() { return lastSeleniumFinalUrl; }

    /**
     * @return the last recorded HTTP status code for the fetch target
     */
    public int getLastHttpStatus() { return lastHttpStatus; }


    /**
     * @return  the set of internal links discovered from web pages and it can used by Main Page to determine have other links to crawl
     */
    public static HashSet<String> getFoundLinks() {
        return foundLinks;
    }


    /**
     * Result of extract website content
     */
    public static class fetchResult {
        /**
         * Cleaned text retrieved using a simple static HTTP request
         */
        private final String staticHtmlWithoutSelenium;
        /**
         * Visible text retrieved using Selenium with scrolling/JS execution enabled
         */
        private final String seleniumJsHtml;
        /**
         * Visible text retrieved using Selenium from the page body
         */
        private final String seleniumNonJsHtml;

        /**
         * @param staticHtmlWithoutSelenium cleaned text from static fetch
         * @param seleniumJsHtml visible text from Selenium (JS/scroll)
         * @param seleniumNonJsHtml visible text from Selenium (body text)
         */
        //  Constractor
        public fetchResult(String staticHtmlWithoutSelenium, String seleniumJsHtml, String seleniumNonJsHtml) {
            this.staticHtmlWithoutSelenium = staticHtmlWithoutSelenium;
            this.seleniumJsHtml = seleniumJsHtml;
            this.seleniumNonJsHtml = seleniumNonJsHtml;
        }

        /**
         * @return text fetched without Selenium
         */
        //  Getter
        public String getStaticHtmlWithoutSelenium() {
            return staticHtmlWithoutSelenium;
        }

        /**
         * @return text fetched using Selenium with JS/scroll logic
         */
        public String getSeleniumJsHtml() {
            return seleniumJsHtml;
        }

        /**
         * @return text fetched using Selenium from the body element
         */
        public String getSeleniumNonJsHtml() {
            return seleniumNonJsHtml;
        }
    }


    /**
     * Fetch content from a website URL
     * @param urlString The URL to fetch
     * @return The website content as string
     */
    //1
    public fetchResult fetchWebsiteContent(String urlString) {
        try{

            // download html code without selenium
            String staticHtml = getHtmlUseStringBuilder(urlString);
            // scroll + docuemtn.body.innerText with selenium
            String seleniumHtmlJs = getHtmlUseSeleniumJs(urlString, true);
            //body.getText() with selenium
            String seleniumHtmlNonJs = getHtmlUseSeleniumJs(urlString, false);

            System.out.println("check baseUrl = " + baseUrl);
            System.out.println("check foundLinks size = " + foundLinks.size());
            if (foundLinks.size() > 0) {
                System.out.println("check next link = " + foundLinks.iterator().next());
            }

            if (lastSeleniumFinalUrl != null && !lastSeleniumFinalUrl.isEmpty()) {
                lastHttpStatus = getHttpStatus(lastSeleniumFinalUrl);
            } else {
                lastHttpStatus = getHttpStatus(urlString);
            }

            return new fetchResult(staticHtml,seleniumHtmlJs, seleniumHtmlNonJs);
        }catch(Exception e){
            readInfo.logs.add(readInfo.timestamp() + " HTagRemove file-fetchWebsiteContent: " + e.getMessage());
            System.err.println(readInfo.timestamp() + " HTagRemove file-fetchWebsiteContent: " + e.getMessage());
            return null;
        }
    }

    /**
     * @param staticHtmlUrl  the URL to fetch
     * @return cleaned text content from static HTML fetch
     */
    //2
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
            //using cleanHTMLContent to remove tags
            return cleanHtmlContent(html.toString());
        }catch (IOException e){
            readInfo.logs.add(readInfo.timestamp() + "HTageRemove file-getHtmlUseStringBuilder: " + e.getMessage());
            System.err.println(readInfo.timestamp() + "HTageRemove file-getHtmlUseStringBuilder: " + e.getMessage());
            return null;
        }
    }

    /**
     * @param norEnv true to use normal user environment, false for minimal
     * @return a new WebDriver instance
     */
    //3
    private WebDriver createDriver(boolean norEnv) {
        ChromeOptions options = new ChromeOptions();
        if(!norEnv) {
            Map<String, Object> profile = new HashMap<>();
            Map<String, Object> prefs = new HashMap<String, Object>();
            Map<String, Object> contentSettings = new HashMap<>();

//  https://petertc.medium.com/pro-tips-for-selenium-setup-1855a11f88f8
//  https://scrapeops.io/selenium-web-scraping-playbook/python-selenium-block-images-resources/
//  Javascript
            prefs.put("profile.managed_default_content_settings.javascript", 1);
//  images
//            prefs.put("profile.managed_default_content_settings.images",2);
            options.addArguments("--blink-settings=imagesEnabled=false");
//  plugins
            prefs.put("profile.managed_default_content_settings.plugins", 2);
//  popups
            prefs.put("profile.managed_default_content_settings.popups", 2);
//  notifications
            prefs.put("profile.managed_default_content_settings.notifications", 2);
//  geolocation
            prefs.put("profile.managed_default_content_settings.geolocation", 2);
//  media_stream
            prefs.put("profile.managed_default_content_settings.media_stream", 2);
//  fonts
            prefs.put("profile.managed_default_content_settings.fonts", 2);
//  stylesheets
            prefs.put("profile.managed_default_content_settings.stylesheets", 2);
            contentSettings.put("profile.default_content_setting_values.cookies", 1);
            profile.put("managed_default_content_settings", contentSettings);
            prefs.put("profile", profile);
            options.addArguments("--window-size=1000,800");
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
            usedriver = new ChromeDriver(options);
            //  Move window off-screen
                usedriver.manage().window().setPosition(new Point(-2000, 0));

            //  Reduce risk to detect selenium
            ((JavascriptExecutor) usedriver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );
            return usedriver;
        }else{

            options.addArguments("--window-size=1000,800");

            options.setPageLoadStrategy(PageLoadStrategy.EAGER);

            usedriver = new ChromeDriver(options);
                //  Move window off-screen
                usedriver.manage().window().setPosition(new Point(-2000, 0));

            ((JavascriptExecutor) usedriver).executeScript(
                    "Object.defineProperty(navigator, 'webdriver', {get: () => undefined})"
            );
            return usedriver;
        }
    }

    /**
     * @param driver Selenium driver containing the loaded page
     * @return true if a CAPTCHA is detected
     */
    private boolean detectCaptcha(WebDriver driver) {
        try {
            String html = driver.getPageSource().toLowerCase();
            String[] checkCaptchaWords = {
                    "verify you are human",
                    "verifying you are human",
                    "are you a robot",
                    "please check the box",
                    "press & hold",
                    "complete the security check",
                    "before accessing this site"
            };

            for (String word : checkCaptchaWords) {
                if (html.contains(word)) {
                    System.out.println("Detected captcha phrase: " + word);
                    return true;
                }
            }


            if (!driver.findElements(By.xpath("//*[contains(@class,'cf-challenge')]")).isEmpty()) {
                System.out.println("Found Cloudflare challenge");
                return true;
            }

            if (!driver.findElements(By.xpath("//*[contains(@class,'cf-turnstile')]")).isEmpty()) {
                System.out.println("Found Cloudflare Turnstile");
                return true;
            }

            return false;
        } catch (WebDriverException e) {
            System.err.println("detectCaptchaByXpath error: " + e.getMessage());
            return false;
        }
    }

    /**
     * @param driver  Selenium driver containing the loaded page
     * @param maxAttempts maximum number of checks
     * @return true if CAPTCHA is solved within attempts
     */
    private boolean checkCaptchaSolve(WebDriver driver, int maxAttempts) {
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }

            if (!detectCaptcha(driver)) {
                System.out.println("CAPTCHA solved");
                return true;
            }

            System.out.println("CAPTCHA not solved");
        }

        return false;
    }


    /**
     * @param url input URL
     * @return normalized URL for comparison like for removing www, http to https, trailing slash
     */
    private static String normalizeUrl(String url) {
        if (url == null) return null;
        url = url.trim();

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
     * @param url load url from user input or next url
     * @param useSeleniumJs true to use JS and scrolling, false for body text
     * @return visible text content from the page
     */
    private String getHtmlUseSeleniumJs(String url, boolean useSeleniumJs) {
        if(usedriver == null){
            usedriver = createDriver(false);
        }
        WebDriver driver = usedriver;

        String visibleText = "";
        if (useSeleniumJs) {
            try{
                driver.get(url);
                System.out.println("current url: " + url);
                String newsUrl = driver.getCurrentUrl();
                System.out.println("news url: " + newsUrl);
                boolean sameUrl = normalizeUrl(newsUrl).equals(normalizeUrl(url));
                if (sameUrl) {
                    lastSeleniumFinalUrl = normalizeUrl(url);
                } else {
                    lastSeleniumFinalUrl = normalizeUrl(newsUrl);
                }
                Thread.sleep(500);
                // if detect captcha, switch to normal user environment
                if (detectCaptcha(driver)) {
                    System.out.println("CAPTCHA detected - swithching to real user environment");
                    closeSharedDriver();
                    usedriver = createDriver(true);
                    driver = usedriver;
                    driver.get(url);
                    System.out.println("Reloaded in real user env: " + driver.getCurrentUrl());
                    if (!checkCaptchaSolve(driver, 2)) {
                        System.out.println("Skipping URL: " + url);
                        return null;
                    }

                }

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
//                driver.quit();
                System.out.println(readInfo.timestamp() + " HTagRemove file-Selenium Js HTML-Fetched website content successfully");
            }catch (InterruptedException e){
                readInfo.logs.add(readInfo.timestamp() + " HTagRemove file-SeleniumJs: " + e.getMessage());
                System.err.println(readInfo.timestamp() + " HTagRemove file-SeleniumJs: " + e.getMessage());
                return null;
            }
        }else{
            try{
                driver.get(url);
                System.out.println("current url: " + url);
                String newsUrl = driver.getCurrentUrl();
                System.out.println("news url: " + newsUrl);
                boolean sameUrl = normalizeUrl(newsUrl).equals(normalizeUrl(url));
                if (sameUrl) {
                    lastSeleniumFinalUrl = normalizeUrl(url);
                } else {
                    lastSeleniumFinalUrl = normalizeUrl(newsUrl);
                }
                Thread.sleep(500);
                if (detectCaptcha(driver)) {
                    System.out.println("CAPTCHA detected - swithching to real user environment");
                    closeSharedDriver();
                    usedriver = createDriver(true);
                    driver = usedriver;
                    driver.get(url);
                    System.out.println("Reloaded in real user env: " + driver.getCurrentUrl());
                    if (!checkCaptchaSolve(driver, 2)) {
                        System.out.println("Skipping URL: " + url);
                        return null;
                    }
                }

                WebElement body = driver.findElement(By.tagName("body"));
                // respects visibility like Ctrl+F
                visibleText = body.getText();
                String renderedHtml = driver.getPageSource();
                reserveHref(renderedHtml);
//                driver.quit();
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
     * Closes and clears the shared Selenium web driver
     */
    public static void closeSharedDriver() {
        if (usedriver != null) {
            try { usedriver.quit(); }
            catch (Exception e) {
                System.err.println("Error closing shared WebDriver: " + e.getMessage());
            }
            usedriver = null;
        }
    }



    /**
     * Clean HTML content by removing tags and scripts
     * @param htmlContent Raw HTML content
     * @return Cleaned text content
     */
    private String cleanHtmlContent(String htmlContent) {
        System.out.println(baseUrl);
        Document doc = Jsoup.parse(htmlContent);

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
                    && (absoluteUrl.startsWith("http://" + baseUrl)
                    || absoluteUrl.startsWith("https://" + baseUrl))
                    && !absoluteUrl.contains("#")
                    && !absoluteUrl.contains(".pdf")) {
                foundLinks.add(absoluteUrl);
            }
        }
        return cleanText;
    }

    /**
     * @param href raw HTML content to extract links
     */
    private void reserveHref(String href) {
        System.out.println("HTagRemove-reserveHref");
        Document doc = Jsoup.parse(href);
        String cleanedWithoutA = doc.html();
        String cleanText = Jsoup.clean(cleanedWithoutA, Safelist.none().addTags("a").addAttributes("a", "href"));
        Document cleanedDoc = Jsoup.parse(cleanText, "http://" +baseUrl);

        Elements links = cleanedDoc.select("a[href]");
        for (Element link : links) {

            String absoluteUrl = link.absUrl("href");

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

    /**
     * @param urlStr URL to check
     * @return HTTP status code, or -1 on error
     */
    private int getHttpStatus(String urlStr) {
        try {
            java.net.HttpURLConnection c = (java.net.HttpURLConnection) new java.net.URL(urlStr).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setRequestMethod("GET");
            c.setConnectTimeout(8000);
            c.setReadTimeout(10000);
            return c.getResponseCode();
        } catch (Exception e) {
            return -1;
        }
    }




}
