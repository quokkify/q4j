package dev.quokkify.kafka.clients;

import java.io.Closeable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import dev.quokkify.constant.StringConstant;
import dev.quokkify.util.Waiter;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.IsolationLevel;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe Kafka consumer.
 *
 * @param <K> Type of kafka message key
 * @param <V> Type of kafka message value
 */
public class ClientConsumer<K, V> implements Closeable {

  private static final Logger LOG = LogManager.getLogger(ClientConsumer.class);
  private static final String GROUP_NAME = "autotest-";
  private static final String OFFSET_LATEST = "latest";
  private final KafkaConsumer<K, V> consumer;

  public ClientConsumer(Properties properties) {
    consumer = new KafkaConsumer<>(properties);
  }

  /**
   * Create kafka consumer client.
   *
   * @param connectionProperties   Connection properties
   * @param keyDeserializerClass   Class for deserialization message key
   * @param valueDeserializerClass Class for deserialization message value
   * @param <K>                    Type of kafka message key
   * @param <V>                    Type of kafka message value
   * @param <T>                    Type of deserializer message key
   * @param <E>                    Type of deserializer message value
   * @return {@link ClientConsumer} instance
   */
  public static <K, V, T extends Deserializer<?>, E extends Deserializer<?>> ClientConsumer<K, V> create(
      ConnectionProperties connectionProperties,
      Class<T> keyDeserializerClass,
      Class<E> valueDeserializerClass) {
    var props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionProperties.bootstrapServers());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializerClass.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializerClass.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, OFFSET_LATEST);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, GROUP_NAME + UUID.randomUUID());
    props.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, IsolationLevel.READ_COMMITTED.toString().toLowerCase(Locale.ROOT));
    if (StringUtils.isNotEmpty(connectionProperties.securityProtocol())) {
      props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, connectionProperties.securityProtocol());
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, connectionProperties.sslTruststoreLocation());
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, connectionProperties.sslTruststorePassword());
      props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, connectionProperties.sslKeystoreLocation());
      props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, connectionProperties.sslKeystorePassword());
    }
    LOG.info("Create kafka consumer with properties:\n{}",
        props.toString().replace(StringConstant.COMMA_SPACE, StringUtils.LF));
    return new ClientConsumer<>(props);
  }

  /**
   * Returns collection of last unread messages from kafka.
   *
   * @param topic   Messages topic name
   * @param timeout timeout to read messages
   * @return Collection of {@link KafkaMessage}
   */
  public List<KafkaMessage<K, V>> getMessages(String topic, Duration timeout) {
    consumer.subscribe(Collections.singleton(topic));
    List<KafkaMessage<K, V>> messages = new ArrayList<>();
    ConsumerRecords<K, V> records = consumer.poll(timeout);
    if (!records.isEmpty()) {
      records.records(topic).forEach(record ->
          messages.add(
              new KafkaMessage<>(record.topic(), record.partition(), record.offset(), record.key(), record.value())));
    }
    consumer.commitSync();
    consumer.unsubscribe();
    return messages;
  }

  /**
   * Returns collection of messages from kafka since partitions offsets.
   *
   * @param partitionsOffsets partitions offsets to start reading messages
   * @param timeout           timeout to read messages
   * @return Collection of {@link KafkaMessage}
   */
  public List<KafkaMessage<K, V>> getMessages(Map<TopicPartition, Long> partitionsOffsets, Duration timeout) {
    consumer.assign(partitionsOffsets.keySet());
    List<KafkaMessage<K, V>> messages = new ArrayList<>();
    partitionsOffsets.forEach(consumer::seek);
    Map<TopicPartition, Long> endOffsets = consumer.endOffsets(partitionsOffsets.keySet());
    Waiter.awaitCondition(() -> {
      ConsumerRecords<K, V> records = consumer.poll(timeout);
      records.forEach(record ->
          messages.add(
              new KafkaMessage<>(record.topic(), record.partition(), record.offset(), record.key(), record.value())));
      return partitionsOffsets.keySet().stream()
          .allMatch(topicPartition -> consumer.position(topicPartition) >= endOffsets.get(topicPartition));
    }, "Can not fetch all messages while consumer polling", Duration.ofSeconds(60), Duration.ofMillis(500));
    consumer.unsubscribe();
    return messages;
  }

  /**
   * Get topic partitions with current offsets.
   *
   * @param topic   Topic name
   * @param timeout timeout to read messages
   * @return Map of {@link TopicPartition} with current offset
   */
  public Map<TopicPartition, Long> getPartitionsOffsets(String topic, Duration timeout) {
    consumer.subscribe(Collections.singleton(topic));
    consumer.poll(timeout);
    Map<TopicPartition, Long> partitionsOffsets =
        consumer.assignment().stream().collect(Collectors.toMap(key -> key, consumer::position));
    consumer.unsubscribe();
    return partitionsOffsets;
  }

  /**
   * Move consumer position to the end of the topic.
   *
   * @param topic   topic name
   * @param timeout timeout for initial assignment poll
   */
  public void seekToEnd(String topic, Duration timeout) {
    consumer.subscribe(Collections.singleton(topic));
    consumer.poll(timeout);
    consumer.seekToEnd(consumer.assignment());
    consumer.unsubscribe();
  }

  @Override
  public void close() {
    consumer.close();
  }
}
