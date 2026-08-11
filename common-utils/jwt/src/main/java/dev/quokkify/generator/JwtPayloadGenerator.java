package dev.quokkify.generator;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import dev.quokkify.model.Payload;

/**
 * Generate JWT {@link Payload} using several parameters.
 */
public class JwtPayloadGenerator {

  private JwtPayloadGenerator() {
  }

  /**
   * Generate JWT Payload obj with mandatory parameters.
   * note: return Payload with generated UUID in JTW ID field.
   *
   * @param userId player id
   * @param ip     ip address
   * @return {@link Payload}
   */
  public static Payload generate(Long userId, String ip) {
    return new Payload(null, userId, ip, null, null, UUID.randomUUID().toString());
  }

  /**
   * Generate JWT Payload obj with all parameters.
   * note: return Payload with generated UUID in JTW ID field and token time:
   * issued at time = now
   * expiration time = tomorrow
   *
   * @param session        player session
   * @param userId         player id
   * @param ip             ip address
   * @return {@link Payload}
   */
  public static Payload generate(String session, Long userId, String ip) {
    LocalDateTime now = LocalDateTimeGenerator.generateNow();
    LocalDateTime exp = now.plusDays(1);
    return generate(session, userId, ip, exp, now);
  }

  /**
   * Generate JWT Payload obj with all parameters.
   * note: return Payload with generated UUID in JTW ID field.
   *
   * @param session  player session
   * @param userId   player id
   * @param ip       ip address
   * @param exp      token expiration time
   * @param issuedAt token issued at time
   * @return {@link Payload}
   */
  public static Payload generate(String session, Long userId, String ip, LocalDateTime exp, LocalDateTime issuedAt) {
    String jti = UUID.randomUUID().toString();
    long expTimeEpoch = exp.toEpochSecond(ZoneOffset.UTC);
    long issuedAtEpoch = issuedAt.toEpochSecond(ZoneOffset.UTC);
    return new Payload(session, userId, ip, expTimeEpoch, issuedAtEpoch, jti);
  }
}
