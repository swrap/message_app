package roofmessage.roofmessageapp;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jesse Saran on 7/1/2016.
 */
public class PhoneNumberFormatter {

    protected PhoneNumberFormatter() {};

    //turns whatever to single numbers

    /**
     * Turns whatever format numbers into a string of only numbers
     * Example:
     *      (123) 456-7891 -> 1234567891
     *
     * @param unfromattedNumber
     * @return
     */
    public static String toSingleNumber(String unfromattedNumber) {
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(unfromattedNumber);
        StringBuilder stringBuilder = new StringBuilder();
        while(matcher.find()) {
            stringBuilder.append(matcher.group(0));
        }
        return stringBuilder.toString();
    }
}
