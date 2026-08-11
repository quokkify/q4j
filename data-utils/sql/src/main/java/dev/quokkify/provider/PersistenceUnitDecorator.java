package dev.quokkify.provider;

import dev.quokkify.parser.RegexParser;

import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

/**
 * Decorator for parsing persistence XML file.
 */
public class PersistenceUnitDecorator {

  private static final String URL_PROPERTY = "url";
  private static final String USER_PROPERTY = "user";
  private static final String PASSWORD_PROPERTY = "password";
  private static final String URL_HOST_REGEX = "\\/\\/(.*):(\\d{4})\\/";
  private final PersistenceUnitDescriptor persistenceUnitDescriptor;

  private PersistenceUnitDecorator(PersistenceUnitDescriptor persistenceUnitDescriptor) {
    this.persistenceUnitDescriptor = persistenceUnitDescriptor;
  }

  public static PersistenceUnitDecorator init(PersistenceUnitDescriptor persistenceUnitDescriptor) {
    return new PersistenceUnitDecorator(persistenceUnitDescriptor);
  }

  /**
   * Get 'host ip' from 'url' unit property.
   */
  public String getHostIp() {
    return dev.quokkify.parser.RegexParser.parse(URL_HOST_REGEX, getProperty(URL_PROPERTY), 1);
  }

  /**
   * Get 'host port' from 'url' unit property.
   */
  public int getHostPort() {
    return Integer.parseInt(RegexParser.parse(URL_HOST_REGEX, getProperty(URL_PROPERTY), 2));
  }

  /**
   * Get 'user name' from 'user' unit property.
   */
  public String getUserName() {
    return getProperty(USER_PROPERTY);
  }

  /**
   * Get 'user password' from 'password' unit property.
   */
  public String getUserPassword() {
    return getProperty(PASSWORD_PROPERTY);
  }

  private String getProperty(String propertyName) {
    return persistenceUnitDescriptor.getProperties().entrySet().stream()
        .filter(property -> property.getKey().toString().endsWith(propertyName))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Can not find '%s' property".formatted(propertyName)))
        .getValue().toString();
  }
}
