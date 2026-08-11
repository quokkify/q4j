package dev.quokkify.step;

import java.util.Arrays;
import java.util.function.Consumer;

import org.assertj.core.api.SoftAssertions;

/**
 * Abstract class for steps with verification.
 *
 * @param <V> verification steps class
 */
public abstract class AbstractSteps<V> {

  /**
   * Start verification chain.
   *
   * @return verification steps class
   */
  public abstract V verify();

  /**
   * Apply soft verification.
   *
   * @param verificationConsumers array of consumer verifications
   * @return verification steps class
   */
  @SafeVarargs
  public final V verifySoftly(Consumer<V>... verificationConsumers) {
    SoftAssertions.assertSoftly(softly ->
        Arrays.stream(verificationConsumers).toList().forEach(verificationConsumer ->
            softly.assertThatCode(() -> verificationConsumer.accept(verify()))
                .doesNotThrowAnyException()));
    return verify();
  }
}
