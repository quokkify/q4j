# 🚀 Publishing Q4J

> Publish every Q4J module as a signed, immutable Maven Central release.

- ✅ Maven group: `dev.quokkify`
- ✅ Java namespace: `dev.quokkify.*`
- ✅ Sources and Javadoc included
- ✅ Every publication is signed
- ✅ Releases are validated against an existing Git tag

---

## 🔐 One-time Central Portal setup

1. Sign in to the [Central Portal](https://central.sonatype.com/).
2. Confirm that the `dev.quokkify` namespace is verified through ownership of `quokkify.dev`.
3. Generate a Central Portal user token.
4. Create a GPG signing key and distribute its public key to a public keyserver.
5. Keep the protected GitHub environment named `maven-central`.
6. Add the following environment secrets:

| Secret                           | Required        | Purpose                                |
| -------------------------------- | --------------- | -------------------------------------- |
| `MAVEN_CENTRAL_USERNAME`         | yes             | Username from the Central Portal token |
| `MAVEN_CENTRAL_PASSWORD`         | yes             | Password from the Central Portal token |
| `SIGNING_IN_MEMORY_KEY`          | yes             | ASCII-armored private GPG key          |
| `SIGNING_IN_MEMORY_KEY_PASSWORD` | when configured | Password protecting the signing key    |
| `SIGNING_IN_MEMORY_KEY_ID`       | no              | Explicit signing key ID                |

Never commit Central credentials or private signing material to the repository.

---

## 🤖 Release workflow

The [Publish to Maven Central](../.github/workflows/publish-maven-central.yml) workflow runs for a published, non-prerelease GitHub release. It can also be dispatched manually for an existing release tag.

```text
GitHub release tag
        ↓
validate stable vMAJOR.MINOR.PATCH format
        ↓
match the tag to version.txt and checked-out commit
        ↓
build 33 module publications
        ↓
generate POM + JAR + sources + Javadoc
        ↓
sign and publish one Central deployment
        ↓
release on Maven Central
```

The workflow rejects:

- malformed or prerelease tags;
- a tag whose version differs from `version.txt`;
- a checkout that does not point at the requested tag;
- missing Central Portal credentials or signing material.

---

## 📦 Published coordinates

Every public artifact follows the same pattern:

```text
dev.quokkify:q4j-<module>:<version>
```

For example:

```text
dev.quokkify:q4j-core:0.2.0
dev.quokkify:q4j-testng:0.2.0
dev.quokkify:q4j-selenide:0.2.0
```

After publication, the artifacts are available from Maven Central and searchable through [Central Search](https://central.sonatype.com/).

---

## 🧪 Local publication validation

Local builds use `<version.txt>-SNAPSHOT`. A stable version is supplied only by the release workflow.

Generate every Maven POM without contacting Central:

```bash
./gradlew generatePomFileForMavenPublication \
  --no-daemon --console=plain
```

Build the binary, sources, and Javadoc artifacts:

```bash
./gradlew assemble \
  --no-daemon --console=plain
```

List the aggregate publishing tasks:

```bash
./gradlew tasks --group publishing \
  --no-daemon --console=plain
```

The structural `q4j-nosql` parent project is not published. Every other configured Q4J module must have unique metadata in `gradle/module-metadata.gradle`.

---

## ⚠️ Immutability

Maven Central releases cannot be overwritten or removed as part of a normal correction workflow. If a released artifact is wrong, fix the source and publish a new version.
