package dev.quokkify.verification;

/**
 * @deprecated Use {@code dev.quokkify:mongodb} ({@code dev.quokkify.mongodb.*}); this module will be removed.
 */
@Deprecated(since = "0.14.0", forRemoval = true)
public final class MongoVerifier extends BaseMongoVerification<MongoVerifier> {

  @Override
  protected MongoVerifier self() {
    return this;
  }
}
