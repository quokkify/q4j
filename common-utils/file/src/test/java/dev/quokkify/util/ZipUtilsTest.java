package dev.quokkify.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ZipUtilsTest {

  private Path temporaryDirectory;
  private Path escapedFile;

  @BeforeMethod
  public void createTemporaryDirectory() throws IOException {
    temporaryDirectory = Files.createTempDirectory("q4j-zip-utils-");
    escapedFile = null;
  }

  @AfterMethod
  public void deleteTemporaryDirectory() throws IOException {
    if (escapedFile != null) {
      Files.deleteIfExists(escapedFile);
    }
    try (var paths = Files.walk(temporaryDirectory)) {
      paths.sorted(Comparator.reverseOrder())
          .map(Path::toFile)
          .forEach(File::delete);
    }
  }

  @Test
  public void testUnzipFileExtractsNestedFile() throws IOException {
    Path archive = createArchive("nested/file.txt", "content");

    File extracted = ZipUtils.unzipFile(archive.toFile());

    Assertions.assertThat(extracted.toPath())
        .isEqualTo(temporaryDirectory.resolve("nested/file.txt").toRealPath());
    Assertions.assertThat(extracted).hasContent("content");
  }

  @Test
  public void testUnzipFileRejectsEntryOutsideDestinationDirectory() throws IOException {
    String escapedFileName = temporaryDirectory.getFileName() + "-escaped.txt";
    Path archive = createArchive("../" + escapedFileName, "malicious content");
    escapedFile = temporaryDirectory.resolveSibling(escapedFileName);

    Assertions.assertThatThrownBy(() -> ZipUtils.unzipFile(archive.toFile()))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("outside the destination directory");
    Assertions.assertThat(escapedFile).doesNotExist();
  }

  private Path createArchive(String entryName, String content) throws IOException {
    Path archive = temporaryDirectory.resolve("archive.zip");
    try (OutputStream output = Files.newOutputStream(archive);
         ZipOutputStream zipOutput = new ZipOutputStream(output)) {
      zipOutput.putNextEntry(new ZipEntry(entryName));
      zipOutput.write(content.getBytes(StandardCharsets.UTF_8));
      zipOutput.closeEntry();
    }
    return archive;
  }
}
