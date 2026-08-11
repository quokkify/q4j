package dev.quokkify.kafka.steps;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import dev.quokkify.kafka.clients.ClientConsumer;
import dev.quokkify.kafka.clients.ConnectionProperties;
import dev.quokkify.kafka.clients.KafkaMessage;

import io.qameta.allure.Step;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Steps for uses kafka consumer.
 *
 * @param <K> Type of kafka message key
 * @param <V> Type of kafka message value
 * @param <T> Type of message deserializer key
 * @param <E> Type of message deserializer value
 */
public class KafkaConsumerSteps<K, V, T extends Deserializer<?>, E extends Deserializer<?>> {

  private static final Logger LOG = LogManager.getLogger(KafkaConsumerSteps.class);
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
  private final ThreadLocal<ClientConsumer<K, V>> clientConsumer = new ThreadLocal<>();
  private final ConnectionProperties connectionProperties;
  private final Class<T> keyDeserializerClass;
  private final Class<E> valueDeserializerClass;

  public KafkaConsumerSteps(ConnectionProperties connectionProperties, Class<T> keyDeserializerClass,
                            Class<E> valueDeserializerClass) {
    this.connectionProperties = connectionProperties;
    this.keyDeserializerClass = keyDeserializerClass;
    this.valueDeserializerClass = valueDeserializerClass;
  }

  /**
   * Get unread messages from kafka topic with custom timeout.
   *
   * @param topic              Topic name
   * @param timeoutPoolMessage duration for waiting read messages
   * @return List of {@link KafkaMessage}
   */
  @Step("Get messages from topic '{topic}' in kafka with timeout '{timeoutPoolMessage}'")
  public List<KafkaMessage<K, V>> getMessages(String topic, Duration timeoutPoolMessage) {
    return getConsumer().getMessages(topic, timeoutPoolMessage);
  }

  /**
   * Get unread messages from kafka topic with default timeout.
   *
   * @param topic Topic name
   * @return List of {@link KafkaMessage}
   */
  @Step("Get messages from topic '{topic}' in Kafka")
  public List<KafkaMessage<K, V>> getMessages(String topic) {
    return getConsumer().getMessages(topic, DEFAULT_TIMEOUT);
  }

  /**
   * Get messages from kafka topic since partitions offsets with custom timeout.
   *
   * @param partitionsOffsets  partitions offsets to start reading messages
   * @param timeoutPoolMessage duration for waiting read messages
   * @return List of {@link KafkaMessage}
   */
  @Step("Get messages from Kafka since partitions offsets '{partitionsOffsets}' with timeout 'timeoutPoolMessage'")
  public List<KafkaMessage<K, V>> getMessages(Map<TopicPartition, Long> partitionsOffsets,
                                              Duration timeoutPoolMessage) {
    return getConsumer().getMessages(partitionsOffsets, timeoutPoolMessage);
  }

  /**
   * Get messages from kafka topic since partitions offsets with default timeout.
   *
   * @param partitionsOffsets partitions offsets to start reading messages
   * @return List of {@link KafkaMessage}
   */
  @Step("Get messages from Kafka since partitions offsets '{partitionsOffsets}'")
  public List<KafkaMessage<K, V>> getMessages(Map<TopicPartition, Long> partitionsOffsets) {
    return getConsumer().getMessages(partitionsOffsets, DEFAULT_TIMEOUT);
  }

  /**
   * Get topic partitions with current offsets.
   *
   * @param topic Topic name
   * @return Map of {@link TopicPartition} with current offset
   */
  @Step("Get topic '{topic}' partitions with current offsets")
  public Map<TopicPartition, Long> getPartitionsOffsets(String topic) {
    return getConsumer().getPartitionsOffsets(topic, DEFAULT_TIMEOUT);
  }

  @Step("Move offset to end for '{topic}' topic")
  public void moveOffsetToEnd(String topic) {
    getConsumer().seekToEnd(topic, DEFAULT_TIMEOUT);
  }

  /**
   * Close all resources in kafka consumer.
   */
  public void closeClientConsumer() {
    ClientConsumer<K, V> consumer = clientConsumer.get();
    if (Objects.nonNull(consumer)) {
      consumer.close();
      clientConsumer.remove();
    }
  }

  private ClientConsumer<K, V> getConsumer() {
    if (Objects.isNull(clientConsumer.get())) {
      try {
        ClientConsumer<K, V> consumer =
            ClientConsumer.create(connectionProperties, keyDeserializerClass, valueDeserializerClass);
        clientConsumer.set(consumer);
      } catch (Throwable e) {
        LOG.error("Kafka consumer does not started: {}", e.getMessage());
        throw e;
      }
    }
    return clientConsumer.get();
  }
}
