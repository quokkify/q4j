package dev.quokkify.architecture.rules;

import java.util.Set;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.ArchitectureRule;
import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the scopes the published rules declare for the report.
 */
public class RuleScopesTest {

  @DataProvider
  public Object[][] publishedRules() {
    return new Object[][]{
        {new JavaConventionsRule(), Set.of(RuleScope.MAIN, RuleScope.TEST)},
        {new NoConsoleOutputRule(), Set.of(RuleScope.MAIN)},
        {new NoDeprecatedApiRule(), Set.of(RuleScope.MAIN, RuleScope.TEST)},
        {new ServiceRegistrationRule(), Set.of(RuleScope.RESOURCES)},
        {new TestClassNamingRule(), Set.of(RuleScope.TEST)},
    };
  }

  @Test(dataProvider = "publishedRules")
  public void publishedRuleDeclaresWhatItVerifies(ArchitectureRule rule, Set<RuleScope> expected) {
    assertThat(rule.scopes()).as(rule.name()).isEqualTo(expected);
  }

  @Test
  public void ruleWithoutDeclaredScopeReportsNone() {
    ArchitectureRule legacy = new ArchitectureRule() {

      @Override
      public String name() {
        return "legacy rule";
      }

      @Override
      public RuleSeverity severity() {
        return RuleSeverity.INFO;
      }

      @Override
      public void verify(ArchitectureContext context) {
      }
    };

    assertThat(legacy.scopes()).isEmpty();
  }
}
