package archfixtures.deprecated.violating;

public class LegacyApi {

  @Deprecated
  public static String oldValue() {
    return "old";
  }
}
