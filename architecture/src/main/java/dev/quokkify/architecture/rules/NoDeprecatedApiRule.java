package dev.quokkify.architecture.rules;

import dev.quokkify.architecture.contract.ClassScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.taikai.TaikaiArchitectureRule;

import com.enofex.taikai.Taikai;

/**
 * Reports the use of deprecated APIs, through Taikai's {@code noUsageOfDeprecatedAPIs}.
 *
 * <p>A deprecation should be fixed, but it usually arrives with a dependency update, and some cannot be avoided,
 * such as an SPI method whose required signature uses a deprecated type. This is therefore a
 * {@link RuleSeverity#WARNING}: reported on every run, and failing only where a build opts in with
 * {@code -Darchitecture.fail.on=WARNING}.
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
  protected ClassScope scope() {
    return ClassScope.ALL;
  }

  @Override
  protected void configure(Taikai.Builder builder) {
    builder.java(java -> java.noUsageOfDeprecatedAPIs());
  }
}
