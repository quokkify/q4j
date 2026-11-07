package dev.quokkify.arch;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import dev.quokkify.archgate.api.ArchitectureRule;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.api.Severity;
import dev.quokkify.archgate.context.archunit.ArchUnitContext;

import com.tngtech.archunit.core.domain.JavaClass;

/**
 * Verifies that every package of main and test code follows the Java naming convention: each segment is lower
 * case letters and digits, starts with a letter, and contains no underscore.
 */
public class PackageNamingRule implements ArchitectureRule<ArchUnitContext> {

  private static final Pattern CONVENTIONAL = Pattern.compile("[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*");
  private static final String EXPECTED_CONTRACT = """
      Package names follow the Java convention: every segment is lower case letters and digits, starts with a \
      letter and contains no underscore. Rename the package.""";

  @Override
  public String name() {
    return "Package names follow the Java convention";
  }

  @Override
  public Severity severity() {
    return Severity.ERROR;
  }

  @Override
  public Set<Scope> scopes() {
    return EnumSet.of(Scope.MAIN, Scope.TEST);
  }

  @Override
  public Class<ArchUnitContext> contextType() {
    return ArchUnitContext.class;
  }

  @Override
  public void verify(ArchUnitContext context) {
    List<String> violations = context.all().stream()
        .map(JavaClass::getPackageName)
        .distinct()
        .filter(name -> !isConventional(name))
        .sorted()
        .map("package %s"::formatted)
        .toList();
    checkViolations(EXPECTED_CONTRACT, violations);
  }

  static boolean isConventional(String packageName) {
    return CONVENTIONAL.matcher(packageName).matches();
  }
}
