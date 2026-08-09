package dev.quokkify.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utils for unzipping files.
 */
public final class ZipUtils {

  private static final Logger LOG = LoggerFactory.getLogger(ZipUtils.class);

  private ZipUtils() {
  }

  /**
   * Extracts the first file or directory from the provided ZIP archive.
   * If the ZIP entry is a directory, it creates the directory structure.
   * If the entry is a file, it extracts and writes the file to the same directory as the ZIP file.
   *
   * @param zippedFile the ZIP file to be unzipped
   * @return the extracted {@link File}
   * @throws FileNotFoundException if the file cannot be found
   * @throws IOException           if an I/O error occurs during extraction
   */
  public static File unzipFile(File zippedFile) throws IOException {
    try (ZipFile zipFile = new ZipFile(zippedFile.getPath())) {
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      if (!entries.hasMoreElements()) {
        throw new IOException("ZIP archive is empty: " + zippedFile.getPath());
      }
      ZipEntry entry = entries.nextElement();
      File destinationDirectory = zippedFile.getCanonicalFile().getParentFile();
      File unzippedFile = new File(destinationDirectory, entry.getName()).getCanonicalFile();
      if (!unzippedFile.toPath().startsWith(destinationDirectory.toPath())) {
        throw new IOException("ZIP entry is outside the destination directory: " + entry.getName());
      }
      boolean isCreated;
      if (entry.isDirectory()) {
        isCreated = unzippedFile.mkdirs();
      } else {
        File parent = unzippedFile.getParentFile();
        if (parent != null) {
          isCreated = parent.mkdirs();
        } else {
          isCreated = false;
        }
        try (InputStream in = zipFile.getInputStream(entry);
             OutputStream out = new FileOutputStream(unzippedFile)) {
          IOUtils.copy(in, out);
        }
      }
      if (isCreated) {
        LOG.debug("Directory for '{}' file is created", unzippedFile.getPath());
      }
      return unzippedFile;
    }
  }
}
