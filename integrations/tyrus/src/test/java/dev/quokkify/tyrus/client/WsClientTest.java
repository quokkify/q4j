package dev.quokkify.tyrus.client;

import java.time.Duration;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.tyrus.steps.WsVerifier;
import dev.quokkify.tyrus.steps.WsVerifierFactory;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class WsClientTest {

  private WsSimulator simulator;
  private WsClient client;
  private WsVerifier verifier;

  @BeforeMethod
  public void setUp() {
    simulator = WsSimulator.create();
    client = simulator.asClient();
    verifier = WsVerifierFactory.create(client)
        .withTimeout(Duration.ofSeconds(5))
        .withPolling(Duration.ofMillis(100));
  }

  @AfterMethod
  public void tearDown() {
    simulator.clear();
  }

  @SingleThread
  @Test
  public void messageCollector_storesMessages() {
    simulator.send("hello world").send("second message");

    Assert.assertEquals(client.getMessages().size(), 2);
    Assert.assertEquals(client.getMessages().get(0).payload(), "hello world");
    Assert.assertEquals(client.getMessages().get(1).payload(), "second message");
  }

  @SingleThread
  @Test
  public void clearMessages_emptiesQueue() {
    simulator.send("to be cleared");

    client.clearMessages();

    Assert.assertTrue(client.getMessages().isEmpty());
  }

  @SingleThread
  @Test
  public void verifier_containsMessage_passes() {
    simulator.send("order status updated");

    verifier.containsMessage("order status");
  }

  @SingleThread
  @Test
  public void verifier_containsMessage_byPredicate_passes() {
    simulator.send("payment confirmed");

    verifier.containsMessage(msg -> msg.payload().startsWith("payment"));
  }

  @SingleThread
  @Test
  public void verifier_doesNotContainMessage_passes() {
    simulator.send("expected message");

    WsVerifierFactory.create(client)
        .withTimeout(Duration.ofSeconds(3))
        .withPolling(Duration.ofMillis(100))
        .doesNotContainMessage("absent substring");
  }

  @SingleThread
  @Test
  public void verifier_hasJsonField_passes() {
    simulator.send("{\"status\":\"active\",\"userId\":\"42\"}");

    verifier.hasJsonField("status", "active");
  }

  @SingleThread
  @Test
  public void verifier_hasMessageCount_passes() {
    simulator.send("first").send("second").send("third");

    verifier.hasMessageCount(3);
  }

  @SingleThread
  @Test
  public void verifier_messagesInOrder_passes() {
    simulator.send("step one complete")
        .send("step two complete")
        .send("step three complete");

    verifier.messagesInOrder("step one", "step two", "step three");
  }

  @SingleThread
  @Test
  public void verifier_containsMessage_withDelay_passes() {
    simulator.sendAfterDelay("delayed arrival", 200);

    verifier.containsMessage("delayed arrival");
  }
}
