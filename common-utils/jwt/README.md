Generates RS512-signed JWT tokens for use in test authentication flows.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-jwt):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-jwt:0.2.3")
}
```

## Environment variables

| Variable          | Description                          |
| ----------------- | ------------------------------------ |
| `JWT_PRIVATE_KEY` | Base64-encoded RSA private key (PEM) |
| `JWT_PUBLIC_KEY`  | Base64-encoded RSA public key (PEM)  |

## Initialization in BaseTest

Define an Owner config interface to read env vars in a type-safe way (requires `common-utils/config`):

```java
@Config.Sources({"system:env"})
interface JwtConfig extends Config {
    @Key("JWT_PRIVATE_KEY") String privateKey();
    @Key("JWT_PUBLIC_KEY")  String publicKey();
}
```

Then initialize in `@BeforeClass`:

```java
private static JwtKeyPair keyPair;
private static Header header;

@BeforeClass
public static void initJwt() {
    JwtConfig config = ConfigRegistry.get(JwtConfig.class);
    keyPair = JwtKeyPairGenerator.generateRs512(config.privateKey(), config.publicKey());
    header  = JwtHeaderGenerator.generateRs512("key-id-1");
}
```

> **Alternative** (without Owner): read values directly via `System.getenv("JWT_PRIVATE_KEY")`, `System.getenv("JWT_PUBLIC_KEY")`.

## Usage in tests

```java
@Test
public void loginWithGeneratedToken() {
    Payload payload = JwtPayloadGenerator.generate("session-abc", "user-42", "10.0.0.1");
    String token = JwtTokenGenerator.generateAsString(keyPair, header, payload);

    given().header("Authorization", "Bearer " + token)
           .get("/api/profile")
           .then().statusCode(200);
}
```

For tests that don't need a real key pair:

```java
JwtKeyPair randomPair = JwtKeyPairGenerator.generateRs512();
JwtToken   jwtToken   = JwtTokenGenerator.generate(randomPair, header, payload);
String     raw        = jwtToken.token();
```

## Key API

| Method                                                         | Returns      | Notes                           |
| -------------------------------------------------------------- | ------------ | ------------------------------- |
| `JwtKeyPairGenerator.generateRs512(privateKey, publicKey)`     | `JwtKeyPair` | Keys are Base64-encoded strings |
| `JwtKeyPairGenerator.generateRs512()`                          | `JwtKeyPair` | Random pair, for isolated tests |
| `JwtHeaderGenerator.generateRs512(keyId)`                      | `Header`     | Reuse across tests              |
| `JwtPayloadGenerator.generate(session, userId, ip)`            | `Payload`    | Sets `iat=now`, `exp=tomorrow`  |
| `JwtPayloadGenerator.generate(userId, ip)`                     | `Payload`    | Minimal payload, no session     |
| `JwtTokenGenerator.generate(keyPair, header, payload)`         | `JwtToken`   | Use `.token()` for raw string   |
| `JwtTokenGenerator.generateAsString(keyPair, header, payload)` | `String`     | Convenience shorthand           |
