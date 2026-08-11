package dev.quokkify.model;

/**
 * JWT token object.
 */
public record JwtToken(
    Header header,
    Payload payload,
    String token
) {

}
