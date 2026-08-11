package dev.quokkify.model;

import java.math.RoundingMode;

/**
 * Class representing the settings for scaling and rounding numerical values.
 *
 * <p>Example usage:</p>
 * <pre>
 *     ScaleSettings settings = new ScaleSettings(2, RoundingMode.HALF_UP);
 * </pre>
 */
public record ScaleSettings(int scale, RoundingMode roundingMode) {

}
