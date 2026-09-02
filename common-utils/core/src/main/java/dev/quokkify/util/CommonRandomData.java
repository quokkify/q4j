package dev.quokkify.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import dev.quokkify.constant.DateFormat;
import dev.quokkify.constant.StringConstant;
import dev.quokkify.formatter.LocalDateFormatter;
import dev.quokkify.spi.LocaleProviders;

import net.datafaker.Faker;
import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for generating various types of random data.
 *
 * <p>This class provides a collection of static methods to generate random strings,
 * numbers, dates, and other data types. While some methods leverage the Faker
 * library for generating realistic data, others provide custom implementations.</p>
 */
public class CommonRandomData {

  protected static final Faker FAKER = new Faker(LocaleProviders.get());
  private static final AtomicLong UNIQUE_LONG = new AtomicLong();
  protected static final String TEMPLATE_EMAIL = "%s.%s@%s";
  protected static final String TEMPLATE_WITH_DOT = "%s.%s";
  protected static final String TEMPLATE_WITH_SPACE = "%s %s";

  /**
   * Get a random element from the array, excluding the specified element.
   *
   * <p>This method selects a random element from the provided array, ensuring that
   * the returned value is not equal to the `exceptValue`.</p>
   *
   * @param <T>         the type of elements in the array
   * @param array       the array of elements to choose from
   * @param exceptValue the value to exclude from the selection
   * @return a random element from the array that is not equal to `exceptValue`
   */
  public static <T> T getRandomExcept(T[] array, T exceptValue) {
    return getRandomExcept(List.of(array), List.of(exceptValue));
  }

  /**
   * Get a random element from the array, excluding the specified elements.
   *
   * <p>This method selects a random element from the provided array, ensuring that
   * the returned value is not present in the `exceptValues` list.</p>
   *
   * @param <T>          the type of elements in the array
   * @param array        the array of elements to choose from
   * @param exceptValues the values to exclude from the selection
   * @return a random element from the array that is not present in `exceptValues`
   */
  public static <T> T getRandomExcept(T[] array, List<T> exceptValues) {
    return getRandomExcept(List.of(array), exceptValues);
  }

  /**
   * Returns a random element from the list, excluding the specified elements.
   *
   * <p>This method selects a random element from the provided list, ensuring that
   * the returned value is not present in the `exceptValues` list.</p>
   *
   * @param <T>          the type of elements in the array
   * @param list         the list of elements to choose from
   * @param exceptValues the values to exclude from the selection
   * @return a random element from the list that is not present in `exceptValues`
   */
  public static <T> T getRandomExcept(List<T> list, List<T> exceptValues) {
    List<T> filteredValues = list.stream()
        .filter(value -> !exceptValues.contains(value))
        .collect(Collectors.toList());
    return getRandom(filteredValues);
  }

  /**
   * Returns a random element from the given array.
   *
   * <p>This method selects a random element from the provided array. The element
   * is chosen with an equal probability for each element in the array.</p>
   *
   * @param <T>   the type of elements in the array
   * @param array the array to choose a random element from
   * @return a random element from the array
   */
  public static <T> T getRandom(T[] array) {
    return getRandom(List.of(array));
  }

  /**
   * Returns a random element from the given list.
   *
   * <p>This method selects a random element from the provided list. The element
   * is chosen with an equal probability for each element in the list.</p>
   *
   * @param <T>  the type of elements in the list
   * @param list the list to choose a random element from
   * @return a random element from the list
   */
  public static <T> T getRandom(List<T> list) {
    return list.get(randomInteger(list.size()));
  }

  /**
   * Returns a list containing a specified number of random elements from the given array.
   *
   * <p>This method selects `countOfValues` random elements from the provided array
   * with no duplicates. The elements are chosen with an equal probability for each
   * element in the array.</p>
   *
   * @param <T>           the type of elements in the array
   * @param array         the array to choose random elements from
   * @param countOfValues the number of random elements to return
   * @return a list containing `countOfValues` random elements from the array, without duplicates.
   */
  public static <T> List<T> getRandomValues(T[] array, int countOfValues) {
    return getRandomValues(Arrays.stream(array).toList(), countOfValues);
  }

  /**
   * Returns a random sample of elements from the given list.
   *
   * <p>This method selects `countOfValues` random elements from the provided list
   * with no duplicates. The elements are chosen with an equal probability for each
   * element in the array.</p>
   *
   * @param <T>           the type of elements in the list
   * @param list          the input list
   * @param countOfValues the desired number of elements in the returned list
   * @return a list containing a random sample of the input list
   */
  public static <T> List<T> getRandomValues(List<T> list, int countOfValues) {
    if (countOfValues > list.size()) {
      throw new RuntimeException(
          "Count of values equals %d, it should be in the range [0 ; inputList.size()]".formatted(countOfValues));
    }
    List<T> copyOfInputList = new ArrayList<>(list);
    IntStream.range(0, list.size() - countOfValues).forEach(i -> copyOfInputList.remove(getRandom(copyOfInputList)));
    return copyOfInputList;
  }

  /**
   * Generates a random MD5 hash string.
   *
   * @return a randomly generated MD5 hash string
   */
  public static String randomMd5Hash() {
    return FAKER.hashing().md5();
  }

  /**
   * Generates a random SHA-512 hash string.
   *
   * @return a randomly generated SHA-512 hash string
   */
  public static String randomSha512() {
    return FAKER.hashing().sha512();
  }

  /**
   * Generates a random UUID (Universally Unique Identifier) version 7.
   *
   * @return a randomly generated UUID version 7
   */
  public static UUID uuid() {
    return UUID.fromString(FAKER.internet().uuidv7());
  }

  /**
   * Generates a random boolean value (true or false).
   *
   * @return a randomly generated boolean value
   */
  public static boolean bool() {
    return FAKER.bool().bool();
  }

  /**
   * Get the random strictly positive digit (including): 1 - 9.
   *
   * <pre>
   * randomDigit()  = 1-9
   * </pre>
   *
   * @return the random strictly positive digit
   */
  public static Integer digit() {
    return randomPositiveIntegerInclude(9);
  }

  /**
   * Get the random string of digits.
   *
   * <pre>
   * digit(1)  = 0-9
   * digit(2)  = '03'
   * </pre>
   *
   * @param digitsCount the number of digits the generated value should have
   * @return the random string of digits.
   */
  public static String digit(int digitsCount) {
    return FAKER.number().digits(digitsCount);
  }

  /**
   * Get the random int percent (include): 1 - 99.
   *
   * <pre>
   * randomIntPercent()  = 1-99
   * </pre>
   *
   * @return the random int percent.
   */
  public static int randomPositiveIntPercent() {
    return betweenIntegerInclude(1, 99);
  }

  /**
   * Get the random double percent.
   *
   * <pre>
   * randomDoublePercent()  = 0.01-99.99
   * randomDoublePercent()  = 2.1
   * randomDoublePercent()  = 21.36
   * </pre>
   *
   * @return the random double percent.
   */
  public static double randomDoublePercent() {
    return (double) betweenIntegerInclude(1, 9999) / 100;
  }

  /**
   * Get the random coefficient.
   *
   * <pre>
   * randomCoefficient()  = 0.01-0.99
   * randomCoefficient()  = 0.2
   * randomCoefficient()  = 0.14
   * </pre>
   *
   * @return the random coefficient.
   */
  public static double randomCoefficient() {
    return FAKER.number().randomDouble(2, 0, 1);
  }

  /**
   * Get the random Integer: -2_147_483_648 - 2_147_483_648 (not include).
   *
   * <pre>
   * randomInteger()  = -131981
   * randomInteger()  = 1068841439
   * </pre>
   *
   * @return the random Integer
   */
  public static int randomInteger() {
    return FAKER.random().nextInt();
  }

  /**
   * Get the random Integer: 0 - max (not include).
   * <br>It is used for getting index of element in the list.
   *
   * <pre>
   * randomInteger(10)  = 0-9
   * </pre>
   *
   * @param max the upper bound (not include max).
   * @return a random Integer between 0 and max
   */
  public static int randomInteger(int max) {
    return FAKER.random().nextInt(max);
  }

  /**
   * Get the random Integer (include): 0 - max.
   *
   * <pre>
   * randomIntegerInclude(10)  = 0-10
   * </pre>
   *
   * @param max the upper bound (include max).
   * @return a random Integer between 0 and max
   */
  public static int randomIntegerInclude(int max) {
    return FAKER.random().nextInt(0, max);
  }

  /**
   * Get the random strictly positive Integer (include): 1 - max.
   *
   * <pre>
   * randomPositiveIntegerInclude(10)  = 1-10
   * </pre>
   *
   * @param max the upper bound (include max).
   * @return a random strictly positive Integer between 1 and max
   */
  public static int randomPositiveIntegerInclude(int max) {
    return FAKER.random().nextInt(1, max);
  }

  /**
   * Get the random Integer between min and max: min - max (not include).
   *
   * <pre>
   * betweenInteger(0, 10)  = 0-9
   * </pre>
   *
   * @param min the lower bound (include min).
   * @param max the upper bound (not include max).
   * @return a random Integer between min and max
   */
  public static int betweenInteger(int min, int max) {
    return betweenIntegerInclude(min, max - 1);
  }

  /**
   * Get the random Integer between min and max (include): min - max.
   *
   * <pre>
   * betweenIntegerInclude(0, 10)  = 0-10
   * </pre>
   *
   * @param min the lower bound (include min).
   * @param max the upper bound (include max).
   * @return a random Integer between min and max
   */
  public static int betweenIntegerInclude(int min, int max) {
    return FAKER.random().nextInt(min, max);
  }

  /**
   * Get the current Unix timestamp as a Long.
   *
   * <pre>
   * uniqLong()  = 1721942536302
   * </pre>
   *
   * @return the current Unix timestamp as a Long
   */
  public static long uniqLong() {
    return UNIQUE_LONG.updateAndGet(previous -> Math.max(previous + 1, System.currentTimeMillis()));
  }

  /**
   * Get the random Long: 0 - 10_000_000_000 (not include).
   *
   * <pre>
   * randomLong()  = 0
   * randomLong()  = 453
   * </pre>
   *
   * @return the random Long
   */
  public static long randomLong() {
    return FAKER.number().randomNumber();
  }

  /**
   * Get the random Long: 0 - max (not include).
   *
   * <pre>
   * randomLong(10)  = 0-9
   * </pre>
   *
   * @param max the upper bound (not include max).
   * @return a random Long between 0 and max
   */
  public static long randomLong(long max) {
    return FAKER.random().nextLong(0, max);
  }

  /**
   * Get the random Long with some digits: 0 (not include) - 10_000_000_000 (not include).
   *
   * <pre>
   * randomLongWithDigits(1)  = 1-9
   * randomLongWithDigits(2)  = 10-99
   * </pre>
   *
   * @return the random Long
   */
  public static long randomLongWithDigits(int numberOfDigits) {
    return FAKER.number().randomNumber(numberOfDigits);
  }

  /**
   * Get the random Long between min and max: min - max (not include).
   *
   * <pre>
   * betweenLong(0, 10)  = 0-9
   * </pre>
   *
   * @param min the lower bound (include min).
   * @param max the upper bound (not include max).
   * @return a random Long between min and max
   */
  public static long betweenLong(long min, long max) {
    return FAKER.number().numberBetween(min, max);
  }

  /**
   * Get the random Double (not include): 0.0 - 1.0.
   *
   * <pre>
   * randomDouble()  = 0.860991183892
   * randomDouble()  = 1.0848553339914968E-4
   * </pre>
   *
   * @return the random Double
   */
  public static double randomDouble() {
    return FAKER.random().nextDouble();
  }

  /**
   * Get the random Double between min and max: min - max (not include).
   *
   * <pre>
   * betweenDouble(0, 10)  = 0.0-10.0
   * betweenDouble(0, 10)  = 0.7431457783190218E-4
   * betweenDouble(0, 10)  = 3.7317542678082
   * betweenDouble(0, 10)  = 5.14477014897738982
   * </pre>
   *
   * @param min the lower bound (include min).
   * @param max the upper bound (not include max).
   * @return a random Double between min and max with scale 2
   */
  public static double betweenDouble(double min, double max) {
    return FAKER.random().nextDouble(min, max);
  }

  /**
   * Get the random Double between min and max with scale 2 (include): min - max.
   *
   * <pre>
   * betweenDoubleInclude(0, 10)  = 0.0-10.0
   * betweenDoubleInclude(0, 10)  = 1.32
   * </pre>
   *
   * @param min the lower bound (include min).
   * @param max the upper bound (include max).
   * @return a random Double between min and max with scale 2
   */
  public static double betweenDoubleInclude(double min, double max) {
    return betweenDoubleInclude(min, max, 2);
  }

  /**
   * Get the random Double between min and max with scale (include): min - max.
   * <br>The rounding mode to apply is HALF_UP {@link RoundingMode#HALF_UP}.
   *
   * <pre>
   * betweenDoubleInclude(0, 10, 2)  = 0.0-10.0
   * betweenDoubleInclude(0, 10, 2)  = 1.32
   * </pre>
   *
   * @param min the lower bound (include min).
   * @param max the upper bound (include max).
   * @return a random Double between min and max with scale
   */
  public static double betweenDoubleInclude(double min, double max, int scale) {
    BigDecimal bigDecimal = new BigDecimal(Double.toString(min + Math.random() * (max - min)));
    bigDecimal = bigDecimal.setScale(scale, RoundingMode.HALF_UP);
    return bigDecimal.doubleValue();
  }

  /**
   * Get the random String with numbers and letters without any special symbols.
   *
   * @param fixedNumberOfCharacters the number of characters
   * @return the random String
   */
  public static String characters(int fixedNumberOfCharacters) {
    return FAKER.lorem().characters(fixedNumberOfCharacters);
  }

  /**
   * Get the random special character.
   *
   * @return the random special character
   */
  public static String randomSpecialCharacter() {
    return getRandom(List.of(
        StringConstant.EXCLAMATION_POINT,
        StringConstant.AT,
        StringConstant.SHARP,
        StringConstant.DOLLAR,
        StringConstant.PERCENT,
        StringConstant.CARET,
        StringConstant.AMPERSAND,
        StringConstant.ASTERISK
    ));
  }

  /**
   * Get the random a lowercase string of 20 to 80 characters long.
   *
   * @return the random a lowercase string
   */
  public static String randomText() {
    return FAKER.text().text();
  }

  /**
   * Can be 'Ansel Adams 5avg1d'.
   */
  public static String internalId() {
    return TEMPLATE_WITH_SPACE.formatted(FAKER.artist().name(), FAKER.lorem().characters(6));
  }

  /**
   * Generates a random alphanumeric string that resembles an ASIN (Amazon Standard Identification Number).
   *
   * @return a random alphanumeric string resembling an ASIN
   */
  public static String code() {
    return FAKER.code().asin();
  }

  /**
   * Generates a random password without special characters.
   *
   * @return a random password without special characters
   */
  public static String passwordWithoutSpecial() {
    return password(false);
  }

  /**
   * Generates a random password with a minimum length of 9 characters and a maximum length of 13 characters
   * and '!' symbol in the end.
   * The password will include uppercase and lowercase letters, digits, and a special character.
   *
   * @return a random password with a special character
   */
  public static String password() {
    return StringUtils.join(password(true), StringConstant.EXCLAMATION_POINT);
  }

  private static String password(boolean special) {
    return FAKER.credentials().password(8, 12, true, special);
  }

  /**
   * Generates a random email address with a username and a custom domain.
   *
   * <pre>
   * email('gmail.com') = 58493107.Lucrecia@gmail.com
   * </pre>
   *
   * @param domain the domain name for the email address
   * @return a random email address with the specified domain
   */
  public static String email(String domain) {
    return TEMPLATE_EMAIL.formatted(FAKER.number().randomNumber(8), FAKER.name().firstName(), domain);
  }

  /**
   * Generates a random email address with a random user name and a random domain name.
   *
   * <pre>
   * email() = wo27tc.hertha.zulauf@gmail.com
   * </pre>
   *
   * @return a random email address with a random domain name
   */
  public static String email() {
    return TEMPLATE_WITH_DOT.formatted(characters(6), FAKER.internet().emailAddress());
  }

  /**
   * Generates a random city name.
   *
   * @return a random city name
   */
  public static String city() {
    return FAKER.address().city();
  }

  /**
   * Generates a unique first name.
   *
   * <p>This method appends a random sequence of 8 letters to the end of a randomly generated first name,
   * creating a unique identifier.</p>
   *
   * <pre>
   * uniqueFirstName() = Dorseyxgonrrmr
   * </pre>
   *
   * @return a unique first name with a random 8-letter suffix
   */
  public static String uniqueFirstName() {
    return uniqueFirstName(8);
  }

  /**
   * Generates a unique first name with a random letter sequence appended.
   *
   * <pre>
   * uniqueFirstName(8) = Brittanysiqhsstk
   * </pre>
   *
   * @param letterCountInEnd the number of random letters to append to the first name
   * @return a unique first name with a random letter sequence
   */
  public static String uniqueFirstName(int letterCountInEnd) {
    return StringUtils.join(
        firstName(),
        FAKER.letterify(StringConstant.QUESTION_MARK.repeat(letterCountInEnd))
    );
  }

  /**
   * Generates a random first name.
   *
   * @return a random first name
   */
  public static String firstName() {
    return FAKER.name().firstName();
  }

  /**
   * Generates a unique last name.
   *
   * <p>This method appends a random sequence of 8 letters to the end of a randomly generated last name,
   * creating a unique identifier.</p>
   *
   * <pre>
   * uniqueLastName() = McGlynndonefiov
   * </pre>
   *
   * @return a unique first name with a random 8-letter suffix
   */
  public static String uniqueLastName() {
    return uniqueLastName(8);
  }

  /**
   * Generates a unique last name with a random letter sequence appended.
   *
   * <pre>
   * uniqueLastName(8) = Bergstromatgpclhl
   * </pre>
   *
   * @param letterCountInEnd the number of random letters to append to the last name
   * @return a unique last name with a random letter sequence
   */
  public static String uniqueLastName(int letterCountInEnd) {
    return StringUtils.join(
        lastName(),
        FAKER.letterify(StringConstant.QUESTION_MARK.repeat(letterCountInEnd))
    );
  }

  /**
   * Generates a random last name.
   *
   * @return a random last name
   */
  public static String lastName() {
    return FAKER.name().lastName();
  }

  /**
   * Generates a full name by combining a random first name and a random last name.
   *
   * @return a full name
   */
  public static String fullName() {
    return String.join(StringUtils.SPACE, firstName(), lastName());
  }

  /**
   * Generates a random postal code.
   *
   * @return a random postal code
   */
  public static String postalCode() {
    return FAKER.address().zipCode();
  }

  /**
   * Generates a random street address.
   *
   * @return a random street address
   */
  public static String address() {
    return FAKER.address().streetAddress();
  }

  /**
   * Generates a random secondary address (e.g., apartment number).
   *
   * <pre>
   * address2() = Suite 380
   * </pre>
   *
   * @return a random secondary address
   */
  public static String address2() {
    return FAKER.address().secondaryAddress();
  }

  /**
   * Generates a random title.
   *
   * <pre>
   * title() = Central Assurance Architect
   * </pre>
   *
   * @return a random title
   */
  public static String title() {
    return FAKER.name().title();
  }

  /**
   * Generates a random username suitable for Skype.
   *
   * @return a random username
   */
  public static String skype() {
    return FAKER.credentials().username();
  }

  /**
   * Generates a random nickname.
   *
   * <pre>
   * nickname() = Robin Money
   * </pre>
   *
   * @return a random nickname
   */
  public static String nickname() {
    return FAKER.funnyName().name();
  }

  /**
   * Generates a random nationality.
   *
   * <pre>
   * nationality() = Sierra Leoneans
   * </pre>
   *
   * @return a random nationality
   */
  public static String nationality() {
    return FAKER.nation().nationality();
  }

  /**
   * Generates a random date of birth for an adult between 21 and 65 years old (inclusive).
   * The format of the returned date string is YYYY-MM-DD.
   *
   * @return a random date of birth for an adult
   */
  public static String dateOfBirth() {
    return dateOfBirth(21, 65);
  }

  /**
   * Generates a random date of birth for a person between the specified minimum and maximum age (inclusive).
   * The format of the returned date string is YYYY-MM-DD.
   *
   * @param minAge the minimum age (inclusive)
   * @param maxAge the maximum age (inclusive)
   * @return a random date of birth within the specified age range
   */
  public static String dateOfBirth(int minAge, int maxAge) {
    return LocalDateFormatter.format(
        FAKER.timeAndDate().birthday(minAge, maxAge),
        DateFormat.YYYY_MM_DD
    );
  }

  /**
   * Generates a random IPv4 address.
   *
   * @return a random IPv4 address
   */
  public static String ip() {
    return FAKER.internet().ipV4Address();
  }
}
