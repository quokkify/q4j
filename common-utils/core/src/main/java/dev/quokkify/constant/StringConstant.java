package dev.quokkify.constant;

import dev.quokkify.model.StringFormat;

public class StringConstant {

  public static final String DOT = ".";
  public static final String COMMA = ",";
  public static final String COMMA_SPACE = ", ";
  public static final String DOUBLE_APOSTROPHE = "''";
  public static final String APOSTROPHE = "'";
  public static final String SEMICOLON = ";";
  public static final String SEMICOLON_SPACE = "; ";
  public static final String COLON = ":";
  public static final String CARET = "^";
  public static final String ASTERISK = "*";
  public static final String UNDERSCORE = "_";
  public static final String DASH = "-"; // Hyphen-Minus U+002D &#45;
  public static final String HYPHEN = "‑"; // Non-Breaking Hyphen U+2011 &#8209;
  public static final String SLASH = "/";
  public static final String BACK_SLASH = "\\";
  public static final String SHARP = "#";
  public static final String PERCENT = "%";
  public static final String AMPERSAND = "&";
  public static final String DOLLAR = "$";
  public static final String NOT_AVAILABLE = "NA";
  public static final String THREE_DOTS = "...";
  public static final String PLUS = "+";
  public static final String RIGHT_ARROW = "→";
  public static final String RIGHT_EQUALS_ARROW = "=>";
  public static final String EXCLAMATION_POINT = "!";
  public static final String QUESTION_MARK = "?";
  public static final String EQUALS_SIGN = "=";
  public static final String AT = "@";
  public static final StringFormat NONE = new StringFormat("none");
  public static final StringFormat YES = new StringFormat("yes");
  public static final StringFormat NO = new StringFormat("no");
  public static final StringFormat EMPTY = new StringFormat("empty");

  private StringConstant() {
  }
}
