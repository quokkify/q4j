package dev.quokkify.ex;

import java.time.Duration;
import java.util.Map;

import com.codeborne.selenide.ex.UIAssertionError;
import org.apache.commons.lang3.StringUtils;

/**
 * Custom table row exception.
 */
public class TableRowException extends UIAssertionError {

  public TableRowException(Enum<?> columnName, String value) {
    super("No row with '%s' value in '%s' column".formatted(value,
        StringUtils.capitalize(columnName.name().toLowerCase())));
  }

  public <T extends Enum<?>> TableRowException(Map<T, String> expectedRowValues) {
    super("No row with expected values: %s".formatted(expectedRowValues));
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
