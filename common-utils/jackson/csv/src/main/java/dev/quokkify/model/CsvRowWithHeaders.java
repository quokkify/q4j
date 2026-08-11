package dev.quokkify.model;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Used for java beans parsing to csv files with headers.
 */
public interface CsvRowWithHeaders<T extends CsvRowWithHeaders.Header> extends CsvRow {

  List<T> headers();

  default List<String> getHeadersValues() {
    return headers().stream().map(Header::title)
        .collect(Collectors.toList());
  }

  /**
   * Used for implemented in java csv row headers.
   */
  interface Header extends ConstantFormat {

    String title();
  }
}
