# Q4J Selenide Proxy

Proxy and HAR capture support for Q4J Selenide tests.

## Installation

Add the module from [Maven Central](https://central.sonatype.com/artifact/dev.quokkify/selenide-proxy):

```kotlin
dependencies {
    testImplementation("dev.quokkify:selenide-proxy:0.14.1")
}
```

The module brings `dev.quokkify:testng` (`AbstractSteps`) transitively through `dev.quokkify:selenide`; do not declare it separately.

See the [Q4J module catalog](../../README.md) for the complete list of modules and common setups.
