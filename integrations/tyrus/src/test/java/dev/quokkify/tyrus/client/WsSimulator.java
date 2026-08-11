package dev.quokkify.tyrus.client;

import java.time.Instant;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class WsSimulator {

  private final Queue<WsMessage> queue;

  private WsSimulator() {
    this.queue = new ConcurrentLinkedQueue<>();
  }

  public static WsSimulator create() {
    return new WsSimulator();
  }

  public WsClient asClient() {
    return new WsClient(queue);
  }

  public WsSimulator send(String payload) {
    queue.add(new WsMessage(payload, Instant.now()));
    return this;
  }

  public WsSimulator sendAfterDelay(String payload, long delayMillis) {
    Thread thread = new Thread(() -> {
      try {
        Thread.sleep(delayMillis);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return;
      }
      queue.add(new WsMessage(payload, Instant.now()));
    });
    thread.setDaemon(true);
    thread.start();
    return this;
  }

  public WsSimulator clear() {
    queue.clear();
    return this;
  }
}
