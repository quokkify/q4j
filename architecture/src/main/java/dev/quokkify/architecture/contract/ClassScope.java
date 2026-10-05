package dev.quokkify.architecture.contract;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.tngtech.archunit.core.domain.JavaClasses;

/**
 * Which compiled classes of the run a bytecode rule verifies.
 */
public enum ClassScope {

  /**
   * Classes compiled from the main source set only, read through {@link ArchitectureContext#mainClasses()}.
   */
  MAIN {
    @Override
    public JavaClasses classesOf(ArchitectureContext context) {
      return context.mainClasses();
    }

    @Override
    public List<Path> dirsOf(ArchitectureContext context) {
      return context.mainClassDirs();
    }
  },

  /**
   * Classes compiled from the test source sets only, read through {@link ArchitectureContext#testClasses()}.
   */
  TEST {
    @Override
    public JavaClasses classesOf(ArchitectureContext context) {
      return context.testClasses();
    }

    @Override
    public List<Path> dirsOf(ArchitectureContext context) {
      return context.testClassDirs();
    }
  },

  /**
   * Every class of the run, read through {@link ArchitectureContext#all()}.
   */
  ALL {
    @Override
    public JavaClasses classesOf(ArchitectureContext context) {
      return context.all();
    }

    @Override
    public List<Path> dirsOf(ArchitectureContext context) {
      return Stream.concat(context.mainClassDirs().stream(), context.testClassDirs().stream()).toList();
    }
  };

  /**
   * Returns the classes of this scope.
   *
   * @param context context of the run
   * @return classes of this scope
   */
  public abstract JavaClasses classesOf(ArchitectureContext context);

  /**
   * Returns the class directories of this scope.
   *
   * @param context context of the run
   * @return directories, empty when the project has no such classes
   * @throws dev.quokkify.architecture.exceptions.ArchitectureRunnerError when the directories were not configured
   */
  public abstract List<Path> dirsOf(ArchitectureContext context);
}
