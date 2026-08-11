package dev.quokkify.tyrus.client;

import java.time.Instant;

public record WsMessage(String payload, Instant receivedAt) { }
