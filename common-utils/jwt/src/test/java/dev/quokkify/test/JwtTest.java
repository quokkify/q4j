package dev.quokkify.test;

import java.util.Locale;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.formatter.JwtKeyPairFormatter;
import dev.quokkify.formatter.JwtTokenFormatter;
import dev.quokkify.generator.JwtHeaderGenerator;
import dev.quokkify.generator.JwtKeyPairGenerator;
import dev.quokkify.generator.JwtPayloadGenerator;
import dev.quokkify.generator.JwtTokenGenerator;
import dev.quokkify.model.Header;
import dev.quokkify.model.JwtKeyPair;
import dev.quokkify.model.JwtToken;
import dev.quokkify.model.Payload;
import dev.quokkify.util.JsonConverter;

import io.qameta.allure.TmsLink;
import net.datafaker.Faker;
import org.assertj.core.api.SoftAssertions;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class JwtTest {

  private static final Faker FAKER = new Faker(Locale.ENGLISH);
  private Header header;
  private Payload payload;
  private JwtKeyPair generatedJwtKeyPair;

  @BeforeClass
  public void prepareTestData() {
    long userId = FAKER.number().randomNumber(15);
    String ip = FAKER.internet().ipV4Address();
    header = JwtHeaderGenerator.generateRs512(FAKER.number().digit());
    payload = JwtPayloadGenerator.generate(userId, ip);
    generatedJwtKeyPair = JwtKeyPairGenerator.generateRs512();
  }

  @TmsLink("JWT_ID_1")
  @TestGroup("Jwt")
  @Test(description = "Verify JWT functionality using JwtKeyPair")
  public void testJwtFunctionalityUsingJwtKeyPair() {
    JwtToken jwtToken = JwtTokenGenerator.generate(generatedJwtKeyPair, header, payload);

    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(jwtToken.header()).as("Header is incorrect").isEqualTo(header);
      softly.assertThat(jwtToken.payload()).as("Payload is incorrect").isEqualTo(payload);
    });
  }

  @TmsLink("JWT_ID_2")
  @TestGroup("Jwt")
  @Test(description = "Verify JWT functionality using private and public keys")
  public void testJwtFunctionalityPrivatePublicKeys() {
    String publicKey = JwtKeyPairFormatter.formatPublicKey(generatedJwtKeyPair);
    String privateKey = JwtKeyPairFormatter.formatPrivateKey(generatedJwtKeyPair);
    JwtKeyPair jwtKeyPair = JwtKeyPairGenerator.generateRs512(privateKey, publicKey);
    JwtToken jwtToken = JwtTokenGenerator.generate(jwtKeyPair, header, payload);

    SoftAssertions.assertSoftly(softly -> {
      softly.assertThat(jwtToken.header()).as("Header is incorrect").isEqualTo(header);
      softly.assertThat(jwtToken.payload()).as("Payload is incorrect").isEqualTo(payload);
    });
  }

  @TmsLink("JWT_ID_3")
  @TestGroup("Jwt")
  @Test(description = "Decrypt payload JSON from compact JWT and map to Payload")
  public void testDecryptPayloadFromJwt() {
    String compactJwt = JwtTokenGenerator.generateAsString(generatedJwtKeyPair, header, payload);
    String payloadJson = JwtTokenFormatter.decryptPayloadAsString(generatedJwtKeyPair, compactJwt);
    Payload actual = JsonConverter.fromString(payloadJson, Payload.class);
    SoftAssertions.assertSoftly(softly -> softly.assertThat(actual)
        .as("Decrypted payload is incorrect")
        .isEqualTo(payload));
  }
}
