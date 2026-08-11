package dev.quokkify.tyrus.steps;

import dev.quokkify.tyrus.client.WsClient;

public final class WsVerifierFactory {

  private WsVerifierFactory() { }

  public static WsVerifier create(WsClient client) {
    return new WsVerifier(client);
  }
}
