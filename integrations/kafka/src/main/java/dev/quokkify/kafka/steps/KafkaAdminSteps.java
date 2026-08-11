package dev.quokkify.kafka.steps;

import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import io.qameta.allure.Step;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Steps for use kafka admin.
 */
public class KafkaAdminSteps {

  private static final Logger LOG = LogManager.getLogger(KafkaAdminSteps.class);
  private final AdminClient adminClient;

  public KafkaAdminSteps(AdminClient adminClient) {
    this.adminClient = adminClient;
  }

  /**
   * Create new topic in kafka.
   * NOTE: 'NewTopic' objects should contain names fields, not null.
   *
   * @param newTopics new topics
   * @return {@link KafkaAdminSteps}
   */
  @Step("Create topics in kafka")
  public KafkaAdminSteps createTopics(Collection<NewTopic> newTopics) {
    try {
      adminClient.createTopics(newTopics).all().get();
    } catch (InterruptedException | ExecutionException exception) {
      Thread.currentThread().interrupt();
      LOG.error("Some of topics was not created: {}",
          newTopics.stream().map(NewTopic::name).collect(Collectors.joining()));
    }
    return this;
  }
}
