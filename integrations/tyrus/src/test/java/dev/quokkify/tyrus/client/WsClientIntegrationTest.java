package dev.quokkify.tyrus.client;

import java.time.Duration;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.tyrus.server.EchoServerEndpoint;
import dev.quokkify.tyrus.steps.WsSteps;

import org.glassfish.tyrus.server.Server;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

public class WsClientIntegrationTest {

  private static final String URL = "ws://localhost:8787";

  private static Server server;
  static WsSteps wsSteps;

  @BeforeSuite
  public void startServer() throws Exception {
    server = new Server("localhost", 8787, "/", null, EchoServerEndpoint.class);
    server.start();
    wsSteps = new WsSteps();
    wsSteps.connect(URL);
  }

  @AfterSuite
  public void stopServer() {
    wsSteps.disconnect();
    if (server != null) {
      server.stop();
    }
  }

  @SingleThread
  @Test
  public void connect_receivesEchoedMessage() {
    wsSteps.clearMessages()
        .sendMessage("hello")
        .verify()
        .withTimeout(Duration.ofSeconds(5))
        .withPolling(Duration.ofMillis(100))
        .containsMessage("hello");
  }

  @SingleThread
  @Test
  public void sendJson_verifyJsonField() {
    wsSteps.clearMessages()
        .sendMessage("{\"type\":\"order_created\",\"orderId\":\"42\"}")
        .verify()
        .hasJsonField("type", "order_created")
        .hasJsonField("orderId", "42");
  }

  @SingleThread
  @Test
  public void multipleMessages_verifiedInOrder() {
    wsSteps.clearMessages()
        .sendMessage("step one")
        .sendMessage("step two")
        .sendMessage("step three")
        .verify()
        .messagesInOrder("step one", "step two", "step three");
  }

  @SingleThread
  @Test
  public void absence_assertedCorrectly() {
    wsSteps.clearMessages()
        .verify()
        .doesNotContainMessage("ghost message");
  }
}
