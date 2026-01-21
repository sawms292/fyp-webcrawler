package org.saw.webcrawler.fxfeatures;

/**
 * Class for checking the validity of user inputs before starting the web crawling process
 */
public class InputCheck {

    /**
     * @param keyword -> the search keyword input by the user
     * @param url -> the URL input by the user
     * @param number -> the number of pages to crawl input by the user
     * @return boolean -> true if all inputs are valid
     */
    public static boolean isValidInput(String keyword, String url, int number){
        System.out.println("InputCheck file: " + keyword);
        System.out.println("InputCheck file: " + url);
        System.out.println("InputCheck file: " + number);
        readInfo.logs.add(readInfo.timestamp() + "InputCheck file: " + keyword + " : " + url + ": " + number);
        return keyword != null && !keyword.isEmpty() && url != null && !url.isEmpty() && number > 0;

    }

    /**
     * @param email -> the email input by the user
     * @return boolean -> true if email input is valid
     */
    public static boolean emailValidInput(String email){
        System.out.println("InputCheck file: " + email);
        readInfo.logs.add(readInfo.timestamp() + "InputCheck file: " + email);
        return email != null && !email.isEmpty();

    }
}
