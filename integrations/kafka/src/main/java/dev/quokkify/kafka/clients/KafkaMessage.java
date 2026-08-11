package dev.quokkify.kafka.clients;

/**
 * General message for send to kafka. Uses with {@link ClientProducer}
 *
 * @param topicName topic name
 * @param partition topic partition
 * @param offset topic offset
 * @param key message key
 * @param value message value
 * @param <K> Type of message key
 * @param <V> Type of message value
 */
public record KafkaMessage<K, V>(String topicName, int partition, long offset, K key, V value) {

  public KafkaMessage(String topicName, K key, V value) {
    this(topicName, 0, 0L, key, value);
  }
}
