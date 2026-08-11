package dev.quokkify.kafka.converters;

import dev.quokkify.kafka.clients.KafkaMessage;
import dev.quokkify.kafka.steps.models.KafkaMessageValue;
import dev.quokkify.util.JsonConverter;

import org.apache.commons.lang3.StringUtils;

/**
 * Used for kafka message converting.
 */
public final class MessageConverter {

  private MessageConverter() {
  }

  public static <M extends KafkaMessageValue> KafkaMessage<String, String> convertToJsonMessage(String topic,
                                                                                                 M messageValue) {
    return new KafkaMessage<>(topic, StringUtils.EMPTY, JsonConverter.toJson(messageValue));
  }
}
