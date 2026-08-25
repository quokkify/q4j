package dev.quokkify.elements.table.model;

/** Optional page metadata exposed by a paginated table. */
public record TablePageMetadata(int pageNumber, int totalPages) {
  public TablePageMetadata {
    if (pageNumber < 1) {
      throw new IllegalArgumentException("pageNumber must be positive: " + pageNumber);
    }
    if (totalPages < 1) {
      throw new IllegalArgumentException("totalPages must be positive: " + totalPages);
    }
    if (pageNumber > totalPages) {
      throw new IllegalArgumentException(
          "pageNumber must not exceed totalPages: " + pageNumber + ">" + totalPages);
    }
  }
}
