# 🏛️ Q4J Architecture

Build gate for architecture and project contracts that would otherwise only be caught in code review.

- ✅ One runner, rules discovered through the `ServiceLoader`
- ✅ Shared ArchUnit and ClassGraph model, scanned once per run
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
}

tasks.check { dependsOn(verifyArchitecture) }
```

Q4J applies itself the same way: see `verifyArchitecture` in [`gradle/architecture.gradle`](../gradle/architecture.gradle).

---

## 🎛️ Properties

| Property                | Default    | Meaning                                                                        |
| ----------------------- | ---------- | ------------------------------------------------------------------------------ |
| `architecture.packages` | _required_ | Comma separated root packages to scan                                          |
| `architecture.fail.on`  | `WARNING`  | Least severe finding that fails the run: `INFO`, `WARNING`, `ERROR` or `NEVER` |

An unknown `architecture.fail.on` value, a missing package list, and an empty rule set all abort the run.
A typo in CI therefore cannot silently disable the gate.

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
Architecture verification (1 rules)
============================================================
[PASS]  Test class naming
============================================================
Architecture verification: 1 passed, 0 error(s), 0 warning(s), 0 info in 412 ms
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

`ArchitectureContext` offers `all()` (ArchUnit `JavaClasses`) and `scan()` (ClassGraph `ScanResult`). Both
are built lazily, once per run, and shared by every rule. Rules only read them.

Rule names are part of the report contract: the report is sorted by `name()`, not by classpath order.

---

## 📚 Shipped rules

| Rule                  | Severity | Protects                                                                                      |
| --------------------- | -------- | --------------------------------------------------------------------------------------------- |
| `TestClassNamingRule` | `ERROR`  | A class declaring TestNG `@Test` must be named `*Test`, or name based selection never runs it |

Q4J applies the rules listed in [`tools/architecture`](../tools/architecture/META-INF/services) to every
module with tests: each module's `check` runs its own `verifyArchitecture`, so every CI build job verifies the
module it builds. `:architecture` itself is skipped, since its test fixtures violate the rules on purpose.
