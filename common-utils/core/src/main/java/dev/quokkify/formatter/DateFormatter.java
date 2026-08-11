package dev.quokkify.formatter;

import dev.quokkify.constant.StringConstant;

/**
 * Utility class for formatting and splitting date strings.
 * This class provides static methods to split date strings using different delimiters.
 */
public class DateFormatter {

  private DateFormatter() {
  }

  /**
   * Splits the provided date string by a dash ("-").
   *
   * @param date The date string to be split, in a format like "DD-MM-YYYY", "YYYY-MM-DD".
   * @return An array of strings representing the date parts (e.g., day, month, year).
   */
  public static String[] splitDateByDash(String date) {
    return splitDate(date, StringConstant.DASH);
  }

  /**
   * Splits the provided date string using the specified regular expression (regex).
   *
   * @param date  The date string to be split.
   * @param regex The regular expression used to split the date string.
   * @return An array of strings representing the split parts of the date.
   */
  public static String[] splitDate(String date, String regex) {
    return date.split(regex);
  }
}
