package dev.quokkify.elements.table.model;

import org.openqa.selenium.By;

/** Built-in and custom factories for table DOM adapters. */
public final class TableDomAdapters {

  private static final TableDomAdapter CLASSIC = of(
      By.xpath(".//tr[td]"),
      By.xpath("./td | ./th[@scope='row']"),
      new TableHeaderRowLocator(By.xpath(".//tr[th]"), By.xpath("./th")));
  private static final TableDomAdapter FLEX = of(
      By.cssSelector(".flex-table-row ~ .flex-table-row"),
      By.cssSelector(":scope > div"),
      new TableHeaderRowLocator(By.cssSelector(".flex-table-row"), By.cssSelector(":scope > div")));
  private static final TableDomAdapter HORIZONTAL = of(
      By.cssSelector("tr"),
      By.cssSelector(":scope > td"),
      new RowHeaderCellLocator(By.cssSelector(":scope > th")));
  private static final TableDomAdapter ARIA_GRID = of(
      By.xpath(".//*[@role='row' and ./*[@role='gridcell']]"),
      By.xpath("./*[@role='gridcell']"),
      new TableHeaderRowLocator(
          By.xpath(".//*[@role='row' and ./*[@role='columnheader']]"),
          By.xpath("./*[@role='columnheader']")));

  private TableDomAdapters() {
  }

  /** Adapter for a semantic HTML table with {@code thead} and {@code tbody}. */
  public static TableDomAdapter classic() {
    return CLASSIC;
  }

  /** Adapter for the legacy q4j flex-table markup. */
  public static TableDomAdapter flex() {
    return FLEX;
  }

  /** Adapter for an HTML table whose header is the first cell of every data row. */
  public static TableDomAdapter horizontal() {
    return HORIZONTAL;
  }

  /** Adapter for a root containing ARIA row, columnheader, and gridcell roles. */
  public static TableDomAdapter ariaGrid() {
    return ARIA_GRID;
  }

  /** Creates an adapter for custom markup, including div-based grids. */
  public static TableDomAdapter of(By mountedDataRowLocator, By dataCellLocator,
                                   TableHeaderLocator headerLocator) {
    return new TableDomAdapter(mountedDataRowLocator, dataCellLocator, headerLocator);
  }
}
