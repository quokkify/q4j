# ☕ Q4J

Generated with `quokkify/project-toolkit` at `v2.12.4`. Run `copier update` to apply future template changes; Renovate updates workflow version references independently.

- ✅ Pick only the modules your project needs
- ✅ Keep test infrastructure under one `dev.quokkify` namespace
- ✅ Connect TestNG, Selenide, REST Assured, databases, messaging, and reporting
- ✅ Publish signed releases to Maven Central
- ✅ Build on Java 21 without adopting a monolithic framework

[![GitHub release](https://img.shields.io/github/v/release/quokkify/q4j)](https://github.com/quokkify/q4j/releases)
[![Tests](https://img.shields.io/github/actions/workflow/status/quokkify/q4j/test.yml?branch=main&label=tests)](https://github.com/quokkify/q4j/actions/workflows/test.yml)
[![Java 21](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

---

## ⚡ Quick start

Choose a module and add it from Maven Central.

**Gradle Kotlin DSL**

```kotlin
dependencies {
    implementation("dev.quokkify:core:<version>")
    testImplementation("dev.quokkify:testng:<version>")
}
```

**Gradle Groovy DSL**

```groovy
dependencies {
    implementation "dev.quokkify:rest-assured:<version>"
}
```

**Maven**

```xml
<dependency>
    <groupId>dev.quokkify</groupId>
    <artifactId>selenide</artifactId>
    <version>VERSION</version>
</dependency>
```

Replace `<version>` or `VERSION` with a published Q4J release.

---

## 📦 What Q4J does

```text
test code
    ↓
choose q4j modules
    ↓
reuse configuration, steps, clients, and verifiers
    ↓
exercise browser, API, database, queue, or reporting system
    ↓
produce a clear test result
```

| Need                  | Add                       | Result                                              |
| --------------------- | ------------------------- | --------------------------------------------------- |
| Shared test utilities | `core`                    | Formatting, generation, models, and reusable APIs   |
| TestNG lifecycle      | `testng`                  | Listeners, retries, annotations, and extensions     |
| Browser automation    | `selenide`                | Components, page workflows, and browser services    |
| API automation        | `rest-assured`            | Requests, filters, steps, and response verification |
| Database verification | `sql`, `morphia`, `redis` | Persistence helpers and reusable assertions         |
| Messaging             | `kafka`, `rabbitmq`       | Producers, consumers, steps, and verifiers          |
| Test management       | `jira-*`, `testrail-*`    | Ticket and test-case lifecycle integrations         |
| Reporting             | `reportportal-*`          | ReportPortal services and TestNG listeners          |

---

## 🧩 Module catalog

Every public module uses the same Maven group:

```text
dev.quokkify:<artifact>:<version>
```

### 🧱 Foundation

| Artifact     | Purpose                                                          |
| ------------ | ---------------------------------------------------------------- |
| `core`       | Shared types, formatting, generators, and utility APIs           |
| `config`     | Typed configuration and locale providers                         |
| `testng`     | TestNG listeners, retries, annotations, and lifecycle extensions |
| `awaitility` | Polling and timeout abstractions                                 |
| `reflection` | Classpath scanning and reflection utilities                      |
| `files`      | Files, archives, locking, and local resources                    |
| `html`       | HTML parsing and generated browser-compatibility models          |
| `jwt`        | JWT models, generators, and formatting                           |
| `crypto`     | Encryption, key, and digital-signature utilities                 |
| `ssh`        | SSH execution and port forwarding                                |

### 🗂️ Data formats

| Artifact          | Purpose                                       |
| ----------------- | --------------------------------------------- |
| `jackson-support` | Shared Jackson dependencies and configuration |
| `jackson-json`    | JSON mapping and JSON Pointer utilities       |
| `jackson-yaml`    | YAML parsing and resource providers           |
| `jackson-xml`     | XML parsing and conversion                    |
| `jackson-csv`     | CSV parsing and conversion                    |

### 🗄️ Data access

| Artifact  | Purpose                                          |
| --------- | ------------------------------------------------ |
| `sql`     | SQL, JPA, persistence, and database verification |
| `morphia` | MongoDB and Morphia persistence helpers          |
| `redis`   | Redis operations and verification                |

### 🔌 Test integrations

| Artifact                | Purpose                                     |
| ----------------------- | ------------------------------------------- |
| `rest-assured`          | REST Assured API testing                    |
| `selenide`              | Browser automation with Selenide            |
| `selenide-proxy`        | Proxy and HAR support for Selenide          |
| `selenide-grid`         | Selenium Grid support for Selenide          |
| `kafka`                 | Kafka producers, consumers, and assertions  |
| `rabbitmq`              | RabbitMQ integration testing                |
| `tyrus`                 | WebSocket testing with Tyrus                |
| `jira-core`             | Jira client and ticket abstractions         |
| `jira-testng`           | Jira integration for TestNG                 |
| `jira-testrail`         | Jira ticket sources for TestRail workflows  |
| `testrail-core`         | TestRail API models and services            |
| `testrail-testng`       | TestRail lifecycle integration for TestNG   |
| `reportportal-core`     | ReportPortal configuration and API services |
| `reportportal-testng`   | ReportPortal listeners for TestNG           |
| `reportportal-testrail` | TestRail descriptions for ReportPortal      |

> `nosql` is an internal Gradle parent project and is not published.

---

## 🧪 Common setups

<details>
<summary>🌐 Browser automation</summary>

```kotlin
dependencies {
    testImplementation("dev.quokkify:testng:<version>")
    testImplementation("dev.quokkify:selenide:<version>")
}
```

Add `selenide-proxy` when a test needs proxy or HAR support, or `selenide-grid` for Selenium Grid integration.

</details>

<details>
<summary>🔗 API automation</summary>

```kotlin
dependencies {
    testImplementation("dev.quokkify:testng:<version>")
    testImplementation("dev.quokkify:rest-assured:<version>")
    testImplementation("dev.quokkify:jackson-json:<version>")
}
```

Use Q4J request services, filters, steps, and verifiers while keeping project-specific API clients in the consuming test project.

</details>

<details>
<summary>🗄️ Database and messaging checks</summary>

```kotlin
dependencies {
    testImplementation("dev.quokkify:sql:<version>")
    testImplementation("dev.quokkify:redis:<version>")
    testImplementation("dev.quokkify:kafka:<version>")
}
```

Choose only the systems exercised by the test suite. Each integration is published independently.

</details>

<details>
<summary>📊 Reporting and test management</summary>

```kotlin
dependencies {
    testImplementation("dev.quokkify:reportportal-testng:<version>")
    testImplementation("dev.quokkify:testrail-testng:<version>")
    testImplementation("dev.quokkify:jira-testng:<version>")
}
```

The integration modules connect test lifecycle events to ReportPortal, TestRail, and Jira without forcing those systems into the Q4J core.

</details>

---

## 🧠 Design principles

- **Modular first** — no umbrella dependency that silently imports every integration.
- **Stable coordinates** — public artifacts follow `dev.quokkify:*`.
- **One source namespace** — Java APIs live under `dev.quokkify.*`.
- **Integration at the edges** — vendor-specific clients stay outside the core modules.
- **Testable releases** — compilation, Checkstyle, SpotBugs, integration matrices, and publication metadata are validated in CI.

---

## 🚀 Publishing

Stable releases are signed and published through the Central Publisher Portal.

```text
GitHub release
      ↓
validate tag and version
      ↓
build POM + JAR + sources + Javadoc
      ↓
sign all publications
      ↓
publish and release on Maven Central
```

See [Maven Central publishing](docs/publishing.md) for namespace verification, required secrets, and the release workflow.

---

## 🔍 Project documentation

- [Maven Central publishing](docs/publishing.md)
- [ReportPortal integration](integrations/reportportal/README.md)
- [RabbitMQ integration](integrations/rabbitmq/README.md)
- [Release history](CHANGELOG.md)

---

## 💻 Local development

Compile and run static checks:

```bash
./gradlew check assemble --no-daemon --console=plain
```

Generate and inspect Maven publication metadata without publishing:

```bash
./gradlew generatePomFileForMavenPublication --no-daemon --console=plain
```

Integration tests may require their corresponding external service or Docker Compose profile.

---

## 🤝 Contributing

Issues and pull requests are welcome. Keep changes focused on one module or shared concern and include the relevant Gradle verification output.

---

## 📄 License

Q4J is available under the [MIT License](LICENSE).
