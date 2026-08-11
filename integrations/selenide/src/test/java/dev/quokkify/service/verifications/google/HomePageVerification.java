package dev.quokkify.service.verifications.google;

import dev.quokkify.model.Verification;
import dev.quokkify.page.google.HomePage;
import dev.quokkify.service.steps.google.HomePageSteps;

public class HomePageVerification extends Verification<HomePageSteps, HomePageVerification, HomePage> {

  public HomePageVerification(HomePageSteps steps, HomePage page) {
    super(steps, page);
  }
}
