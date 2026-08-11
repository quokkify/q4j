package dev.quokkify.model;

/**
 * POJOs for Reqres API user request/response.
 */
public final class ReqresUserPojo {

  /**
   * Request payload for creating/updating a user.
   */
  public record Request(
      String name,
      String job
  ) {

  }

  /**
   * Response payload returned after creating/updating a user.
   */
  public record Response(
      String createdAt,
      String name,
      String id,
      String job
  ) {

  }
}
