package dev.quokkify.rabbitmq.clients;

import java.io.Closeable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

import dev.quokkify.rabbitmq.configs.RabbitMqConfiguration;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RabbitClient implements Closeable {

  private static final Logger LOG = LogManager.getLogger(RabbitClient.class);
  private static final int RECOVERY_INTERVAL_MILLIS = 3000;
  private static final boolean AUTOMATIC_RECOVERY_ENABLED = true;

  private Connection connection;
  private Channel channel;

  private RabbitClient() {
  }

  public static RabbitClient create(String uri) {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setUri(uri);
      factory.setNetworkRecoveryInterval(RECOVERY_INTERVAL_MILLIS);
      factory.setAutomaticRecoveryEnabled(AUTOMATIC_RECOVERY_ENABLED);

      RabbitClient client = new RabbitClient();
      client.connection = factory.newConnection();
      client.channel = client.connection.createChannel();
      return client;
    } catch (IOException | TimeoutException | URISyntaxException | NoSuchAlgorithmException
             | KeyManagementException e) {
      throw new RuntimeException(e);
    }
  }

  public static RabbitClient create(RabbitMqConfiguration configuration) {
    try {
      ConnectionFactory factory = new ConnectionFactory();
      factory.setUsername(configuration.rabbitUser());
      factory.setPassword(configuration.rabbitPassword());
      factory.setVirtualHost(configuration.rabbitVirtualHost());
      factory.setHost(configuration.rabbitHost());
      factory.setPort(configuration.rabbitPort());
      factory.setNetworkRecoveryInterval(RECOVERY_INTERVAL_MILLIS);
      factory.setAutomaticRecoveryEnabled(AUTOMATIC_RECOVERY_ENABLED);

      RabbitClient client = new RabbitClient();
      client.connection = factory.newConnection();
      client.channel = client.connection.createChannel();
      return client;
    } catch (IOException | TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  public Channel getChannel() {
    return channel;
  }

  public <T> void sendMessage(RabbitMessage<T> message) {
    try {
      channel.basicPublish(message.getEnvelope().getExchange(), message.getEnvelope().getRoutingKey(),
          message.getProperties(), message.getPayload());
    } catch (IOException e) {
      LOG.error("Unable to send message to RabbitMQ", e);
    }
  }

  public <T> Optional<RabbitMessage<T>> getMessage(String queueName) {
    return getMessage(queueName, true);
  }

  public <T> Optional<RabbitMessage<T>> getMessage(String queueName, boolean autoAck) {
    Optional<RabbitMessage<T>> message = Optional.empty();
    try {
      GetResponse response = channel.basicGet(queueName, autoAck);
      if (Objects.nonNull(response)) {
        message = Optional.of(new RabbitMessage<>(response.getEnvelope(), response.getProps(), response.getBody()));
      }
    } catch (IOException e) {
      LOG.error("Unable to get message from RabbitMQ", e);
    }
    return message;
  }

  @Override
  public void close() {
    try {
      if (Objects.nonNull(channel) && channel.isOpen()) {
        channel.close();
      }
    } catch (IOException | TimeoutException e) {
      LOG.error("Unable to close rabbit client", e);
    } finally {
      try {
        if (Objects.nonNull(connection) && connection.isOpen()) {
          connection.close();
        }
      } catch (IOException e) {
        LOG.error("Unable to close rabbit connection", e);
      }
    }
  }
}
