package dev.quokkify.constant;

/**
 * The {@code DecimalPattern} interface defines a contract for retrieving a numeric pattern.
 * It is intended to provide a format pattern that can be applied to numeric values.
 *
 * <p>
 * Example usage:
 * <pre>
 *   public enum DecimalFormat implements DecimalPattern {
 *     DECIMAL_FORMAT_HASH_GROUP_DOT_00_HASH("###,##0.00#"),
 *     BET_DECIMAL_FORMAT("#0.0#######");
 *     ...
 * </pre>
 */
public interface DecimalPattern {

  /**
   * Returns a string representation of the numeric format pattern.
   *
   * <p>
   * This method should be implemented to provide a pattern for numeric formatting. For example, a pattern like
   * {@code "#,###.##"} might be returned to format numbers with thousands separators and two decimal places.
   */
  String getPattern();
}
