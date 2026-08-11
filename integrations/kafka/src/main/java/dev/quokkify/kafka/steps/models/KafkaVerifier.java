package dev.quokkify.kafka.steps.models;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

public final class KafkaVerifier<M extends KafkaMessageValue>
    extends BaseKafkaVerification<KafkaVerifier<M>, M> {

  KafkaVerifier(Supplier<List<M>> messagesSupplier) {
    super(messagesSupplier);
  }

  KafkaVerifier(Supplier<List<M>> messagesSupplier, Duration timeout, Duration pollingInterval) {
    super(messagesSupplier, timeout, pollingInterval);
  }

  @Override
  protected KafkaVerifier<M> self() {
    return this;
  }
}
