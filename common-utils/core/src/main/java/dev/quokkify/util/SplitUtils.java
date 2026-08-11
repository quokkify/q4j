package dev.quokkify.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Utils for string splitting.
 */
public class SplitUtils {

  private static final String DEFAULT_SPLITERATOR = "[,;]";

  private SplitUtils() {
  }

  /**
   * Split value of env variable by default spliterator {@value #DEFAULT_SPLITERATOR}.
   *
   * @param name name of env variable
   * @return list of strings or empty list if env variable is {@code null} or empty
   */
  public static List<String> splitEnvVariable(String name) {
    return split(System.getenv(name));
  }

  /**
   * Split string by default spliterator {@value #DEFAULT_SPLITERATOR}.
   *
   * @param origin source string
   * @return list of strings or empty list if {@code origin} is {@code null} or empty
   */
  public static List<String> split(String origin) {
    return StringUtils.isBlank(origin)
        ? new ArrayList<>()
        : Arrays.stream(origin.split(DEFAULT_SPLITERATOR)).map(String::trim).toList();
  }
}
