package dev.quokkify.model;

import java.util.Arrays;
import java.util.List;

import dev.quokkify.constant.StringConstant;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.CaseUtils;

public interface ConstantFormat {

  String formatValue();

  default String value() {
    return lowerCase();
  }

  /**
   * Converts a string where all words start with a capital letter separated by a space.
   *
   * <pre>
   * "first".capitalize()      = "First"
   * "NAME".capitalize()       = "Name"
   * "FIrsT_nAMe".capitalize() = "First Name"
   * </pre>
   *
   * @return converted String
   */
  default String capitalize() {
    List<String> value = Arrays.stream(lowerCase().split(StringConstant.UNDERSCORE))
        .map(StringUtils::capitalize)
        .toList();
    return String.join(StringUtils.SPACE, value);
  }

  /**
   * Converts a string where the first word starts with a capital letter.
   *
   * <pre>
   * "first".capitalizeFirstWord()      = "First"
   * "NAME".capitalizeFirstWord()       = "Name"
   * "FIrsT_nAMe".capitalizeFirstWord() = "First name"
   * </pre>
   *
   * @return converted String
   */
  default String capitalizeFirstWord() {
    return StringUtils.capitalize(lowerCase().replace(StringConstant.UNDERSCORE, StringUtils.SPACE));
  }

  /**
   * Converts a string where all words are in lowercase separated by spaces.
   *
   * <pre>
   * "first".lowerCaseWithSpace()      = first
   * "NAME".lowerCaseWithSpace()       = name
   * "FIrsT_nAMe".lowerCaseWithSpace() = first name
   * </pre>
   *
   * @return converted String
   */
  default String lowerCaseWithSpace() {
    return String.join(StringUtils.SPACE, lowerCase().split(StringConstant.UNDERSCORE));
  }

  /**
   * Converts a string where all words are in lowercase separated by dashes.
   *
   * <pre>
   * "first".lowerCaseWithDash()      = first
   * "NAME".lowerCaseWithDash()       = name
   * "FIrsT_nAMe".lowerCaseWithDash() = first-name
   * </pre>
   *
   * @return converted String
   */
  default String lowerCaseWithDash() {
    return String.join(StringConstant.DASH, lowerCase().split(StringConstant.UNDERSCORE));
  }

  default String lowerCase() {
    return formatValue().toLowerCase();
  }

  /**
   * Converts a string where all words are in uppercase separated by spaces.
   *
   * <pre>
   * "first".upperCaseWithSpace()      = FIRST
   * "NAME".upperCaseWithSpace()       = NAME
   * "FIrsT_nAMe".upperCaseWithSpace() = FIRST NAME
   * </pre>
   *
   * @return converted String
   */
  default String upperCaseWithSpace() {
    return String.join(StringUtils.SPACE, upperCase().split(StringConstant.UNDERSCORE));
  }

  default String upperCase() {
    return formatValue().toUpperCase();
  }

  /**
   * Converts all the delimiter (underscore) separated words in a String into UpperCamelCase (Pascal case).
   *
   * <pre>
   * "first_name".upperCamelCase() = FirstName
   * "NAME_FIRST".upperCamelCase() = NameFirst
   * "FIrsT_nAMe".upperCamelCase() = FirstName
   * </pre>
   *
   * @return converted String
   */
  default String upperCamelCase() {
    return CaseUtils.toCamelCase(formatValue(), true, StringConstant.UNDERSCORE.toCharArray());
  }

  /**
   * Converts all the delimiter (underscore) separated words in a String into camelCase.
   *
   * <pre>
   * "first_name".camelCase() = firstName
   * "NAME_FIRST".camelCase() = nameFirst
   * "FIrsT_nAMe".camelCase() = firstName
   * </pre>
   *
   * @return converted String
   */
  default String camelCase() {
    return CaseUtils.toCamelCase(formatValue(), false, StringConstant.UNDERSCORE.toCharArray());
  }

  /**
   * Converts a string where all words are in uppercase separated by dashes.
   *
   * <pre>
   * "first".upperCaseWithDash()      = FIRST
   * "NAME".upperCaseWithDash()       = NAME
   * "FIrsT_nAMe".upperCaseWithDash() = FIRST-NAME
   * </pre>
   *
   * @return converted String
   */
  default String upperCaseWithDash() {
    return String.join(StringConstant.DASH, upperCase().split(StringConstant.UNDERSCORE));
  }
}
