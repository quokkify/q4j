package dev.quokkify.formatter;

import dev.quokkify.constant.StringConstant;

/**
 * This class provides methods to convert strings into various regular expression (regex) formats,
 * including matching patterns with prefixes, handling digit replacements, and handling special characters like dots.
 *
 * <p>Additionally, it includes commonly used regex patterns, such as for currencies, UUIDs, and JSON structures.</p>
 */
public class RegexFormatter {

  private static final String ALL_MATCH_PREFIX_REGEX = ".*%s";
  private static final String ALL_MATCH_REGEX = "^.*%s.*$";
  private static final String DIGITS_PATTERN = "\\d+";
  private static final String DOT_PATTERN = "\\.";
  public static final String CURRENCY_PATTERN = "[A-Z]{3,4}";
  public static final String ANY_NUMBER_PATTERN = "\\d+\\.?\\d+";
  public static final String JSON_PATTERN =
      "\\{(?:\"[^\"]*\"\\s*:\\s*(?:\"[^\"]*\"|\\d+|true|false|null|\\[.*?]|\\{.*?})\\s*,?\\s*)+}";
  public static final String UUID_PATTERN =
      "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";

  private RegexFormatter() {
  }

  /**
   * Formats a string to match a regex pattern that includes a prefix.
   * This pattern allows the string to be preceded by any number of characters.
   *
   * @param formattedString the string to be included in the pattern
   * @return the regex pattern as a string (Example: "test" -> ".*test")
   */
  public static String formatToAllMatchPatternWithPrefix(String formattedString) {
    return ALL_MATCH_PREFIX_REGEX.formatted(formattedString);
  }

  /**
   * Formats a string to match a regex pattern that allows the string to appear anywhere.
   * This pattern allows the string to be surrounded by any characters.
   *
   * @param formattedString the string to be included in the pattern
   * @return the regex pattern as a string (Example: "test" -> "^.*test.*$")
   */
  public static String formatToAllMatchPattern(String formattedString) {
    return ALL_MATCH_REGEX.formatted(formattedString);
  }

  /**
   * Replaces the "%d" placeholder in the given string with a pattern that matches any sequence of digits.
   *
   * @param formattedString the string containing "%d" to be replaced
   * @return the string with "%d" replaced by a digits regex pattern (Example: "Price: %d" -> "Price: \\d+")
   */
  public static String formatDigitsFormattedParts(String formattedString) {
    return formattedString.replace("%d", DIGITS_PATTERN);
  }

  /**
   * Replaces dot characters in the string with a regex pattern that matches a literal dot.
   * This is useful when you need to match actual dots in strings rather than their meaning in regex as any character.
   *
   * @param formattedString the string where dots need to be replaced
   * @return the string with dots replaced by the regex dot pattern (Example: "1.2.3" -> "1\\.2\\.3")
   */
  public static String formatDotsToMatchPattern(String formattedString) {
    return formattedString.replace(StringConstant.DOT, DOT_PATTERN);
  }
}
