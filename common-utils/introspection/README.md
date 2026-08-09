# common-utils/introspection

Scans the classpath to discover methods carrying a specific annotation — useful for auto-registering step definitions or audit checks in test frameworks.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-reflection):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-reflection:0.2.2")
}
```

## Usage

Collect every method annotated with `@Step` from a package and verify the full catalogue:

```java
Set<Method> steps = ClasspathScanner.getMethodsWithAnnotationFromPackage(
        "dev.quokkify.steps",
        Step.class
);
assertThat(steps).hasSizeGreaterThan(0);
```

Cross-check that all discovered step methods are also present in a documentation index:

```java
Set<Method> steps = ClasspathScanner.getMethodsWithAnnotationFromPackage(
        "dev.quokkify.steps",
        Step.class
);
Set<String> stepNames = steps.stream().map(Method::getName).collect(toSet());
assertThat(docIndex.getRegisteredSteps()).containsAll(stepNames);
```

## Key API

| Method                                                                  | Returns       | Notes                                                                |
| ----------------------------------------------------------------------- | ------------- | -------------------------------------------------------------------- |
| `ClasspathScanner.getMethodsWithAnnotationFromPackage(pkg, annotation)` | `Set<Method>` | recursive package scan                                               |
| `ReflectionUtils`                                                       | —             | general reflection helpers (field access, private method invocation) |
