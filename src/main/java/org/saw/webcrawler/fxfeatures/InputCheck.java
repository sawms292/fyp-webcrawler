package org.saw.webcrawler.fxfeatures;

public class InputCheck {

    public static boolean isValidInput(String keyword, String url, int number){
        System.out.println("InputCheck file: " + keyword);
        System.out.println("InputCheck file: " + url);
        System.out.println("InputCheck file: " + number);
        readInfo.logs.add(readInfo.timestamp() + "InputCheck file: " + keyword + " : " + url + ": " + number);
        return keyword != null && !keyword.isEmpty() && url != null && !url.isEmpty() && number > 0;

    }
}
