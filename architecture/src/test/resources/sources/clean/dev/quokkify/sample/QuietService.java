package dev.quokkify.sample;

import java.io.PrintWriter;
import java.io.StringWriter;

public class QuietService {

  public String describe(Exception failure) {
    StringWriter trace = new StringWriter();
    failure.printStackTrace(new PrintWriter(trace));
    return trace.toString();
  }
}
