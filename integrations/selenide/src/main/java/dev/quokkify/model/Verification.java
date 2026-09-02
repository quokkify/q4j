package dev.quokkify.model;

import java.time.Duration;

import dev.quokkify.impl.Page;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public abstract class Verification<S extends PageSteps<S, V, P>, V extends Verification<S, V, P>, P extends Page> {

  private final S steps;
  protected P page;
  private Duration timeout = Duration.ofSeconds(10);
  private Duration pollingInterval = Duration.ofMillis(500);

  public Verification(S steps, P page) {
    this.steps = steps;
    this.page = page;
  }

  @SuppressWarnings("unchecked")
  public V withTimeout(Duration timeout) {
    this.timeout = timeout;
    return (V) this;
  }

  @SuppressWarnings("unchecked")
  public V withPolling(Duration pollingInterval) {
    this.pollingInterval = pollingInterval;
    return (V) this;
  }

  void apply(TimeoutOptions options) {
    if (options.getTimeout() != null) {
      timeout = options.getTimeout();
    }
    if (options.getPollingInterval() != null) {
      pollingInterval = options.getPollingInterval();
    }
  }

  protected Duration getTimeout() {
    return timeout;
  }

  protected Duration getPollingInterval() {
    return pollingInterval;
  }

  public S backToSteps() {
    return steps;
  }
}
