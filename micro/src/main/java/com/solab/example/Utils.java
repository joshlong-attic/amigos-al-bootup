package com.solab.example;

/**
 * @author Enrique Zamudio
 * Date: 2019-03-23 18:17
 */
public class Utils {

    /** Returns true is x is not null and not an empty string. */
    public static boolean nonempty(String x) {
        return x != null && !x.isEmpty();
    }
}
