package dev.quokkify.formatter;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;

import dev.quokkify.constant.DecimalPattern;

/**
 * Utility class for formatting and rounding numerical values with customizable decimal places.
 * Provides methods for formatting numbers to specific decimal precision and handling various number operations,
 * such as moving decimal points or handling null values.
 */
public class NumberFormatter {

  private static final DecimalFormatSymbols DEFAULT_DECIMAL_FORMAT = new DecimalFormatSymbols(Locale.ENGLISH);
  private static final String EIGHT_SYMBOLS_AFTER_COMMA_DECIMAL_FORMAT = "#0.00000000";
  private static final String TWO_SYMBOLS_AFTER_COMMA_DECIMAL_FORMAT = "#0.00";
  private static final double ONE_HUNDRED_MILLION = 100000000.0;
  private static final double ONE_HUNDRED = 100.0;

  private NumberFormatter() {
  }

  /**
   * Convert double value to value with 8 symbols after comma.
   *
   * @param number double value
   * @return formatted double value to String value with 8 symbols after comma (Example: 0.1234 -> 0.12340000)
   */
  public static String formatToEightSymbolsAfterComma(double number) {
    DecimalFormat decimalFormat = new DecimalFormat(EIGHT_SYMBOLS_AFTER_COMMA_DECIMAL_FORMAT, DEFAULT_DECIMAL_FORMAT);
    return decimalFormat.format(number);
  }

  /**
   * Convert double value to value with 2 symbols after comma.
   *
   * @param number double value
   * @return formatted double value to String value with 2 symbols after comma (Example: 10.1 -> 10.10)
   */
  public static String formatToTwoSymbolsAfterComma(double number) {
    DecimalFormat decimalFormat = new DecimalFormat(TWO_SYMBOLS_AFTER_COMMA_DECIMAL_FORMAT, DEFAULT_DECIMAL_FORMAT);
    return decimalFormat.format(number);
  }

  /**
   * Round number to 8 symbols after comma.
   *
   * @param number double value
   * @return rounded value (Example: 123.123456789 -> 123.12345679)
   */
  public static double doubleRoundOffWithEightDigitsAfterComma(double number) {
    return Math.round(number * ONE_HUNDRED_MILLION) / ONE_HUNDRED_MILLION;
  }

  /**
   * Round number to 2 symbols after comma.
   *
   * @param number double value
   * @return rounded value (Example: 19.302932171507923 -> 19.30)
   */
  public static double doubleRoundOffWithTwoDigitsAfterComma(double number) {
    return Math.round(number * ONE_HUNDRED) / ONE_HUNDRED;
  }

  /**
   * Format decimal number in specific pattern.
   * <br/>Example: Double: 1.56, Pattern: '000.###' -> 001.56; Double: 1000.0, Pattern: '0.00' -> 1000.00
   *
   * @param value          formatted decimal number
   * @param decimalPattern formatting pattern
   * @return formatted number as string
   */
  public static String formatNumberByPattern(Number value, DecimalPattern decimalPattern) {
    DecimalFormat decimalFormat = new DecimalFormat(
        decimalPattern.getPattern(), new DecimalFormatSymbols(Locale.ENGLISH));
    return decimalFormat.format(value);
  }

  /**
   * Get null value if {@link Number} is null, else returns its value as String.
   * <br/>note: helps in places where you need to call 'String.valueOf(null)', which returns "null"
   *
   * @param number {@link Number} inheritors: {@link Long}, {@link Double}, {@link Integer}
   * @return number value as string or null
   */
  public static String formatNumberIfNull(Number number) {
    return Objects.isNull(number) ? null : String.valueOf(number);
  }

  /**
   * Move value point right.
   * <br/>Example: 1.56 -> 156; 1.01 -> 101
   *
   * @param value {@link BigDecimal} value to move points right
   * @return {@link BigDecimal} formatted value with moved points
   */
  public static BigDecimal movePointRight(BigDecimal value, int movePointCount) {
    return value.movePointRight(movePointCount);
  }
}
