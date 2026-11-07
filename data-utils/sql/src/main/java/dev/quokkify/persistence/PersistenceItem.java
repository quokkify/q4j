package dev.quokkify.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * Persistence unit description for Hibernate.
 *
 * @deprecated use {@link PersistenceConfigurationFactory#create(String, Map)} and
 *     {@link dev.quokkify.service.DatabaseService#createSqlQuery(jakarta.persistence.PersistenceConfiguration)}
 */
@Deprecated(since = "0.14.0", forRemoval = true)
public class PersistenceItem implements PersistenceUnitInfo {

  private final String persistenceName;
  private final Map<String, Object> persistenceProperties;

  public PersistenceItem(String persistenceName, Map<String, Object> persistenceProperties) {
    this.persistenceName = persistenceName;
    this.persistenceProperties = persistenceProperties;
  }

  /**
   * Convert this item to a {@link HibernatePersistenceConfiguration}, honoring {@link #getManagedClassNames()}.
   * Also carried when set: the non-JTA data source, mapping file names, shared cache mode and validation mode.
   * Not carried: the JTA data source, transaction type (always resource-local), persistence provider class name
   * (always Hibernate), jar file URLs, {@link #excludeUnlistedClasses()} and the class loader.
   *
   * @return configuration
   * @deprecated use {@link PersistenceConfigurationFactory#create(String, Map)}
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  public HibernatePersistenceConfiguration toPersistenceConfiguration() {
    Map<String, Object> properties = new HashMap<>();
    getProperties().forEach((key, value) -> properties.put(String.valueOf(key), value));
    DataSource dataSource = getNonJtaDataSource();
    if (dataSource != null) {
      properties.put(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE, dataSource);
    }
    HibernatePersistenceConfiguration configuration =
        PersistenceConfigurationFactory.createWithClassNames(getPersistenceUnitName(), properties,
            getManagedClassNames());
    List<String> mappingFileNames = getMappingFileNames();
    if (mappingFileNames != null) {
      mappingFileNames.forEach(configuration::mappingFile);
    }
    if (getSharedCacheMode() != null) {
      configuration.sharedCacheMode(getSharedCacheMode());
    }
    if (getValidationMode() != null) {
      configuration.validationMode(getValidationMode());
    }
    return configuration;
  }

  @Override
  public String getPersistenceUnitName() {
    return persistenceName;
  }

  @Override
  public String getPersistenceProviderClassName() {
    return HibernatePersistenceProvider.class.getName();
  }

  @Override
  public String getScopeAnnotationName() {
    return StringUtils.EMPTY;
  }

  @Override
  public List<String> getQualifierAnnotationNames() {
    return List.of();
  }

  @Override
  @SuppressWarnings("removal")
  public jakarta.persistence.spi.PersistenceUnitTransactionType getTransactionType() {
    return jakarta.persistence.spi.PersistenceUnitTransactionType.RESOURCE_LOCAL;
  }

  @Override
  public DataSource getJtaDataSource() {
    return null;
  }

  @Override
  public DataSource getNonJtaDataSource() {
    return null;
  }

  @Override
  public List<String> getMappingFileNames() {
    return List.of();
  }

  @Override
  public List<URL> getJarFileUrls() {
    try {
      return Collections.list(this.getClass()
          .getClassLoader()
          .getResources(""));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public URL getPersistenceUnitRootUrl() {
    return null;
  }

  @Override
  public List<String> getManagedClassNames() {
    return PersistenceClasspath.entityClassNames();
  }

  @Override
  public boolean excludeUnlistedClasses() {
    return false;
  }

  @Override
  public SharedCacheMode getSharedCacheMode() {
    return null;
  }

  @Override
  public ValidationMode getValidationMode() {
    return null;
  }

  @Override
  public Properties getProperties() {
    Properties properties = new Properties();
    if (persistenceProperties != null) {
      properties.putAll(persistenceProperties);
    }
    return properties;
  }

  @Override
  public String getPersistenceXMLSchemaVersion() {
    return StringUtils.EMPTY;
  }

  @Override
  public ClassLoader getClassLoader() {
    return null;
  }

  @Override
  public void addTransformer(ClassTransformer transformer) {
  }

  @Override
  public ClassLoader getNewTempClassLoader() {
    return null;
  }
}
