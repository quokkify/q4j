package dev.quokkify.test;

import java.time.Duration;

import dev.quokkify.elements.table.model.RowConditions;
import dev.quokkify.elements.table.model.SelenideTableQuery;
import dev.quokkify.elements.table.model.TableDomAdapters;
import dev.quokkify.elements.table.model.TableQueryRow;

import com.codeborne.selenide.Selenide;
import org.testng.annotations.Test;

public class ReproHorizontalAsyncTest extends BaseTest {

  private enum Dh {
    NAME("Name"), TELEPHONE_1("Telephone 1"), TELEPHONE_2("Telephone 2");
    private final String displayed;

    Dh(String displayed) {
      this.displayed = displayed;
    }
  }

  @Test(description = "FIXED: requiredRow waits for a delayed HORIZONTAL column row (N1)")
  public void requiredRowWaitsForDelayedHorizontalRow() {
    String baseUrl = System.getenv().getOrDefault("NGINX_BASE_URL", "http://localhost");
    Selenide.open(baseUrl + "/table/delayed-table.html");

    SelenideTableQuery<Dh> query = SelenideTableQuery.of(
        Selenide.$("#horizontal-customers"), TableDomAdapters.horizontal(), key -> key.displayed);

    // 'Telephone 2' row is appended after ~1500ms. The query must keep polling and find it.
    TableQueryRow<Dh> row = query.requiredRow(
        RowConditions.exact(Dh.TELEPHONE_2, "555 77 855"), Duration.ofSeconds(5));

    row.requiredCell(Dh.TELEPHONE_2).text().equals("555 77 855");
  }
}
