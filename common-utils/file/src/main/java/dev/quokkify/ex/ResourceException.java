package dev.quokkify.ex;

/**
 * Exception thrown when there is an error reading data from a resource.
 * Specifically, when the resource cannot be accessed or read from the provided resource path.
 */
public class ResourceException extends RuntimeException {

  /**
   * Constructs a new ResourceException with a detailed error message.
   *
   * @param resourcePath The path to the resource that could not be read.
   */
  public ResourceException(String resourcePath) {
    super("Can not read data from resourcePath: %s".formatted(resourcePath));
  }
}
