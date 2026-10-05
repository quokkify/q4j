package dev.quokkify.architecture.rules;

import dev.quokkify.architecture.contract.RuleSeverity;
import dev.quokkify.architecture.taikai.TaikaiArchitectureRule;

import com.enofex.taikai.Taikai;

/**
 * General Java conventions taken from Taikai's {@code java} and {@code logging} rule sets.
 *
 * <p>Only conventions that hold for main and test code alike are included: Taikai evaluates them against every
 * class of the run, tests included.
 *
 * <ul>
 *   <li>a class overriding {@code equals} or {@code hashCode} overrides both;</li>
 *   <li>{@code serialVersionUID} is {@code static final long};</li>
 *   <li>package names follow the Java convention, interfaces carry no {@code I} prefix;</li>
 *   <li>a Log4j or SLF4J logger field is named {@code LOG}.</li>
 * </ul>
 */
public class JavaConventionsRule extends TaikaiArchitectureRule {

  private static final String LOGGER_NAME = "LOG";

  @Override
  public String name() {
    return "Java conventions (Taikai)";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  protected void configure(Taikai.Builder builder) {
    builder
        .java(java -> java
            .classesShouldImplementHashCodeAndEquals()
            .serialVersionUIDFieldsShouldBeStaticFinalLong()
            .naming(naming -> naming
                .packagesShouldMatchDefault()
                .interfacesShouldNotHavePrefixI()))
        .logging(logging -> logging
            .loggersShouldFollowConventions("org.apache.logging.log4j.Logger", LOGGER_NAME)
            .loggersShouldFollowConventions("org.slf4j.Logger", LOGGER_NAME));
  }
}
