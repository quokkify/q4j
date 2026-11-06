package archfixtures.deprecated.violating;

public class UsesLegacyApi {

  @SuppressWarnings("deprecation")
  public String value() {
    return LegacyApi.oldValue();
  }
}
