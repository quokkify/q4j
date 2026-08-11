package dev.quokkify.util;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.stream.Stream;

import dev.quokkify.generator.LocalDateGenerator;
import dev.quokkify.generator.LocalDateTimeGenerator;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

public class LocalDateUtils {

  private static final ZoneId DEFAULT_ZONE = ZoneId.of("Etc/UTC");
  private static final LocalTime DEFAULT_LOCAL_TIME = LocalTime.MIDNIGHT;
  private static final ZoneOffset DEFAULT_ZONE_OFFSET = DEFAULT_ZONE.getRules()
      .getOffset(LocalDateTimeGenerator.generateNow());

  private LocalDateUtils() {
  }

  /**
   * Convert local date time to Unix time.
   *
   * @param localDateTime local date time to evaluate unix time
   * @return amount of seconds as {@link Long}
   */
  public static long convertToUnix(LocalDateTime localDateTime) {
    return localDateTime.atZone(DEFAULT_ZONE).toInstant().getEpochSecond();
  }

  /**
   * Convert local date to Unix time.
   *
   * @param localDate local date to evaluate unix time
   * @return amount of seconds as {@link Long}
   */
  public static long convertToUnix(LocalDate localDate) {
    return localDate.toEpochSecond(DEFAULT_LOCAL_TIME, DEFAULT_ZONE_OFFSET);
  }

  /**
   * Convert Unix to local date.
   *
   * @param unixTime unix time seconds as {@link Long}
   * @return date as {@link LocalDateTime}
   */
  public static LocalDateTime convertToLocalDateTime(long unixTime) {
    Instant instant = Instant.ofEpochSecond(unixTime);
    return instant.atZone(DEFAULT_ZONE).toLocalDateTime();
  }

  /**
   * Converts the given number of days into seconds.
   *
   * @param days number of days to be converted into seconds.
   * @return number of seconds corresponding to the given number of days as {@link Long}.
   */
  public static long convertDaysToSeconds(Long days) {
    return Duration.ofDays(days).getSeconds();
  }

  /**
   * Checks if the given {@link LocalDateTime} is earlier than the specified hour.
   *
   * @param localDateTime local date time to be checked as {@link LocalDateTime}.
   * @param hour          hour to compare against.
   * @return {@code true} if the time is earlier than the given hour, otherwise {@code false}.
   */
  public static boolean isEarlierThanHour(LocalDateTime localDateTime, int hour) {
    return localDateTime.toLocalTime().isBefore(LocalTime.of(hour, NumberUtils.INTEGER_ZERO));
  }

  /**
   * Get current age in specific date.
   *
   * @param localDate date to evaluate current age
   * @return age amount as {@link Integer}
   */
  public static int getCurrentAge(LocalDate localDate) {
    return Period.between(localDate, LocalDateGenerator.generateNow()).getYears();
  }

  /**
   * Get LocalDates delta with unit as string.
   *
   * @param firstDateTime  first date to evaluate delta unit string
   * @param secondDateTime second date to evaluate delta unit string
   * @return delta unit as {@link String}
   */
  public static String getDeltaWithUnitBetweenLocalDates(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
    ChronoUnit chronoUnit = getDeltaUnitBetweenLocalDates(firstDateTime, secondDateTime);
    return getDeltaWithUnitBetweenLocalDates(firstDateTime, secondDateTime, chronoUnit);
  }

  /**
   * Get LocalDates delta with unit string.
   *
   * <p>EXAMPLE: 24h - 23h -> 1 hour; 1d - 3d -> 2 days; 5m - 5m -> 0 month</p>
   *
   * @param firstDateTime  first date to evaluate delta unit string
   * @param secondDateTime second date to evaluate delta unit string
   * @param unit           date unit to evaluate delta unit string
   * @return delta unit as {@link String}
   */
  private static String getDeltaWithUnitBetweenLocalDates(LocalDateTime firstDateTime, LocalDateTime secondDateTime,
                                                          ChronoUnit unit) {
    long deltaAmount = getDeltaBetweenLocalDates(firstDateTime, secondDateTime, unit);
    String unitName = deltaAmount == 1
        ? StringUtils.chop(unit.name().toLowerCase())
        : unit.name().toLowerCase();
    return "%d %s".formatted(deltaAmount, unitName);
  }

  /**
   * Get LocalDates delta unit.
   *
   * @param firstDateTime  first date to evaluate delta unit
   * @param secondDateTime second date to evaluate delta unit
   * @return delta unit as {@link ChronoUnit}
   */
  public static ChronoUnit getDeltaUnitBetweenLocalDates(LocalDateTime firstDateTime, LocalDateTime secondDateTime) {
    return Stream.of(ChronoUnit.YEARS, ChronoUnit.MONTHS, ChronoUnit.DAYS, ChronoUnit.HOURS, ChronoUnit.MINUTES,
            ChronoUnit.SECONDS, ChronoUnit.MILLIS)
        .filter(unit -> isLocalDatesDeltaContainsUnit(firstDateTime, secondDateTime, unit))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Can not find any unit for current dates delta"));
  }

  /**
   * Get LocalDates delta amount for specific unit.
   *
   * @param firstDateTime  first date to evaluate delta amount
   * @param secondDateTime second date to evaluate delta amount
   * @param unit           date unit to evaluate delta amount
   * @return delta amount as {@link Long}
   */
  public static Long getDeltaBetweenLocalDates(LocalDateTime firstDateTime, LocalDateTime secondDateTime,
                                               ChronoUnit unit) {
    return Math.abs(unit.between(firstDateTime, secondDateTime));
  }

  /**
   * Get LocalDates delta relating state to unit.
   *
   * @param firstDateTime  first date to evaluate state
   * @param secondDateTime second date to evaluate state
   * @param unit           date unit to evaluate state
   * @return true if dates contains unit, false if not
   */
  private static boolean isLocalDatesDeltaContainsUnit(LocalDateTime firstDateTime, LocalDateTime secondDateTime,
                                                       ChronoUnit unit) {
    return Math.abs(unit.between(firstDateTime, secondDateTime)) > 0;
  }

  /**
   * Attempts to parse a {@link CharSequence} into a {@link LocalDate} using the provided {@link DateTimeFormatter}.
   *
   * @param text      text to be parsed into a {@link LocalDate}.
   * @param formatter parsing date time formatter as {@link DateTimeFormatter}.
   * @return {@link Optional} containing the parsed {@link LocalDate} if successful, or an empty if fails.
   */
  public static Optional<LocalDate> dateParse(CharSequence text, DateTimeFormatter formatter) {
    try {
      return Optional.of(LocalDate.parse(text, formatter));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }

  /**
   * Attempts to parse a {@link CharSequence} into a {@link LocalDateTime} using the provided {@link DateTimeFormatter}.
   *
   * @param text      text to be parsed into a {@link LocalDateTime}.
   * @param formatter parsing date time formatter as {@link DateTimeFormatter}.
   * @return {@link Optional} containing the parsed {@link LocalDateTime} if successful, or an empty if fails.
   */
  public static Optional<LocalDateTime> dateTimeParse(CharSequence text, DateTimeFormatter formatter) {
    try {
      return Optional.of(LocalDateTime.parse(text, formatter));
    } catch (DateTimeParseException e) {
      return Optional.empty();
    }
  }
}
