package rooftext.rooftextapp.utils;

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
     * @param unformattedNumber
     * @return
     */
    public static String toSingleNumber(String unformattedNumber) {
        return unformattedNumber.replace("\\D+","");
    }
}
