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

The [Publish to Maven Central](../.github/workflows/publish-maven-central.yml) workflow starts automatically after every successful **Release Please** run on `main`. It looks for a stable `vMAJOR.MINOR.PATCH` tag on the commit that run processed: when the run cut a release, the tag is there and publication proceeds; otherwise the workflow ends without publishing.

Release Please creates releases with `GITHUB_TOKEN`, and GitHub does not start workflows from events made with that token, so the workflow follows the Release Please run instead of the `release` event.

If publication fails, recover in either way:

- open the failed run and choose **Re-run failed jobs**;
- dispatch the workflow manually with the existing release tag, for example `v0.2.0`.

Publication only starts after a **successful** Release Please run on the tagged commit. If that run failed after tagging, a later successful run on a newer commit will not publish the release: re-run the failed Release Please run, or dispatch this workflow with the tag.

```text
Release Please run on main (or manual tag)
        ↓
find the release tag on the released commit
        ↓
validate stable vMAJOR.MINOR.PATCH format
        ↓
match the tag to the release manifest and checked-out commit
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
- a tag whose version differs from `.github/release-please/manifest.json`;
- a checkout that does not point at the requested tag;
- missing Central Portal credentials or signing material.

---

## 📦 Published coordinates

Every public artifact follows the same pattern:

```text
dev.quokkify:<module>:<version>
```

For example:

```text
dev.quokkify:core:0.2.0
dev.quokkify:testng:0.2.0
dev.quokkify:selenide:0.2.0
```

After publication, the artifacts are available from Maven Central and searchable through [Central Search](https://central.sonatype.com/).

---

## 🧪 Local publication validation

Local builds use the manifest version with `-SNAPSHOT`. A stable version is supplied only by the release workflow.

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

The structural `nosql` parent project is not published. Every other configured Q4J module must have unique metadata in `gradle/module-metadata.gradle`.

---

## ⚠️ Immutability

Maven Central releases cannot be overwritten or removed as part of a normal correction workflow. If a released artifact is wrong, fix the source and publish a new version.
