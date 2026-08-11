package dev.quokkify.rabbitmq.test;

import java.io.IOException;
import java.util.Optional;

import dev.quokkify.rabbitmq.clients.RabbitMessage;
import dev.quokkify.rabbitmq.clients.RabbitSteps;
import dev.quokkify.rabbitmq.services.RabbitService;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.MessageProperties;
import io.qameta.allure.TmsLink;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RabbitIntegrationTest {

  private static final Logger LOG = LogManager.getLogger(RabbitIntegrationTest.class);
  private static final String EXCHANGE_NAME = "test";
  private static final String ROUTING_KEY = "test.message";

  private RabbitService rabbitService;
  private RabbitSteps rabbitSteps;
  private String queueName;

  @BeforeClass(alwaysRun = true)
  public void initResources() {
    try {
      rabbitService = new RabbitService();
      rabbitSteps = rabbitService.getRabbitSteps();
      Channel rabbitChannel = rabbitSteps.getClient().getChannel();
      rabbitChannel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
      queueName = rabbitChannel.queueDeclare().getQueue();
      rabbitChannel.queueBind(queueName, EXCHANGE_NAME, ROUTING_KEY);
    } catch (IOException e) {
      LOG.error("RabbitMQ does not started: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

  @TmsLink("RABBIT_ID_1")
  @Test(description = "Verify the publication of RabbitMQ message")
  public void testRabbitMqPublishMessage() {
    String text = "Hello World!";
    RabbitMessage<String> message =
        new RabbitMessage<>(EXCHANGE_NAME, ROUTING_KEY, MessageProperties.PERSISTENT_TEXT_PLAIN, text);
    rabbitSteps.sendMessage(message);

    Optional<RabbitMessage<String>> actualMessage = rabbitSteps.getMessage(queueName);
    Assertions.assertThat(actualMessage).isPresent();
    Assertions.assertThat(message.getPayload()).isEqualTo(actualMessage.orElseThrow().getPayload());
    Assertions.assertThat(text).isEqualTo(actualMessage.orElseThrow().getPayloadAsObject());
  }

  @AfterClass(alwaysRun = true)
  public void closeResources() {
    if (rabbitService != null) {
      rabbitService.closeRabbit();
    }
  }
}
