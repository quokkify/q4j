package dev.quokkify.rabbitmq.services;

import java.util.Objects;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.rabbitmq.clients.RabbitSteps;
import dev.quokkify.rabbitmq.configs.RabbitMqConfiguration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitService {

  private static final Logger LOG = LogManager.getLogger(RabbitService.class);

  private final RabbitMqConfiguration rabbitConfig;
  private RabbitSteps rabbitSteps;

  public RabbitService() {
    this(ConfigRegistry.get(RabbitMqConfiguration.class));
  }

  public RabbitService(RabbitMqConfiguration rabbitConfig) {
    this.rabbitConfig = rabbitConfig;
  }

  public RabbitSteps getRabbitSteps() {
    if (Objects.isNull(rabbitSteps)) {
      try {
        rabbitSteps = new RabbitSteps(rabbitConfig);
      } catch (Throwable error) {
        LOG.error("RabbitMQ steps does not started: {}", error.getMessage());
        throw error;
      }
    }
    return rabbitSteps;
  }

  public void closeRabbit() {
    if (Objects.nonNull(rabbitSteps)) {
      rabbitSteps.closeClient();
    }
  }
}
