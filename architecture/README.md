# 🏛️ Q4J Architecture

Build gate for architecture and project contracts that would otherwise only be caught in code review.

- ✅ One runner, rules discovered through the `ServiceLoader`
- ✅ Shared ArchUnit and ClassGraph class models, scanned once per run
- ✅ ClassGraph resource model for `META-INF/services` and other non-class files
- ✅ JavaParser source model for rules that read method bodies
- ✅ Taikai rule sets through one adapter
- ✅ Severity per rule, gate threshold per build
- ✅ A rule that cannot run fails the build instead of passing silently

```text
rules registered in META-INF/services
    ↓
ArchitectureRunner scans -Darchitecture.packages once
    ↓
every rule runs on its own virtual thread
    ↓
one report, ordered by rule name
    ↓
build fails when a finding reaches -Darchitecture.fail.on
```

---

## ⚡ Quick start

**1. Add the dependency** to a configuration that sees your compiled main and test classes. Requires Java 21.
`log4j-core` is only needed to print the report.

```kotlin
val architecture by configurations.creating

dependencies {
    architecture("dev.quokkify:architecture:<version>")
    architecture("org.apache.logging.log4j:log4j-core:<version>")
}
```

**2. Register rules** in `src/<sourceSet>/resources/META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule`.
The published jar registers none, so every rule is opt-in.

```text
dev.quokkify.architecture.rules.TestClassNamingRule
com.example.architecture.NoServiceDependsOnStepsRule
```

**3. Run the runner** as part of `check`.

```kotlin
val verifyArchitecture by tasks.registering(JavaExec::class) {
    group = "verification"
    mainClass = "dev.quokkify.architecture.ArchitectureRunner"
    classpath = architecture + sourceSets.test.get().runtimeClasspath
    systemProperty("architecture.packages", "com.example")
    systemProperty("architecture.main.classes", sourceSets.main.get().output.classesDirs.asPath.replace(File.pathSeparator, ","))
    systemProperty("architecture.test.classes", sourceSets.test.get().output.classesDirs.asPath.replace(File.pathSeparator, ","))
    systemProperty("architecture.main.resources", sourceSets.main.get().output.resourcesDir!!.absolutePath)
    systemProperty("architecture.test.resources", sourceSets.test.get().output.resourcesDir!!.absolutePath)
    systemProperty("architecture.main.sources", file("src/main/java").absolutePath)
    systemProperty("architecture.test.sources", file("src/test/java").absolutePath)
}

tasks.check { dependsOn(verifyArchitecture) }
```

The test runtime classpath covers the main classes, the test classes and every dependency. The dependencies are
needed even though they are not verified: a rule sees the deprecations and supertypes of a library class only
when that class resolves. Pass only class directories that exist; a missing one is skipped.

Q4J applies itself the same way: see `verifyArchitecture` in [`gradle/architecture.gradle`](../gradle/architecture.gradle).

---

## 🎛️ Properties

| Property                      | Default    | Meaning                                                                        |
| ----------------------------- | ---------- | ------------------------------------------------------------------------------ |
| `architecture.packages`       | _required_ | Comma separated root packages to scan                                          |
| `architecture.fail.on`        | `ERROR`    | Least severe finding that fails the run: `INFO`, `WARNING`, `ERROR` or `NEVER` |
| `architecture.main.classes`   | _unset_    | Comma separated main class directories for `mainClasses()`; empty means none   |
| `architecture.test.classes`   | _unset_    | Comma separated test class directories for `testClasses()`; empty means none   |
| `architecture.main.resources` | _unset_    | Comma separated main resource directories for `resources()`; empty means none  |
| `architecture.test.resources` | _unset_    | Comma separated test resource directories for `resources()`; empty means none  |
| `architecture.main.sources`   | _unset_    | Comma separated main source roots for `mainSources()`; empty means none        |
| `architecture.test.sources`   | _unset_    | Comma separated test source roots for `testSources()`; empty means none        |
| `architecture.module`         | _unset_    | Name of the verified module shown in the report header, such as `:core`        |
| `architecture.color`          | `auto`     | Colored report: `auto` on a terminal, `always` or `never`                      |

An unknown `architecture.fail.on` or `architecture.color` value, a missing package list, and an empty rule set all abort the run.
A typo in CI therefore cannot silently disable the gate. A rule that reads a source group whose property is
unset aborts too, and so does a rule that reads resources while a resource property is unset; an empty value
states that the project has no such sources or resources.

---

## 🚦 Severity and gate

| Severity  | Log level | Fails the default gate |
| --------- | --------- | ---------------------- |
| `INFO`    | `INFO`    | no                     |
| `WARNING` | `WARN`    | no, opt in             |
| `ERROR`   | `ERROR`   | **yes**                |

A rule declares how serious its contract is. Whether the build fails is decided once, at the end, by comparing
the worst finding with `architecture.fail.on`. The same rule set can then run as a hard gate in one job and as
a report in another.

Only `ERROR` fails by default. A `WARNING` is reported and fails only where a build opts in, for example a
stricter CI job. In Q4J, pass the threshold as a Gradle property:

```bash
./gradlew check -Parchitecture.fail.on=WARNING
```

The whole report is logged as one event at the worst severity found. It first lists the shared models the run
built, each with its build time; a build nested in another, such as the ArchUnit import parsing the sources, is
listed on its own. Each rule then shows what it verifies (`MAIN`, `TEST`, `RESOURCES`, or `-` when undeclared),
its declared severity and the time of its own work. Rules run in parallel, and a rule waiting for a model that
another rule is building is not charged with that wait, so the rule times do not add up to the total.

```text
============================================================
Architecture verification of :common-utils:core (3 rules)
============================================================
Shared models, built once (rule times below exclude them):
  ArchUnit classes         355 ms
  JavaParser main sources   82 ms
  ClassGraph classes        20 ms
------------------------------------------------------------
[PASS]  No console output in main code    MAIN       ERROR    19 ms
[WARN]  No deprecated API usage (Taikai)  MAIN+TEST  WARNING  34 ms
    Architecture Violation [Priority: MEDIUM] - Rule 'No classes should use deprecated APIs' ...
[PASS]  Test class naming                 TEST       ERROR     2 ms
============================================================
Architecture verification: 2 passed, 0 error(s), 1 warning(s), 0 info in 498 ms
============================================================
Gate: fail on ERROR -> passed
```

This plain layout carries no escape sequences, so a CI log stays greppable. On a terminal the report is colored
and marks each rule with `✔`, `✘`, `⚠` or `ℹ`, or with ASCII symbols when standard output is not UTF-8. A build
tool that pipes the runner output cannot be detected as a terminal, so it sets `architecture.color` itself: Q4J
passes `never` for `--console=plain`, when `NO_COLOR` is set, or on CI (`CI` set) unless a `--console` mode
asks for color, and `always` otherwise. Give the runner logger
a `%msg%n` layout to print the report without a timestamp prefix, as
[`tools/architecture/log4j2.xml`](../tools/architecture/log4j2.xml) does.

---

## ✍️ Writing a rule

1. Implement `ArchitectureRule` with a public no-argument constructor, or extend `TaikaiArchitectureRule`.
2. List its class name in your `META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule`.
3. Read one of the shared models from the `ArchitectureContext`, and report what you find.
4. Declare what the rule verifies in `scopes()`: `MAIN`, `TEST`, `RESOURCES` or a combination. The report shows
   it next to the rule; an undeclared scope shows as `-`. A `TaikaiArchitectureRule` derives it from `scope()`.

| Throw                                               | Meaning                | Effect                                   |
| --------------------------------------------------- | ---------------------- | ---------------------------------------- |
| `ArchitectureViolationException` / `AssertionError` | the contract is broken | reported under the rule severity         |
| `ArchitectureRunnerError`                           | the rule could not run | **always fails the build**, any severity |

Throw `ArchitectureRunnerError` when a selector is unexpectedly empty or the classpath misses what the rule
reads. A rule that cannot run proves nothing and must never turn into a green `WARNING`. End a rule that
collects violations itself with `checkViolations(contract, violations)`: an empty list passes, anything else is
reported as one finding.

### 🧭 Pick the engine

| Accessor                         | Backed by  | Reads                  | Use it for                                                           |
| -------------------------------- | ---------- | ---------------------- | -------------------------------------------------------------------- |
| `all()`                          | ArchUnit   | bytecode               | dependencies, layering, calls and accesses between classes           |
| `mainClasses()`, `testClasses()` | ArchUnit   | bytecode               | the same, restricted to main or test classes                         |
| `TaikaiArchitectureRule`         | Taikai     | bytecode, via ArchUnit | predefined conventions: Java, logging, Spring, Quarkus, JUnit        |
| `scan()`                         | ClassGraph | bytecode               | class, method, field and annotation metadata, `static final` values  |
| `resources()`                    | ClassGraph | resource files         | `META-INF/services`, properties, YAML and every other non-class file |
| `mainSources()`, `testSources()` | JavaParser | Java sources           | what bytecode loses: comments, source-only annotations, statements   |

Taikai is not a fourth scanner: it is a catalogue of ArchUnit rules evaluated on the same `all()` model.

When class directories are set, the ArchUnit and ClassGraph class models contain only those directories: the
runner, your dependencies and anything else on the classpath are never verified. Set both groups, or neither:
one without the other aborts the run, since `ALL` would silently miss half of the project. When main and test
sources are set too, the ArchUnit models also drop every class whose top level type has no authored Java source,
so generated code (QueryDSL, annotation processors) is not verified either. `scan()` is not filtered this way.
Only Java sources are parsed, so a project with Kotlin, Groovy or Scala classes under the verified packages
aborts instead of losing them; leave the source properties unset there.

`resources()` reads every file of the configured resource directories, not only those under
`architecture.packages`, since most resources lie outside any package. It needs both resource groups; a
configured directory that does not exist is skipped.

Every model is built lazily, once per run, and shared by every rule. Rules only read them. A source file that
does not parse aborts the run instead of being skipped, and so do sources of which none lies under
`architecture.packages`. Rules run concurrently on one shared tree: read it, never mutate it, and do not call
`Node.toString()` on it.

Rule names are part of the report contract: the report is sorted by `name()`, not by classpath order.

### 🧱 ArchUnit: dependencies between classes

Write any ArchUnit rule and check it against the shared model. Its `AssertionError` becomes the finding.

```java
public class NoServiceDependsOnStepsRule implements ArchitectureRule {

  @Override
  public String name() {
    return "Services do not depend on steps";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public void verify(ArchitectureContext context) {
    ArchRuleDefinition.noClasses()
        .that().resideInAPackage("..services..")
        .should().dependOnClassesThat().resideInAPackage("..steps..")
        .check(context.mainClasses());
  }
}
```

Pick `mainClasses()`, `testClasses()` or `all()` to choose what is verified. A rule whose `that()` matches
nothing fails by default (`archRule.failOnEmptyShould`), which keeps a mistyped package from passing silently.

### 🥋 Taikai: predefined rule sets

[Taikai](https://github.com/enofex/taikai) ships predefined ArchUnit rule sets. Extend `TaikaiArchitectureRule`
to run one of them inside this runner. Taikai then evaluates against the shared ArchUnit model, and its findings
land in the same report under the severity of your rule:

```java
public class NoDeprecatedApiRule extends TaikaiArchitectureRule {

  @Override
  public String name() {
    return "No deprecated API usage (Taikai)";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.WARNING;
  }

  @Override
  protected ClassScope scope() {
    return ClassScope.ALL;
  }

  @Override
  protected void configure(Taikai.Builder builder) {
    builder.java(java -> java.noUsageOfDeprecatedAPIs());
  }
}
```

Your own ArchUnit rule joins the same set through `addRule(TaikaiRule.of(...))`, as `NoConsoleOutputRule` does.

- `scope()` picks the classes: `MAIN` (the default, like Taikai's own), `TEST` or `ALL`. The adapter supplies
  them, so `configure` must not set a namespace or classes.
- A single Taikai rule that matches no class holds, for example the `serialVersionUID` convention in a module
  without serializable classes. A selector that never matches, such as a mistyped logger type, therefore passes
  too; override `allowsRulesMatchingNothing()` to make every rule of a set find something. A scope that imports
  no class at all aborts the run, unless its class directories are configured as empty.
- The adapter never changes the global ArchUnit configuration: Taikai is built in a thread local ArchUnit scope,
  so your `archunit.properties` and concurrently evaluated rules keep their behaviour.
- Taikai settings the adapter cannot honour abort the run instead of being ignored: a namespace set in
  `configure`, or a Taikai rule importing `ONLY_TESTS` or `WITH_TESTS` outside the scope. Exclusions, of a rule
  or of the whole set, are applied.
- One adapter rule is one report entry with one severity. Split rule sets that need different severities into
  separate classes.
- Taikai's JUnit rules look for JUnit 5 annotations only. TestNG projects use `TestClassNamingRule` instead.

### 📝 JavaParser: what the source says

A source rule walks the syntax tree of each compilation unit. It sees what compilation erases: comments,
`SOURCE` retention annotations such as `@SuppressWarnings`, and the statements of a method as written.

```java
public class NoSuppressedWarningsRule implements ArchitectureRule {

  @Override
  public String name() {
    return "No suppressed warnings in main code";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.WARNING;
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<String> violations = context.mainSources().units().stream()
        .flatMap(unit -> unit.findAll(AnnotationExpr.class).stream()
            .filter(annotation -> annotation.getNameAsString().equals("SuppressWarnings"))
            .map(annotation -> "%s line %d".formatted(
                unit.getStorage().orElseThrow().getPath(), annotation.getBegin().orElseThrow().line)))
        .toList();
    checkViolations("Main code fixes warnings instead of suppressing them.", violations);
  }
}
```

Names are matched syntactically: JavaParser runs without a symbol solver, so a rule sees names as written, not
resolved types. Format positions from `getBegin()` and the unit's storage, never from `Node.toString()`.

### 🔎 ClassGraph: metadata and resources

`scan()` answers metadata queries quickly and keeps the values of `static final` constants, which the compiler
inlines at every use and ArchUnit therefore cannot see:

```java
public class PropertyKeysAreNamespacedRule implements ArchitectureRule {

  @Override
  public String name() {
    return "Property keys are namespaced";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<String> violations = context.scan().getAllClasses().stream()
        .flatMap(type -> type.getDeclaredFieldInfo().stream())
        .filter(field -> field.getName().endsWith("_PROPERTY"))
        .filter(field -> field.getConstantInitializerValue() instanceof String key && !key.startsWith("example."))
        .map(field -> "%s.%s = %s".formatted(
            field.getClassName(), field.getName(), field.getConstantInitializerValue()))
        .toList();
    checkViolations("Every *_PROPERTY key starts with 'example.'.", violations);
  }
}
```

`resources()` lists the files next to the classes. Read a resource's content while the run is in progress, and
treat an unreadable file as a rule that cannot run:

```java
public class NoPlainPasswordsInResourcesRule implements ArchitectureRule {

  private static final Pattern PASSWORD = Pattern.compile("(?im)^\\s*[\\w.-]*password\\s*[=:]\\s*\\S+");

  @Override
  public String name() {
    return "No plain passwords in resources";
  }

  @Override
  public RuleSeverity severity() {
    return RuleSeverity.ERROR;
  }

  @Override
  public void verify(ArchitectureContext context) {
    List<String> violations = context.resources()
        .filter(resource -> resource.getPath().endsWith(".properties"))
        .stream()
        .filter(resource -> PASSWORD.matcher(contentOf(resource)).find())
        .map(resource -> "%s in %s".formatted(resource.getPath(), resource.getClasspathElementFile()))
        .toList();
    checkViolations("Passwords come from the environment, not from a resource.", violations);
  }

  private static String contentOf(Resource resource) {
    try {
      return resource.getContentAsString();
    } catch (IOException unreadable) {
      throw new ArchitectureRunnerError("Cannot read " + resource.getPath(), unreadable);
    }
  }
}
```

The pattern matches a key ending in `password`, then `=` or `:`, then a non-empty value, at the start of any
line. The shipped `ServiceRegistrationRule` is a complete resource rule: it resolves every
`META-INF/services` entry the way the `ServiceLoader` would.

---

## 📚 Shipped rules

| Rule                      | Severity  | Protects                                                                                              |
| ------------------------- | --------- | ----------------------------------------------------------------------------------------------------- |
| `TestClassNamingRule`     | `ERROR`   | ClassGraph: a class declaring TestNG `@Test` must be named `*Test`, or name-based selection skips it  |
| `ServiceRegistrationRule` | `ERROR`   | ClassGraph: every `META-INF/services` entry names a public, concrete, constructible provider          |
| `NoConsoleOutputRule`     | `ERROR`   | Taikai and ArchUnit: main code logs instead of `System.out`/`err`, `printStackTrace()`, `dumpStack()` |
| `JavaConventionsRule`     | `ERROR`   | Taikai: `equals`/`hashCode` together, `serialVersionUID`, package and interface naming, `LOG` loggers |
| `NoDeprecatedApiRule`     | `WARNING` | Taikai: reports use of deprecated APIs; fails only with `fail.on=WARNING`                             |

Q4J applies the rules listed in [`tools/architecture`](../tools/architecture/META-INF/services) to every
module with compiled classes: each module's `check` runs its own `verifyArchitecture` on the class and resource
directories and authored sources of its source sets (`main` as main, every other source set as test; generated
sources under `build/` are not verified), so every CI build job verifies the module it builds. `:architecture`
itself is skipped, since its test fixtures violate the rules on purpose.
