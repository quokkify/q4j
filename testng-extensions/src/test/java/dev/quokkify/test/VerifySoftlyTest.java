package dev.quokkify.test;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.step.AbstractSteps;

import io.qameta.allure.Step;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class VerifySoftlyTest {

  @TmsLink("VERIFY_SOFTLY_ID_1")
  @TestGroup("TestNG")
  @Test(description = "Verify Softly")
  public void testVerifySoftly() {
    new VerifySoftSteps()
        .verifySoftly(
            verification -> verification.verifyString("Test"),
            verification -> verification.verifyInteger(1),
            verification -> verification.verifyBoolean(true));
  }

  public class VerifySoftSteps extends AbstractSteps<VerifySoftVerification> {

    private final VerifySoftVerification verification;

    public VerifySoftSteps() {
      verification = new VerifySoftVerification();
    }

    @Override
    public VerifySoftVerification verify() {
      return verification;
    }
  }

  public class VerifySoftVerification {

    @Step("Verify String")
    public VerifySoftVerification verifyString(String string) {
      Assertions.assertThat("Test").as("String is incorrect").isEqualTo(string);
      return this;
    }

    @Step("Verify Integer")
    public VerifySoftVerification verifyInteger(Integer integer) {
      Assertions.assertThat(1).as("Integer is incorrect").isEqualTo(integer);
      return this;
    }

    @Step("Verify Boolean")
    public VerifySoftVerification verifyBoolean(Boolean booleanValue) {
      Assertions.assertThat(true).as("Boolean is incorrect").isEqualTo(booleanValue);
      return this;
    }
  }
}
