package dev.quokkify.model;

import java.security.KeyPair;

import io.jsonwebtoken.security.SignatureAlgorithm;

/**
 * JWT key pair object.
 */
public record JwtKeyPair(
    SignatureAlgorithm algorithm,
    KeyPair keyPair
) {

}
