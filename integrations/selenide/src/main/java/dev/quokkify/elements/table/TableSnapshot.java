package dev.quokkify.elements.table;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Headers and rows of a {@link Table} read at one moment by {@link Table#snapshot()}. Plain immutable data: it does not
 * wait, re-read or follow the DOM. Headers and texts are whitespace-normalized like header lookup in {@link Table}.
 */
public final class TableSnapshot {

  private final List<String> headers;
  private final List<SnapshotRow> rows;
  private final String table;

  private TableSnapshot(List<String> headers, List<SnapshotRow> rows, String table) {
    this.headers = headers;
    this.rows = rows;
    this.table = table;
  }

  public List<String> headers() {
    return headers;
  }

  public List<SnapshotRow> rows() {
    return rows;
  }

  /**
   * Text of the column with the given header in every row; a row with too few cells contributes {@code ""}.
   *
   * @throws TableColumnException when the header is not displayed or is displayed more than once
   */
  public List<String> column(String header) {
    return mapColumn(header, cell -> Objects.isNull(cell) ? "" : cell.text());
  }

  /**
   * Items of the column with the given header in every row; a row with too few cells contributes an empty list.
   *
   * @throws TableColumnException when the header is not displayed or is displayed more than once
   */
  public List<List<String>> columnValues(String header) {
    return mapColumn(header, cell -> Objects.isNull(cell) ? List.of() : cell.values());
  }

  /**
   * Texts of the given columns, one list per row in row order; a row with too few cells contributes {@code ""} for
   * the missing columns.
   *
   * @throws TableColumnException when a header is not displayed or is displayed more than once
   */
  public List<List<String>> texts(List<String> headers) {
    List<Integer> columns = headers.stream()
        .map(header -> ColumnResolver.indexOf(this.headers, header, table)).toList();
    return rows.stream().map(row -> columns.stream().map(column -> {
      SnapshotCell cell = row.cellOrNull(column);
      return Objects.isNull(cell) ? "" : cell.text();
    }).toList()).toList();
  }

  /**
   * Text of column {@code header} keyed by the text of column {@code keyHeader}, in row order.
   *
   * @throws TableColumnException when a header is not displayed or is displayed more than once
   * @throws IllegalStateException when a row has no key cell or two rows have the same key
   */
  public Map<String, String> columnBy(String keyHeader, String header) {
    return keyed(keyHeader, column(header));
  }

  /**
   * Items of column {@code header} keyed by the text of column {@code keyHeader}, in row order. Built for comparing a
   * whole table with an expected {@code Map<key, List<item>>}; {@link Map#equals} ignores row order.
   *
   * @throws TableColumnException when a header is not displayed or is displayed more than once
   * @throws IllegalStateException when a row has no key cell or two rows have the same key
   */
  public Map<String, List<String>> columnValuesBy(String keyHeader, String header) {
    return keyed(keyHeader, columnValues(header));
  }

  private <T> Map<String, T> keyed(String keyHeader, List<T> values) {
    int keyColumn = ColumnResolver.indexOf(headers, keyHeader, table);
    Map<String, T> result = new LinkedHashMap<>();
    Map<String, Integer> seen = new HashMap<>();
    for (int i = 0; i < rows.size(); i++) {
      SnapshotCell key = rows.get(i).cellOrNull(keyColumn);
      if (Objects.isNull(key)) {
        throw new TableKeyException("row " + i + " has no cell for key column \"" + keyHeader + "\" in " + table);
      }
      Integer previous = seen.put(key.text(), i);
      if (Objects.nonNull(previous)) {
        throw new TableKeyException("rows " + previous + " and " + i + " both have \"" + key.text()
            + "\" in key column \"" + keyHeader + "\" of " + table);
      }
      result.put(key.text(), values.get(i));
    }
    return Collections.unmodifiableMap(result);
  }

  private <T> List<T> mapColumn(String header, Function<SnapshotCell, T> mapper) {
    int column = ColumnResolver.indexOf(headers, header, table);
    return rows.stream().map(row -> mapper.apply(row.cellOrNull(column))).toList();
  }

  static TableSnapshot fromScript(Map<?, ?> result, String table) {
    if (Objects.isNull(result)) {
      throw new IllegalStateException("snapshot script returned no result for " + table);
    }
    List<String> headers = ColumnResolver.normalize(strings(result.get("headers"), "headers"));
    List<SnapshotRow> rows = new ArrayList<>();
    for (Object raw : list(result.get("rows"), "rows")) {
      rows.add(row(rows.size(), (Map<?, ?>) raw, headers, table));
    }
    return new TableSnapshot(headers, List.copyOf(rows), table);
  }

  private static SnapshotRow row(int index, Map<?, ?> raw, List<String> headers, String table) {
    List<String> texts = strings(raw.get("texts"), "texts");
    Object rawValues = raw.get("values");
    List<?> values = Objects.isNull(rawValues) ? null : list(rawValues, "values");
    List<SnapshotCell> cells = new ArrayList<>(texts.size());
    for (int i = 0; i < texts.size(); i++) {
      List<String> items = Objects.isNull(values) ? List.of()
          : ColumnResolver.normalize(strings(values.get(i), "values"));
      cells.add(new SnapshotCell(ColumnResolver.normalize(texts.get(i)), items));
    }
    return new SnapshotRow(index, cells, headers, table);
  }

  private static List<?> list(Object value, String name) {
    if (value instanceof List<?> list) {
      return list;
    }
    throw new IllegalStateException("snapshot script returned no list for \"" + name + "\": " + value);
  }

  private static List<String> strings(Object value, String name) {
    return list(value, name).stream().map(item -> Objects.isNull(item) ? "" : item.toString()).toList();
  }
}
