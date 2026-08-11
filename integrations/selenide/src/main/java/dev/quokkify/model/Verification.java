package dev.quokkify.model;

import java.time.Duration;

import dev.quokkify.impl.Page;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings("URF_UNREAD_PUBLIC_OR_PROTECTED_FIELD")
public abstract class Verification<S extends PageSteps<S, V, P>, V extends Verification<S, V, P>, P extends Page> {

  private final S steps;
  protected P page;
  private Duration timeout = WaitDefaults.DEFAULT_TIMEOUT;
  private Duration pollingInterval = WaitDefaults.DEFAULT_POLLING_INTERVAL;

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
