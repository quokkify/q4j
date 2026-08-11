package dev.quokkify.model;

import java.io.File;
import java.util.Map;

/**
 * Interface to configure the file upload parameters.
 */
public interface FileParams {

  String getFileParamName();

  String getFileContentType();

  File getFile();

  Map<String, String> getParameters();
}
