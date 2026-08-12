Provides RSA and HMAC signature generation plus AES encryption utilities for test data handling.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-crypto):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-crypto:0.2.3")
}
```

## Environment variables

| Variable             | Description                                           |
| -------------------- | ----------------------------------------------------- |
| `PRIVATE_KEY`        | Base64 or PEM PKCS#8 RSA private key (for RSA tests)  |
| `AES_SECRET_KEY`     | AES secret key (read by `AesEncryption.fromConfig()`) |
| `AES_ALGORITHM_MODE` | AES mode, e.g. `AES/CBC/PKCS5Padding`                 |
| `AES_IV`             | Base64-encoded initialization vector                  |

## Initialization in BaseTest

Define an Owner config interface for the RSA private key (requires `common-utils/config`). `AesEncryption.fromConfig()` already uses Owner internally — no extra setup needed for AES.

```java
@Config.Sources({"system:env"})
interface SignatureConfig extends Config {
    @Key("PRIVATE_KEY") String privateKey();
}
```

Then initialize in `@BeforeClass`:

```java
private static String        privateKey;
private static AesEncryption aesEncryption;

@BeforeClass
public static void initCrypto() {
    SignatureConfig config = ConfigRegistry.get(SignatureConfig.class);
    privateKey    = config.privateKey();
    aesEncryption = AesEncryption.fromConfig();  // reads AES_SECRET_KEY, AES_ALGORITHM_MODE, AES_IV via Owner
}
```

> **Alternative** (without Owner): read values directly via `System.getenv("PRIVATE_KEY")`.

## Usage in tests

```java
@Test
public void signRequestPayload() {
    String body      = "{\"amount\":100}";
    String signature = SignatureGenerator.generateRsaSignature(
        privateKey, body, SignatureAlgorithm.SHA256_WITH_RSA
    );
    given().header("X-Signature", signature)
           .body(body)
           .post("/api/payment");
}

@Test
public void verifyHmacChecksum() {
    String hex = SignatureGenerator.generateHmacSignature("payload-data", "secret-key");
    assertThat(hex).matches("[0-9a-f]+");
}

@Test
public void decryptConfigValue() {
    String plain = aesEncryption.decrypt(System.getenv("ENCRYPTED_TOKEN"));
    assertThat(plain).isNotBlank();
}
```

## Key API

| Method                                                                 | Returns         | Notes                             |
| ---------------------------------------------------------------------- | --------------- | --------------------------------- |
| `SignatureGenerator.generateRsaSignature(privateKey, data, algorithm)` | `String`        | Base64-encoded; key is Base64/PEM |
| `SignatureGenerator.generateHmacSignature(data, key)`                  | `String`        | Lowercase hex                     |
| `AesEncryption.fromConfig()`                                           | `AesEncryption` | Reads env vars via Owner          |
| `aesEncryption.decrypt(encryptedValue)`                                | `String`        | Returns plain text                |
| `EncryptionUtils.encodeBytes(bytes)`                                   | `String`        | Base64 encode                     |
| `EncryptionUtils.decodeString(str)`                                    | `byte[]`        | Base64 decode                     |
