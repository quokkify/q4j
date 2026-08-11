package dev.quokkify.rabbitmq.clients;

import java.util.Objects;
import java.util.Optional;

import dev.quokkify.rabbitmq.configs.RabbitMqConfiguration;
import dev.quokkify.rabbitmq.verification.RabbitVerifier;
import dev.quokkify.step.AbstractSteps;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitSteps extends AbstractSteps<RabbitVerifier> {

  private static final Logger LOG = LogManager.getLogger(RabbitSteps.class);

  private final ThreadLocal<RabbitClient> client = new ThreadLocal<>();
  private final RabbitMqConfiguration rabbitMqConfiguration;
  private final String configurationUrl;

  public RabbitSteps(RabbitMqConfiguration configuration) {
    this.rabbitMqConfiguration = configuration;
    this.configurationUrl = null;
  }

  public RabbitSteps(String configurationUrl) {
    this.rabbitMqConfiguration = null;
    this.configurationUrl = configurationUrl;
  }

  @Override
  public RabbitVerifier verify() {
    return new RabbitVerifier(this::getClient);
  }

  @Step("Send message to RabbitMQ")
  public <T> RabbitSteps sendMessage(RabbitMessage<T> message) {
    getClient().sendMessage(message);
    return this;
  }

  @Step("Get single message from {queueName} queue in RabbitMQ")
  public <T> Optional<RabbitMessage<T>> getMessage(String queueName, boolean autoAck) {
    return getClient().getMessage(queueName, autoAck);
  }

  @Step("Get single message from {queueName} queue in RabbitMQ with ack")
  public <T> Optional<RabbitMessage<T>> getMessage(String queueName) {
    return getClient().getMessage(queueName);
  }

  public RabbitClient getClient() {
    if (Objects.isNull(client.get())) {
      try {
        RabbitClient rabbitClient;
        if (Objects.nonNull(rabbitMqConfiguration)) {
          rabbitClient = RabbitClient.create(rabbitMqConfiguration);
        } else {
          rabbitClient = RabbitClient.create(configurationUrl);
        }
        client.set(rabbitClient);
      } catch (Throwable e) {
        LOG.error("RabbitMQ client does not started: {}", e.getMessage());
        throw e;
      }
    }
    return client.get();
  }

  public void closeClient() {
    RabbitClient rabbitClient = client.get();
    if (Objects.nonNull(rabbitClient)) {
      rabbitClient.close();
      client.remove();
    }
  }
}
