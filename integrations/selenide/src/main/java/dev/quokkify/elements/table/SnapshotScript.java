package dev.quokkify.elements.table;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

final class SnapshotScript {

  private static final String RESOURCE = "snapshot.js";

  static final String SOURCE = load();

  private SnapshotScript() {
  }

  private static String load() {
    try (InputStream in = SnapshotScript.class.getResourceAsStream(RESOURCE)) {
      if (Objects.isNull(in)) {
        throw new IllegalStateException("missing classpath resource " + RESOURCE + " next to " + SnapshotScript.class);
      }
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new UncheckedIOException("cannot read " + RESOURCE, e);
    }
  }
}
