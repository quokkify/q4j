package dev.quokkify.tyrus.steps;

import dev.quokkify.tyrus.client.WsClient;

public final class WsVerifier extends BaseWsVerification<WsVerifier> {

  WsVerifier(WsClient client) {
    super(client);
  }

  @Override
  protected WsVerifier self() {
    return this;
  }
}
