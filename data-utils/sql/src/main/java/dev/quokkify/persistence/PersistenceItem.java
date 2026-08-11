package dev.quokkify.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.sql.DataSource;

import dev.quokkify.entity.SqlEntityInterface;

import io.github.classgraph.ClassGraph;
import jakarta.persistence.Entity;
import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.PersistenceUnitInfo;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.jpa.HibernatePersistenceProvider;

public class PersistenceItem implements PersistenceUnitInfo {

  private final String persistenceName;
  private final Map<String, Object> persistenceProperties;

  public PersistenceItem(String persistenceName, Map<String, Object> persistenceProperties) {
    this.persistenceName = persistenceName;
    this.persistenceProperties = persistenceProperties;
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
    try (var scanResult = new ClassGraph()
        .enableClassInfo()
        .enableAnnotationInfo()
        .ignoreClassVisibility()
        .acceptPackages("dev.quokkify")
        .scan()) {
      return scanResult.getClassesImplementing(SqlEntityInterface.class.getName())
          .filter(classInfo -> classInfo.hasAnnotation(Entity.class.getName()))
          .getNames();
    }
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
