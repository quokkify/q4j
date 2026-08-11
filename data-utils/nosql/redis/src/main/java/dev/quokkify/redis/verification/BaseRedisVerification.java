package dev.quokkify.redis.verification;

import java.time.Duration;

import dev.quokkify.util.Waiter;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.redisson.api.RedissonClient;

public abstract class BaseRedisVerification<T extends BaseRedisVerification<T>>
    implements RedisVerification {

  private static final Logger LOG = LogManager.getLogger(BaseRedisVerification.class);

  private final RedissonClient client;
  private Duration timeout;
  private Duration pollingInterval;

  protected BaseRedisVerification(RedissonClient client) {
    this(client, Duration.ofSeconds(10), Duration.ofMillis(500));
  }

  protected BaseRedisVerification(RedissonClient client, Duration timeout, Duration pollingInterval) {
    this.client = client;
    this.timeout = timeout;
    this.pollingInterval = pollingInterval;
  }

  protected abstract T self();

  public T withTimeout(Duration timeout) {
    this.timeout = timeout;
    return self();
  }

  public T withPolling(Duration pollingInterval) {
    this.pollingInterval = pollingInterval;
    return self();
  }

  @Step("Redis: key '{key}' exists")
  public T hasKey(String key) {
    LOG.debug("Redis verify: key '{}' exists", key);
    Waiter.awaitCondition(
        () -> client.getBucket(key).isExists(),
        "Expected Redis key to exist: " + key,
        timeout,
        pollingInterval);
    return self();
  }

  @Step("Redis: key '{key}' does not exist")
  public T doesNotHaveKey(String key) {
    LOG.debug("Redis verify: key '{}' does not exist", key);
    Waiter.assertNeverTrue(
        () -> client.getBucket(key).isExists(),
        timeout,
        pollingInterval,
        "Unexpected Redis key found: " + key);
    return self();
  }

  @Step("Redis: key '{key}' has value '{expectedValue}'")
  public T hasValue(String key, String expectedValue) {
    LOG.debug("Redis verify: key '{}' has value '{}'", key, expectedValue);
    Waiter.awaitCondition(
        () -> expectedValue.equals(client.<String>getBucket(key).get()),
        "Expected Redis key '" + key + "' to have value: " + expectedValue,
        timeout,
        pollingInterval);
    return self();
  }

  @Step("Redis: map '{mapKey}' has field '{field}' = '{expectedValue}'")
  public T hasMapEntry(String mapKey, String field, String expectedValue) {
    LOG.debug("Redis verify: map '{}' has field '{}' = '{}'", mapKey, field, expectedValue);
    Waiter.awaitCondition(
        () -> expectedValue.equals(client.<String, String>getMap(mapKey).get(field)),
        "Expected Redis map '" + mapKey + "' field '" + field + "' = '" + expectedValue + "'",
        timeout,
        pollingInterval);
    return self();
  }

  @Step("Redis: set '{setKey}' contains '{member}'")
  public T hasSetMember(String setKey, String member) {
    LOG.debug("Redis verify: set '{}' contains '{}'", setKey, member);
    Waiter.awaitCondition(
        () -> client.<String>getSet(setKey).contains(member),
        "Expected Redis set '" + setKey + "' to contain: " + member,
        timeout,
        pollingInterval);
    return self();
  }
}
