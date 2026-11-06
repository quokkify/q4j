package archfixtures.iprefix.clean;

public class IPrefixedClass implements Client {

  @Override
  public void call() {
    // A class may start with I followed by an upper-case letter; the rule covers interfaces only.
  }
}
