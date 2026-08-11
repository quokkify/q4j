package dev.quokkify.test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.annotation.TestGroup;
import dev.quokkify.service.SlowApiService;

import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ConnectionTimeoutTest {

  private ServerSocket slowServer;
  private SlowApiService slowApiService;

  @BeforeClass
  public void startSlowServer() throws IOException {
    slowServer = new ServerSocket(0);
    int port = slowServer.getLocalPort();
    Thread serverThread = new Thread(() -> {
      while (!slowServer.isClosed()) {
        try {
          Socket socket = slowServer.accept();
          Thread.sleep(30_000);
          socket.close();
        } catch (InterruptedException ignored) {
          Thread.currentThread().interrupt();
          break;
        } catch (IOException ignored) {
        }
      }
    });
    serverThread.setDaemon(true);
    serverThread.start();
    slowApiService = new SlowApiService("http://localhost:" + port, 1_000);
  }

  @AfterClass
  public void stopSlowServer() throws IOException {
    slowServer.close();
  }

  @SingleThread
  @TestGroup("API")
  @Test(description = "Verify connection is closed when MAX_RESPONSE_TIME_SECONDS is exceeded")
  public void testConnectionClosedOnTimeout() {
    long startMs = System.currentTimeMillis();
    try {
      slowApiService.getResource("/test");
      Assertions.fail("Expected socket timeout exception was not thrown");
    } catch (Exception ignored) {
    }
    Assertions.assertThat(System.currentTimeMillis() - startMs)
        .as("Connection should be closed within 5 seconds")
        .isLessThan(5_000L);
  }
}
