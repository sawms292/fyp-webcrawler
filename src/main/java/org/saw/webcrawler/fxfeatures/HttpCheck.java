package org.saw.webcrawler.fxfeatures;
//https://www.youtube.com/watch?v=aC544kkYtBs
//https://stackoverflow.com/questions/66325516/how-to-follow-through-on-http-303-status-code-when-using-httpclient-in-java-11-a

import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * To verify the accessibility of a given URL via HTTP/HTTPS before crawling
 */
public class HttpCheck {
    /**
     * @param url url to be checked
     * @return true if accessible, else false help to prevent crawling inaccessible URLs
     */
    public boolean checkAccess(String url) {
        //if null
        if(url == null || url.isEmpty()) {
            readInfo.logs.add("url is null or empty");
            System.out.println("url is null or empty");
            return false;
        }

        //if did not start with
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
            readInfo.logs.add("HttpCheck file-Is URL:" + url);
            System.out.println("HttpCheck file-Is URL:" + url);
        }

        //if these three will return false
        String lowerUrl = url.toLowerCase();
        if (lowerUrl.startsWith("javascript:")
                || lowerUrl.startsWith("mailto:")
                || lowerUrl.startsWith("tel:")) {
            readInfo.logs.add("HttpCheck file-Not URL:" + lowerUrl);
            System.out.println("HttpCheck file-Not URL:" + lowerUrl);
            return false;
        }
        try {
            URL endPoint = new URL(url.trim());
            URI uri = endPoint.toURI();
            HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.ALWAYS).build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 (HTML, like Gecko) "
                            + "Chrome/123.0.0.0 Safari/537.36")
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Accept-Language", "en-US,en;q=0.5")
                    .method("GET", HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            int code = response.statusCode();
            boolean ok = (code >= 200 && code < 400);

            if (uri.getScheme() == null) {
                throw new IllegalArgumentException("Missing scheme: " + endPoint);
            }

            if (!ok) {
                // Try selenium as a fallback for anything 4xx/5xx
                if (checkWithSelenium(url)) {
                    System.out.println("Using Selenium success check http");
                    return true;
                } else {
                    System.out.println("HttpCheck: GET fallback → " + code + " for " + endPoint + " → returning false (even Selenium failed)");
                    return false;
                }
            }

            if(code == 403) {
                if(checkWithSelenium(url)){
                    ok = true;
                    System.out.println("Using Selenium success check http");
                    return ok;
                }
            }


            if(code == 405 || code == 501) {
                HttpRequest getNewRequest = HttpRequest.newBuilder().uri(uri).build();
                HttpResponse<String> getNewResponse = httpClient.send(getNewRequest, HttpResponse.BodyHandlers.ofString());
                readInfo.logs.add("HttpCheck file-Success(200):" + endPoint);
                System.out.println("HttpCheck file-Success(200):" + endPoint);
                int getCode = getNewResponse.statusCode();
                if(getCode >= 200 && getCode < 400) {
                    ok = true;
                    System.out.println("HttpCheck: for 405, 501 → " + getCode + " for " + endPoint + " → returning " + ok);
                    return ok;
                }else{
                    ok = false;
                    System.out.println("HttpCheck: GET fallback → " + getCode + " for " + endPoint + " → returning " + ok);
                    return ok;
                }

            }

            System.out.println("HttpCheck: GET fallback → " + code + " for " + endPoint + " → returning " + ok);
            return ok;
        }catch (EOFException ex){
            System.out.println(ex.getMessage() +"\n HttpCheck file-EOFException: " +url);
            return false;
        }
        catch (IllegalArgumentException ex) {
            System.out.println(ex.getMessage() +"\nHttpCheck file-IllegalArgumentException: " +url);
            return false;
        } catch (ConnectException e) {
            System.out.println(e.getMessage() +"\nHttpCheck file-Network issue: " + url);
            return false;
        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage() +"\nHttpCheck file-IOException || InterruptedException: " +url);
            return false;
        } catch (Exception e) {
            System.out.println(e.getMessage() +"\nHttpCheck file-Exception: " +url);
            return false;
        }
    }

    /**
     * Used when normal HTTP checks fail, to see if Selenium can access the URL
     * @param url url to be checked
     * @return true if accessible via Selenium, else false
     */
    private boolean checkWithSelenium(String url) {
            try{
                Map<String, Object> profile = new HashMap<>();
                Map< String, Object > prefs = new HashMap< String, Object >();
                Map<String, Object> contentSettings = new HashMap<>();
                prefs.put("profile.managed_default_content_settings.javascript", 1);
                ChromeOptions options = new ChromeOptions();
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
//  Headless
//                options.addArguments("--headless=new");
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

                contentSettings.put("profile.default_content_setting_values.cookies", 1);
                profile.put("managed_default_content_settings", contentSettings);
//  Loading as normal
                options.setPageLoadStrategy(PageLoadStrategy.NORMAL);
                prefs.put("profile", profile);
                options.setExperimentalOption("prefs", prefs);
                WebDriver driver = new ChromeDriver(options);
//                try {
                    driver.manage().window().setPosition(new Point(-2000, 0));
//                    driver.manage().window().minimize();
//                } catch (Exception ignore) {}
                driver.get(url);
                String title = driver.getTitle();
                String html = driver.getPageSource();
                driver.quit();
                readInfo.logs.add("HttpCheck file-selenium check http: Success");
                System.out.println("HttpCheck file-selenium check http: Success");
                return (title != null && !title.isEmpty() || html != null && !html.isEmpty());
            }catch(Exception e){
                System.out.println(e.getMessage() + "HttpCheck file-exception for 403: " + url);
                return false;
            }

        }


    /**
     * @return simple test to check internet connection availability
     */
    //    https://www.youtube.com/watch?v=FCZ28hSxL0U
    public boolean testConnection(){
        try{
            URL url = new URL("https://www.google.com.my/");
            URLConnection con = url.openConnection();
            con.connect();
            readInfo.logs.add(readInfo.timestamp() + "HttpCheck file: Test Connection Success");
            System.out.println(readInfo.timestamp() + "HttpCheck file: Test Connection Success");
            return true;
        }catch(Exception e){
            readInfo.logs.add(readInfo.timestamp() + "HttpCheck file: Test Connection not supported");
            System.out.println(readInfo.timestamp() + "HttpCheck file: Test Connection not supported");
            return false;
        }
    }

}
