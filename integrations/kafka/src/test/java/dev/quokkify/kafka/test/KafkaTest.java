package dev.quokkify.kafka.test;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import dev.quokkify.kafka.clients.ConnectionProperties;
import dev.quokkify.kafka.clients.KafkaMessage;
import dev.quokkify.kafka.steps.KafkaConsumerSteps;
import dev.quokkify.kafka.steps.KafkaProducerSteps;

import io.qameta.allure.TmsLink;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class KafkaTest {

  private static final Logger LOG = LogManager.getLogger(KafkaTest.class);
  private static final String TOPIC_NAME = "messages";
  private static final String DEFAULT_KAFKA_ADDRESS = "localhost:29092";
  private KafkaProducerSteps<String, String, StringSerializer, StringSerializer> kafkaProducerSteps;
  private KafkaConsumerSteps<String, String, StringDeserializer, StringDeserializer> kafkaConsumerSteps;

  @BeforeClass(alwaysRun = true)
  public void initResources() {
    try {
      String kafkaAddress = resolveKafkaAddress();
      ConnectionProperties connectionProperties = new ConnectionProperties(kafkaAddress);
      kafkaProducerSteps =
          new KafkaProducerSteps<>(connectionProperties, StringSerializer.class, StringSerializer.class);
      kafkaConsumerSteps =
          new KafkaConsumerSteps<>(connectionProperties, StringDeserializer.class, StringDeserializer.class);
      kafkaConsumerSteps.getMessages(TOPIC_NAME); // first initialize consumer
    } catch (Exception e) {
      LOG.error("Kafka does not started: {}", e.getMessage());
      throw e;
    }
  }

  @TmsLink("KAFKA_ID_1")
  @Test(description = "Verify the publication of Kafka message")
  public void testKafkaPublishMessage() {
    KafkaMessage<String, String> message =
        new KafkaMessage<>(TOPIC_NAME, UUID.randomUUID().toString(), UUID.randomUUID().toString());
    kafkaProducerSteps.sendSyncMessage(message);
    List<KafkaMessage<String, String>> messages = kafkaConsumerSteps.getMessages(TOPIC_NAME);
    Assertions.assertThat(messages)
        .anySatisfy(actual -> {
          Assertions.assertThat(actual.topicName()).isEqualTo(message.topicName());
          Assertions.assertThat(actual.key()).isEqualTo(message.key());
          Assertions.assertThat(actual.value()).isEqualTo(message.value());
        });
  }

  @AfterClass(alwaysRun = true)
  public void closeResources() {
    if (Objects.nonNull(kafkaProducerSteps)) {
      kafkaProducerSteps.closeClientProducer();
    }
    if (Objects.nonNull(kafkaConsumerSteps)) {
      kafkaConsumerSteps.closeClientConsumer();
    }
  }

  private String resolveKafkaAddress() {
    String fromProperty = System.getProperty("KAFKA_SERVER_ADDRESS");
    if (fromProperty != null && !fromProperty.isBlank()) {
      return fromProperty;
    }

    String fromEnv = System.getenv("KAFKA_SERVER_ADDRESS");
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }

    String bootstrapFromEnv = System.getenv("KAFKA_BOOTSTRAP_SERVERS");
    if (bootstrapFromEnv != null && !bootstrapFromEnv.isBlank()) {
      return bootstrapFromEnv;
    }

    return DEFAULT_KAFKA_ADDRESS;
  }
}
