package dev.quokkify.rabbitmq.clients;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;

public final class RabbitMessage<T> {

  private final Envelope envelope;
  private final AMQP.BasicProperties properties;
  private final byte[] payload;

  public RabbitMessage(Envelope envelope, AMQP.BasicProperties properties, byte[] payload) {
    this.envelope = envelope;
    this.properties = properties;
    this.payload = payload;
  }

  public RabbitMessage(String exchange, String routingKey, AMQP.BasicProperties properties, byte[] payload) {
    this(new Envelope(0L, false, exchange, routingKey), properties, payload);
  }

  public RabbitMessage(String exchange, String routingKey, AMQP.BasicProperties properties, T payload) {
    this.envelope = new Envelope(0L, false, exchange, routingKey);
    this.properties = properties;
    this.payload = serializePayload(payload);
  }

  public Envelope getEnvelope() {
    return envelope;
  }

  public AMQP.BasicProperties getProperties() {
    return properties;
  }

  public byte[] getPayload() {
    return payload;
  }

  @SuppressWarnings("unchecked")
  public T getPayloadAsObject() {
    try (ByteArrayInputStream bis = new ByteArrayInputStream(payload);
         ObjectInputStream ois = new ObjectInputStream(bis)) {
      return (T) ois.readObject();
    } catch (IOException | ClassNotFoundException e) {
      throw new RuntimeException("Can't deserialize payload to message", e);
    }
  }

  private static <T> byte[] serializePayload(T payload) {
    try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
         ObjectOutputStream oos = new ObjectOutputStream(bos)) {
      oos.writeObject(payload);
      return bos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Can't serialize payload to message", e);
    }
  }
}
