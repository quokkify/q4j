package dev.quokkify.step;

import dev.quokkify.helper.MockApiHelper;
import dev.quokkify.model.CatFactPojo;

import io.qameta.allure.Step;

public class CatFactApiSteps extends ApiSteps<CatFactApiVerification> {

  public CatFactApiSteps() {
    this.verification = new CatFactApiVerification();
  }

  @Step("Get Cat Fact")
  public CatFactPojo getCatFact() {
    return MockApiHelper.getCatFact();
  }
}
