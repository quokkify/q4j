package dev.quokkify.elements.table.horizontal;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import dev.quokkify.elements.table.classic.base.BaseRow;
import dev.quokkify.util.LocalDateUtils;

import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;

/**
 * Horizontal table row UI element and methods of working with it.
 */
public class HorizontalRow<T extends Enum<T>> extends BaseRow<T> {

  public HorizontalRow(SelenideElement element) {
    super(element);
  }

  /**
   * Verify table row value.
   *
   * @param expectedRowValue expected row value text
   */
  public void verifyRow(String expectedRowValue) {
    Assertions.assertThat(this.getSelf().getText()).as("Cell value incorrect").isEqualToIgnoringCase(expectedRowValue);
  }

  /**
   * Verify table row value.
   *
   * @param expectedRowValue expected row value
   */
  public <N extends Number> void verifyRow(N expectedRowValue) {
    Assertions.assertThat(this.getSelf().getText()).as("Cell value incorrect")
        .isEqualToIgnoringCase(String.valueOf(expectedRowValue));
  }

  /**
   * Verify table row contains value.
   *
   * @param expectedRowValue expected row value text
   */
  public void verifyRowContains(String expectedRowValue) {
    Assertions.assertThat(this.getSelf().getText()).as("Cell doesn't contain expected value")
        .containsIgnoringCase(expectedRowValue);
  }

  /**
   * Verify table row contains value.
   *
   * @param expectedRowValue expected row value
   */
  public <N extends Number> void verifyRowContains(N expectedRowValue) {
    Assertions.assertThat(this.getSelf().getText()).as("Cell doesn't contain expected value")
        .containsIgnoringCase(String.valueOf(expectedRowValue));
  }

  /**
   * Verify table row contains pattern.
   *
   * @param pattern expected row pattern
   */
  public void verifyRowContainsPattern(String pattern) {
    Assertions.assertThat(this.getSelf().getText()).as("Row doesn't contain expected pattern")
        .containsPattern(pattern);
  }

  /**
   * Verify table row empty value.
   */
  public void verifyRowEmptyValue() {
    Assertions.assertThat(this.getSelf().getText()).as("Cell value is not empty").isEmpty();
  }

  /**
   * Verify row date time value.
   *
   * @param actualDateTimeFormatter actual date time formatter
   * @param expectedRowValue        expected row value
   * @param defaultValue            expected default row value
   */
  public void verifyRowDateTime(DateTimeFormatter actualDateTimeFormatter, LocalDateTime expectedRowValue,
                                String defaultValue) {
    if (Objects.isNull(expectedRowValue)) {
      verifyRow(defaultValue);
    } else {
      verifyRowDateTime(actualDateTimeFormatter, expectedRowValue);
    }
  }

  /**
   * Verify row date time value.
   *
   * @param actualDateTimeFormatter actual date time formatter
   * @param expectedRowValue        expectedRowValue row value
   */
  public void verifyRowDateTime(DateTimeFormatter actualDateTimeFormatter, LocalDateTime expectedRowValue) {
    verifyRowDateTime(actualDateTimeFormatter, expectedRowValue, Duration.ofMillis(62_000));
  }

  /**
   * Verify row date time value.
   *
   * @param actualDateTimeFormatter actual date time formatter
   * @param expectedRowValue        expectedRowValue row value
   * @param deltaDuration           time delta for verification of expectedRowValue
   */
  public void verifyRowDateTime(DateTimeFormatter actualDateTimeFormatter, LocalDateTime expectedRowValue,
                                Duration deltaDuration) {
    LocalDateUtils.dateTimeParse(this.getSelf().getText(), actualDateTimeFormatter).ifPresentOrElse(actual ->
            Assertions.assertThat(actual)
                .as("Date Time value incorrect")
                .isBetween(expectedRowValue.minus(deltaDuration.toMillis(), ChronoUnit.MILLIS),
                    expectedRowValue.plus(deltaDuration.toMillis(), ChronoUnit.MILLIS)),
        () -> Assertions.fail("""
            Date Time value incorrect. Failed on a parsing date and time.%n\
            Expected format: '%s'%n\
            Actual text: '%s'%n\
            """.formatted(actualDateTimeFormatter.toString(), this.getSelf().getText())));
  }

  /**
   * Verify row date value.
   *
   * @param actualDateTimeFormatter actual date time formatter
   * @param expectedRowValue        expectedRowValue row value
   */
  public void verifyRowDate(DateTimeFormatter actualDateTimeFormatter, LocalDate expectedRowValue) {
    LocalDateUtils.dateParse(this.getSelf().getText(), actualDateTimeFormatter).ifPresentOrElse(actual ->
            Assertions.assertThat(actual)
                .as("Date value incorrect")
                .isEqualTo(expectedRowValue),
        () -> Assertions.fail("""
            Date value incorrect. Failed on a parsing date.%n\
            Expected format: '%s'%n\
            Actual text: '%s'%n\
            """.formatted(actualDateTimeFormatter.toString(), this.getSelf().getText())));
  }

  /**
   * Click link in cell.
   */
  public void clickCellLink() {
    getCell().clickLink();
  }

  /**
   * Click clink in cell by given link title.
   *
   * @param linkTitle link title text
   */
  public void clickCellLink(String linkTitle) {
    getCell().clickLink(linkTitle);
  }

  /**
   * Get horizontal table cell.
   *
   * @return horizontal table {@link HorizontalCell} element.
   */
  public HorizontalCell getCell() {
    return new HorizontalCell(getSelf());
  }
}
