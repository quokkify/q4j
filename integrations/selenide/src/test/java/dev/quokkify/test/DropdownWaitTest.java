package dev.quokkify.test;

import dev.quokkify.page.local.DelayedDropdownPage;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class DropdownWaitTest extends BaseTest {

  private static final String DELAYED_DROPDOWN_HTML = """
      <!doctype html>
      <html lang="en">
        <head>
          <meta charset="utf-8" />
          <title>Delayed dropdown</title>
          <style>
            .option {
              cursor: pointer;
              padding: 4px 8px;
            }
          </style>
        </head>
        <body>
          <div id="custom-dropdown" role="application">
            <input
              id="custom-dropdown-input"
              type="text"
              role="combobox"
              aria-expanded="false"
              aria-controls="custom-dropdown-listbox"
              placeholder="Search fruit"
            />
            <button id="custom-dropdown-toggle" type="button">Open fruit menu</button>
            <div id="custom-dropdown-selected" class="item"></div>
            <div id="custom-dropdown-listbox" role="listbox" hidden>
              <div class="option" role="option">Banana</div>
            </div>
          </div>

          <script>
            (function () {
              const input = document.getElementById("custom-dropdown-input");
              const toggle = document.getElementById("custom-dropdown-toggle");
              const listbox = document.getElementById("custom-dropdown-listbox");
              const selected = document.getElementById("custom-dropdown-selected");

              function setOpen(isOpen) {
                listbox.hidden = !isOpen;
                input.setAttribute("aria-expanded", String(isOpen));
              }

              function ensureDelayedOption() {
                if (listbox.querySelector('[data-delayed="true"]')) {
                  return;
                }

                const delayed = document.createElement("div");
                delayed.className = "option";
                delayed.setAttribute("role", "option");
                delayed.setAttribute("data-delayed", "true");
                delayed.textContent = "Dragon Fruit";
                delayed.addEventListener("click", function () {
                  selected.textContent = delayed.textContent;
                  setOpen(false);
                });
                listbox.appendChild(delayed);
              }

              toggle.addEventListener("click", function () {
                const shouldOpen = listbox.hidden;
                setOpen(shouldOpen);
                if (shouldOpen) {
                  setTimeout(ensureDelayedOption, 1500);
                }
              });

              input.addEventListener("keydown", function (event) {
                if (event.key === "Escape") {
                  setOpen(false);
                }
              });

              listbox.querySelectorAll('[role="option"]').forEach(function (option) {
                option.addEventListener("click", function () {
                  selected.textContent = option.textContent;
                  setOpen(false);
                });
              });

            })();
          </script>
        </body>
      </html>
      """;

  private HttpServer dropdownServer;

  @TmsLink("UI_ID_24")
  @Test(description = "Verify custom dropdown waits for a delayed option and selects it by exact text")
  public void testCustomDropdownWaitsForDelayedOption() throws IOException {
    try {
      DelayedDropdownPage page = Selenide.open(startDropdownServer(), DelayedDropdownPage.class);

      page.selectDelayedFruit("Dragon Fruit");

      Assertions.assertThat(page.getSelectedFruit()).isEqualTo("Dragon Fruit");
      Assertions.assertThat(page.isDropdownClosed()).isTrue();
    } finally {
      if (dropdownServer != null) {
        dropdownServer.stop(0);
      }
    }
  }

  @AfterMethod(alwaysRun = true)
  public void stopDropdownServer() {
    if (dropdownServer != null) {
      dropdownServer.stop(0);
      dropdownServer = null;
    }
  }

  private String startDropdownServer() throws IOException {
    dropdownServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    dropdownServer.createContext("/dropdown.html", exchange -> {
      byte[] response = DELAYED_DROPDOWN_HTML.getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
      exchange.sendResponseHeaders(200, response.length);
      try (OutputStream output = exchange.getResponseBody()) {
        output.write(response);
      }
    });
    dropdownServer.start();
    return "http://127.0.0.1:" + dropdownServer.getAddress().getPort() + "/dropdown.html";
  }
}
