# Changelog

## [0.3.0](https://github.com/quokkify/q4j/compare/v0.2.3...v0.3.0) (2026-08-11)


### ⚠ BREAKING CHANGES

* **selenide:** wait for asynchronously-appearing rows in table lookups ([#461](https://github.com/quokkify/q4j/issues/461))
* **selenide:** remove semantic element aliases ([#458](https://github.com/quokkify/q4j/issues/458))
* **selenide:** remove unused public API BaseBlock, ColorFormatter, PageTitle ([#453](https://github.com/quokkify/q4j/issues/453))
* **selenide:** wire Verification timeout/polling into custom waits ([#452](https://github.com/quokkify/q4j/issues/452))
* **selenide:** use native container lists ([#441](https://github.com/quokkify/q4j/issues/441))

### 🐛 Bug Fixes

* **allure:** restore Java and Gradle metadata ([#464](https://github.com/quokkify/q4j/issues/464)) ([51d07b5](https://github.com/quokkify/q4j/commit/51d07b57488e66a30e2aec3d14cf41bc12482fb5))
* **ci:** roll out scoped Allure environments ([#462](https://github.com/quokkify/q4j/issues/462)) ([aae17a5](https://github.com/quokkify/q4j/commit/aae17a5bf69f5d1695d68ce743d5b09d78381d3f))
* **deps:** update io.github.classgraph:classgraph to v4.8.187 ([#455](https://github.com/quokkify/q4j/issues/455)) ([11a75da](https://github.com/quokkify/q4j/commit/11a75da33cea7492c7b725eca63775ea7802cb84))
* **deps:** update org.bouncycastle:bcprov-jdk18on to v1.85.2 ([#456](https://github.com/quokkify/q4j/issues/456)) ([4ff11d5](https://github.com/quokkify/q4j/commit/4ff11d5542a37e7459f9e74737f15881685f781a))
* **selenide:** enable remote Grid downloads ([#466](https://github.com/quokkify/q4j/issues/466)) ([9475dfa](https://github.com/quokkify/q4j/commit/9475dfaf7b9717a356acae3d42a41b34ace11dde))
* **selenide:** stop leaking Basic Auth credentials into url and Allure ([#450](https://github.com/quokkify/q4j/issues/450)) ([d2c4749](https://github.com/quokkify/q4j/commit/d2c47497ee87b89835f0ab291eca73cfce4c9ddd))
* **selenide:** wait for asynchronously-appearing rows in table lookups ([#461](https://github.com/quokkify/q4j/issues/461)) ([366c264](https://github.com/quokkify/q4j/commit/366c26497dcf5a8b904b9efd86d473284fd7fd59))
* **selenide:** wire Verification timeout/polling into custom waits ([#452](https://github.com/quokkify/q4j/issues/452)) ([e7827e1](https://github.com/quokkify/q4j/commit/e7827e1d8018a95bf0144e62d6312ac35139a50e))


### ⚙️ CI

* **compose:** run health lifecycle ([#436](https://github.com/quokkify/q4j/issues/436)) ([b44dbf5](https://github.com/quokkify/q4j/commit/b44dbf5a1c3bd47f3752eb144b2563f601323007))


### ♻️ Code Refactoring

* **selenide:** remove semantic element aliases ([#458](https://github.com/quokkify/q4j/issues/458)) ([0917d6b](https://github.com/quokkify/q4j/commit/0917d6bebbc288131655957d39a60448858dfa87))
* **selenide:** remove unused public API BaseBlock, ColorFormatter, PageTitle ([#453](https://github.com/quokkify/q4j/issues/453)) ([7234bcd](https://github.com/quokkify/q4j/commit/7234bcd2cac3965b115b9bc8bad53c95dbc11ad2))
* **selenide:** shrink Browser facade to project-level configuration ([#451](https://github.com/quokkify/q4j/issues/451)) ([716dff4](https://github.com/quokkify/q4j/commit/716dff477bb11fd69652aa3ef9b92afa81929b64))
* **selenide:** use native container lists ([#441](https://github.com/quokkify/q4j/issues/441)) ([fbb952a](https://github.com/quokkify/q4j/commit/fbb952a98cee6da896e71b661074a353acf02cfc))


### 🧹 Chores

* **deps:** update github-actions ([#440](https://github.com/quokkify/q4j/issues/440)) ([69f54ee](https://github.com/quokkify/q4j/commit/69f54eeebd58f64dbf9063522429e4f814b953f1))
* **deps:** update gradle to v9.7.0 ([#457](https://github.com/quokkify/q4j/issues/457)) ([b0c64e3](https://github.com/quokkify/q4j/commit/b0c64e3e5af903bfa9a95761d1b8a1aa51618904))
* **deps:** update nginx:1.31.3 docker digest to 8541484 ([#459](https://github.com/quokkify/q4j/issues/459)) ([bb23657](https://github.com/quokkify/q4j/commit/bb23657b406197e632f863187d8547969a516592))
* **deps:** update redis docker tag to v8.10.0 ([#439](https://github.com/quokkify/q4j/issues/439)) ([b6397e9](https://github.com/quokkify/q4j/commit/b6397e950acce3755c0abba5403be145285d3927))
* **deps:** update zookeeper:3.9 docker digest to e0e03e7 ([#460](https://github.com/quokkify/q4j/issues/460)) ([97a0e28](https://github.com/quokkify/q4j/commit/97a0e2897bbe831ce83c5dfd5267c302ee81cb1b))

## [0.2.3](https://github.com/quokkify/q4j/compare/v0.2.2...v0.2.3) (2026-08-09)


### 🐛 Bug Fixes

* **ci:** restore Allure report summaries ([#420](https://github.com/quokkify/q4j/issues/420)) ([e33f273](https://github.com/quokkify/q4j/commit/e33f273545bc7a1c4269e5cdd4471f34aa0c896a))
* **ci:** reuse project-toolkit Java setup action ([#417](https://github.com/quokkify/q4j/issues/417)) ([c9f9e70](https://github.com/quokkify/q4j/commit/c9f9e70b661a2d29700a12e2ff7fdd6539f6fa71))
* **deps:** update org.redisson:redisson to v4.7.0 ([#419](https://github.com/quokkify/q4j/issues/419)) ([a80a576](https://github.com/quokkify/q4j/commit/a80a576f07139ccb8877cfe73d0df1de36940b8f))
* **files:** prevent zip slip during extraction ([#423](https://github.com/quokkify/q4j/issues/423)) ([834ffbe](https://github.com/quokkify/q4j/commit/834ffbe17666892a308b88f04eed88c2ed99f0f4))
* **publishing:** clean up Javadoc diagnostics ([#416](https://github.com/quokkify/q4j/issues/416)) ([e62332e](https://github.com/quokkify/q4j/commit/e62332ea7434e24a47961edb0ff96ee469ef48b3))


### 📚 Documentation

* **modules:** document Maven Central dependencies ([#427](https://github.com/quokkify/q4j/issues/427)) ([d7f6647](https://github.com/quokkify/q4j/commit/d7f6647d1cb05caa8535d2a499882c35b2e8ccfc))


### ♻️ Code Refactoring

* **ci:** consolidate Allure helpers under tools ([#421](https://github.com/quokkify/q4j/issues/421)) ([79223e6](https://github.com/quokkify/q4j/commit/79223e6ab2bcdb1b273664bbfeb5f0f6bd22a74c))


### 🧹 Chores

* **deps:** update opensearchproject/opensearch docker tag to v3.8.0 ([#425](https://github.com/quokkify/q4j/issues/425)) ([653e207](https://github.com/quokkify/q4j/commit/653e20722d9f41ccf43461d6f93e10725aedab22))
* **deps:** update plugin com.github.spotbugs to v6.5.10 ([#424](https://github.com/quokkify/q4j/issues/424)) ([441436c](https://github.com/quokkify/q4j/commit/441436ce9fc41d6244ab40fd46901a120befc5da))
* **deps:** update reportportal/migrations docker tag to v5.15.3 ([#426](https://github.com/quokkify/q4j/issues/426)) ([87850dd](https://github.com/quokkify/q4j/commit/87850dde9b796c3ab8b648b57e8f85c7f001b599))
* **deps:** update reportportal/service-api docker tag to v5.15.3 ([#428](https://github.com/quokkify/q4j/issues/428)) ([f53bcf5](https://github.com/quokkify/q4j/commit/f53bcf5c0ddc2d31f57a23c056cbcd5e28eb97a7))
* **deps:** update reportportal/service-authorization docker tag to v5.15.1 ([#429](https://github.com/quokkify/q4j/issues/429)) ([be92c2c](https://github.com/quokkify/q4j/commit/be92c2cf5b4858d0a267746f9338d3ebd6fb0b77))
* **deps:** update reportportal/service-auto-analyzer docker tag to v5.15.5 ([#432](https://github.com/quokkify/q4j/issues/432)) ([7afda5a](https://github.com/quokkify/q4j/commit/7afda5a82d5d010cda3a225c8bd82778756c2f27))
* **deps:** update reportportal/service-index docker tag to v5.15.1 ([#430](https://github.com/quokkify/q4j/issues/430)) ([5bc155e](https://github.com/quokkify/q4j/commit/5bc155eed47d1be1acf059a8f6acf65724112c4c))
* **deps:** update reportportal/service-jobs docker tag to v5.15.2 ([#431](https://github.com/quokkify/q4j/issues/431)) ([a1473d3](https://github.com/quokkify/q4j/commit/a1473d30e40b08bb109e575e7a85bc8f631a7b12))
* **deps:** update reportportal/service-ui docker tag to v5.15.4 ([#434](https://github.com/quokkify/q4j/issues/434)) ([98578ea](https://github.com/quokkify/q4j/commit/98578ea1420694bc1868c553519221f8be8312fc))

## [0.2.2](https://github.com/quokkify/q4j/compare/v0.2.1...v0.2.2) (2026-08-09)


### 🐛 Bug Fixes

* **publishing:** include resolved dependency versions ([#414](https://github.com/quokkify/q4j/issues/414)) ([e3f51df](https://github.com/quokkify/q4j/commit/e3f51dfd1e949bb0cc63566c9074966cca4857d9))

## [0.2.1](https://github.com/quokkify/q4j/compare/v0.2.0...v0.2.1) (2026-08-09)


### 🐛 Bug Fixes

* **publishing:** avoid duplicate Javadoc artifacts ([#412](https://github.com/quokkify/q4j/issues/412)) ([6c05ec5](https://github.com/quokkify/q4j/commit/6c05ec579db6415c159a8edcdfe22f4b706a42e4))


### 🧹 Chores

* **deps:** update actions/github-script action to v9 ([#411](https://github.com/quokkify/q4j/issues/411)) ([a484cd3](https://github.com/quokkify/q4j/commit/a484cd395b171ef3bcf3720fb91324bf716ddf56))

## [0.2.0](https://github.com/quokkify/q4j/compare/v0.1.0...v0.2.0) (2026-08-08)


### ✨ Features

* **ci:** migrate Allure reporting to managed workflow ([#406](https://github.com/quokkify/q4j/issues/406)) ([32dbef4](https://github.com/quokkify/q4j/commit/32dbef419e3cefa20ffe54dc6640a593f6ce1b58))
* **q4j:** rebrand modules and add Maven Central publishing ([#407](https://github.com/quokkify/q4j/issues/407)) ([5deb487](https://github.com/quokkify/q4j/commit/5deb4876db6c6d895fac393fcf258d8c59180096))


### 🐛 Bug Fixes

* **ci:** allow q4j package paths in trusted scan policy ([#408](https://github.com/quokkify/q4j/issues/408)) ([dc442bf](https://github.com/quokkify/q4j/commit/dc442bf66b43fd10589a6e4dbce018467959094d))


### 🧹 Chores

* **template:** update shared project template ([#404](https://github.com/quokkify/q4j/issues/404)) ([444b081](https://github.com/quokkify/q4j/commit/444b081640e86ad2d1c7009ab06b89539cbe2c55))

## [0.1.0](https://github.com/quokkify/q4j/releases/tag/v0.1.0) (2026-08-08)

### ✨ Features

- Modular Java toolkit for test automation and reusable TestNG extensions.
- Integrations for REST Assured, Selenide, Kafka, RabbitMQ, WebSockets, Jira, TestRail, and ReportPortal.
- Shared utilities for configuration, structured data, databases, files, HTML, JWT, signatures, and console commands.

### 📝 Release status

- Q4J is under active development and is not yet considered production-ready.
- Versioning has been restarted at `0.1.0`; breaking changes may be introduced in future `0.x` releases.
