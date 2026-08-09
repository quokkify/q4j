package dev.quokkify.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.NoSuchElementException;

import dev.quokkify.model.ScaleSettings;

/**
 * Utils for numerical calculations.
 */
public class CalculationUtils {

  private static final int HUNDRED = 100;
  private static final ScaleSettings DEFAULT_SCALE_SETTINGS = new ScaleSettings(2, RoundingMode.HALF_UP);

  private CalculationUtils() {
  }

  public static Double calculatePercentOf(Integer value, Double percent) {
    return calculatePercentOf(BigDecimal.valueOf(value), BigDecimal.valueOf(percent), DEFAULT_SCALE_SETTINGS);
  }

  public static Double calculatePercentOf(Integer value, Integer percent) {
    return calculatePercentOf(value, percent, DEFAULT_SCALE_SETTINGS);
  }

  public static Double calculatePercentOf(Integer value, Integer percent, ScaleSettings scaleSettings) {
    return calculatePercentOf(BigDecimal.valueOf(value), BigDecimal.valueOf(percent), scaleSettings);
  }

  public static Double calculatePercentOf(Long value, Double percent) {
    return calculatePercentOf(BigDecimal.valueOf(value), BigDecimal.valueOf(percent), DEFAULT_SCALE_SETTINGS);
  }

  public static Double calculatePercentOf(Long value, Long percent) {
    return calculatePercentOf(value, percent, DEFAULT_SCALE_SETTINGS);
  }

  public static Double calculatePercentOf(Long value, Long percent, ScaleSettings scaleSettings) {
    return calculatePercentOf(BigDecimal.valueOf(value), BigDecimal.valueOf(percent), scaleSettings);
  }

  public static Double calculatePercentOf(Double value, Double percent) {
    return calculatePercentOf(value, percent, DEFAULT_SCALE_SETTINGS);
  }

  public static Double calculatePercentOf(Double value, Double percent, ScaleSettings scaleSettings) {
    return calculatePercentOf(BigDecimal.valueOf(value), BigDecimal.valueOf(percent), scaleSettings);
  }

  /**
   * Get percent value of number.
   *
   * <p>Example: value = 10.99; percentage = 50.0 (%) -> result = (5.495) -> 5.xx (x - depends on roundingMode).</p>
   *
   * @param value         number value to calculate percentage
   * @param percent       percentage amount of number
   * @param scaleSettings scale settings with number of decimal places and rounding mode
   * @return {@link BigDecimal} percentage value result
   */
  public static Double calculatePercentOf(BigDecimal value, BigDecimal percent, ScaleSettings scaleSettings) {
    return value.multiply(percent)
        .divide(BigDecimal.valueOf(HUNDRED), scaleSettings.roundingMode())
        .setScale(scaleSettings.scale(), scaleSettings.roundingMode())
        .doubleValue();
  }

  /**
   * Get the percentage of two numbers.
   *
   * <p>Example: targetValue = 51; valueToCalculatePercent = 80 -> result = (63.75) -> 63.xx</p>
   *
   * <p>(x - depends on roundingMode).</p>
   *
   * @param targetValue             target number
   * @param valueToCalculatePercent value to calculate percent
   * @param scaleSettings           scale settings with number of decimal places and rounding mode
   * @return {@link BigDecimal} percentage value result
   */
  public static BigDecimal calculatePercent(BigDecimal targetValue, BigDecimal valueToCalculatePercent,
                                            ScaleSettings scaleSettings) {
    return targetValue.divide(valueToCalculatePercent, scaleSettings.scale(), scaleSettings.roundingMode())
        .multiply(BigDecimal.valueOf(HUNDRED));
  }

  /**
   * Get average value of values.
   *
   * @param values values to calculate average value
   * @return {@link Double} average value result
   */
  public static <T extends Number> Double getAverageOf(List<T> values) {
    return values.stream()
        .mapToDouble(Number::doubleValue)
        .average()
        .orElseThrow(() -> new NoSuchElementException("List of values for calculation average amount is empty"));
  }
}
