package dev.quokkify.provider;

/**
 * Database stage type.
 */
public interface DatabaseStage {

  /**
   * Project name.
   * <br>NOTE: in parent pom.xml 'project.artifactId'
   *
   * @return {@link String} name of project
   */
  String getProjectName();

  /**
   * Persistence name.
   *
   * @return {@link String} name of Persistence unit
   */
  String getPersistenceName();

  /**
   * Path to persistence properties in project resources.
   *
   * @return {@link String} path to project resources
   */
  String getPersistencePropertyPath();
}
