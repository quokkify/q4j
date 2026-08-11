package dev.quokkify.kafka.steps;

import java.util.Objects;

import dev.quokkify.kafka.clients.ClientProducer;
import dev.quokkify.kafka.clients.ConnectionProperties;
import dev.quokkify.kafka.clients.KafkaMessage;

import io.qameta.allure.Step;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class KafkaProducerSteps<K, V, T extends Serializer<?>, E extends Serializer<?>> {

  private static final Logger LOG = LogManager.getLogger(KafkaProducerSteps.class);
  private final ThreadLocal<ClientProducer<K, V>> clientProducer = new ThreadLocal<>();
  private final ConnectionProperties connectionProperties;
  private final Class<T> keySerializerClass;
  private final Class<E> valueSerializerClass;

  public KafkaProducerSteps(ConnectionProperties connectionProperties, Class<T> keySerializerClass,
                            Class<E> valueSerializerClass) {
    this.connectionProperties = connectionProperties;
    this.keySerializerClass = keySerializerClass;
    this.valueSerializerClass = valueSerializerClass;
  }

  /**
   * Send synchronized message to kafka.
   *
   * @param message {@link KafkaMessage}
   * @return {@link KafkaProducerSteps}
   */
  @Step("Send synchronized message to kafka")
  public KafkaProducerSteps<K, V, T, E> sendSyncMessage(KafkaMessage<K, V> message) {
    getProducer().syncSend(message);
    return this;
  }

  /**
   * Close all resources in kafka producer.
   */
  public void closeClientProducer() {
    ClientProducer<K, V> producer = clientProducer.get();
    if (Objects.nonNull(producer)) {
      producer.close();
      clientProducer.remove();
    }
  }

  private ClientProducer<K, V> getProducer() {
    if (Objects.isNull(clientProducer.get())) {
      try {
        ClientProducer<K, V> producer =
            ClientProducer.create(connectionProperties, keySerializerClass, valueSerializerClass);
        clientProducer.set(producer);
      } catch (Throwable e) {
        LOG.error("Kafka producer does not started: {}", e.getMessage());
        throw e;
      }
    }
    return clientProducer.get();
  }
}
