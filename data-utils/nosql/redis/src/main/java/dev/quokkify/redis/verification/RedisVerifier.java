package dev.quokkify.redis.verification;

import org.redisson.api.RedissonClient;

public final class RedisVerifier extends BaseRedisVerification<RedisVerifier> {

  public RedisVerifier(RedissonClient client) {
    super(client);
  }

  @Override
  protected RedisVerifier self() {
    return this;
  }
}
