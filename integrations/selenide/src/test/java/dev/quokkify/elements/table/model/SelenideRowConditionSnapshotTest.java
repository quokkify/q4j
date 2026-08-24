package dev.quokkify.elements.table.model;

import java.time.Duration;

import dev.quokkify.test.BaseTest;

import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class SelenideRowConditionSnapshotTest extends BaseTest {

  private enum Header {
    COUNTRY("Country"),
    COMPANY("Company"),
    EMPLOYEES("Employees");

    private final String displayed;

    Header(String displayed) {
      this.displayed = displayed;
    }
  }

  @Test(description = "Native row waits keep cell reads bound to the current candidate snapshot")
  public void keepsRowConditionCellReadsInsideOneSnapshot() {
    String baseUrl = System.getenv().getOrDefault("NGINX_BASE_URL", "http://localhost");
    Selenide.open(baseUrl + "/table-model-contract/");

    SelenideDomTableModel<Header> model = SelenideDomTableModel.of(
        Selenide.$("#query-classic"), TableDomAdapters.classic(), header -> header.displayed);

    int matchedIndex = model.requiredRowIndex((index, candidate) -> {
      if (index == 1) {
        Selenide.executeJavaScript(
            "const body = document.querySelector('#query-classic tbody');"
                + "body.prepend(body.lastElementChild);");
      }
      return candidate.cell(Header.COMPANY).map(TableCell::text)
          .filter("Berglunds"::equals)
          .isPresent();
    }, "snapshot condition", Duration.ofSeconds(2));

    Assertions.assertThat(matchedIndex).isEqualTo(1);
  }
}
