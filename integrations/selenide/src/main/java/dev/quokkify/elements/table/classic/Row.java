package dev.quokkify.elements.table.classic;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.function.Function;

import dev.quokkify.elements.table.classic.base.BaseRow;
import dev.quokkify.elements.table.model.CheckType;
import dev.quokkify.elements.table.model.RowData;
import dev.quokkify.html.model.HtmlTag;
import dev.quokkify.util.LocalDateUtils;

import com.codeborne.selenide.SelenideElement;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.openqa.selenium.By;

/**
 * Classic table row UI element and methods of working with it.
 */
public class Row<T extends Enum<T>> extends BaseRow<T> {

  private static final String VERIFY_CELL_ERROR_MESSAGE = "Cell value in '%s' column incorrect";
  private static final String VERIFY_CELL_LINK_ERROR_MESSAGE = "Cell link value in '%s' column incorrect";
  private final Function<T, Integer> fetchColumnIndex;

  public Row(SelenideElement element, Function<T, Integer> fetchColumnIndex) {
    super(element);
    this.fetchColumnIndex = fetchColumnIndex;
  }

  /**
   * Verify cell date time value.
   *
   * @param columnHeader            column enum
   * @param actualDateTimeFormatter actual date time formatter
   * @param expectedCellValue       expected cell value
   */
  public void verifyCellDateTime(T columnHeader, DateTimeFormatter actualDateTimeFormatter,
                                 LocalDateTime expectedCellValue) {
    LocalDateTime actual = LocalDateTime.parse(this.cellValue(columnHeader), actualDateTimeFormatter);
    Assertions.assertThat(actual).as("Date Time value incorrect")
        .isBetween(expectedCellValue.minusSeconds(62),
            expectedCellValue.plusSeconds(62));
  }

  /**
   * Verify cell date value.
   *
   * @param columnHeader            column enum
   * @param actualDateTimeFormatter actual date time formatter
   * @param expectedCellValue       expected cell value
   */
  public void verifyCellDate(T columnHeader, DateTimeFormatter actualDateTimeFormatter, LocalDate expectedCellValue) {
    verifyRow(RowData.<T>builder().add(columnHeader, expectedCellValue, actualDateTimeFormatter).build());
  }

  /**
   * Verify cell link value.
   *
   * @param columnHeader          column enum
   * @param expectedCellLinkValue expected cell link value
   */
  public void verifyCellLink(T columnHeader, String expectedCellLinkValue) {
    verifyRow(RowData.<T>builder().add(CheckType.LINK, columnHeader, expectedCellLinkValue).build());
  }

  /**
   * Verify cell value.
   *
   * @param columnHeader      column enum
   * @param expectedCellValue expected cell value
   */
  public void verifyCell(T columnHeader, String expectedCellValue) {
    verifyRow(RowData.<T>builder().add(CheckType.EQUALS, columnHeader, expectedCellValue).build());
  }

  /**
   * Verify cell contains value.
   *
   * @param columnHeader              column enum
   * @param expectedCellContainsValue expected cell contains value
   */
  public void verifyCellContains(T columnHeader, String expectedCellContainsValue) {
    verifyRow(RowData.<T>builder().add(CheckType.CONTAINS, columnHeader, expectedCellContainsValue).build());
  }

  /**
   * Verify cell starts with value.
   *
   * @param columnHeader                column enum
   * @param expectedCellStartsWithValue expected cell starts with value
   */
  public void verifyCellStartWith(T columnHeader, String expectedCellStartsWithValue) {
    verifyRow(RowData.<T>builder().add(CheckType.STARTS_WITH, columnHeader, expectedCellStartsWithValue).build());
  }

  /**
   * Verify cell ends with value.
   *
   * @param columnHeader              column enum
   * @param expectedCellEndsWithValue expected cell ends with value
   */
  public void verifyCellEndWith(T columnHeader, String expectedCellEndsWithValue) {
    verifyRow(RowData.<T>builder().add(CheckType.ENDS_WITH, columnHeader, expectedCellEndsWithValue).build());
  }

  /**
   * Verify cell empty value.
   *
   * @param columnHeader column enum
   */
  public void verifyEmptyCell(T columnHeader) {
    verifyRow(RowData.<T>builder().add(CheckType.EMPTY, columnHeader, StringUtils.EMPTY).build());
  }

  /**
   * Verify cell contains given pattern.
   *
   * @param columnHeader             column enum
   * @param expectedCellValuePattern expected cell value pattern
   */
  public void verifyCellContainsPattern(T columnHeader, String expectedCellValuePattern) {
    verifyRow(RowData.<T>builder().add(CheckType.PATTERN, columnHeader, expectedCellValuePattern).build());
  }

  /**
   * Verify table row by given values and {@link CheckType}.
   *
   * @param rowData list of expected values with {@link CheckType}
   */
  public void verifyRow(RowData<T> rowData) {
    SoftAssertions softAssertions = new SoftAssertions();
    rowData.getExpectedValues().forEach(expectedValue -> {
      switch (expectedValue.checkType()) {
        case EQUALS -> softAssertions.assertThat(this.cellValue(expectedValue.columnHeader()))
            .as(VERIFY_CELL_ERROR_MESSAGE, StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase()))
            .isEqualToIgnoringCase(expectedValue.cellValue());
        case CONTAINS -> softAssertions.assertThat(this.cellValue(expectedValue.columnHeader()))
            .as(VERIFY_CELL_ERROR_MESSAGE, StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase()))
            .containsIgnoringCase(expectedValue.cellValue());
        case STARTS_WITH -> softAssertions.assertThat(this.cellValue(expectedValue.columnHeader()))
            .as(VERIFY_CELL_ERROR_MESSAGE, StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase()))
            .startsWith(expectedValue.cellValue());
        case ENDS_WITH -> softAssertions.assertThat(this.cellValue(expectedValue.columnHeader()))
            .as(VERIFY_CELL_ERROR_MESSAGE, StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase()))
            .endsWith(expectedValue.cellValue());
        case EMPTY -> softAssertions.assertThat(this.cellValue(expectedValue.columnHeader()))
            .as("Cell value in '%s' column is not empty",
                StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase())).isEmpty();
        case PATTERN -> softAssertions.assertThat(this.cellValue(expectedValue.columnHeader()))
            .as("Cell value in '%s' column does not match '%s' pattern",
                StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase()),
                expectedValue.cellValue()).containsPattern(expectedValue.cellValue());
        case DATE -> {
          String text = this.cellValue(expectedValue.columnHeader());
          DateTimeFormatter dateTimeFormatter = expectedValue.dateTimeFormatter();
          LocalDateUtils.dateParse(text, dateTimeFormatter)
              .ifPresentOrElse(actual -> softAssertions.assertThat(actual).as("Date value incorrect")
                      .isEqualTo(expectedValue.cellLocalDateValue()),
                  () -> softAssertions.fail("""
                      Date value incorrect. Failed on a parsing date.%n\
                      Expected format: '%s'%n\
                      Actual text: '%s'%n\
                      """.formatted(dateTimeFormatter.toString(), text)));
        }
        case DATE_TIME -> {
          String text = this.cellValue(expectedValue.columnHeader());
          DateTimeFormatter dateTimeFormatter = expectedValue.dateTimeFormatter();
          LocalDateTime expectedLocalDateTimeValue = expectedValue.cellLocalDateTimeValue();
          LocalDateUtils.dateTimeParse(text, dateTimeFormatter)
              .ifPresentOrElse(actual -> softAssertions.assertThat(actual).as("Date Time value incorrect")
                      .isBetween(expectedLocalDateTimeValue.minusSeconds(62),
                          expectedLocalDateTimeValue.plusSeconds(62)),
                  () -> softAssertions.fail("""
                      Date Time value incorrect. Failed on a parsing date and time.%n\
                      Expected format: '%s'%n\
                      Actual text: '%s'%n\
                      """.formatted(dateTimeFormatter.toString(), text)));
        }
        case LINK -> softAssertions.assertThat(this.getCellLinkValue(expectedValue.columnHeader()))
            .as(VERIFY_CELL_LINK_ERROR_MESSAGE,
                StringUtils.capitalize(expectedValue.columnHeader().name().toLowerCase()))
            .isEqualToIgnoringCase(expectedValue.cellValue());
        default -> throw new IllegalArgumentException("Check type is unknown");
      }
    });
    softAssertions.assertAll();
  }

  /**
   * Verify table row by given map of values.
   *
   * @param expectedRowValues map: key - column, value - cell value
   * @deprecated please use {@link #verifyRow(RowData)}
   */
  @Deprecated(since = "0.4.31")
  public void verifyRow(Map<T, String> expectedRowValues) {
    SoftAssertions softAssertions = new SoftAssertions();
    expectedRowValues.forEach(
        (columnHeader, expectedCellValue) -> softAssertions.assertThat(this.cellValue(columnHeader))
            .as(VERIFY_CELL_ERROR_MESSAGE, StringUtils.capitalize(columnHeader.name().toLowerCase()))
            .isEqualToIgnoringCase(expectedCellValue));
    softAssertions.assertAll();
  }

  /**
   * Get cell value by given column.
   *
   * @param columnHeader column enum
   * @return cell value text
   */
  public String cellValue(T columnHeader) {
    return getCell(columnHeader).getSelf().getText();
  }

  /**
   * Get cell link value by given column.
   *
   * @param columnHeader column enum
   * @return cell value link text
   */
  public String getCellLinkValue(T columnHeader) {
    return getCell(columnHeader).getLinkValue();
  }

  /**
   * Click link in cell by given column.
   *
   * @param columnHeader column enum
   */
  public void clickCellLink(T columnHeader) {
    getCell(columnHeader).clickLink();
  }

  /**
   * Click link in cell by given column and link title or text.
   *
   * @param columnHeader    column enum
   * @param linkTitleOrText link title or text
   */
  public void clickCellLink(T columnHeader, String linkTitleOrText) {
    getCell(columnHeader).clickLink(linkTitleOrText);
  }

  /**
   * Click button in cell by given column and button title or text.
   *
   * @param columnHeader      column enum
   * @param buttonTitleOrText link title or text
   */
  public void clickCellButton(T columnHeader, String buttonTitleOrText) {
    getCell(columnHeader).clickButton(buttonTitleOrText);
  }

  /**
   * Select checkbox in cell by given column.
   *
   * @param columnHeader column enum
   */
  public void selectCellCheckbox(T columnHeader) {
    getCell(columnHeader).selectCheckbox();
  }

  /**
   * Get cell checkbox element.
   *
   * @param columnHeader column enum
   */
  public SelenideElement getCellCheckbox(T columnHeader) {
    return getCell(columnHeader).getCheckbox();
  }

  /**
   * Get table cell.
   *
   * @param columnHeader column enum
   * @return table {@link Cell} element.
   */
  public Cell<T> getCell(T columnHeader) {
    int cellIndex = fetchColumnIndex.apply(columnHeader) + HTML_START_INDEX;
    return new Cell<>(getSelf().find(By.xpath("./%s[%d]".formatted(HtmlTag.TD, cellIndex))), this);
  }
}
