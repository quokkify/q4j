package dev.quokkify.mongodb.test;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ClasspathGuardTest {

  @Test
  public void checkNoLegacyDriverOrMorphiaOnClasspath() {
    assertThatThrownBy(() -> Class.forName("com.mongodb.DBCollection")).isInstanceOf(ClassNotFoundException.class);
    assertThatThrownBy(() -> Class.forName("dev.morphia.Datastore")).isInstanceOf(ClassNotFoundException.class);
  }
}
