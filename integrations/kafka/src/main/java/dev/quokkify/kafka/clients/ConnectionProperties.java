package dev.quokkify.kafka.clients;

public record ConnectionProperties(
    String bootstrapServers,
    String securityProtocol,
    String sslTruststoreLocation,
    String sslTruststorePassword,
    String sslKeystoreLocation,
    String sslKeystorePassword
) {

  public ConnectionProperties(String bootstrapServers) {
    this(bootstrapServers, null, null, null, null, null);
  }
}
