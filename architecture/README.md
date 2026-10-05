# 🏛️ Q4J Architecture

Build gate for architecture and project contracts that would otherwise only be caught in code review.

- ✅ One runner, rules discovered through the `ServiceLoader`
- ✅ Shared ArchUnit and ClassGraph model, scanned once per run
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
The published jar registers none, so every rule is opt in.

```text
dev.quokkify.architecture.rules.TestClassNamingRule
com.example.architecture.NoServiceDependsOnStepsRule
```

**3. Run the runner** as part of `check`.

```kotlin
val verifyArchitecture by tasks.registering(JavaExec::class) {
    group = "verification"
    mainClass = "dev.quokkify.architecture.ArchitectureRunner"
    classpath = architecture + sourceSets.main.get().runtimeClasspath + sourceSets.test.get().output
    systemProperty("architecture.packages", "com.example")
    systemProperty("architecture.main.classes", sourceSets.main.get().output.classesDirs.asPath.replace(File.pathSeparator, ","))
    systemProperty("architecture.test.classes", sourceSets.test.get().output.classesDirs.asPath.replace(File.pathSeparator, ","))
    systemProperty("architecture.main.sources", file("src/main/java").absolutePath)
    systemProperty("architecture.test.sources", file("src/test/java").absolutePath)
}

tasks.check { dependsOn(verifyArchitecture) }
```

Q4J applies itself the same way: see `verifyArchitecture` in [`gradle/architecture.gradle`](../gradle/architecture.gradle).

---

## 🎛️ Properties

| Property                    | Default    | Meaning                                                                        |
| --------------------------- | ---------- | ------------------------------------------------------------------------------ |
| `architecture.packages`     | _required_ | Comma separated root packages to scan                                          |
| `architecture.fail.on`      | `WARNING`  | Least severe finding that fails the run: `INFO`, `WARNING`, `ERROR` or `NEVER` |
| `architecture.main.classes` | _unset_    | Comma separated main class directories for `mainClasses()`; empty means none   |
| `architecture.test.classes` | _unset_    | Comma separated test class directories for `testClasses()`; empty means none   |
| `architecture.main.sources` | _unset_    | Comma separated main source roots for `mainSources()`; empty means none        |
| `architecture.test.sources` | _unset_    | Comma separated test source roots for `testSources()`; empty means none        |

An unknown `architecture.fail.on` value, a missing package list, and an empty rule set all abort the run.
A typo in CI therefore cannot silently disable the gate. A rule that reads a source group whose property is
unset aborts too, while an empty value states that the project has no such sources.

---

## 🚦 Severity and gate

| Severity  | Log level | Fails the default gate |
| --------- | --------- | ---------------------- |
| `INFO`    | `INFO`    | no                     |
| `WARNING` | `WARN`    | **yes**                |
| `ERROR`   | `ERROR`   | **yes**                |

A rule declares how serious its contract is. Whether the build fails is decided once, at the end, by comparing
the worst finding with `architecture.fail.on`. The same rule set can then run as a hard gate in one job and as
a report in another.

The whole report is logged as one event at the worst severity found:

```text
============================================================
Architecture verification (2 rules)
============================================================
[PASS]  No console output in main code
[PASS]  Test class naming
============================================================
Architecture verification: 2 passed, 0 error(s), 0 warning(s), 0 info in 412 ms
============================================================
Gate: fail on WARNING -> passed
```

---

## ✍️ Writing a rule

1. Implement `ArchitectureRule` with a public no argument constructor.
2. List its class name in your `META-INF/services/dev.quokkify.architecture.contract.ArchitectureRule`.

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
        .check(context.all());
  }
}
```

| Throw                                               | Meaning                | Effect                                   |
| --------------------------------------------------- | ---------------------- | ---------------------------------------- |
| `ArchitectureViolationException` / `AssertionError` | the contract is broken | reported under the rule severity         |
| `ArchitectureRunnerError`                           | the rule could not run | **always fails the build**, any severity |

Throw `ArchitectureRunnerError` when a selector is unexpectedly empty or the classpath misses what the rule
reads. A rule that cannot run proves nothing and must never turn into a green `WARNING`.

| Accessor                         | Backed by  | Use it for                                                  |
| -------------------------------- | ---------- | ----------------------------------------------------------- |
| `all()`                          | ArchUnit   | dependencies and layering between classes                   |
| `mainClasses()`, `testClasses()` | ArchUnit   | the same, restricted to main or test classes                |
| `scan()`                         | ClassGraph | class, method and annotation metadata                       |
| `mainSources()`, `testSources()` | JavaParser | content of classes and methods: calls, literals, statements |

When class directories are set, the ArchUnit and ClassGraph models contain only those directories: the runner,
your dependencies and anything else on the classpath are never verified. When main and test sources are set
too, the ArchUnit models also drop every class whose top level type has no authored Java source, so generated
code (QueryDSL, annotation processors) is not verified either.

Every model is built lazily, once per run, and shared by every rule. Rules only read them. A source file that
does not parse aborts the run instead of being skipped, and so do sources of which none lies under
`architecture.packages`. Rules run concurrently on one shared tree: read it, never mutate it, and do not call
`Node.toString()` on it.

A source rule walks the syntax tree of each compilation unit:

```java
@Override
public void verify(ArchitectureContext context) {
  List<String> violations = context.mainSources().units().stream()
      .flatMap(unit -> unit.findAll(MethodCallExpr.class).stream())
      .filter(call -> call.getNameAsString().equals("sleep"))
      .map(call -> "line %d calls %s".formatted(call.getBegin().orElseThrow().line, call))
      .toList();
  checkViolations("Main code must not sleep; wait for a condition instead.", violations);
}
```

Calls are matched syntactically: JavaParser runs without a symbol solver, so a rule sees names, not resolved types.

### Reusing Taikai rules

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
    return RuleSeverity.INFO;
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

- `scope()` picks the classes: `MAIN` (the default, like Taikai's own), `TEST` or `ALL`. The adapter supplies
  them, so `configure` must not set a namespace or classes.
- A single Taikai rule that matches no class holds, for example the `serialVersionUID` convention in a module
  without serializable classes. A scope that imports no class at all aborts the run, unless its class
  directories are configured as empty.
- The adapter never changes the global ArchUnit configuration: Taikai's `failOnEmpty` write is neutralised, so
  your `archunit.properties` and concurrently evaluated rules keep their behaviour.
- One adapter rule is one report entry with one severity. Split rule sets that need different severities into
  separate classes.

Rule names are part of the report contract: the report is sorted by `name()`, not by classpath order.

---

## 📚 Shipped rules

| Rule                  | Severity | Protects                                                                                              |
| --------------------- | -------- | ----------------------------------------------------------------------------------------------------- |
| `TestClassNamingRule` | `ERROR`  | A class declaring TestNG `@Test` must be named `*Test`, or name based selection never runs it         |
| `NoConsoleOutputRule` | `ERROR`  | Taikai and ArchUnit: main code logs instead of `System.out`/`err`, `printStackTrace()`, `dumpStack()` |
| `JavaConventionsRule` | `ERROR`  | Taikai: `equals`/`hashCode` together, `serialVersionUID`, package and interface naming, `LOG` loggers |
| `NoDeprecatedApiRule` | `INFO`   | Taikai: reports use of deprecated APIs without blocking dependency updates                            |

Q4J applies the rules listed in [`tools/architecture`](../tools/architecture/META-INF/services) to every
module with compiled classes: each module's `check` runs its own `verifyArchitecture` on the class directories
and authored sources of its source sets (`main` as main, every other source set as test; generated sources
under `build/` are not verified), so every CI build job verifies the module it builds. `:architecture` itself is
skipped, since its test fixtures violate the rules on purpose.
