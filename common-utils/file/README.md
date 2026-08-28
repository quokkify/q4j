# common-utils/file

Classpath resource loading, file I/O utilities, and zip archive support for tests.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/q4j-files):

```kotlin
dependencies {
    testImplementation("dev.quokkify:q4j-files:0.6.1")
}
```

## Usage

Load test data from the classpath and compare actual output against an expected file:

```java
InputStream payload = FileUtils.getNonNullResourceAsStream("data/users.json");
String path = FileUtils.getResourcePath("data/expected-report.csv");

File expected = new File(path);
File actual = generateReport();
assertThat(FileUtils.isFilesContentEquals(expected, actual)).isTrue();
```

Write results incrementally to a temp file during a test run:

```java
File results = FileUtils.createTempFile(FileUtils.FileExtension.CSV);
FileUtils.addTextToFile(results.getName(), "id,status");
FileUtils.addTextsToFile(results.getName(), List.of("1,PASS", "2,FAIL"));
```

Read a classpath resource as a string directly:

```java
String schema = FileUtils.getResourceAsString("schemas/user.json");

List<String> paths = FileUtils.getResourcePath("config/defaults.properties", "api");

String content = FileUtils.readAsString(Path.of("/tmp/report.csv"));
```

## Key API

| Method                                                           | Returns        | Notes                                |
| ---------------------------------------------------------------- | -------------- | ------------------------------------ |
| `getResourceAsStream(path)`                                      | `InputStream`  | `null` if resource missing           |
| `getNonNullResourceAsStream(path)`                               | `InputStream`  | throws if missing                    |
| `getResourcePath(path)`                                          | `String`       | absolute filesystem path             |
| `readAsString(path)`                                             | `String`       | reads file from `java.nio.file.Path` |
| `getResourceAsString(path)`                                      | `String`       | reads classpath resource as string   |
| `getResourcePath(path, module)`                                  | `List<String>` | paths filtered by module name        |
| `getDirectoriesAsEnumValuesFromConfiguration(configPath, clazz)` | `List<T>`      | maps sub-dirs to enum values         |
| `isResourceExist(path)`                                          | `boolean`      | —                                    |
| `addTextToFile(fileName, text)`                                  | `void`         | appends line, thread-safe            |
| `addTextsToFile(fileName, list)`                                 | `void`         | appends as `[a,b]`                   |
| `createTempFile(extension)`                                      | `File`         | uses `FileExtension` enum            |
| `isFilesContentEquals(f1, f2)`                                   | `boolean`      | byte-level comparison                |
| `ZipUtils`                                                       | —              | zip / unzip archive operations       |
