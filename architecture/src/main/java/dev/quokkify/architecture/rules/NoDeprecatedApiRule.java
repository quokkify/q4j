package dev.quokkify.architecture.rules;

import dev.quokkify.architecture.contract.ClassScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.taikai.TaikaiArchitectureRule;

import com.enofex.taikai.Taikai;

/**
 * Reports the use of deprecated APIs, through Taikai's {@code noUsageOfDeprecatedAPIs}.
 *
 * <p>A deprecation usually arrives with a dependency update rather than with a code change. Failing every build
 * of the repository on such an update would block the update itself, so this is {@link RuleSeverity#INFO}: the
 * report lists every deprecated call, and a job can still turn it into a gate with
 * {@code -Darchitecture.fail.on=INFO}.
 */
public class NoDeprecatedApiRule extends TaikaiArchitectureRule {

  @Override
  public String name() {
    return "No deprecated API usage (Taikai)";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.INFO;
  }

  @Override
  protected ClassScope scope() {
    return ClassScope.ALL;
  }

  @Override
  protected void configure(Taikai.Builder builder) {
    builder.java(java -> java.noUsageOfDeprecatedAPIs());
  }
}
