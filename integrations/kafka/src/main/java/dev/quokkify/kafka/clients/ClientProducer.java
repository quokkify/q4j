package dev.quokkify.kafka.clients;

import java.io.Closeable;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import dev.quokkify.constant.StringConstant;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Thread-safe Kafka producer.
 *
 * @param <K> Type of kafka message key
 * @param <V> Type of kafka message value
 */
public class ClientProducer<K, V> implements Closeable {

  private static final Logger LOG = LogManager.getLogger(ClientProducer.class);
  private final KafkaProducer<K, V> producer;

  public ClientProducer(Properties properties) {
    this.producer = new KafkaProducer<>(properties);
  }

  /**
   * Create client producer for kafka.
   *
   * @param connectionProperties Connection properties
   * @param keySerializerClass   Class for serialization message key
   * @param valueSerializerClass Class for serialization message value
   * @param <K>                  Type of kafka message key
   * @param <V>                  Type of kafka message value
   * @param <T>                  Type of serializer message key
   * @param <E>                  Type of serializer message value
   * @return {@link ClientProducer} instance
   */
  public static <K, V, T extends Serializer<?>, E extends Serializer<?>> ClientProducer<K, V> create(
      ConnectionProperties connectionProperties,
      Class<T> keySerializerClass,
      Class<E> valueSerializerClass) {
    var props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, connectionProperties.bootstrapServers());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializerClass.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializerClass.getName());
    if (StringUtils.isNotEmpty(connectionProperties.securityProtocol())) {
      props.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, connectionProperties.securityProtocol());
      props.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, connectionProperties.sslTruststoreLocation());
      props.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, connectionProperties.sslTruststorePassword());
      props.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, connectionProperties.sslKeystoreLocation());
      props.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, connectionProperties.sslKeystorePassword());
    }
    LOG.info("Create kafka producer with properties:\n{}",
        props.toString().replace(StringConstant.COMMA_SPACE, StringUtils.LF));
    return new ClientProducer<>(props);
  }

  /**
   * Send message to kafka with waiting result.
   *
   * @param message Type of {@link KafkaMessage}
   * @return {@link RecordMetadata}
   */
  public RecordMetadata syncSend(KafkaMessage<K, V> message) {
    try {
      Future<RecordMetadata> future = producer.send(
          new ProducerRecord<>(message.topicName(), message.key(), message.value()));
      return future.get();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException("Unable to send a message to kafka: " + message, e);
    } catch (ExecutionException e) {
      throw new RuntimeException("Unable to send a message to kafka: " + message, e);
    }
  }

  /**
   * Send message to kafka without waiting result.
   *
   * @param message  Type of {@link KafkaMessage}
   * @param callback callback for ack result
   */
  public void asyncSend(KafkaMessage<K, V> message, Callback callback) {
    producer.send(new ProducerRecord<>(message.topicName(), message.key(), message.value()), callback);
  }

  /**
   * Send message to kafka without waiting result.
   *
   * @param message Type of {@link KafkaMessage}
   */
  public void asyncSend(KafkaMessage<K, V> message) {
    producer.send(new ProducerRecord<>(message.topicName(), message.key(), message.value()));
  }

  @Override
  public void close() {
    producer.close();
  }
}
