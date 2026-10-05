package dev.quokkify.architecture.rules;

import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.taikai.TaikaiArchitectureRule;

import com.enofex.taikai.Taikai;

/**
 * Reports the use of deprecated APIs, through Taikai's {@code noUsageOfDeprecatedAPIs}.
 *
 * <p>A deprecation usually arrives with a dependency update rather than with a code change, so this is a
 * {@link RuleSeverity#WARNING}: it should be fixed, yet a run can let it through with
 * {@code -Darchitecture.fail.on=ERROR} while the replacement is planned.
 */
public class NoDeprecatedApiRule extends TaikaiArchitectureRule {

  @Override
  public String name() {
    return "No deprecated API usage (Taikai)";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.WARNING;
  }

  @Override
  protected void configure(Taikai.Builder builder) {
    builder.java(java -> java.noUsageOfDeprecatedAPIs());
  }
}
