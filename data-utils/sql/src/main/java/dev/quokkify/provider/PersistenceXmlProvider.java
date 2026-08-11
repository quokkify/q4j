package dev.quokkify.provider;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.registry.classloading.internal.ClassLoaderServiceImpl;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceXmlParser;

/**
 * Provider for getting all XML persistences.
 */
public class PersistenceXmlProvider {

  public static final String DEFAULT_CLASSPATH_PERSISTENCE_XML = "META-INF/persistence.xml";

  private static volatile PersistenceXmlProvider instance;
  private final List<PersistenceUnitDescriptor> parsedPersistenceXmlDescriptors;

  private PersistenceXmlProvider() {
    this.parsedPersistenceXmlDescriptors = getPersistenceXmlDescriptors();
  }

  public static PersistenceXmlProvider getInstance() {
    PersistenceXmlProvider localInstance = instance;
    if (localInstance == null) {
      synchronized (PersistenceXmlProvider.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = new PersistenceXmlProvider();
        }
      }
    }
    return instance;
  }

  /**
   * Get persistence unit according to persistence name.
   * Parse all local 'persistence.xml' files.
   *
   * @param unitName name of required persistence unit
   * @return persistence unit as {@link PersistenceUnitDecorator}
   */
  public static PersistenceUnitDecorator getPersistenceUnit(String unitName) {
    return PersistenceUnitDecorator.init(getPersistenceXmlDescriptor(unitName));
  }

  private static PersistenceUnitDescriptor getPersistenceXmlDescriptor(String unitName) {
    return getInstance().parsedPersistenceXmlDescriptors.stream()
        .filter(unit -> unit.getName().equals(unitName))
        .findFirst()
        .orElseThrow(() -> new RuntimeException("Can't find persistence with '%s' unit name".formatted(unitName)));
  }

  private static List<PersistenceUnitDescriptor> getPersistenceXmlDescriptors() {
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    ClassLoaderService classLoaderService = new ClassLoaderServiceImpl(classLoader);
    PersistenceXmlParser parser = PersistenceXmlParser.create(
        Collections.emptyMap(),
        classLoader,
        classLoaderService
    );
    List<URL> urls = parser.getClassLoaderService().locateResources(DEFAULT_CLASSPATH_PERSISTENCE_XML);
    Map<String, PersistenceUnitDescriptor> descriptors = parser.parse(urls);
    return new ArrayList<>(descriptors.values());
  }
}
