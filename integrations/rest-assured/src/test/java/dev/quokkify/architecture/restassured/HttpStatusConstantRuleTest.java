package dev.quokkify.architecture.restassured;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import dev.quokkify.architecture.contract.ArchitectureContext;
import dev.quokkify.architecture.contract.RuleScope;
import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.exceptions.ArchitectureRunnerError;
import dev.quokkify.architecture.exceptions.ArchitectureViolationException;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class HttpStatusConstantRuleTest {

  private static final List<String> PACKAGES = List.of("dev.quokkify.sample");

  @Test
  public void statusLiteralsInApiPackageAreReportedWithTheirConstant() {
    try (ArchitectureContext context = contextFor("violating")) {
      assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (2)")
          .hasMessageContaining("line 10: verifyResponseStatusCode(...) passes 200; use HttpStatus.SC_OK")
          .hasMessageContaining("line 11: verifyStatus(...) passes 401; use HttpStatus.SC_UNAUTHORIZED")
          .as("constants, non-status methods and numbers outside 100-599 are not status literals")
          .hasMessageNotContaining("line 16")
          .hasMessageNotContaining("line 17")
          .hasMessageNotContaining("line 18");
    }
  }

  @Test
  public void constantsPass() {
    try (ArchitectureContext context = contextFor("compliant")) {
      assertThatCode(() -> new HttpStatusConstantRule("api").verify(context)).doesNotThrowAnyException();
    }
  }

  @Test
  public void apiGroupMarksOnlyItsOwnMethods() {
    try (ArchitectureContext context = contextFor("grouped")) {
      assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (1)")
          .hasMessageContaining("passes 404; use HttpStatus.SC_NOT_FOUND")
          .hasMessageNotContaining("500");
    }
  }

  @Test
  public void classLevelApiGroupMarksTheWholeClassAndNonStatusMethodsAreIgnored() {
    try (ArchitectureContext context = contextFor("class-grouped")) {
      assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (1)")
          .hasMessageContaining("expectStatus(...) passes 503; use HttpStatus.SC_SERVICE_UNAVAILABLE")
          .as("a timeout setter whose name merely contains status is not a status check")
          .hasMessageNotContaining("300");
    }
  }

  @Test
  public void customMarkerSelectsItsPackage() {
    try (ArchitectureContext context = contextFor("custom")) {
      assertThatThrownBy(() -> new HttpStatusConstantRule("http").verify(context))
          .isInstanceOf(ArchitectureViolationException.class)
          .hasMessageContaining("Violations (2)");
    }
  }

  @Test
  public void testSourcesWithoutApiTestsMeanTheMarkerIsWrong() {
    try (ArchitectureContext context = contextFor("unmarked")) {
      assertThatThrownBy(() -> new HttpStatusConstantRule("api").verify(context))
          .as("a marker that selects nothing proves nothing and must not pass silently")
          .isInstanceOf(ArchitectureRunnerError.class)
          .hasMessageContaining(HttpStatusConstantRule.MARKER_VARIABLE);
    }
  }

  @Test
  public void projectWithoutTestSourcesHasNothingToVerify() {
    try (ArchitectureContext context = ArchitectureContext.builder(PACKAGES).testSources(List.of()).build()) {
      assertThatCode(() -> new HttpStatusConstantRule("api").verify(context)).doesNotThrowAnyException();
    }
  }

  @Test
  public void ruleIsAMandatoryTestScopeContract() {
    HttpStatusConstantRule rule = new HttpStatusConstantRule();
    assertThat(rule.severity()).isEqualTo(RuleSeverity.ERROR);
    assertThat(rule.scopes()).containsExactly(RuleScope.TEST);
  }

  private static ArchitectureContext contextFor(String fixture) {
    return ArchitectureContext.builder(PACKAGES).mainSources(List.of()).testSources(List.of(sourceRoot(fixture))).build();
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
