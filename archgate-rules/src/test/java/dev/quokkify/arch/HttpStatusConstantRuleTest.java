package dev.quokkify.arch;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import dev.quokkify.archgate.api.ArchitectureViolation;
import dev.quokkify.archgate.api.Scope;
import dev.quokkify.archgate.api.Severity;
import dev.quokkify.archgate.api.UnableToVerifyError;
import dev.quokkify.archgate.context.javaparser.JavaParserContext;
import dev.quokkify.archgate.context.javaparser.JavaParserContextProvider;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpStatusConstantRuleTest {

  private static final String ROOT_PACKAGE = "dev.quokkify.sample";

  @Test
  public void statusLiteralsInApiPackageAreReportedWithTheirConstant() {
    assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(contextFor("violating")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (2)")
        .hasMessageContaining("dev/quokkify/sample/test/api/LiteralStatusTest.java line 10: "
            + "verifyResponseStatusCode(...) passes 200; use HttpStatus.SC_OK")
        .hasMessageContaining("line 11: verifyStatus(...) passes 401; use HttpStatus.SC_UNAUTHORIZED")
        .as("constants, non-status methods and numbers outside 100-599 are not status literals")
        .hasMessageNotContaining("line 16")
        .hasMessageNotContaining("line 17")
        .hasMessageNotContaining("line 18");
  }

  @Test
  public void constantsPass() {
    assertThatCode(() -> new HttpStatusConstantRule("api").verify(contextFor("compliant")))
        .doesNotThrowAnyException();
  }

  @Test
  public void apiGroupMarksOnlyItsOwnMethods() {
    assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(contextFor("grouped")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (1)")
        .hasMessageContaining("passes 404; use HttpStatus.SC_NOT_FOUND")
        .hasMessageNotContaining("500");
  }

  @Test
  public void classLevelApiGroupMarksTheWholeClassAndNonStatusMethodsAreIgnored() {
    assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(contextFor("class-grouped")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (1)")
        .hasMessageContaining("expectStatus(...) passes 503; use HttpStatus.SC_SERVICE_UNAVAILABLE")
        .as("a timeout setter whose name merely contains status is not a status check")
        .hasMessageNotContaining("300");
  }

  @Test
  public void customMarkerSelectsItsPackage() {
    assertThatThrownBy(() -> new HttpStatusConstantRule("http").verify(contextFor("custom")))
        .isInstanceOf(ArchitectureViolation.class)
        .hasMessageContaining("Violations (2)");
  }

  @Test
  public void testSourcesWithoutApiTestsMeanTheMarkerIsWrong() {
    assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(contextFor("unmarked")))
        .as("a marker that selects nothing proves nothing and must not pass silently")
        .isInstanceOf(UnableToVerifyError.class)
        .hasMessageContaining(HttpStatusConstantRule.MARKER_VARIABLE);
  }

  @Test
  public void projectWithoutTestSourcesIsUnableToVerify() {
    JavaParserContext empty = new JavaParserContextProvider().create(FixtureLayout.testSources(ROOT_PACKAGE, List.of()));
    assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(empty))
        .as("a rule that checked nothing must not pass")
        .isInstanceOf(UnableToVerifyError.class);
  }

  @Test
  public void ruleIsAnErrorOverTestSources() {
    HttpStatusConstantRule rule = new HttpStatusConstantRule();
    assertThat(rule.severity()).isEqualTo(Severity.ERROR);
    assertThat(rule.scopes()).isEqualTo(Set.of(Scope.TEST));
    assertThat(rule.contextType()).isEqualTo(JavaParserContext.class);
  }

  private static JavaParserContext contextFor(String fixture) {
    return new JavaParserContextProvider().create(FixtureLayout.testSources(ROOT_PACKAGE, List.of(sourceRoot(fixture))));
  }

  private static Path sourceRoot(String fixture) {
    try {
      return Path.of(Objects.requireNonNull(
          HttpStatusConstantRuleTest.class.getResource("/http-status-sources/" + fixture), fixture).toURI());
    } catch (URISyntaxException invalid) {
      throw new IllegalStateException(invalid);
    }
  }
}
