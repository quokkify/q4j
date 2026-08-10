package dev.quokkify.kafka.steps.models;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.quokkify.constant.StringConstant;
import dev.quokkify.kafka.clients.KafkaMessage;
import dev.quokkify.kafka.converters.MessageConverter;
import dev.quokkify.kafka.matchers.KafkaMatcher;
import dev.quokkify.kafka.services.KafkaService;
import dev.quokkify.kafka.steps.KafkaConsumerSteps;
import dev.quokkify.kafka.steps.KafkaProducerSteps;
import dev.quokkify.reflection.ReflectionUtils;
import dev.quokkify.step.AbstractSteps;
import dev.quokkify.util.JsonConverter;
import dev.quokkify.util.Waiter;

import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.awaitility.core.ConditionTimeoutException;
import org.hamcrest.Matchers;

public abstract class BaseKafkaSteps<M extends KafkaMessageValue, V extends KafkaVerification>
    extends AbstractSteps<V> {

  private static final int DEFAULT_LOG_MESSAGE_COUNT = 50;
  private static final int DEFAULT_LOG_MESSAGE_LENGTH = 1000;
  private final KafkaConsumerSteps<String, String, StringDeserializer, StringDeserializer> kafkaConsumerSteps;
  private final KafkaProducerSteps<String, String, StringSerializer, StringSerializer> kafkaProducerSteps;

  public BaseKafkaSteps(KafkaService kafkaService) {
    this.kafkaConsumerSteps = kafkaService.getKafkaConsumerSteps();
    this.kafkaProducerSteps = kafkaService.getKafkaProducerSteps();
  }

  protected KafkaConsumerSteps<String, String, StringDeserializer, StringDeserializer> getKafkaConsumerSteps() {
    return kafkaConsumerSteps;
  }

  protected KafkaProducerSteps<String, String, StringSerializer, StringSerializer> getKafkaProducerSteps() {
    return kafkaProducerSteps;
  }

  protected abstract String getTopic();

  /**
   * Get topic partitions offsets.
   *
   * @return Map of {@link TopicPartition} partitions with current offsets
   */
  protected Map<TopicPartition, Long> getPartitionsOffsets() {
    return kafkaConsumerSteps.getPartitionsOffsets(getTopic());
  }

  /**
   * Get message that satisfy specific predicate.
   *
   * @param predicate predicate for filtering messages
   * @return message
   */
  protected M getMessageValue(Predicate<M> predicate) {
    return getMessageValue(predicate, Duration.ofSeconds(30), Duration.ofMillis(1000));
  }

  /**
   * Get message that satisfy specific predicate.
   *
   * @param filterPredicate predicate for filtering messages
   * @param timeout         timeout for waiting message
   * @param pollingInterval pollingInterval for waiting message
   * @return message
   */
  protected M getMessageValue(Predicate<M> filterPredicate, Duration timeout, Duration pollingInterval) {
    return waitMessages(filterPredicate, timeout, pollingInterval).stream()
        .filter(filterPredicate)
        .findFirst()
        .orElseThrow(() ->
            new RuntimeException("No kafka message found in '%s' topic by given predicate".formatted(getTopic())));
  }

  /**
   * Get message that satisfy specific predicate since offset.
   *
   * @param filterPredicate   predicate for filtering messages
   * @param partitionsOffsets partitions offsets to start reading messages
   * @return message
   */
  protected M getMessageValue(Predicate<M> filterPredicate, Map<TopicPartition, Long> partitionsOffsets) {
    return getMessageValue(filterPredicate, partitionsOffsets, Duration.ofSeconds(30), Duration.ofMillis(1000));
  }

  /**
   * Get message that satisfy specific predicate since offset.
   *
   * @param predicate         predicate for filtering messages
   * @param partitionsOffsets partitions offsets to start reading messages
   * @param timeout           timeout for waiting message
   * @param pollingInterval   pollingInterval for waiting message
   * @return message
   */
  protected M getMessageValue(Predicate<M> predicate, Map<TopicPartition, Long> partitionsOffsets,
                              Duration timeout, Duration pollingInterval) {
    return waitMessages(predicate, timeout, pollingInterval, partitionsOffsets).stream()
        .filter(predicate)
        .findFirst()
        .orElseThrow(() ->
            new RuntimeException("No kafka message found in '%s' topic by given predicate".formatted(getTopic())));
  }

  /**
   * Get messages that satisfy specific predicate.
   *
   * @param predicate predicate for filtering messages
   * @return list of messages
   */
  protected List<M> getMessageValues(Predicate<M> predicate) {
    return waitMessages(predicate, Duration.ofSeconds(30), Duration.ofMillis(1000));
  }

  /**
   * Get messages that satisfy specific predicate.
   *
   * @param predicate                predicate for filtering messages
   * @param minRequiredMessagesCount min required count of messages that satisfy predicate
   * @return list of messages
   */
  protected List<M> getMessageValues(Predicate<M> predicate, int minRequiredMessagesCount) {
    List<M> messages = new ArrayList<>();
    Callable<List<M>> getMessagesSupplier = () -> {
      messages.addAll(getMessageValues(predicate));
      return messages;
    };
    return Waiter.awaitCondition(getMessagesSupplier,
        Matchers.hasSize(Matchers.greaterThanOrEqualTo(minRequiredMessagesCount)),
        "Count of messages less than min required '%d'".formatted(minRequiredMessagesCount),
        Duration.ofSeconds(30), Duration.ofMillis(1000));
  }

  /**
   * Get messages that satisfy specific predicate since offset.
   *
   * @param predicate         predicate for filtering messages
   * @param partitionsOffsets partitions offsets to read messages
   * @return list of messages
   */
  protected List<M> getMessageValues(Predicate<M> predicate, Map<TopicPartition, Long> partitionsOffsets) {
    return waitMessages(predicate, Duration.ofSeconds(30), Duration.ofMillis(1000), partitionsOffsets);
  }

  /**
   * Send message to Kafka.
   *
   * @param messageValue message to send
   */
  public void sendMessageValue(M messageValue) {
    kafkaProducerSteps.sendSyncMessage(MessageConverter.convertToJsonMessage(getTopic(), messageValue));
  }

  /**
   * Wait for Kafka messages.
   *
   * @param predicate       predicate for waiting messages
   * @param timeout         timeout for waiting messages
   * @param pollingInterval for waiting messages
   * @return list of messages
   */
  private List<M> waitMessages(Predicate<M> predicate, Duration timeout, Duration pollingInterval) {
    return Waiter.awaitCondition(this::readMessageValues, KafkaMatcher.hasItem(predicate),
        "'%s' topic has no required messages".formatted(getTopic()), timeout, pollingInterval);
  }

  /**
   * Wait for Kafka messages since partitions offset.
   *
   * @param predicate         predicate for waiting messages
   * @param timeout           timeout for waiting messages
   * @param pollingInterval   for waiting messages
   * @param partitionsOffsets partitions offsets to start reading messages
   * @return list of required messages or throw exception with log all received messages
   */
  private List<M> waitMessages(Predicate<M> predicate, Duration timeout, Duration pollingInterval,
                               Map<TopicPartition, Long> partitionsOffsets) {
    try {
      return Waiter.awaitCondition(() -> readMessageValues(partitionsOffsets), KafkaMatcher.hasItem(predicate),
          StringUtils.EMPTY, timeout, pollingInterval);
    } catch (ConditionTimeoutException conditionTimeoutException) {
      throw new ConditionTimeoutException(
          logMessages(kafkaConsumerSteps.getMessages(partitionsOffsets), DEFAULT_LOG_MESSAGE_COUNT,
              DEFAULT_LOG_MESSAGE_LENGTH), conditionTimeoutException);
    }
  }

  /**
   * Read all messages from Kafka.
   *
   * @return list of messages
   */
  public List<M> readMessageValues() {
    List<KafkaMessage<String, String>> messageList = kafkaConsumerSteps.getMessages(getTopic());
    return getConvertedMessages(messageList);
  }

  /**
   * Read all messages from Kafka since partitions offsets.
   *
   * @param partitionsOffsets partitions offsets to read messages
   * @return list of messages
   */
  public List<M> readMessageValues(Map<TopicPartition, Long> partitionsOffsets) {
    List<KafkaMessage<String, String>> messageList = kafkaConsumerSteps.getMessages(partitionsOffsets);
    return getConvertedMessages(messageList);
  }

  /**
   * Move topic offset to end.
   */
  public void moveOffsetToEnd() {
    kafkaConsumerSteps.moveOffsetToEnd(getTopic());
  }

  public String logMessages(List<M> messages) {
    return logMessages(messages.stream().map(message -> new KafkaMessage<>(StringUtils.EMPTY, StringUtils.EMPTY,
        message.toString())).collect(Collectors.toList()), DEFAULT_LOG_MESSAGE_COUNT, DEFAULT_LOG_MESSAGE_LENGTH);
  }

  private String logMessages(List<KafkaMessage<String, String>> messages, int messagesCount, int maxSymbolsInLine) {
    if (messages == null || messages.isEmpty()) {
      return "Kafka topic has no any messages since consumption connection";
    }
    return ("Kafka topic has no expected messages. Only the following %d messages were received since consumption "
        + "connection.%nLast messages (max %d) with partition sort:%n%s")
        .formatted(messages.size(), messagesCount,
            getRecentMessages(sortedMessagesByPartition(getLastMessages(messages, messagesCount)), maxSymbolsInLine));
  }

  private List<KafkaMessage<String, String>> getLastMessages(List<KafkaMessage<String, String>> messages,
                                                             int messagesCount) {
    return messages.stream().skip(Math.max(messages.size() - messagesCount, 0)).collect(Collectors.toList());
  }

  private List<KafkaMessage<String, String>> sortedMessagesByPartition(List<KafkaMessage<String, String>> messages) {
    return messages.stream()
        .sorted(Comparator.comparingInt((KafkaMessage<String, String> message) -> message.partition())
            .thenComparing(
                Comparator.comparingLong((KafkaMessage<String, String> message) -> message.offset()).reversed()))
        .collect(Collectors.toList());
  }

  private String getRecentMessages(List<KafkaMessage<String, String>> messages, int maxSymbolsInLine) {
    return messages.stream().map(msg -> {
      String messageAsString = msg.toString().replaceAll("[\\r\\n]+", StringUtils.EMPTY);
      if (messageAsString.length() > maxSymbolsInLine) {
        messageAsString = messageAsString.substring(0, maxSymbolsInLine) + StringConstant.THREE_DOTS;
      }
      return messageAsString;
    }).collect(Collectors.joining(System.lineSeparator()));
  }

  @SuppressWarnings("unchecked")
  private List<M> getConvertedMessages(List<KafkaMessage<String, String>> messageList) {
    Class<M> messageValueClassType = (Class<M>) ReflectionUtils.getGenericClassType(this.getClass(), 0);
    return messageList.stream()
        .map(message -> (M) JsonConverter.fromString(message.value(), messageValueClassType))
        .collect(Collectors.toList());
  }
}
