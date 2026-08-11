package dev.quokkify.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexParser {

  private RegexParser() {
  }

  public static String parse(String regex, String text, String errorMessage) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group();
    } else {
      throw new RuntimeException("%s%nRegex: '%s'%nText:'%s'".formatted(errorMessage, regex, text));
    }
  }

  /**
   * Returns the input subsequence captured by the given group during the previous match operation.
   * Group zero denotes the entire pattern
   *
   * @param group index from left to right, starting at one. If 'group = 0' - return the entire pattern
   * @return return string captured by the given group
   */
  public static String parse(String regex, String text, int group) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(group);
    } else {
      throw new RuntimeException("Parsing failed.%nRegex: '%s'%nText:'%s'".formatted(regex, text));
    }
  }

  public static String parse(Pattern pattern, String text, int group) {
    Matcher matcher = pattern.matcher(text);
    if (matcher.find()) {
      return matcher.group(group);
    } else {
      throw new RuntimeException("Parsing failed.%nPattern: '%s'%nText:'%s'".formatted(pattern.pattern(), text));
    }
  }

  public static List<String> parseAllMatches(String regex, String text) {
    List<String> allMatches = new ArrayList<>();
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    while (matcher.find()) {
      allMatches.add(matcher.group());
    }
    return allMatches;
  }

  public static boolean isMatched(String regex, String text) {
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher = pattern.matcher(text);
    return matcher.find();
  }

  public static boolean nonMatched(Pattern pattern, String text) {
    return !pattern.matcher(text).find();
  }

  public static String replaceFirst(Pattern pattern, String text, String replacement) {
    Matcher matcher = pattern.matcher(text);
    return matcher.replaceFirst(Matcher.quoteReplacement(replacement));
  }
}
