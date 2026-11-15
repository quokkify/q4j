package dev.quokkify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for tests that should not run in parallel with each other.
 * {@link dev.quokkify.listener.lifecycle.SuiteListener} places annotated tests in the generated
 * {@code Sequential} test, where they are executed one at a time.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Sequential {
}
