package dev.quokkify.steps;

import dev.quokkify.step.AbstractSteps;
import dev.quokkify.verification.MongoVerification;

/**
 * @deprecated Use {@code dev.quokkify:mongodb} ({@code dev.quokkify.mongodb.*}); this module will be removed.
 */
@Deprecated(since = "0.14.0", forRemoval = true)
public abstract class AbstractMongoSteps<V extends MongoVerification> extends AbstractSteps<V> {
}
