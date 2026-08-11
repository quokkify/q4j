package dev.quokkify.redis.steps;

import dev.quokkify.redis.verification.RedisVerifier;
import dev.quokkify.step.AbstractSteps;

import org.redisson.api.RedissonClient;

public class RedisSteps extends AbstractSteps<RedisVerifier> {

  private final RedissonClient client;

  public RedisSteps(RedissonClient client) {
    this.client = client;
  }

  @Override
  public RedisVerifier verify() {
    return new RedisVerifier(client);
  }
}
