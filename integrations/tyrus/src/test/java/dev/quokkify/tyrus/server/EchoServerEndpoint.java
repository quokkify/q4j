package dev.quokkify.tyrus.server;

import jakarta.websocket.OnMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

@ServerEndpoint("/")
public class EchoServerEndpoint {

  @OnMessage
  public void onMessage(String message, Session session) throws Exception {
    session.getBasicRemote().sendText(message);
  }
}
