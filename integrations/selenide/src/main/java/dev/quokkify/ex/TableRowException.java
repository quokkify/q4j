package dev.quokkify.ex;

import java.time.Duration;
import java.util.Map;

import dev.quokkify.model.WaitDefaults;

import com.codeborne.selenide.ex.UIAssertionError;
import org.apache.commons.lang3.StringUtils;

/**
 * Custom table row exception thrown once a waited-for row does not appear within a timeout.
 *
 * <p>Chains the {@link org.awaitility.core.ConditionTimeoutException} that triggered it so the
 * original wait failure is not lost, using {@link UIAssertionError}'s timeout-aware constructor.
 */
public class TableRowException extends UIAssertionError {

  /**
   * @deprecated kept for source/binary compatibility with callers built against the pre-wait API;
   *     use {@link #TableRowException(Enum, String, Duration, Throwable)} to report the timeout
   *     that was actually applied and to chain the triggering cause.
   */
  @Deprecated(since = "0.x")
  public TableRowException(Enum<?> columnName, String value) {
    this(columnName, value, WaitDefaults.DEFAULT_TIMEOUT, null);
  }

  /**
   * @deprecated kept for source/binary compatibility with callers built against the pre-wait API;
   *     use {@link #TableRowException(Map, Duration, Throwable)} to report the timeout that was
   *     actually applied and to chain the triggering cause.
   */
  @Deprecated(since = "0.x")
  public <T extends Enum<?>> TableRowException(Map<T, String> expectedRowValues) {
    this(expectedRowValues, WaitDefaults.DEFAULT_TIMEOUT, null);
  }

  public TableRowException(Enum<?> columnName, String value, Duration timeout, Throwable cause) {
    super("No row with '%s' value in '%s' column".formatted(value,
        StringUtils.capitalize(columnName.name().toLowerCase())), timeout.toMillis(), cause);
  }

  public <T extends Enum<?>> TableRowException(Map<T, String> expectedRowValues, Duration timeout, Throwable cause) {
    super("No row with expected values: %s".formatted(expectedRowValues), timeout.toMillis(), cause);
  }

  public TableRowException(Enum<?> columnName, Duration timeout, Throwable cause) {
    super("No row found for '%s'".formatted(StringUtils.capitalize(columnName.name().toLowerCase())),
        timeout.toMillis(), cause);
  }
}
