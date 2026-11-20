package dev.quokkify.elements.table;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import org.openqa.selenium.remote.RemoteWebDriver;

/**
 * The browser script behind {@link Table#snapshot()}. It is prefixed with Selenium's own {@code isDisplayed} atom, the
 * one {@code WebElement.isDisplayed()} runs, so the snapshot judges visibility exactly like the lazy API.
 */
final class SnapshotScript {

  private static final String VISIBILITY_ATOM = "isDisplayed.js";
  private static final String SCRIPT = "snapshot.js";

  static final String SOURCE = "const isShown = " + load(RemoteWebDriver.class, VISIBILITY_ATOM) + ";\n"
      + load(SnapshotScript.class, SCRIPT);

  private SnapshotScript() {
  }

  private static String load(Class<?> anchor, String resource) {
    try (InputStream in = anchor.getResourceAsStream(resource)) {
      if (Objects.isNull(in)) {
        throw new IllegalStateException("missing classpath resource " + resource + " next to " + anchor);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8).strip();
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + resource, e);
    }
  }
}
