package dev.quokkify.util;

import java.util.stream.Stream;

import dev.quokkify.constant.StringConstant;

/**
 * Utils for string replacement.
 */
public class ReplaceUtils {

  private ReplaceUtils() {
  }

  public static String replaceDashToUnderscore(String targetString) {
    return targetString.replace(StringConstant.DASH, StringConstant.UNDERSCORE);
  }

  public static String replaceAmpersandParamToSymbol(String targetString) {
    return Stream.of(targetString)
        .map(str -> str.replace("&amp;", StringConstant.AMPERSAND))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Symbols to replace not found"));
  }

  public static String replaceDashToHyphen(String targetString) {
    return targetString.replace(StringConstant.DASH, StringConstant.HYPHEN);
  }
}
