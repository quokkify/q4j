package dev.quokkify.elements.table.model;

/**
 * Describes how a table exposes displayed headers.
 *
 * <p>Use one of the immutable implementations supplied by this package.</p>
 */
public sealed interface TableHeaderLocator
    permits NoTableHeaders, RowHeaderCellLocator, TableHeaderRowLocator {
}
