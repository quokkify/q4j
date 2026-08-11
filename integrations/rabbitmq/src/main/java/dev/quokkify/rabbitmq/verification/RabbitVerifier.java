package dev.quokkify.rabbitmq.verification;

import java.util.function.Supplier;

import dev.quokkify.rabbitmq.clients.RabbitClient;

public final class RabbitVerifier extends BaseRabbitVerification<RabbitVerifier> {

  public RabbitVerifier(Supplier<RabbitClient> clientSupplier) {
    super(clientSupplier);
  }

  @Override
  protected RabbitVerifier self() {
    return this;
  }
}
