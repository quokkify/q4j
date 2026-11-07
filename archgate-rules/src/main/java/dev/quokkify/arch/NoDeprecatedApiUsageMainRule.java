package dev.quokkify.arch;

import java.util.EnumSet;
import java.util.Set;

import dev.quokkify.archgate.api.ProjectLayout;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;
import dev.quokkify.archgate.rules.archunit.NoDeprecatedApiUsageRule;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;

/**
 * ArchGate's {@link NoDeprecatedApiUsageRule} restricted to main classes, for modules without tests.
 *
 * <p>The upstream rule checks the main and the test view, and an empty test view cannot be verified. This rule
 * runs the upstream check unchanged, with the same contract, severity and findings, but hands it an empty test
 * view, so only main classes are checked. Modules with tests register the upstream rule instead.
 */
public class NoDeprecatedApiUsageMainRule extends NoDeprecatedApiUsageRule {

  @Override
  public String name() {
    return super.name() + " (main)";
  }

  @Override
  public Set<Scope> scopes() {
    return EnumSet.of(Scope.MAIN);
  }

  @Override
  public void verify(ArchUnitContext context) {
    super.verify(new MainOnlyContext(context));
  }

  /**
   * Delegates to the shared context, except that the test view is empty.
   */
  private record MainOnlyContext(ArchUnitContext delegate) implements ArchUnitContext {

    @Override
    public String rootPackage() {
      return delegate.rootPackage();
    }

    @Override
    public JavaClasses main() {
      return delegate.main();
    }

    @Override
    public JavaClasses test() {
      return new ClassFileImporter().importClasses();
    }

    @Override
    public JavaClasses all() {
      return delegate.main();
    }

    @Override
    public ProjectLayout layout() {
      return delegate.layout();
    }
  }
}
