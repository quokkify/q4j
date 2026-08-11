package dev.quokkify.model;

import java.util.Comparator;
import java.util.Set;
import java.util.stream.Collectors;

import dev.quokkify.constant.StringConstant;

/**
 * Interface for working with url query parameters.
 */
public interface QueryParamConstantFormat {

  /**
   * Need to override query name for specific page.
   *
   * @return {@link String} query name
   */
  String name();

  /**
   * Get query param in 'q[query_name]' format.
   *
   * @return {@link String} url query name
   */
  default String queryValue() {
    return "q[%s]".formatted(value());
  }

  /**
   * Get query param in 'q[query_name][]' format for arrays.
   *
   * @return {@link String} url query name
   */
  default String queryArrayValue() {
    return "q[%s][]".formatted(value());
  }

  /**
   * Get param in 'query_name[]' format for arrays.
   *
   * @return {@link String} url query name
   */
  default String arrayValue() {
    return "%s[]".formatted(value());
  }

  /**
   * Get query param.
   *
   * @return {@link String} url query name
   */
  default String value() {
    return name().toLowerCase();
  }

  /**
   * Get param in 'query_name[index]' format for arrays.
   *
   * @param index array index as {@link Integer}
   * @return {@link String} url query array with index name
   */
  default String arrayIndexValue(Integer index) {
    return "%s[%d]".formatted(value(), index);
  }

  /**
   * Get param in 'query_name[index_string]' format for arrays.
   *
   * @param indexString array index as {@link String}
   * @return {@link String} url query array with index name
   */
  default String arrayIndexValue(String indexString) {
    return "%s[%s]".formatted(value(), indexString);
  }

  /**
   * Get param in 'query_name[index1, index2]' format for arrays.
   *
   * @param indexes array indexes as {@link Set}&lt;{@link Integer}&gt;
   * @return {@link String} url query array with indexes name
   */
  default String arrayIndexesValue(Set<Integer> indexes) {
    String indexesString = indexes.stream()
        .sorted(Comparator.naturalOrder())
        .map(String::valueOf)
        .collect(Collectors.joining(StringConstant.COMMA));
    return "%s[%s]".formatted(value(), indexesString);
  }
}
