package dev.quokkify.model;

/**
 * Class representing a string-based format that implements the {@link ConstantFormat} interface.
 * This class provides a string value as a format and allows various string transformations,
 * such as capitalization, camel case conversion, and other formatting utilities,
 * by leveraging the default methods in the {@link ConstantFormat} interface.
 *
 * <p>The format value is supplied during instantiation and can be retrieved using {@link #formatValue ()}.</p>
 *
 * <p>Example usage:</p>
 * <pre>
 *     StringFormat stringFormat = new StringFormat("FIRST_NAME");
 *     String formattedValue = stringFormat.upperCamelCase();  // Returns "FirstName"
 *     String lowerCaseValue = stringFormat.lowerCaseWithSpace();  // Returns "first name"
 * </pre>
 */
public record StringFormat(String formatValue) implements ConstantFormat {

}
