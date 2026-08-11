package dev.quokkify.rabbitmq.test;

import dev.quokkify.rabbitmq.clients.RabbitMessage;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class RabbitMessageTest {

  @TmsLink("RABBIT_ID_2")
  @Test(description = "Verify RabbitMessage object payload serialization")
  public void testPayloadSerialization() {
    RabbitMessage<String> message = new RabbitMessage<>("exchange", "route", null, "payload");

    Assertions.assertThat(message.getPayload()).isNotEmpty();
    Assertions.assertThat(message.getPayloadAsObject()).isEqualTo("payload");
  }
}
