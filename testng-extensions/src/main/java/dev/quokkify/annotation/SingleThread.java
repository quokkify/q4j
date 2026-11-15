package dev.quokkify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used for tests that should be executed in a single thread.
 *
 * @deprecated use {@link Sequential} instead
 */
@Deprecated(since = "0.16.0", forRemoval = true)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SingleThread {
}
