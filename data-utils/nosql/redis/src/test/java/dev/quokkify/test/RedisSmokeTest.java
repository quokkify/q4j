package dev.quokkify.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RMap;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.testng.Assert;
import org.testng.annotations.Test;

public class RedisSmokeTest {

  @Test
  public void shouldStoreAndReadBasicTypes() {
    String host = System.getProperty("redis.host", System.getenv().getOrDefault("REDIS_HOST", "127.0.0.1"));
    String port = System.getProperty("redis.port", System.getenv().getOrDefault("REDIS_PORT", "6379"));
    String password = System.getProperty("redis.password", System.getenv().getOrDefault("REDIS_PASSWORD", ""));
    String clusterNodes = System.getProperty("redis.cluster.nodes",
        System.getenv().getOrDefault("REDIS_CLUSTER_NODES", ""));
    verifyRedisAvailability(host, port);

    Config config = new Config();
    config.setCodec(StringCodec.INSTANCE);
    if (!clusterNodes.isBlank()) {
      String[] nodes = clusterNodes.split(",");
      var cluster = config.useClusterServers();
      for (String node : nodes) {
        String address = node.trim();
        if (!address.isEmpty()) {
          cluster.addNodeAddress(address.startsWith("redis://") ? address : "redis://" + address);
        }
      }
      cluster.setPassword(password.isBlank() ? null : password);
    } else {
      config.useSingleServer()
          .setAddress("redis://" + host + ":" + port)
          .setPassword(password.isBlank() ? null : password);
    }

    RedissonClient client = Redisson.create(config);
    try {
      String prefix = "quokkify:test:" + System.currentTimeMillis();
      RBucket<String> bucket = client.getBucket(prefix + ":bucket");
      bucket.set("value");
      Assert.assertEquals(bucket.get(), "value");

      RMap<String, String> map = client.getMap(prefix + ":map");
      map.put("field", "value");
      Assert.assertEquals(map.get("field"), "value");

      RSet<String> set = client.getSet(prefix + ":set");
      set.add("item");
      Assert.assertTrue(set.contains("item"));

      client.getKeys().delete(prefix + ":bucket", prefix + ":map", prefix + ":set");
    } finally {
      client.shutdown();
    }
  }

  private static void verifyRedisAvailability(String host, String port) {
    int parsedPort = Integer.parseInt(port);
    try (Socket socket = new Socket()) {
      socket.connect(new InetSocketAddress(host, parsedPort), 2000);
    } catch (IOException e) {
      throw new AssertionError(
          "Redis is required for RedisSmokeTest but is unreachable at " + host + ":" + parsedPort
              + ". Start Redis infra or set REDIS_HOST/REDIS_PORT.",
          e);
    }
  }
}
