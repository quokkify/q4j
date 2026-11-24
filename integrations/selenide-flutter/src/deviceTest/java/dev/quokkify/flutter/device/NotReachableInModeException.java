package dev.quokkify.flutter.device;

/**
 * A scenario step that the given mode cannot express; {@link BaseMobileTest} reports it as a skip.
 */
public class NotReachableInModeException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public NotReachableInModeException(MobileMode mode, String detail) {
    super("not reachable in " + mode.label() + " mode: " + detail);
  }
}
