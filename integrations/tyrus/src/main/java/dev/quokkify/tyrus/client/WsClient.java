package dev.quokkify.tyrus.client;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.tyrus.config.WsConfiguration;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@ClientEndpoint
public final class WsClient implements Closeable {

  private static final Logger LOG = LogManager.getLogger(WsClient.class);

  private final Queue<WsMessage> messageQueue;
  private Session session;

  private WsClient() {
    this.messageQueue = new ConcurrentLinkedQueue<>();
  }

  WsClient(Queue<WsMessage> messageQueue) {
    this.messageQueue = messageQueue;
  }

  public static WsClient connect() {
    WsConfiguration config = ConfigRegistry.get(WsConfiguration.class);
    return connect(config.wsUrl());
  }

  public static WsClient connect(String url) {
    WsClient client = new WsClient();
    try {
      WebSocketContainer container = ContainerProvider.getWebSocketContainer();
      container.connectToServer(client, URI.create(url));
    } catch (Exception e) {
      throw new RuntimeException("Failed to connect to WebSocket: " + url, e);
    }
    return client;
  }

  @OnOpen
  public void onOpen(Session session) {
    this.session = session;
    LOG.debug("WS connected: {}", session.getId());
  }

  @OnMessage
  public void onMessage(String payload) {
    LOG.debug("WS message received: {}", payload);
    messageQueue.add(new WsMessage(payload, Instant.now()));
  }

  @OnClose
  public void onClose(Session session, CloseReason reason) {
    LOG.debug("WS closed: {}", reason.getReasonPhrase());
  }

  @OnError
  public void onError(Session session, Throwable error) {
    LOG.error("WS error", error);
  }

  public WsClient sendMessage(String message) {
    try {
      session.getBasicRemote().sendText(message);
    } catch (IOException e) {
      throw new RuntimeException("Failed to send WS message", e);
    }
    return this;
  }

  public List<WsMessage> getMessages() {
    return new ArrayList<>(messageQueue);
  }

  public WsClient clearMessages() {
    messageQueue.clear();
    return this;
  }

  public boolean isConnected() {
    return session != null && session.isOpen();
  }

  @Override
  public void close() {
    if (session != null && session.isOpen()) {
      try {
        session.close();
      } catch (IOException e) {
        LOG.warn("Error closing WS session", e);
      }
    }
  }
}
