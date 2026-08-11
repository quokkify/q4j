package dev.quokkify.kafka.matchers;

import java.util.Collection;
import java.util.function.Predicate;

import dev.quokkify.kafka.steps.models.KafkaMessageValue;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

/**
 * Matcher for kafka messages verifying.
 */
public class KafkaMatcher<T extends KafkaMessageValue> extends TypeSafeMatcher<Collection<T>> {

  private final Predicate<T> predicate;

  public KafkaMatcher(Predicate<T> predicate) {
    this.predicate = predicate;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("has no such item");
  }

  @Override
  protected boolean matchesSafely(Collection<T> collection) {
    return !collection.isEmpty() && collection.stream().anyMatch(predicate);
  }

  public static <T extends KafkaMessageValue> KafkaMatcher<T> hasItem(Predicate<T> predicate) {
    return new KafkaMatcher<>(predicate);
  }
}
