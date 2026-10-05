package dev.quokkify.architecture.rules;

import java.util.List;

import dev.quokkify.architecture.contract.ArchitectureRule;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The rules shipped here are opt in: this module registers none of them, a consumer lists the ones it wants in
 * its own {@code META-INF/services} file. That only works when every rule can be created by the
 * {@link java.util.ServiceLoader}, which is asserted here for every rule class, including future ones.
 */
public class RuleCatalogTest {

  private static final String RULES_PACKAGE = "dev.quokkify.architecture.rules";

  @Test
  public void everyShippedRuleCanBeRegisteredThroughTheServiceLoader() {
    List<Class<ArchitectureRule>> rules = shippedRules();

    assertThat(rules).isNotEmpty().allSatisfy(ruleClass -> {
      ArchitectureRule rule = ruleClass.getConstructor().newInstance();
      assertThat(rule.name()).isNotBlank();
      assertThat(rule.severity()).isNotNull();
    });
  }

  private static List<Class<ArchitectureRule>> shippedRules() {
    try (ScanResult scan = new ClassGraph().acceptPackages(RULES_PACKAGE).enableClassInfo().scan()) {
      return scan.getClassesImplementing(ArchitectureRule.class).stream()
          .filter(rule -> !rule.isAbstract() && !rule.isInterface() && !rule.isAnonymousInnerClass())
          .map(ClassInfo::getName)
          .map(RuleCatalogTest::load)
          .toList();
    }
  }

  @SuppressWarnings("unchecked")
  private static Class<ArchitectureRule> load(String name) {
    try {
      return (Class<ArchitectureRule>) Class.forName(name);
    } catch (ClassNotFoundException missing) {
      throw new IllegalStateException(missing);
    }
  }
}
