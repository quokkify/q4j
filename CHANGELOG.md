# Changelog

## [0.6.2](https://github.com/quokkify/q4j/compare/v0.6.1...v0.6.2) (2026-08-30)


### 🐛 Bug Fixes

* **ci:** isolate Allure artifacts per module ([#560](https://github.com/quokkify/q4j/issues/560)) ([3d70203](https://github.com/quokkify/q4j/commit/3d7020342820a759519a3e4c082ba0a75bba4ed5))
* **ci:** preserve module paths in Allure artifacts ([#562](https://github.com/quokkify/q4j/issues/562)) ([61cc29c](https://github.com/quokkify/q4j/commit/61cc29cd74e2a80d321da16e23b916c6131089b0))
* **ci:** restore Allure extractor paths ([#558](https://github.com/quokkify/q4j/issues/558)) ([cce7f21](https://github.com/quokkify/q4j/commit/cce7f21d3b8f02654d31b240737e1bf7b6c7e3cc))
* **ci:** separate stability Allure source path ([#565](https://github.com/quokkify/q4j/issues/565)) ([00574e2](https://github.com/quokkify/q4j/commit/00574e2943fb8b84a3c3babe9d3901410ed504a7))
* **deps:** update selenide to v7.18.1 ([#569](https://github.com/quokkify/q4j/issues/569)) ([be139a4](https://github.com/quokkify/q4j/commit/be139a4537718dbdd613ed99886ac351321070d8))
* **selenide:** harden and audit table API ([#566](https://github.com/quokkify/q4j/issues/566)) ([c6a581c](https://github.com/quokkify/q4j/commit/c6a581cfd4a0cc03fa0d7c899e85adb236d8c5b2))


### 🧹 Chores

* **ci:** temporarily disable Stability tests ([#568](https://github.com/quokkify/q4j/issues/568)) ([cdf6975](https://github.com/quokkify/q4j/commit/cdf6975d314517d7e8e617fab92a72e65e5ad121))
* **deps:** pin quokkify/project-toolkit action to aa4efad ([#561](https://github.com/quokkify/q4j/issues/561)) ([def0b06](https://github.com/quokkify/q4j/commit/def0b0698ca4576bb7b08cc0f4cb413a9eccf3db))
* **deps:** update github/codeql-action digest to cdf488f ([#559](https://github.com/quokkify/q4j/issues/559)) ([66f1372](https://github.com/quokkify/q4j/commit/66f1372864e9cdc3bd1c3aee03742f73fc849e0b))
* **deps:** update q4j module documentation to v0.6.1 ([#557](https://github.com/quokkify/q4j/issues/557)) ([0206da8](https://github.com/quokkify/q4j/commit/0206da8826eb73bf24de55217fa990e5fbc58d78))
* **deps:** update redis:8.10.1 docker digest to 298e5b3 ([#564](https://github.com/quokkify/q4j/issues/564)) ([c6ac3f3](https://github.com/quokkify/q4j/commit/c6ac3f31e08062530df18dcd068bf14390890734))
* **renovate:** pin helper Docker images ([#555](https://github.com/quokkify/q4j/issues/555)) ([dd950e2](https://github.com/quokkify/q4j/commit/dd950e266fab0692ec61a1f4f99e2c1867dc434a))
* **template:** update shared project template ([#554](https://github.com/quokkify/q4j/issues/554)) ([b2687b8](https://github.com/quokkify/q4j/commit/b2687b88de483081a35dcc1de9ea9420f5e46c03))

## [0.6.1](https://github.com/quokkify/q4j/compare/v0.6.0...v0.6.1) (2026-08-28)


### 🐛 Bug Fixes

* **ci:** include report directory in Allure URL ([#545](https://github.com/quokkify/q4j/issues/545)) ([fd8321e](https://github.com/quokkify/q4j/commit/fd8321e2d91edb95c83f637a4b8f292e38b57959))
* **ci:** point Allure comment to PR report ([#540](https://github.com/quokkify/q4j/issues/540)) ([cf77fa3](https://github.com/quokkify/q4j/commit/cf77fa3de351a4ef492b2a24ca630ee1ce03a74e))
* **ci:** reuse project-toolkit Java setup action ([#546](https://github.com/quokkify/q4j/issues/546)) ([b031c7c](https://github.com/quokkify/q4j/commit/b031c7c82b9d214fcd1e8a6e21a414f1bc6641f1))
* **config:** enable Release Please metadata ([#541](https://github.com/quokkify/q4j/issues/541)) ([c1314e6](https://github.com/quokkify/q4j/commit/c1314e6d78e904a516bcee694b43b681dff48c72))
* **deps:** update io.github.classgraph:classgraph to v4.8.194 ([#544](https://github.com/quokkify/q4j/issues/544)) ([081c1ab](https://github.com/quokkify/q4j/commit/081c1ab98406e0c5715396d323cb4164ddf2d3ce))
* **selenide:** restore table remount and row semantics ([#543](https://github.com/quokkify/q4j/issues/543)) ([41c5a86](https://github.com/quokkify/q4j/commit/41c5a863a41998b7c307f75d5325c185ab07142d))


### 🧪 Tests

* **selenide:** stabilize table model timing coverage ([#549](https://github.com/quokkify/q4j/issues/549)) ([2e92d1f](https://github.com/quokkify/q4j/commit/2e92d1fe8d2b33450d525e5fc56732c8a7a4c580))


### 🧹 Chores

* **ci:** silence Docker image pulls ([#552](https://github.com/quokkify/q4j/issues/552)) ([deffec6](https://github.com/quokkify/q4j/commit/deffec6af82891a27af0d5baaac876c8c4ea8faf))
* **ci:** update project-toolkit to v2.12.2 ([#537](https://github.com/quokkify/q4j/issues/537)) ([736effe](https://github.com/quokkify/q4j/commit/736effeb26e1388c334421df72aaf97a9bc1f26d))
* **deps:** update actions/download-artifact action to v8 ([#538](https://github.com/quokkify/q4j/issues/538)) ([0015014](https://github.com/quokkify/q4j/commit/001501476ebb17dd345020ae077fe54fa0caa3b8))
* **deps:** update myoung34/github-runner docker tag to v2.337.0 ([#553](https://github.com/quokkify/q4j/issues/553)) ([a78a4c1](https://github.com/quokkify/q4j/commit/a78a4c1b2b8e4943c1b70893793ab7cb3b755129))
* **deps:** update nginx docker tag to v1.31.4 ([#550](https://github.com/quokkify/q4j/issues/550)) ([b9aa3df](https://github.com/quokkify/q4j/commit/b9aa3df88a700c01a7b9f5a1c7ba0c7f07779dad))
* **deps:** update q4j module documentation to v0.6.0 ([#535](https://github.com/quokkify/q4j/issues/535)) ([0e9811b](https://github.com/quokkify/q4j/commit/0e9811b3f6b7b2cd5b8804063f0ecc42a0214069))
* **deps:** update traefik docker tag to v3.7.12 ([#551](https://github.com/quokkify/q4j/issues/551)) ([7f809f1](https://github.com/quokkify/q4j/commit/7f809f1a8364f88a2824a87706058d24052a8bf4))
* **deps:** update zookeeper:3.9 docker digest to f258365 ([#532](https://github.com/quokkify/q4j/issues/532)) ([3eef666](https://github.com/quokkify/q4j/commit/3eef66635a6d3907403754ea103190e4313596b8))
* **project-toolkit:** update to v2.12.4 ([#539](https://github.com/quokkify/q4j/issues/539)) ([7aa4aa2](https://github.com/quokkify/q4j/commit/7aa4aa2bf3ef3f39b8652e64f85ed542c62e7f61))

## [0.6.0](https://github.com/quokkify/q4j/compare/v0.5.3...v0.6.0) (2026-08-25)


### ✨ Features

* **selenide:** add framework-independent table DOM model ([#522](https://github.com/quokkify/q4j/issues/522)) ([e448dd2](https://github.com/quokkify/q4j/commit/e448dd2a42b9994c0b68af33c10b059cb0b4e812))
* **selenide:** add lazy table query API ([#529](https://github.com/quokkify/q4j/issues/529)) ([84c4830](https://github.com/quokkify/q4j/commit/84c4830f7f036010971b6df66e00372b0c1585ff))
* **selenide:** add table assertions and action handles ([#530](https://github.com/quokkify/q4j/issues/530)) ([ebc46ab](https://github.com/quokkify/q4j/commit/ebc46abf78bf53c38c3e93ceba2708c545b01f73))
* **selenide:** add table DOM adapter SPI ([#528](https://github.com/quokkify/q4j/issues/528)) ([2b4e206](https://github.com/quokkify/q4j/commit/2b4e2068218b8221e3c12f38ee61f093aee8e1cf))


### 🐛 Bug Fixes

* **deps:** update com.rabbitmq:amqp-client to v5.35.0 ([#519](https://github.com/quokkify/q4j/issues/519)) ([1acbdfe](https://github.com/quokkify/q4j/commit/1acbdfe2382ff4e946536138fd40cc4cda279d13))


### 🧪 Tests

* **selenide:** cover local table API variants ([#517](https://github.com/quokkify/q4j/issues/517)) ([50f89a3](https://github.com/quokkify/q4j/commit/50f89a3d723973c3b307f7bcb339842ef7544b6b))


### 🧹 Chores

* **testng:** simplify test log names ([#534](https://github.com/quokkify/q4j/issues/534)) ([a2cc282](https://github.com/quokkify/q4j/commit/a2cc2827d321c7d02e6fee850b18409931084594))

## [0.5.3](https://github.com/quokkify/q4j/compare/v0.5.2...v0.5.3) (2026-08-24)


### 🐛 Bug Fixes

* **deps:** update com.github.spotbugs:spotbugs-annotations to v4.10.4 ([#510](https://github.com/quokkify/q4j/issues/510)) ([206d364](https://github.com/quokkify/q4j/commit/206d36465dba8d38af2f7bead2b0ca9652eb665d))
* **deps:** update com.google.guava:guava to v33.7.1-jre ([#506](https://github.com/quokkify/q4j/issues/506)) ([2939f0a](https://github.com/quokkify/q4j/commit/2939f0a77c358d2a71034bcf4a31416a104a7521))
* **deps:** update feign monorepo to v13.14 ([#511](https://github.com/quokkify/q4j/issues/511)) ([ddf7faa](https://github.com/quokkify/q4j/commit/ddf7faa6310082db290dcab9c3df4eba1628168f))
* **deps:** update hibernate-orm monorepo to v7.4.6.final ([#514](https://github.com/quokkify/q4j/issues/514)) ([64a9cf9](https://github.com/quokkify/q4j/commit/64a9cf967bd0ade4f5e08d3a57c32b475a5bf920))
* **deps:** update io.github.classgraph:classgraph to v4.8.193 ([#513](https://github.com/quokkify/q4j/issues/513)) ([13902b0](https://github.com/quokkify/q4j/commit/13902b04671f2db034f51cf534bd688af97e7006))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-73 to v3.15.5 ([#499](https://github.com/quokkify/q4j/issues/499)) ([2c33db8](https://github.com/quokkify/q4j/commit/2c33db8c8788241993ca722fde50962b10f0eaaf))


### 🧹 Chores

* **deps:** update docker:29.7-dind docker digest to 12e683a ([#482](https://github.com/quokkify/q4j/issues/482)) ([851e343](https://github.com/quokkify/q4j/commit/851e3436adec45533c108345e90a5c4bf333930d))
* **deps:** update github/codeql-action digest to db488dd ([#493](https://github.com/quokkify/q4j/issues/493)) ([40a1890](https://github.com/quokkify/q4j/commit/40a1890eb9b3d12c0d83e3bdaa3c800204b94e84))
* **deps:** update mockserver/mockserver docker tag to v7.6.0 ([#505](https://github.com/quokkify/q4j/issues/505)) ([7d04566](https://github.com/quokkify/q4j/commit/7d045664bb6029222a9f76b26f193102b56c46b5))
* **deps:** update mongo:8.3.8 docker digest to 5211c51 ([#507](https://github.com/quokkify/q4j/issues/507)) ([3705b19](https://github.com/quokkify/q4j/commit/3705b19a0380d7f0bd61a0e769fa3fe16cff8308))
* **deps:** update plugin com.github.spotbugs to v6.5.11 ([#508](https://github.com/quokkify/q4j/issues/508)) ([dee1834](https://github.com/quokkify/q4j/commit/dee18343e9d690ec646509214865e29ecd36226a))
* **deps:** update redis docker tag to v8.10.1 ([#512](https://github.com/quokkify/q4j/issues/512)) ([6c569d7](https://github.com/quokkify/q4j/commit/6c569d7247100657ca8825fd99423a0934c9cb0e))
* **deps:** update traefik docker tag to v3.7.11 ([#509](https://github.com/quokkify/q4j/issues/509)) ([8dac7d3](https://github.com/quokkify/q4j/commit/8dac7d30dc94e7b0a1bc572efac031b2e370db0e))

## [0.5.2](https://github.com/quokkify/q4j/compare/v0.5.1...v0.5.2) (2026-08-21)


### 🐛 Bug Fixes

* **deps:** update checkstyle to v14 ([#496](https://github.com/quokkify/q4j/issues/496)) ([457a244](https://github.com/quokkify/q4j/commit/457a24400a7600b2041401601d3e6f57d59c27dd))
* **deps:** update com.fasterxml.jackson:jackson-bom to v2.22.2 ([#497](https://github.com/quokkify/q4j/issues/497)) ([9ff95ed](https://github.com/quokkify/q4j/commit/9ff95ed05b7544114dfcfc1c1bc48942a42bcc81))
* **deps:** update io.github.classgraph:classgraph to v4.8.190 ([#488](https://github.com/quokkify/q4j/issues/488)) ([87ac938](https://github.com/quokkify/q4j/commit/87ac93826b42cbb70811ceee96ed8de3188ceda4))
* **deps:** update io.github.classgraph:classgraph to v4.8.191 ([#491](https://github.com/quokkify/q4j/issues/491)) ([0376e55](https://github.com/quokkify/q4j/commit/0376e551929c5c4b34df7a87a0563feb22ee69d3))
* **deps:** update io.github.classgraph:classgraph to v4.8.192 ([#492](https://github.com/quokkify/q4j/issues/492)) ([e2c8e78](https://github.com/quokkify/q4j/commit/e2c8e78d364e6e4f2244c05dd7920fc35b2d5e7e))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.10.0 ([#495](https://github.com/quokkify/q4j/issues/495)) ([813b104](https://github.com/quokkify/q4j/commit/813b1049d7e262fb10ac0f76b25d8f74914c0f1f))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.9.2 ([#489](https://github.com/quokkify/q4j/issues/489)) ([4ffb641](https://github.com/quokkify/q4j/commit/4ffb6419a6469679972dc11fae116fc779f9ffc6))
* **deps:** update selenide to v7.18.0 ([#500](https://github.com/quokkify/q4j/issues/500)) ([e5198c6](https://github.com/quokkify/q4j/commit/e5198c6a91500ef505d31d072085a0252a7363a0))


### 🧹 Chores

* **deps:** update gradle to v9.7.1 ([#498](https://github.com/quokkify/q4j/issues/498)) ([24fead0](https://github.com/quokkify/q4j/commit/24fead0a67187d6cd33795aacf6bff9f7e1a4f87))
* **deps:** update mongo docker tag to v8.3.8 ([#490](https://github.com/quokkify/q4j/issues/490)) ([7977ae1](https://github.com/quokkify/q4j/commit/7977ae1f9b66a75ea91fd242f70d3d7e3c92c3e8))
* **deps:** update q4j module documentation to v0.5.1 ([#494](https://github.com/quokkify/q4j/issues/494)) ([91dd22d](https://github.com/quokkify/q4j/commit/91dd22daf9e94011e1d40c4eb4655f9af3c04fef))
* **deps:** update reportportal/service-api docker tag to v5.15.4 ([#485](https://github.com/quokkify/q4j/issues/485)) ([7251148](https://github.com/quokkify/q4j/commit/7251148ec90da702d7a3efc67894fcf5411a068a))
* **deps:** update reportportal/service-ui docker tag to v5.15.5 ([#487](https://github.com/quokkify/q4j/issues/487)) ([aef435f](https://github.com/quokkify/q4j/commit/aef435fc94dfcb1f1ffe060760207d943a15125a))

## [0.5.1](https://github.com/quokkify/q4j/compare/v0.5.0...v0.5.1) (2026-08-14)


### 🐛 Bug Fixes

* **selenide:** use proxy HAR dependency ([#483](https://github.com/quokkify/q4j/issues/483)) ([d77487f](https://github.com/quokkify/q4j/commit/d77487f10f4073a36e5ade2cf6e145b46b0be0d4))

## [0.5.0](https://github.com/quokkify/q4j/compare/v0.4.0...v0.5.0) (2026-08-13)


### ✨ Features

* **selenide:** simplify dropdown abstractions ([#480](https://github.com/quokkify/q4j/issues/480)) ([2b5ee80](https://github.com/quokkify/q4j/commit/2b5ee80a6d337728326d78c542d55480db541288))


### 🐛 Bug Fixes

* **deps:** update checkstyle to v13.10.0 ([#479](https://github.com/quokkify/q4j/issues/479)) ([3c49673](https://github.com/quokkify/q4j/commit/3c496732eb69c21aadb3e2eeea9723747598526c))
* **deps:** update io.github.classgraph:classgraph to v4.8.189 ([#470](https://github.com/quokkify/q4j/issues/470)) ([63862b6](https://github.com/quokkify/q4j/commit/63862b62beeae20d0d9c477535d1908e69b54ed9))
* **renovate:** align Selenium Grid image updates ([#473](https://github.com/quokkify/q4j/issues/473)) ([5746f96](https://github.com/quokkify/q4j/commit/5746f9690c334de8707939cd422ecbb71ecae48d))
* **renovate:** consolidate selenium extraction ([#475](https://github.com/quokkify/q4j/issues/475)) ([176516d](https://github.com/quokkify/q4j/commit/176516d6db982da5b075315a62c5cab163297f40))


### 🧹 Chores

* **deps:** update q4j module documentation to v0.2.3 ([#476](https://github.com/quokkify/q4j/issues/476)) ([839b480](https://github.com/quokkify/q4j/commit/839b480172329db2cf5e689c6750029d0a90d296))
* **deps:** update q4j module documentation to v0.4.0 ([#477](https://github.com/quokkify/q4j/issues/477)) ([be3f65b](https://github.com/quokkify/q4j/commit/be3f65b817cf5003366f8484cafdc95c13d69dbb))
* **deps:** update reportportal/migrations docker tag to v5.15.4 ([#469](https://github.com/quokkify/q4j/issues/469)) ([b820eda](https://github.com/quokkify/q4j/commit/b820edaab621b05802dd784c891d063e46248d28))
* **deps:** update selenium grid images to v4.47.0-20260808 ([#478](https://github.com/quokkify/q4j/issues/478)) ([54e033e](https://github.com/quokkify/q4j/commit/54e033e69b01ca7f739edca6b3dc4262afbf7fec))
* **deps:** update selenium/standalone-chromium docker tag to v151 ([#472](https://github.com/quokkify/q4j/issues/472)) ([eda92df](https://github.com/quokkify/q4j/commit/eda92df81c604713a2a4160709a572fd5a5d397a))

## [0.4.0](https://github.com/quokkify/q4j/compare/v0.3.0...v0.4.0) (2026-08-11)


### ⚠ BREAKING CHANGES

* **selenide:** wait for asynchronously-appearing rows in table lookups ([#461](https://github.com/quokkify/q4j/issues/461))
* **selenide:** remove semantic element aliases ([#458](https://github.com/quokkify/q4j/issues/458))
* **selenide:** remove unused public API BaseBlock, ColorFormatter, PageTitle ([#453](https://github.com/quokkify/q4j/issues/453))
* **selenide:** wire Verification timeout/polling into custom waits ([#452](https://github.com/quokkify/q4j/issues/452))
* **selenide:** use native container lists ([#441](https://github.com/quokkify/q4j/issues/441))

### ✨ Features

* add module with Selenide ([#51](https://github.com/quokkify/q4j/issues/51)) ([1caa9ce](https://github.com/quokkify/q4j/commit/1caa9ce86f8d32fcb9a2eab7b868eab780d8ee0a))
* add Tyrus WebSocket test module ([#195](https://github.com/quokkify/q4j/issues/195)) ([315fd24](https://github.com/quokkify/q4j/commit/315fd249195e47a5b3e8069d5ac3d5cab0e245c0))
* **ci:** migrate Allure reporting to managed workflow ([#406](https://github.com/quokkify/q4j/issues/406)) ([32dbef4](https://github.com/quokkify/q4j/commit/32dbef419e3cefa20ffe54dc6640a593f6ce1b58))
* **ci:** prefer self-hosted runner with safe fallback ([#115](https://github.com/quokkify/q4j/issues/115)) ([078dc51](https://github.com/quokkify/q4j/commit/078dc516833421355f8d35f1f338545691a5f02d))
* configure ci to use concurrent jobs (1 module per 1 runner) ([#98](https://github.com/quokkify/q4j/issues/98)) ([1828d89](https://github.com/quokkify/q4j/commit/1828d896b452dd7e7d4a9f8014e1be8627493020))
* create config for TestNG extension ([#107](https://github.com/quokkify/q4j/issues/107)) ([c76952e](https://github.com/quokkify/q4j/commit/c76952e2cdbca2d6b8cce9c83759ce7096229c7c))
* fluent verify().withTimeout().withPolling() chain across all modules ([#252](https://github.com/quokkify/q4j/issues/252)) ([086559e](https://github.com/quokkify/q4j/commit/086559eb7c844eea67a62f39ea2d9133eaa058c5))
* **jackson:** add json pointer api to JsonPojo and move it to jackson-json ([#397](https://github.com/quokkify/q4j/issues/397)) ([dc6bfb6](https://github.com/quokkify/q4j/commit/dc6bfb6a565ec67227fdc10b03b03970a9346e10))
* port Group 1 improvements ([#191](https://github.com/quokkify/q4j/issues/191)) ([5efd161](https://github.com/quokkify/q4j/commit/5efd161adf09b0ba47eb1b65519bc9fa9dd27927))
* **q4j:** rebrand modules and add Maven Central publishing ([#407](https://github.com/quokkify/q4j/issues/407)) ([5deb487](https://github.com/quokkify/q4j/commit/5deb4876db6c6d895fac393fcf258d8c59180096))
* **reportportal:** decouple TMS integration via SPI (closes [#165](https://github.com/quokkify/q4j/issues/165)) ([#247](https://github.com/quokkify/q4j/issues/247)) ([f8126d4](https://github.com/quokkify/q4j/commit/f8126d4b604734ce02391072e353969a40ef6c6f))
* **reportportal:** expand test coverage — unit + integration tests (closes [#166](https://github.com/quokkify/q4j/issues/166)) ([#237](https://github.com/quokkify/q4j/issues/237)) ([a3d421e](https://github.com/quokkify/q4j/commit/a3d421eaaa44f38c19b4ffcaac682677925c29f0))
* selenide separate modules ([#101](https://github.com/quokkify/q4j/issues/101)) ([#201](https://github.com/quokkify/q4j/issues/201)) ([84d3c01](https://github.com/quokkify/q4j/commit/84d3c010e00572e8bc74bd3e21891db314409a5c))


### 🐛 Bug Fixes

* **allure:** restore Java and Gradle metadata ([#464](https://github.com/quokkify/q4j/issues/464)) ([51d07b5](https://github.com/quokkify/q4j/commit/51d07b57488e66a30e2aec3d14cf41bc12482fb5))
* **ci:** align Copier template baseline ([#389](https://github.com/quokkify/q4j/issues/389)) ([5ab37dd](https://github.com/quokkify/q4j/commit/5ab37ddbb02233ad8c8e6a200631e06862a848c8))
* **ci:** allow q4j package paths in trusted scan policy ([#408](https://github.com/quokkify/q4j/issues/408)) ([dc442bf](https://github.com/quokkify/q4j/commit/dc442bf66b43fd10589a6e4dbce018467959094d))
* **ci:** restore Allure report summaries ([#420](https://github.com/quokkify/q4j/issues/420)) ([e33f273](https://github.com/quokkify/q4j/commit/e33f273545bc7a1c4269e5cdd4471f34aa0c896a))
* **ci:** reuse project-toolkit Java setup action ([#417](https://github.com/quokkify/q4j/issues/417)) ([c9f9e70](https://github.com/quokkify/q4j/commit/c9f9e70b661a2d29700a12e2ff7fdd6539f6fa71))
* **ci:** roll out scoped Allure environments ([#462](https://github.com/quokkify/q4j/issues/462)) ([aae17a5](https://github.com/quokkify/q4j/commit/aae17a5bf69f5d1695d68ce743d5b09d78381d3f))
* **ci:** use Renovate-compatible Copier source URL ([#386](https://github.com/quokkify/q4j/issues/386)) ([7baee5e](https://github.com/quokkify/q4j/commit/7baee5eeb5f354e8a7807b09fb388be8c0a6a2e4))
* close connection when max_response_time is exceeded in rest-assured ([#202](https://github.com/quokkify/q4j/issues/202)) ([ab042d0](https://github.com/quokkify/q4j/commit/ab042d09d27f2794c79489f77b388a0dbde95cdf))
* **deps:** update allure to v2.32.0 ([#86](https://github.com/quokkify/q4j/issues/86)) ([b7edb05](https://github.com/quokkify/q4j/commit/b7edb0553a0393dfad6d7229908223e65da4456e))
* **deps:** update allure to v2.34.0 ([#200](https://github.com/quokkify/q4j/issues/200)) ([8210750](https://github.com/quokkify/q4j/commit/8210750822661a45107155803d674df2b865e541))
* **deps:** update allure to v2.35.2 ([#291](https://github.com/quokkify/q4j/issues/291)) ([1aa81db](https://github.com/quokkify/q4j/commit/1aa81dbb88f342267a50ed856f195aef3ce62c53))
* **deps:** update allure to v2.35.3 ([#321](https://github.com/quokkify/q4j/issues/321)) ([4627a94](https://github.com/quokkify/q4j/commit/4627a941543e2967e85b98ec3ab794bad6e985db))
* **deps:** update allure to v2.35.4 ([#377](https://github.com/quokkify/q4j/issues/377)) ([5a66f4c](https://github.com/quokkify/q4j/commit/5a66f4cef29787f2f6b9453f09c83b3bc288fec9))
* **deps:** update checkstyle to v12.1.1 ([#63](https://github.com/quokkify/q4j/issues/63)) ([a0c5cf7](https://github.com/quokkify/q4j/commit/a0c5cf730b8fa7add18883d1077c07ca87be189b))
* **deps:** update checkstyle to v12.1.2 ([#71](https://github.com/quokkify/q4j/issues/71)) ([9aa9338](https://github.com/quokkify/q4j/commit/9aa9338bf81370eeefd16aa2a5e612807564a6b7))
* **deps:** update checkstyle to v12.3.0 ([#87](https://github.com/quokkify/q4j/issues/87)) ([56ea494](https://github.com/quokkify/q4j/commit/56ea494aa36cfa8814235f43690fce8499234adf))
* **deps:** update checkstyle to v13 ([#110](https://github.com/quokkify/q4j/issues/110)) ([d8779b6](https://github.com/quokkify/q4j/commit/d8779b6709661121a69860e413259e3e4450cefa))
* **deps:** update checkstyle to v13.1.0 ([#141](https://github.com/quokkify/q4j/issues/141)) ([faa06ff](https://github.com/quokkify/q4j/commit/faa06ff362d1923ceec141411678a3c02bd8ee55))
* **deps:** update checkstyle to v13.2.0 ([#155](https://github.com/quokkify/q4j/issues/155)) ([1347ad9](https://github.com/quokkify/q4j/commit/1347ad9cb6e1a462a8e3b2424ee0fb7a08274640))
* **deps:** update checkstyle to v13.4.2 ([#234](https://github.com/quokkify/q4j/issues/234)) ([fda854c](https://github.com/quokkify/q4j/commit/fda854ce57d3bfa8348c1cd18f33744d1d0641e1))
* **deps:** update checkstyle to v13.6.0 ([#292](https://github.com/quokkify/q4j/issues/292)) ([c69a9a4](https://github.com/quokkify/q4j/commit/c69a9a4e105f4e0c10bb88e4a7a52c20d87fe4ad))
* **deps:** update checkstyle to v13.7.0 ([#306](https://github.com/quokkify/q4j/issues/306)) ([0d8245c](https://github.com/quokkify/q4j/commit/0d8245c428f7c8c20d11f33085d37e2ad58aa37e))
* **deps:** update checkstyle to v13.8.0 ([#361](https://github.com/quokkify/q4j/issues/361)) ([aad5824](https://github.com/quokkify/q4j/commit/aad58249c1a2ee0f945a9b0a814697b9a32ae51f))
* **deps:** update checkstyle to v13.9.0 ([#392](https://github.com/quokkify/q4j/issues/392)) ([e1c1f71](https://github.com/quokkify/q4j/commit/e1c1f710990e3b59453d20e0f640b110604d84ca))
* **deps:** update com.fasterxml.jackson:jackson-bom to v2.20.1 ([#58](https://github.com/quokkify/q4j/issues/58)) ([55714db](https://github.com/quokkify/q4j/commit/55714db185e55991721a5a7fe81e20849dac0bd2))
* **deps:** update com.fasterxml.jackson:jackson-bom to v2.21.0 ([#119](https://github.com/quokkify/q4j/issues/119)) ([6ba75c2](https://github.com/quokkify/q4j/commit/6ba75c215c91a5a7ce9c0c56470c42b3f960df85))
* **deps:** update com.fasterxml.jackson:jackson-bom to v2.21.3 ([#182](https://github.com/quokkify/q4j/issues/182)) ([925be24](https://github.com/quokkify/q4j/commit/925be24f0aae2cd8f6294cb5a14418b5f0b478a6))
* **deps:** update com.fasterxml.jackson:jackson-bom to v2.22.0 ([#289](https://github.com/quokkify/q4j/issues/289)) ([1ca183b](https://github.com/quokkify/q4j/commit/1ca183bf1e70eddaf0f2060032bfdb408d078b56))
* **deps:** update com.fasterxml.jackson:jackson-bom to v2.22.1 ([#322](https://github.com/quokkify/q4j/issues/322)) ([6774851](https://github.com/quokkify/q4j/commit/67748510d183c3e35e2f8c90593395b56bbfa8c4))
* **deps:** update com.github.spotbugs:spotbugs-annotations to v4.10.2 ([#297](https://github.com/quokkify/q4j/issues/297)) ([5832c4a](https://github.com/quokkify/q4j/commit/5832c4a8d7fe42501178b0b34493dc909a6f3119))
* **deps:** update com.github.spotbugs:spotbugs-annotations to v4.10.3 ([#371](https://github.com/quokkify/q4j/issues/371)) ([64e442b](https://github.com/quokkify/q4j/commit/64e442b699224432020f60fb3c2d16869174321b))
* **deps:** update com.github.spotbugs:spotbugs-annotations to v4.9.8 ([#59](https://github.com/quokkify/q4j/issues/59)) ([69f3e94](https://github.com/quokkify/q4j/commit/69f3e944244b5ab7378e667f260ee10286796a57))
* **deps:** update com.google.guava:guava to v33.6.0-jre ([#196](https://github.com/quokkify/q4j/issues/196)) ([63ede88](https://github.com/quokkify/q4j/commit/63ede883c34676d0386787adfcbe810b8118332b))
* **deps:** update com.rabbitmq:amqp-client to v5.30.0 ([#232](https://github.com/quokkify/q4j/issues/232)) ([00313ca](https://github.com/quokkify/q4j/commit/00313caf73085e18662b6e56245c55553c9d2910))
* **deps:** update com.rabbitmq:amqp-client to v5.31.0 ([#295](https://github.com/quokkify/q4j/issues/295)) ([742ab2d](https://github.com/quokkify/q4j/commit/742ab2d27f1bd57ef75efcd1262b2130cf759e1e))
* **deps:** update com.rabbitmq:amqp-client to v5.33.0 ([#312](https://github.com/quokkify/q4j/issues/312)) ([59d0324](https://github.com/quokkify/q4j/commit/59d032452e6aa6f3644a9968a696b031f6fb55c6))
* **deps:** update com.rabbitmq:amqp-client to v5.33.1 ([#323](https://github.com/quokkify/q4j/issues/323)) ([e5fc2b8](https://github.com/quokkify/q4j/commit/e5fc2b8b13e089bac1eccdf9bccab38d6f106a00))
* **deps:** update com.rabbitmq:amqp-client to v5.34.0 ([#336](https://github.com/quokkify/q4j/issues/336)) ([d6ac7f9](https://github.com/quokkify/q4j/commit/d6ac7f9d617907ae343b2d6dc9fc7f5c06df9fa8))
* **deps:** update commons-io:commons-io to v2.21.0 ([#68](https://github.com/quokkify/q4j/issues/68)) ([6074469](https://github.com/quokkify/q4j/commit/6074469eb041c51b36714afa5716ff4c6ceda6bb))
* **deps:** update commons-io:commons-io to v2.22.0 ([#188](https://github.com/quokkify/q4j/issues/188)) ([e0cd290](https://github.com/quokkify/q4j/commit/e0cd29080346824988ffa087656a4d71bbbb92b0))
* **deps:** update dev.morphia.morphia:morphia-core to v2.5.3 ([#258](https://github.com/quokkify/q4j/issues/258)) ([80d37f6](https://github.com/quokkify/q4j/commit/80d37f66f256553fcc621047b0ab0777afcf96a2))
* **deps:** update feign monorepo to v13.12 ([#198](https://github.com/quokkify/q4j/issues/198)) ([0ede68c](https://github.com/quokkify/q4j/commit/0ede68c2f38c0efabdcb2320856ca35ab3e362d0))
* **deps:** update feign monorepo to v13.13 ([#311](https://github.com/quokkify/q4j/issues/311)) ([e808ba4](https://github.com/quokkify/q4j/commit/e808ba4a174a131140f48b472791e2f7f0f4b3bd))
* **deps:** update feign monorepo to v13.8 ([#163](https://github.com/quokkify/q4j/issues/163)) ([fe936e0](https://github.com/quokkify/q4j/commit/fe936e04576821d4c2ece74957cc827f9fb07c8f))
* **deps:** update hibernate-orm monorepo to v7.1.6.final ([#60](https://github.com/quokkify/q4j/issues/60)) ([7214a16](https://github.com/quokkify/q4j/commit/7214a16863f28abcd714240c47cb6fe6a6b19f6e))
* **deps:** update hibernate-orm monorepo to v7.1.7.final ([#66](https://github.com/quokkify/q4j/issues/66)) ([4bc214c](https://github.com/quokkify/q4j/commit/4bc214c29854a0536380a898d546d06aa2a67643))
* **deps:** update hibernate-orm monorepo to v7.2.0.final ([#88](https://github.com/quokkify/q4j/issues/88)) ([b96eb57](https://github.com/quokkify/q4j/commit/b96eb57639ab7a2d3ccb5801e4c4b77b3cbed57e))
* **deps:** update hibernate-orm monorepo to v7.2.1.final ([#111](https://github.com/quokkify/q4j/issues/111)) ([bf670be](https://github.com/quokkify/q4j/commit/bf670bef5ce99ce905174707b83dec54b225faec))
* **deps:** update hibernate-orm monorepo to v7.2.2.final ([#124](https://github.com/quokkify/q4j/issues/124)) ([b7e501e](https://github.com/quokkify/q4j/commit/b7e501ee68f6c98a677f742b1ab0bb937b694754))
* **deps:** update hibernate-orm monorepo to v7.2.4.final ([#144](https://github.com/quokkify/q4j/issues/144)) ([fd30fc7](https://github.com/quokkify/q4j/commit/fd30fc7f46162e8e465441949da72bf0d7c89843))
* **deps:** update hibernate-orm monorepo to v7.3.3.final ([#199](https://github.com/quokkify/q4j/issues/199)) ([187bb51](https://github.com/quokkify/q4j/commit/187bb51e90f2d4af56a8c35f5cf6d8b5f7761f44))
* **deps:** update hibernate-orm monorepo to v7.3.5.final ([#253](https://github.com/quokkify/q4j/issues/253)) ([81e0842](https://github.com/quokkify/q4j/commit/81e084283f10f35eb378158fc172c36b8b436190))
* **deps:** update hibernate-orm monorepo to v7.3.6.final ([#276](https://github.com/quokkify/q4j/issues/276)) ([9ee1583](https://github.com/quokkify/q4j/commit/9ee158307a4b8afaaeaa15e62c1318b05cd7c28c))
* **deps:** update hibernate-orm monorepo to v7.4.2.final ([#305](https://github.com/quokkify/q4j/issues/305)) ([a3865c2](https://github.com/quokkify/q4j/commit/a3865c2f927ac4f243549c9698da94480afcfbfa))
* **deps:** update hibernate-orm monorepo to v7.4.3.final ([#328](https://github.com/quokkify/q4j/issues/328)) ([efe379b](https://github.com/quokkify/q4j/commit/efe379b07f2f9eb9edbf187945f15b41ee9b7090))
* **deps:** update hibernate-orm monorepo to v7.4.4.final ([#335](https://github.com/quokkify/q4j/issues/335)) ([e8c1ba2](https://github.com/quokkify/q4j/commit/e8c1ba26456430994bd746e76f856903d8e9fd01))
* **deps:** update hibernate-orm monorepo to v7.4.5.final ([#360](https://github.com/quokkify/q4j/issues/360)) ([33b3057](https://github.com/quokkify/q4j/commit/33b3057c13fea7590e248c9924048b771434f947))
* **deps:** update io.atlassian.fugue:fugue to v6.1.2 ([#145](https://github.com/quokkify/q4j/issues/145)) ([b014be3](https://github.com/quokkify/q4j/commit/b014be390a14b4a5ff2f4b2aaa6214b0f6d73058))
* **deps:** update io.atlassian.fugue:fugue to v6.1.3 ([#183](https://github.com/quokkify/q4j/issues/183)) ([03bb6d2](https://github.com/quokkify/q4j/commit/03bb6d23876c7d25849a92d6526055b40bfe55af))
* **deps:** update io.atlassian.fugue:fugue to v6.1.4 ([#259](https://github.com/quokkify/q4j/issues/259)) ([98a44cf](https://github.com/quokkify/q4j/commit/98a44cf249cb94e228f20e70d11bb19c23fa6d19))
* **deps:** update io.atlassian.fugue:fugue to v6.1.5 ([#315](https://github.com/quokkify/q4j/issues/315)) ([b586268](https://github.com/quokkify/q4j/commit/b5862689ec476d1901272b02153b6c0c18e9576b))
* **deps:** update io.github.classgraph:classgraph to v4.8.186 ([#385](https://github.com/quokkify/q4j/issues/385)) ([2a2c655](https://github.com/quokkify/q4j/commit/2a2c6552312e6eecb54d15470878583b10f0da5f))
* **deps:** update io.github.classgraph:classgraph to v4.8.187 ([#455](https://github.com/quokkify/q4j/issues/455)) ([11a75da](https://github.com/quokkify/q4j/commit/11a75da33cea7492c7b725eca63775ea7802cb84))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-71 to v3.12.0 ([#74](https://github.com/quokkify/q4j/issues/74)) ([f8b2c97](https://github.com/quokkify/q4j/commit/f8b2c97c067ab558427a72d6d8df6c01b382ad04))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-71 to v3.13.3 ([#89](https://github.com/quokkify/q4j/issues/89)) ([4db8235](https://github.com/quokkify/q4j/commit/4db823500e1a2c7317c52c2429d992f38b8f5252))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-71 to v3.14.0 ([#99](https://github.com/quokkify/q4j/issues/99)) ([552d127](https://github.com/quokkify/q4j/commit/552d12787c4654a8230bcc29e0dff8e8d85e093c))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-71 to v3.14.1 ([#109](https://github.com/quokkify/q4j/issues/109)) ([93c3879](https://github.com/quokkify/q4j/commit/93c387986cb58d423c37c4c0dad8c0b77f7666e4))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-71 to v3.15.1 ([#126](https://github.com/quokkify/q4j/issues/126)) ([d972221](https://github.com/quokkify/q4j/commit/d972221c4eac8ba2cd17e859c75fd0a68311cf56))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-71 to v3.15.2 ([#153](https://github.com/quokkify/q4j/issues/153)) ([c53e8fd](https://github.com/quokkify/q4j/commit/c53e8fdb50e785bbc92a94ecf47395f587f9ae42))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-73 to v3.15.3 ([#288](https://github.com/quokkify/q4j/issues/288)) ([a5560b1](https://github.com/quokkify/q4j/commit/a5560b13359dd8508f2ec90a9558c76dfe8406f4))
* **deps:** update io.hypersistence:hypersistence-utils-hibernate-73 to v3.15.4 ([#329](https://github.com/quokkify/q4j/issues/329)) ([2e2ca5d](https://github.com/quokkify/q4j/commit/2e2ca5d632498fdaade14c0af5cf8bf1fda81508))
* **deps:** update jira_rest_client_core to v7 ([#146](https://github.com/quokkify/q4j/issues/146)) ([be54c2b](https://github.com/quokkify/q4j/commit/be54c2b0d0f06bc806e83de39d3383de6ab4744a))
* **deps:** update jira_rest_client_core to v7.0.2 ([#324](https://github.com/quokkify/q4j/issues/324)) ([9d0e219](https://github.com/quokkify/q4j/commit/9d0e21983f7e7f9c78269336184c509184302335))
* **deps:** update log4j2 monorepo to v2.25.3 ([#102](https://github.com/quokkify/q4j/issues/102)) ([2d1f8d7](https://github.com/quokkify/q4j/commit/2d1f8d7d1bff881af35cc79010896b4aca022e52))
* **deps:** update log4j2 monorepo to v2.25.4 ([#184](https://github.com/quokkify/q4j/issues/184)) ([1cef1f8](https://github.com/quokkify/q4j/commit/1cef1f8d0b5443237733a6a639800cdf5e1e6fbb))
* **deps:** update log4j2 monorepo to v2.26.0 ([#231](https://github.com/quokkify/q4j/issues/231)) ([45d2224](https://github.com/quokkify/q4j/commit/45d22245d57ce070d26ec9d6ecc866881c256787))
* **deps:** update log4j2 monorepo to v2.26.1 ([#309](https://github.com/quokkify/q4j/issues/309)) ([d914fec](https://github.com/quokkify/q4j/commit/d914fec0aed6cde2ef182556a9259980368ee3d2))
* **deps:** update net.datafaker:datafaker to v2.5.3 ([#61](https://github.com/quokkify/q4j/issues/61)) ([df98e57](https://github.com/quokkify/q4j/commit/df98e571e28ed5406e57d499472c3c8c427be96a))
* **deps:** update net.datafaker:datafaker to v2.5.4 ([#185](https://github.com/quokkify/q4j/issues/185)) ([9eea85c](https://github.com/quokkify/q4j/commit/9eea85c1f4d1086e77a780bd88950d9b9599f68f))
* **deps:** update net.datafaker:datafaker to v2.6.0 ([#301](https://github.com/quokkify/q4j/issues/301)) ([24a4060](https://github.com/quokkify/q4j/commit/24a4060d86584e4fa18d44625fce87d3122f7081))
* **deps:** update net.datafaker:datafaker to v2.7.0 ([#310](https://github.com/quokkify/q4j/issues/310)) ([38fc5ab](https://github.com/quokkify/q4j/commit/38fc5abd8e71eb15c7de4dc7a8261e4cd728503f))
* **deps:** update org.apache.commons:commons-lang3 to v3.20.0 ([#90](https://github.com/quokkify/q4j/issues/90)) ([bee0521](https://github.com/quokkify/q4j/commit/bee0521f0768b8770d2be4456c6dd7578e0edba7))
* **deps:** update org.apache.commons:commons-text to v1.15.0 ([#91](https://github.com/quokkify/q4j/issues/91)) ([101e6f1](https://github.com/quokkify/q4j/commit/101e6f123dcb80219089a92116a3b4bacd554542))
* **deps:** update org.apache.httpcomponents.core5:httpcore5 to v5.4 ([#92](https://github.com/quokkify/q4j/issues/92)) ([7289f34](https://github.com/quokkify/q4j/commit/7289f349192673d977934fec202107dc496f2ace))
* **deps:** update org.apache.kafka:kafka-clients to v4.2.0 ([#192](https://github.com/quokkify/q4j/issues/192)) ([ae2a199](https://github.com/quokkify/q4j/commit/ae2a199f6561ff10a0021499ac1b0d1677529bdf))
* **deps:** update org.apache.kafka:kafka-clients to v4.3.0 ([#263](https://github.com/quokkify/q4j/issues/263)) ([84661c9](https://github.com/quokkify/q4j/commit/84661c91412a0409316bfd19cc386705e5562f3b))
* **deps:** update org.apache.kafka:kafka-clients to v4.3.1 ([#308](https://github.com/quokkify/q4j/issues/308)) ([61c97ef](https://github.com/quokkify/q4j/commit/61c97efac3586448d1c2746c64639439d551008e))
* **deps:** update org.aspectj:aspectjweaver to v1.9.25 ([#62](https://github.com/quokkify/q4j/issues/62)) ([5896be4](https://github.com/quokkify/q4j/commit/5896be4b3ac123621aba58dc1da4082dbdc28501))
* **deps:** update org.aspectj:aspectjweaver to v1.9.25.1 ([#106](https://github.com/quokkify/q4j/issues/106)) ([a811e72](https://github.com/quokkify/q4j/commit/a811e72c5fdd0152d05c414c94b52e4231139a15))
* **deps:** update org.assertj:assertj-core to v3.27.7 ([#123](https://github.com/quokkify/q4j/issues/123)) ([79de5e4](https://github.com/quokkify/q4j/commit/79de5e4ac5d0257da2e02c464f56bab7a6a1a05b))
* **deps:** update org.bouncycastle:bcprov-jdk18on to v1.83 ([#93](https://github.com/quokkify/q4j/issues/93)) ([85db7a2](https://github.com/quokkify/q4j/commit/85db7a2227e7c945320ad7456b7ded9ab4e4da44))
* **deps:** update org.bouncycastle:bcprov-jdk18on to v1.84 ([#193](https://github.com/quokkify/q4j/issues/193)) ([30eac08](https://github.com/quokkify/q4j/commit/30eac08756a7134d6cfeaafe4821eb0a4eefa53e))
* **deps:** update org.bouncycastle:bcprov-jdk18on to v1.85 ([#365](https://github.com/quokkify/q4j/issues/365)) ([5f9f067](https://github.com/quokkify/q4j/commit/5f9f0679d35ac7db30253f1bdb8dcead8ee28b9d))
* **deps:** update org.bouncycastle:bcprov-jdk18on to v1.85.2 ([#456](https://github.com/quokkify/q4j/issues/456)) ([4ff11d5](https://github.com/quokkify/q4j/commit/4ff11d5542a37e7459f9e74737f15881685f781a))
* **deps:** update org.jetbrains:annotations to v26.1.0 ([#217](https://github.com/quokkify/q4j/issues/217)) ([18e3717](https://github.com/quokkify/q4j/commit/18e371797a9ab77533b26c3525d1bb3301560b82))
* **deps:** update org.mockito:mockito-core to v5.23.0 ([#244](https://github.com/quokkify/q4j/issues/244)) ([cd7e6d4](https://github.com/quokkify/q4j/commit/cd7e6d48f43ea9b9b76b40fb61c1a1887c714277))
* **deps:** update org.modelmapper:modelmapper to v3.2.6 ([#72](https://github.com/quokkify/q4j/issues/72)) ([8626745](https://github.com/quokkify/q4j/commit/862674575ec865c7bf9e0b386e74d6b6a6e684bd))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.6.3 ([#154](https://github.com/quokkify/q4j/issues/154)) ([ca84cbe](https://github.com/quokkify/q4j/commit/ca84cbe57ab0bae7df0332e8723c6d7231aed7d5))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.7.0 ([#189](https://github.com/quokkify/q4j/issues/189)) ([a62f0de](https://github.com/quokkify/q4j/commit/a62f0de3cbf812c37fd1e1b1e1e4474ce95250e4))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.8.0 ([#294](https://github.com/quokkify/q4j/issues/294)) ([44bab28](https://github.com/quokkify/q4j/commit/44bab2803fdde949242c5027d4b972a574305cc4))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.9.0 ([#326](https://github.com/quokkify/q4j/issues/326)) ([3cf6ee2](https://github.com/quokkify/q4j/commit/3cf6ee295ebf2b8b88853452aa49e1e1842ad50b))
* **deps:** update org.mongodb:mongodb-driver-sync to v5.9.1 ([#378](https://github.com/quokkify/q4j/issues/378)) ([de7e54e](https://github.com/quokkify/q4j/commit/de7e54ea8cce9592c0bcc6b122fe797706ee9fce))
* **deps:** update org.redisson:redisson to v4 ([#160](https://github.com/quokkify/q4j/issues/160)) ([8d8d2fe](https://github.com/quokkify/q4j/commit/8d8d2fe615ca26b8e3f31673d3a356dd8c84d109))
* **deps:** update org.redisson:redisson to v4.3.1 ([#194](https://github.com/quokkify/q4j/issues/194)) ([e7e47b0](https://github.com/quokkify/q4j/commit/e7e47b074d6a3af4c908679a9d3b59e91ff462c5))
* **deps:** update org.redisson:redisson to v4.4.0 ([#264](https://github.com/quokkify/q4j/issues/264)) ([361bc6e](https://github.com/quokkify/q4j/commit/361bc6e0de6a0a1d9e5699cfc24f87715155e678))
* **deps:** update org.redisson:redisson to v4.6.0 ([#296](https://github.com/quokkify/q4j/issues/296)) ([9233504](https://github.com/quokkify/q4j/commit/92335047877c75644f05d5c99d27176ccff7b1dc))
* **deps:** update org.redisson:redisson to v4.6.1 ([#316](https://github.com/quokkify/q4j/issues/316)) ([e740580](https://github.com/quokkify/q4j/commit/e740580707f48d401b49864a259b8fa5e34cdb2a))
* **deps:** update org.redisson:redisson to v4.7.0 ([#419](https://github.com/quokkify/q4j/issues/419)) ([a80a576](https://github.com/quokkify/q4j/commit/a80a576f07139ccb8877cfe73d0df1de36940b8f))
* **deps:** update org.testng:testng to v7.12.0 ([#120](https://github.com/quokkify/q4j/issues/120)) ([825b78f](https://github.com/quokkify/q4j/commit/825b78f617f25b5ab079dae01ba2dfece286a780))
* **deps:** update rest_assured to v6 ([#96](https://github.com/quokkify/q4j/issues/96)) ([7928826](https://github.com/quokkify/q4j/commit/7928826d9507703b50079059fec6c29f1138d89a))
* **deps:** update rest_assured to v6.0.1 ([#345](https://github.com/quokkify/q4j/issues/345)) ([ac02887](https://github.com/quokkify/q4j/commit/ac02887ca7c886d46d8f13d2bff72541cc6ace39))
* **deps:** update selenide to v7.14.0 ([#121](https://github.com/quokkify/q4j/issues/121)) ([8d5fdaf](https://github.com/quokkify/q4j/commit/8d5fdaf84a97bdda9bec3b7f8647037996ca52d6))
* **deps:** update selenide to v7.16.0 ([#190](https://github.com/quokkify/q4j/issues/190)) ([04aab0e](https://github.com/quokkify/q4j/commit/04aab0e7a475e2baf346c6c13cf115bfc33009b6))
* **deps:** update selenide to v7.16.1 ([#212](https://github.com/quokkify/q4j/issues/212)) ([b7a71f1](https://github.com/quokkify/q4j/commit/b7a71f17cc9150922199f2e05c0d337e9e36e443))
* **deps:** update selenide to v7.16.2 ([#278](https://github.com/quokkify/q4j/issues/278)) ([3e90a42](https://github.com/quokkify/q4j/commit/3e90a42a6b1c5ade6a43433f7742aa59779c469a))
* **deps:** update selenide to v7.17.0 ([#362](https://github.com/quokkify/q4j/issues/362)) ([1dbec12](https://github.com/quokkify/q4j/commit/1dbec126af62e5faf2280043e3b040272227f64b))
* **deps:** update slf4j monorepo to v2.0.18 ([#260](https://github.com/quokkify/q4j/issues/260)) ([2c83b6f](https://github.com/quokkify/q4j/commit/2c83b6f61370e469c6c88b306c664372c6db2e05))
* **deps:** update tyrus to v2.2.2 ([#218](https://github.com/quokkify/q4j/issues/218)) ([c09cac3](https://github.com/quokkify/q4j/commit/c09cac367a635a5c2a618e76b9e4331c90f81660))
* **files:** prevent zip slip during extraction ([#423](https://github.com/quokkify/q4j/issues/423)) ([834ffbe](https://github.com/quokkify/q4j/commit/834ffbe17666892a308b88f04eed88c2ed99f0f4))
* **publishing:** avoid duplicate Javadoc artifacts ([#412](https://github.com/quokkify/q4j/issues/412)) ([6c05ec5](https://github.com/quokkify/q4j/commit/6c05ec579db6415c159a8edcdfe22f4b706a42e4))
* **publishing:** clean up Javadoc diagnostics ([#416](https://github.com/quokkify/q4j/issues/416)) ([e62332e](https://github.com/quokkify/q4j/commit/e62332ea7434e24a47961edb0ff96ee469ef48b3))
* **publishing:** include resolved dependency versions ([#414](https://github.com/quokkify/q4j/issues/414)) ([e3f51df](https://github.com/quokkify/q4j/commit/e3f51dfd1e949bb0cc63566c9074966cca4857d9))
* revert actions permission to none in detect-runner ([8b4f1f5](https://github.com/quokkify/q4j/commit/8b4f1f52b71d54510678f947848cffc9f9b1f5f7))
* **selenide:** enable remote Grid downloads ([#466](https://github.com/quokkify/q4j/issues/466)) ([9475dfa](https://github.com/quokkify/q4j/commit/9475dfaf7b9717a356acae3d42a41b34ace11dde))
* **selenide:** stop leaking Basic Auth credentials into url and Allure ([#450](https://github.com/quokkify/q4j/issues/450)) ([d2c4749](https://github.com/quokkify/q4j/commit/d2c47497ee87b89835f0ab291eca73cfce4c9ddd))
* **selenide:** wait for asynchronously-appearing rows in table lookups ([#461](https://github.com/quokkify/q4j/issues/461)) ([366c264](https://github.com/quokkify/q4j/commit/366c26497dcf5a8b904b9efd86d473284fd7fd59))
* **selenide:** wire Verification timeout/polling into custom waits ([#452](https://github.com/quokkify/q4j/issues/452)) ([e7827e1](https://github.com/quokkify/q4j/commit/e7827e1d8018a95bf0144e62d6312ac35139a50e))


### 📚 Documentation

* added badge with renovate ([8c30468](https://github.com/quokkify/q4j/commit/8c30468478a5f77591d0ab16fe93f7af3d300d25))
* **changelog:** curate initial release notes ([#352](https://github.com/quokkify/q4j/issues/352)) ([b0677e7](https://github.com/quokkify/q4j/commit/b0677e7f02a2cc07f5c2306a793e18992e909623))
* **changelog:** format release notes ([#350](https://github.com/quokkify/q4j/issues/350)) ([2756ad0](https://github.com/quokkify/q4j/commit/2756ad08cd0da74bbdfb4deef2ca375856fdeca3))
* **modules:** document Maven Central dependencies ([#427](https://github.com/quokkify/q4j/issues/427)) ([d7f6647](https://github.com/quokkify/q4j/commit/d7f6647d1cb05caa8535d2a499882c35b2e8ccfc))


### ⚙️ CI

* **compose:** run health lifecycle ([#436](https://github.com/quokkify/q4j/issues/436)) ([b44dbf5](https://github.com/quokkify/q4j/commit/b44dbf5a1c3bd47f3752eb144b2563f601323007))
* **prettier:** ignore generated changelog ([#351](https://github.com/quokkify/q4j/issues/351)) ([90ea1d4](https://github.com/quokkify/q4j/commit/90ea1d4d144d93d5e3ab222df28d2b25eade0a11))
* **release:** add emoji headings in release changelog ([#357](https://github.com/quokkify/q4j/issues/357)) ([4774552](https://github.com/quokkify/q4j/commit/4774552357021927eb46baa0c0b807281730ed5f))
* **release:** configure changelog sections ([#353](https://github.com/quokkify/q4j/issues/353)) ([353dba1](https://github.com/quokkify/q4j/commit/353dba1ec5ee636fa53cbd81ef250bff769a7708))
* **report:** allow test report workflow to read contents ([#347](https://github.com/quokkify/q4j/issues/347)) ([530862c](https://github.com/quokkify/q4j/commit/530862c3e668f2020bfea885d9bc9fac117b6dde))
* **report:** reuse project-toolkit for Allure reporting ([#396](https://github.com/quokkify/q4j/issues/396)) ([89b8539](https://github.com/quokkify/q4j/commit/89b85398f3ef5a0d2c2540bb35402aed398574d9))
* **report:** split Allure environments by module family ([#349](https://github.com/quokkify/q4j/issues/349)) ([dc6c176](https://github.com/quokkify/q4j/commit/dc6c176a3ce84317f5c255639834faf72983ac7a))
* **reports:** publish Allure reports and automate releases ([#346](https://github.com/quokkify/q4j/issues/346)) ([346836e](https://github.com/quokkify/q4j/commit/346836ea6fcd2467238b8be376087972e6682dd6))


### ♻️ Code Refactoring

* **ci:** consolidate Allure helpers under tools ([#421](https://github.com/quokkify/q4j/issues/421)) ([79223e6](https://github.com/quokkify/q4j/commit/79223e6ab2bcdb1b273664bbfeb5f0f6bd22a74c))
* **selenide:** remove semantic element aliases ([#458](https://github.com/quokkify/q4j/issues/458)) ([0917d6b](https://github.com/quokkify/q4j/commit/0917d6bebbc288131655957d39a60448858dfa87))
* **selenide:** remove unused public API BaseBlock, ColorFormatter, PageTitle ([#453](https://github.com/quokkify/q4j/issues/453)) ([7234bcd](https://github.com/quokkify/q4j/commit/7234bcd2cac3965b115b9bc8bad53c95dbc11ad2))
* **selenide:** shrink Browser facade to project-level configuration ([#451](https://github.com/quokkify/q4j/issues/451)) ([716dff4](https://github.com/quokkify/q4j/commit/716dff477bb11fd69652aa3ef9b92afa81929b64))
* **selenide:** use native container lists ([#441](https://github.com/quokkify/q4j/issues/441)) ([fbb952a](https://github.com/quokkify/q4j/commit/fbb952a98cee6da896e71b661074a353acf02cfc))


### 🧹 Chores

* apply prettier formatting to markdown files ([ca1dfb1](https://github.com/quokkify/q4j/commit/ca1dfb19ce5afab6ee547bbbf2714b5a4f3cd341))
* **ci:** adopt project-toolkit v2.8.1 ([#382](https://github.com/quokkify/q4j/issues/382)) ([0290a26](https://github.com/quokkify/q4j/commit/0290a26074f1adcd75f2f7b7f93ef609b55a1576))
* **deps:** pin curlimages/curl docker tag to d43bdb2 ([#241](https://github.com/quokkify/q4j/issues/241)) ([081b5e2](https://github.com/quokkify/q4j/commit/081b5e2a9ed77398859c31f3b88dbd3405b9b11e))
* **deps:** pin dependencies ([#157](https://github.com/quokkify/q4j/issues/157)) ([e348a62](https://github.com/quokkify/q4j/commit/e348a62d3da5110e93caae33148d363aa9248767))
* **deps:** pin dependencies ([#236](https://github.com/quokkify/q4j/issues/236)) ([3a431d0](https://github.com/quokkify/q4j/commit/3a431d01f3d980896b9d9ee483b446ae3fb4674f))
* **deps:** pin myoung34/github-runner docker tag to 388d7e9 ([#129](https://github.com/quokkify/q4j/issues/129)) ([2607b1e](https://github.com/quokkify/q4j/commit/2607b1eb5d5060461719869331648ddbe0a93f7f))
* **deps:** update actions/checkout action to v6 ([#94](https://github.com/quokkify/q4j/issues/94)) ([363fd2e](https://github.com/quokkify/q4j/commit/363fd2e66c42753ac742f1c7324dfada4942c51c))
* **deps:** update actions/checkout action to v7 ([#302](https://github.com/quokkify/q4j/issues/302)) ([bc1e3df](https://github.com/quokkify/q4j/commit/bc1e3dffd9688d92cb7ef2ed7678178fe416e24e))
* **deps:** update actions/checkout digest to de0fac2 ([#150](https://github.com/quokkify/q4j/issues/150)) ([d08104d](https://github.com/quokkify/q4j/commit/d08104d17058b6ab8e989dd860583c34643de737))
* **deps:** update actions/github-script action to v8 ([#116](https://github.com/quokkify/q4j/issues/116)) ([ab0dec1](https://github.com/quokkify/q4j/commit/ab0dec1d0d354c0823f97d2f4424b4f512f42624))
* **deps:** update actions/github-script action to v9 ([#176](https://github.com/quokkify/q4j/issues/176)) ([a9892a9](https://github.com/quokkify/q4j/commit/a9892a953dc61db13ecc931bb1372d977777f8c5))
* **deps:** update actions/github-script action to v9 ([#411](https://github.com/quokkify/q4j/issues/411)) ([a484cd3](https://github.com/quokkify/q4j/commit/a484cd395b171ef3bcf3720fb91324bf716ddf56))
* **deps:** update actions/setup-java digest to 0f481fc ([#313](https://github.com/quokkify/q4j/issues/313)) ([effeecb](https://github.com/quokkify/q4j/commit/effeecb2d9e0885dfe06563ed0864d9c44b428be))
* **deps:** update actions/setup-java digest to be666c2 ([#130](https://github.com/quokkify/q4j/issues/130)) ([454641e](https://github.com/quokkify/q4j/commit/454641ee9dae97cde6f9b91a872dd534fafab886))
* **deps:** update actions/setup-java digest to f2beeb2 ([#78](https://github.com/quokkify/q4j/issues/78)) ([3dc1bfb](https://github.com/quokkify/q4j/commit/3dc1bfbbf36437799a5c641fb726840fc6e7f469))
* **deps:** update actions/setup-node action to v6 ([#147](https://github.com/quokkify/q4j/issues/147)) ([ef2bef4](https://github.com/quokkify/q4j/commit/ef2bef4730d30a7be5d52a1cd2413c9621abe6d6))
* **deps:** update actions/setup-node action to v7 ([#363](https://github.com/quokkify/q4j/issues/363)) ([27af211](https://github.com/quokkify/q4j/commit/27af2114e8d1af7912814f9a58765227e9911528))
* **deps:** update actions/setup-node digest to 48b55a0 ([#203](https://github.com/quokkify/q4j/issues/203)) ([8bac40e](https://github.com/quokkify/q4j/commit/8bac40e082ab0fc5757e323d614c7ef3f993956d))
* **deps:** update bitnamilegacy/rabbitmq docker tag to v4.1.3 ([#222](https://github.com/quokkify/q4j/issues/222)) ([a82ebaf](https://github.com/quokkify/q4j/commit/a82ebaf280cbc1b10a301ef92fc7c06fabe3214f))
* **deps:** update centrifugo/centrifugo docker tag to v6 ([#64](https://github.com/quokkify/q4j/issues/64)) ([cd6cd53](https://github.com/quokkify/q4j/commit/cd6cd53db0484552b71765f81c2e9d0b3b0181c0))
* **deps:** update centrifugo/centrifugo docker tag to v6.5.1 ([#73](https://github.com/quokkify/q4j/issues/73)) ([29b4b12](https://github.com/quokkify/q4j/commit/29b4b1294ce3c17e5a36fa331a5d3071f232e922))
* **deps:** update centrifugo/centrifugo docker tag to v6.5.2 ([#81](https://github.com/quokkify/q4j/issues/81)) ([01cf7ea](https://github.com/quokkify/q4j/commit/01cf7ea5709eced73d2f0563450477d006ece1c0))
* **deps:** update curlimages/curl docker tag to v8.20.0 ([#251](https://github.com/quokkify/q4j/issues/251)) ([7db688a](https://github.com/quokkify/q4j/commit/7db688a745a8cc105656afc3b507001e8554b69f))
* **deps:** update curlimages/curl docker tag to v8.21.0 ([#330](https://github.com/quokkify/q4j/issues/330)) ([f308526](https://github.com/quokkify/q4j/commit/f308526a736b96352ac1bb3751ffc88a5da5cfeb))
* **deps:** update docker docker tag to v29.2 ([#125](https://github.com/quokkify/q4j/issues/125)) ([817a52f](https://github.com/quokkify/q4j/commit/817a52fc3e6f29224fc6c90a770df25865a7559f))
* **deps:** update docker docker tag to v29.4 ([#213](https://github.com/quokkify/q4j/issues/213)) ([081959d](https://github.com/quokkify/q4j/commit/081959db6cb09b92e422d4b86112784dd7905cd1))
* **deps:** update docker docker tag to v29.5 ([#261](https://github.com/quokkify/q4j/issues/261)) ([bd6dcc4](https://github.com/quokkify/q4j/commit/bd6dcc4dbe20585c8647f6fad704d24b2ca23658))
* **deps:** update docker docker tag to v29.6 ([#331](https://github.com/quokkify/q4j/issues/331)) ([eb26a77](https://github.com/quokkify/q4j/commit/eb26a77f6345f5fde27a62239f9a3c06aa584248))
* **deps:** update docker docker tag to v29.7 ([#393](https://github.com/quokkify/q4j/issues/393)) ([03eac68](https://github.com/quokkify/q4j/commit/03eac68f815d7511b1fd765ff39c8503039b3708))
* **deps:** update docker:29.2-dind docker digest to 68f6d9a ([#204](https://github.com/quokkify/q4j/issues/204)) ([e8d38cc](https://github.com/quokkify/q4j/commit/e8d38ccff44b323b6636127b6cb83f35045b2af2))
* **deps:** update docker:29.2-dind docker digest to 8bcbad4 ([#151](https://github.com/quokkify/q4j/issues/151)) ([6dce7be](https://github.com/quokkify/q4j/commit/6dce7bee261a6a882e8131787222aa2070a61d8f))
* **deps:** update docker:29.2-dind docker digest to a284d31 ([#131](https://github.com/quokkify/q4j/issues/131)) ([6adf699](https://github.com/quokkify/q4j/commit/6adf69968fa09d2eaed510d81ee4605e0df7653d))
* **deps:** update docker:29.4-dind docker digest to 685b91d ([#248](https://github.com/quokkify/q4j/issues/248)) ([8d57f89](https://github.com/quokkify/q4j/commit/8d57f89c7f6ab0c78179e83a56886d2fe4f9a1e7))
* **deps:** update docker:29.5-dind docker digest to 7278248 ([#281](https://github.com/quokkify/q4j/issues/281)) ([0dbe29f](https://github.com/quokkify/q4j/commit/0dbe29f94808710c3b94b7401dc82eb8974597b3))
* **deps:** update dorny/test-reporter action to v3 ([#177](https://github.com/quokkify/q4j/issues/177)) ([08c8002](https://github.com/quokkify/q4j/commit/08c80028df92a63f2cd9d29581e828f6b1bb2d44))
* **deps:** update dorny/test-reporter digest to b082adf ([#113](https://github.com/quokkify/q4j/issues/113)) ([4d94392](https://github.com/quokkify/q4j/commit/4d943922769ac4bad05b9f34aa8afd80f8682cf6))
* **deps:** update dorny/test-reporter digest to fe45e95 ([#79](https://github.com/quokkify/q4j/issues/79)) ([fb0939c](https://github.com/quokkify/q4j/commit/fb0939ca40d5340f554619b217cb183223dfbbec))
* **deps:** update github artifact actions ([#178](https://github.com/quokkify/q4j/issues/178)) ([a86442f](https://github.com/quokkify/q4j/commit/a86442f945b3e1a38e2331c2483a36e8a842c76f))
* **deps:** update github artifact actions ([#52](https://github.com/quokkify/q4j/issues/52)) ([9da9bc8](https://github.com/quokkify/q4j/commit/9da9bc81c79ae8c28b04494104ab90f889ce0d2b))
* **deps:** update github artifact actions ([#95](https://github.com/quokkify/q4j/issues/95)) ([97c9011](https://github.com/quokkify/q4j/commit/97c901194df06b29449b1bb79c3fca1f2da5849a))
* **deps:** update github-actions ([#440](https://github.com/quokkify/q4j/issues/440)) ([69f54ee](https://github.com/quokkify/q4j/commit/69f54eeebd58f64dbf9063522429e4f814b953f1))
* **deps:** update gradle to v9.2.0 ([#53](https://github.com/quokkify/q4j/issues/53)) ([38f83dc](https://github.com/quokkify/q4j/commit/38f83dc0e7b93de099942669f00888123d38e805))
* **deps:** update gradle to v9.2.1 ([#82](https://github.com/quokkify/q4j/issues/82)) ([46c5d77](https://github.com/quokkify/q4j/commit/46c5d7703102b5843aed17d782796aec5cbbb0a2))
* **deps:** update gradle to v9.3.0 ([#112](https://github.com/quokkify/q4j/issues/112)) ([81b7fd6](https://github.com/quokkify/q4j/commit/81b7fd6a99dc6de9dd1f7c35d76772cca36106a7))
* **deps:** update gradle to v9.3.1 ([#143](https://github.com/quokkify/q4j/issues/143)) ([f736e06](https://github.com/quokkify/q4j/commit/f736e0664d901f0d91cceef3a74940f69e3333b7))
* **deps:** update gradle to v9.5.0 ([#175](https://github.com/quokkify/q4j/issues/175)) ([ca5ae47](https://github.com/quokkify/q4j/commit/ca5ae4749d289f6986b246bad332f0c2c76d5caf))
* **deps:** update gradle to v9.5.1 ([#268](https://github.com/quokkify/q4j/issues/268)) ([1afd0ca](https://github.com/quokkify/q4j/commit/1afd0ca8ccf1cd126e87244965b7d9d0d6f919f6))
* **deps:** update gradle to v9.6.0 ([#303](https://github.com/quokkify/q4j/issues/303)) ([fbacfd7](https://github.com/quokkify/q4j/commit/fbacfd72aa571c6e96f17da1a05135b0ad7a2ba9))
* **deps:** update gradle to v9.6.1 ([#304](https://github.com/quokkify/q4j/issues/304)) ([14de3e5](https://github.com/quokkify/q4j/commit/14de3e5061c61a5be6e34c111a678856c60531f1))
* **deps:** update gradle to v9.7.0 ([#457](https://github.com/quokkify/q4j/issues/457)) ([b0c64e3](https://github.com/quokkify/q4j/commit/b0c64e3e5af903bfa9a95761d1b8a1aa51618904))
* **deps:** update gradle/actions action to v6 ([#179](https://github.com/quokkify/q4j/issues/179)) ([4f0b298](https://github.com/quokkify/q4j/commit/4f0b29863ec57a3506d6aec5ea91b0581f5af14e))
* **deps:** update gradle/actions digest to 3f131e8 ([#314](https://github.com/quokkify/q4j/issues/314)) ([7278466](https://github.com/quokkify/q4j/commit/7278466a19ee7047064c8cc150cdfd1673fa8dfc))
* **deps:** update gradle/actions digest to f29f5a9 ([#132](https://github.com/quokkify/q4j/issues/132)) ([401acdb](https://github.com/quokkify/q4j/commit/401acdb945d5c48627414986a420852f71931c49))
* **deps:** update mockserver/mockserver docker tag to v6 ([#269](https://github.com/quokkify/q4j/issues/269)) ([746104c](https://github.com/quokkify/q4j/commit/746104c19e47e63c1c3c80d5c3d584596de49dc1))
* **deps:** update mockserver/mockserver docker tag to v7 ([#298](https://github.com/quokkify/q4j/issues/298)) ([5dcf684](https://github.com/quokkify/q4j/commit/5dcf684f1a197d8da6783091322417b09fdf9071))
* **deps:** update mockserver/mockserver docker tag to v7.4.0 ([#325](https://github.com/quokkify/q4j/issues/325)) ([eb41815](https://github.com/quokkify/q4j/commit/eb41815ea8a001b761bd244b84aad610a1cd4265))
* **deps:** update mockserver/mockserver docker tag to v7.5.0 ([#388](https://github.com/quokkify/q4j/issues/388)) ([5c0842a](https://github.com/quokkify/q4j/commit/5c0842a78e65b44356e3aee2212ae566e0cc758d))
* **deps:** update mongo docker tag to v8 ([#65](https://github.com/quokkify/q4j/issues/65)) ([535b812](https://github.com/quokkify/q4j/commit/535b812d32a4fdbdd4fcd4624115facb2d497e7f))
* **deps:** update mongo docker tag to v8.2.2 ([#83](https://github.com/quokkify/q4j/issues/83)) ([d7964b3](https://github.com/quokkify/q4j/commit/d7964b3ba540125c15b3033f803e7adac14968b9))
* **deps:** update mongo docker tag to v8.2.3 ([#114](https://github.com/quokkify/q4j/issues/114)) ([27e0629](https://github.com/quokkify/q4j/commit/27e06294f9e61ddcf1c9144060b2d87eab90b04b))
* **deps:** update mongo docker tag to v8.2.4 ([#128](https://github.com/quokkify/q4j/issues/128)) ([d4dee26](https://github.com/quokkify/q4j/commit/d4dee26bbcce1bb3a77c27e9c31bf55326e473f3))
* **deps:** update mongo docker tag to v8.2.7 ([#181](https://github.com/quokkify/q4j/issues/181)) ([d5fe6f3](https://github.com/quokkify/q4j/commit/d5fe6f33b8f7662df80a69cfd474aa79c5ecb14a))
* **deps:** update mongo docker tag to v8.3.1 ([#214](https://github.com/quokkify/q4j/issues/214)) ([d79c9c3](https://github.com/quokkify/q4j/commit/d79c9c3271bec73fb9b30b8324be80a03652710b))
* **deps:** update mongo docker tag to v8.3.2 ([#256](https://github.com/quokkify/q4j/issues/256)) ([416822c](https://github.com/quokkify/q4j/commit/416822c39ed09ff276429e0c0d13adbefc24c710))
* **deps:** update mongo docker tag to v8.3.2 ([#271](https://github.com/quokkify/q4j/issues/271)) ([f0b1b97](https://github.com/quokkify/q4j/commit/f0b1b97d6d98b4321c0c8b31b92d95f978924526))
* **deps:** update mongo docker tag to v8.3.3 ([#340](https://github.com/quokkify/q4j/issues/340)) ([940066a](https://github.com/quokkify/q4j/commit/940066a155674bb70c085548f2d83bf8de540610))
* **deps:** update mongo docker tag to v8.3.4 ([#344](https://github.com/quokkify/q4j/issues/344)) ([5b918d5](https://github.com/quokkify/q4j/commit/5b918d53edd2931770e9a314d859fb182c0b6559))
* **deps:** update mongo docker tag to v8.3.7 ([#373](https://github.com/quokkify/q4j/issues/373)) ([f2166f3](https://github.com/quokkify/q4j/commit/f2166f312a3813e69ff6417edfde4bbf54643898))
* **deps:** update mongo:8.2.1 docker digest to 7d1a1a6 ([#69](https://github.com/quokkify/q4j/issues/69)) ([038950f](https://github.com/quokkify/q4j/commit/038950f4f380ef2a4ba26ceb83ae3d1817e8c686))
* **deps:** update mongo:8.2.1 docker digest to e64a984 ([#75](https://github.com/quokkify/q4j/issues/75)) ([4c1e180](https://github.com/quokkify/q4j/commit/4c1e18060c72cbaccd2428bd52b86ffa35529ee0))
* **deps:** update mongo:8.3.1 docker digest to 707dc00 ([#265](https://github.com/quokkify/q4j/issues/265)) ([6850ae1](https://github.com/quokkify/q4j/commit/6850ae126976f974a50c216b6b141d1df53089b8))
* **deps:** update mongo:8.3.2 docker digest to c4089dd ([#282](https://github.com/quokkify/q4j/issues/282)) ([5a98b9c](https://github.com/quokkify/q4j/commit/5a98b9c68bfa28b45a75d2fdde5e07251863207b))
* **deps:** update mongo:8.3.7 docker digest to 2f02e21 ([#401](https://github.com/quokkify/q4j/issues/401)) ([4f6c53a](https://github.com/quokkify/q4j/commit/4f6c53ab8c189ffd675c31096584fb273e5eb383))
* **deps:** update myoung34/github-runner docker tag to v2.335.1 ([#332](https://github.com/quokkify/q4j/issues/332)) ([76304eb](https://github.com/quokkify/q4j/commit/76304eb794c0cae54a9f4f2df6c3d15a3100f6f4))
* **deps:** update myoung34/github-runner docker tag to v2.336.0 ([#390](https://github.com/quokkify/q4j/issues/390)) ([fa50aa7](https://github.com/quokkify/q4j/commit/fa50aa7969155637bf1314d35d734bc9d8af921e))
* **deps:** update nginx docker tag to v1.29.3 ([#56](https://github.com/quokkify/q4j/issues/56)) ([b8eb781](https://github.com/quokkify/q4j/commit/b8eb781def7a150120e40d9d580b866e00ebe029))
* **deps:** update nginx docker tag to v1.29.4 ([#84](https://github.com/quokkify/q4j/issues/84)) ([5293813](https://github.com/quokkify/q4j/commit/5293813cadfe74d2f04d39c926cc24b0ad5accd0))
* **deps:** update nginx docker tag to v1.29.5 ([#152](https://github.com/quokkify/q4j/issues/152)) ([68f6d9e](https://github.com/quokkify/q4j/commit/68f6d9e29e16e119b9c7998d9fe96716ef8e1399))
* **deps:** update nginx docker tag to v1.30.0 ([#227](https://github.com/quokkify/q4j/issues/227)) ([bfae146](https://github.com/quokkify/q4j/commit/bfae14680b687feb7e7705934252e1ece51b71d1))
* **deps:** update nginx docker tag to v1.31.0 ([#262](https://github.com/quokkify/q4j/issues/262)) ([3f1f564](https://github.com/quokkify/q4j/commit/3f1f564097c56c68f3c2ade76e6d55139fcabcde))
* **deps:** update nginx docker tag to v1.31.1 ([#273](https://github.com/quokkify/q4j/issues/273)) ([4042f9e](https://github.com/quokkify/q4j/commit/4042f9ea0a6bd53b36ebfdd22ad9857e7e3adf5a))
* **deps:** update nginx docker tag to v1.31.2 ([#341](https://github.com/quokkify/q4j/issues/341)) ([82184b5](https://github.com/quokkify/q4j/commit/82184b5167da9fc9d559a6ff15348cf9361f69d1))
* **deps:** update nginx docker tag to v1.31.3 ([#368](https://github.com/quokkify/q4j/issues/368)) ([05400be](https://github.com/quokkify/q4j/commit/05400be71ef719683d3602babe18c0352950f5a7))
* **deps:** update nginx:1.29.4 docker digest to c881927 ([#133](https://github.com/quokkify/q4j/issues/133)) ([bdbae62](https://github.com/quokkify/q4j/commit/bdbae62ca82311676336259a30fda16ef6203336))
* **deps:** update nginx:1.30.0 docker digest to 50e2952 ([#249](https://github.com/quokkify/q4j/issues/249)) ([bafc63a](https://github.com/quokkify/q4j/commit/bafc63ad587fbd5c7e371a42fd45b7899836ca88))
* **deps:** update nginx:1.30.0 docker digest to 55d1fb0 ([#254](https://github.com/quokkify/q4j/issues/254)) ([d08883d](https://github.com/quokkify/q4j/commit/d08883d149fc472c29b00fb794fd45d677046c7c))
* **deps:** update nginx:1.31.1 docker digest to 5aca995 ([#274](https://github.com/quokkify/q4j/issues/274)) ([d5fa59b](https://github.com/quokkify/q4j/commit/d5fa59bf09fc4e0691d69f3be8596de02156e654))
* **deps:** update nginx:1.31.3 docker digest to 8541484 ([#459](https://github.com/quokkify/q4j/issues/459)) ([bb23657](https://github.com/quokkify/q4j/commit/bb23657b406197e632f863187d8547969a516592))
* **deps:** update node to v24 ([#156](https://github.com/quokkify/q4j/issues/156)) ([da9ca2a](https://github.com/quokkify/q4j/commit/da9ca2ac767fcab2c4d8e41828db157604eb8e97))
* **deps:** update opensearchproject/opensearch docker tag to v3 ([#205](https://github.com/quokkify/q4j/issues/205)) ([52f36e8](https://github.com/quokkify/q4j/commit/52f36e826eb22668fa5855aee4acf3e643882b90))
* **deps:** update opensearchproject/opensearch docker tag to v3.7.0 ([#333](https://github.com/quokkify/q4j/issues/333)) ([19aa007](https://github.com/quokkify/q4j/commit/19aa0077d21d9ebe75e650475b8298595eb370f2))
* **deps:** update opensearchproject/opensearch docker tag to v3.8.0 ([#425](https://github.com/quokkify/q4j/issues/425)) ([653e207](https://github.com/quokkify/q4j/commit/653e20722d9f41ccf43461d6f93e10725aedab22))
* **deps:** update opensearchproject/opensearch:3.6.0 docker digest to b5dd151 ([#266](https://github.com/quokkify/q4j/issues/266)) ([cbc2004](https://github.com/quokkify/q4j/commit/cbc200417f3792e7df5f88f7d35a7853d2988529))
* **deps:** update plugin com.github.spotbugs to v6.4.4 ([#57](https://github.com/quokkify/q4j/issues/57)) ([eb8f311](https://github.com/quokkify/q4j/commit/eb8f3110c8612c2872852d1d89f85323ef6b4fd6))
* **deps:** update plugin com.github.spotbugs to v6.4.5 ([#70](https://github.com/quokkify/q4j/issues/70)) ([258b70d](https://github.com/quokkify/q4j/commit/258b70d073f1604dd31462b3d9d23de2cf180d1c))
* **deps:** update plugin com.github.spotbugs to v6.4.8 ([#85](https://github.com/quokkify/q4j/issues/85)) ([a6bee4f](https://github.com/quokkify/q4j/commit/a6bee4f7ff2c90f72f46ace8f1782d1611d6a9d8))
* **deps:** update plugin com.github.spotbugs to v6.5.10 ([#424](https://github.com/quokkify/q4j/issues/424)) ([441436c](https://github.com/quokkify/q4j/commit/441436ce9fc41d6244ab40fd46901a120befc5da))
* **deps:** update plugin com.github.spotbugs to v6.5.4 ([#186](https://github.com/quokkify/q4j/issues/186)) ([80be193](https://github.com/quokkify/q4j/commit/80be193f107184f3437eb8745e2af4b5d99e5a31))
* **deps:** update plugin com.github.spotbugs to v6.5.5 ([#257](https://github.com/quokkify/q4j/issues/257)) ([6cb18a0](https://github.com/quokkify/q4j/commit/6cb18a073266e02fa5a53c696edecd07753272eb))
* **deps:** update plugin com.github.spotbugs to v6.5.8 ([#342](https://github.com/quokkify/q4j/issues/342)) ([72d4c0e](https://github.com/quokkify/q4j/commit/72d4c0e448b2dbbc2d53bf6fc85c134258901a6d))
* **deps:** update redis docker tag to v8 ([#161](https://github.com/quokkify/q4j/issues/161)) ([217ac8f](https://github.com/quokkify/q4j/commit/217ac8f9a441ec17c71d1fa2024bb952b3172b31))
* **deps:** update redis docker tag to v8.10.0 ([#439](https://github.com/quokkify/q4j/issues/439)) ([b6397e9](https://github.com/quokkify/q4j/commit/b6397e950acce3755c0abba5403be145285d3927))
* **deps:** update redis docker tag to v8.6.3 ([#228](https://github.com/quokkify/q4j/issues/228)) ([fedd58b](https://github.com/quokkify/q4j/commit/fedd58b7c4f80edad81271009c9c32e09aea5847))
* **deps:** update redis docker tag to v8.8.0 ([#277](https://github.com/quokkify/q4j/issues/277)) ([9570f12](https://github.com/quokkify/q4j/commit/9570f123b0a0ed2a5fa3100fe191fa685374742a))
* **deps:** update redis docker tag to v8.8.1 ([#374](https://github.com/quokkify/q4j/issues/374)) ([f23486c](https://github.com/quokkify/q4j/commit/f23486c6a633b7ed7b96a826deb06fde3e272b92))
* **deps:** update redis:8.6.3 docker digest to 4d25e2f ([#255](https://github.com/quokkify/q4j/issues/255)) ([ceaaf34](https://github.com/quokkify/q4j/commit/ceaaf34abafd346be437a900eff9af9cbb09b061))
* **deps:** update redis:8.6.3 docker digest to 718b017 ([#238](https://github.com/quokkify/q4j/issues/238)) ([a216d9c](https://github.com/quokkify/q4j/commit/a216d9c4be1bc8ed784fd1d3f99a6cc20ee52504))
* **deps:** update reportportal/migrations docker tag to v5.15.1 ([#223](https://github.com/quokkify/q4j/issues/223)) ([47fe68d](https://github.com/quokkify/q4j/commit/47fe68d111ae938dc745aad1c250e9dcaf73eb63))
* **deps:** update reportportal/migrations docker tag to v5.15.2 ([#283](https://github.com/quokkify/q4j/issues/283)) ([07ac69b](https://github.com/quokkify/q4j/commit/07ac69bae8908cd14bbbb9bab9eaa224b6aad427))
* **deps:** update reportportal/migrations docker tag to v5.15.3 ([#426](https://github.com/quokkify/q4j/issues/426)) ([87850dd](https://github.com/quokkify/q4j/commit/87850dde9b796c3ab8b648b57e8f85c7f001b599))
* **deps:** update reportportal/service-api docker tag to v5.15.1 ([#224](https://github.com/quokkify/q4j/issues/224)) ([02ff287](https://github.com/quokkify/q4j/commit/02ff287712348d2e9d06ecaa5634a126e0865b41))
* **deps:** update reportportal/service-api docker tag to v5.15.2 ([#284](https://github.com/quokkify/q4j/issues/284)) ([eb61453](https://github.com/quokkify/q4j/commit/eb61453d0002f18be2ef4980820811735ae6cf73))
* **deps:** update reportportal/service-api docker tag to v5.15.3 ([#428](https://github.com/quokkify/q4j/issues/428)) ([f53bcf5](https://github.com/quokkify/q4j/commit/f53bcf5c0ddc2d31f57a23c056cbcd5e28eb97a7))
* **deps:** update reportportal/service-authorization docker tag to v5.15.1 ([#429](https://github.com/quokkify/q4j/issues/429)) ([be92c2c](https://github.com/quokkify/q4j/commit/be92c2cf5b4858d0a267746f9338d3ebd6fb0b77))
* **deps:** update reportportal/service-auto-analyzer docker tag to v5.15.2 ([#285](https://github.com/quokkify/q4j/issues/285)) ([cf5ad98](https://github.com/quokkify/q4j/commit/cf5ad98829534fda3db4c036e5c7560b3adb847e))
* **deps:** update reportportal/service-auto-analyzer docker tag to v5.15.3 ([#343](https://github.com/quokkify/q4j/issues/343)) ([ce6243f](https://github.com/quokkify/q4j/commit/ce6243f13277ccf5927325b76886a67f6d22e2ce))
* **deps:** update reportportal/service-auto-analyzer docker tag to v5.15.4 ([#375](https://github.com/quokkify/q4j/issues/375)) ([51f09e9](https://github.com/quokkify/q4j/commit/51f09e9ecd2e0365f119b1e183b29c1e11e47b38))
* **deps:** update reportportal/service-auto-analyzer docker tag to v5.15.5 ([#432](https://github.com/quokkify/q4j/issues/432)) ([7afda5a](https://github.com/quokkify/q4j/commit/7afda5a82d5d010cda3a225c8bd82778756c2f27))
* **deps:** update reportportal/service-index docker tag to v5.15.1 ([#430](https://github.com/quokkify/q4j/issues/430)) ([5bc155e](https://github.com/quokkify/q4j/commit/5bc155eed47d1be1acf059a8f6acf65724112c4c))
* **deps:** update reportportal/service-jobs docker tag to v5.15.1 ([#286](https://github.com/quokkify/q4j/issues/286)) ([0b50825](https://github.com/quokkify/q4j/commit/0b50825a1ec9e1216eb0c431e9af5353178b8bd6))
* **deps:** update reportportal/service-jobs docker tag to v5.15.2 ([#431](https://github.com/quokkify/q4j/issues/431)) ([a1473d3](https://github.com/quokkify/q4j/commit/a1473d30e40b08bb109e575e7a85bc8f631a7b12))
* **deps:** update reportportal/service-ui docker tag to v5.15.2 ([#225](https://github.com/quokkify/q4j/issues/225)) ([fa17dae](https://github.com/quokkify/q4j/commit/fa17dae12f41e309eb090cccfe9ffc0fbc90071d))
* **deps:** update reportportal/service-ui docker tag to v5.15.3 ([#287](https://github.com/quokkify/q4j/issues/287)) ([d213b44](https://github.com/quokkify/q4j/commit/d213b44c46c905125d338b0608bfb89be779e260))
* **deps:** update reportportal/service-ui docker tag to v5.15.4 ([#434](https://github.com/quokkify/q4j/issues/434)) ([98578ea](https://github.com/quokkify/q4j/commit/98578ea1420694bc1868c553519221f8be8312fc))
* **deps:** update tecnativa/docker-socket-proxy docker tag to v0.4.2 ([#142](https://github.com/quokkify/q4j/issues/142)) ([2ef1c6b](https://github.com/quokkify/q4j/commit/2ef1c6b23452ea73d0b9fd217202d09560fb1803))
* **deps:** update tecnativa/docker-socket-proxy docker tag to v0.5.0 ([#391](https://github.com/quokkify/q4j/issues/391)) ([801eea2](https://github.com/quokkify/q4j/commit/801eea220045a66194ae769a4f02677d5d2fdae6))
* **deps:** update traefik docker tag to v2.11.45 ([#226](https://github.com/quokkify/q4j/issues/226)) ([6322c65](https://github.com/quokkify/q4j/commit/6322c65b1083ed8d3c65587ddd23e06227f932f9))
* **deps:** update traefik docker tag to v2.11.46 ([#272](https://github.com/quokkify/q4j/issues/272)) ([6a73623](https://github.com/quokkify/q4j/commit/6a73623fdf9270b28ee14d7c2f1797088d40998b))
* **deps:** update traefik docker tag to v2.11.50 ([#327](https://github.com/quokkify/q4j/issues/327)) ([908c597](https://github.com/quokkify/q4j/commit/908c597650f9814351ea56374270cb546dad5dbb))
* **deps:** update traefik docker tag to v2.11.51 ([#334](https://github.com/quokkify/q4j/issues/334)) ([d4287ce](https://github.com/quokkify/q4j/commit/d4287ce164fccab9d37afba895e658764408e96b))
* **deps:** update traefik docker tag to v3 ([#337](https://github.com/quokkify/q4j/issues/337)) ([2a45891](https://github.com/quokkify/q4j/commit/2a45891f6fe03c5708a923cc18d03872f9a348de))
* **deps:** update traefik docker tag to v3.7.10 ([#394](https://github.com/quokkify/q4j/issues/394)) ([896b628](https://github.com/quokkify/q4j/commit/896b628f3bbb4575854b04a07dbd4d10d06f802e))
* **deps:** update traefik docker tag to v3.7.8 ([#370](https://github.com/quokkify/q4j/issues/370)) ([ab25bc0](https://github.com/quokkify/q4j/commit/ab25bc0190cecbf25ffe706550ab4a73d8a27889))
* **deps:** update traefik docker tag to v3.7.9 ([#376](https://github.com/quokkify/q4j/issues/376)) ([bb9f276](https://github.com/quokkify/q4j/commit/bb9f276a8700fb978dc86642b16c5a932dda74cf))
* **deps:** update zookeeper:3.9 docker digest to 0598e33 ([#233](https://github.com/quokkify/q4j/issues/233)) ([3d17a56](https://github.com/quokkify/q4j/commit/3d17a56ae90bfa0b4848aa3d1f3aaa8f6ef2797d))
* **deps:** update zookeeper:3.9 docker digest to 2408ba6 ([#55](https://github.com/quokkify/q4j/issues/55)) ([1c9d4d6](https://github.com/quokkify/q4j/commit/1c9d4d661656abe9275022afd3c29e1124648eb6))
* **deps:** update zookeeper:3.9 docker digest to 4c6f15f ([#339](https://github.com/quokkify/q4j/issues/339)) ([ec63977](https://github.com/quokkify/q4j/commit/ec639775e38b09c887efe337ba6d4a7addb50c75))
* **deps:** update zookeeper:3.9 docker digest to 861113a ([#245](https://github.com/quokkify/q4j/issues/245)) ([93622aa](https://github.com/quokkify/q4j/commit/93622aa267fee080fabc36989fcd0fd2e0832e7b))
* **deps:** update zookeeper:3.9 docker digest to 895cf2a ([#67](https://github.com/quokkify/q4j/issues/67)) ([5fe4197](https://github.com/quokkify/q4j/commit/5fe419760b550f35ba87235e72d5955232702013))
* **deps:** update zookeeper:3.9 docker digest to 98be0d0 ([#127](https://github.com/quokkify/q4j/issues/127)) ([5002354](https://github.com/quokkify/q4j/commit/500235420963e1e85ff2fbb9a5a6a2c36b1cb8f1))
* **deps:** update zookeeper:3.9 docker digest to b87f5ea ([#76](https://github.com/quokkify/q4j/issues/76)) ([da9e38a](https://github.com/quokkify/q4j/commit/da9e38a6a43a02d4545c761cabde6031c49ed871))
* **deps:** update zookeeper:3.9 docker digest to e0e03e7 ([#460](https://github.com/quokkify/q4j/issues/460)) ([97a0e28](https://github.com/quokkify/q4j/commit/97a0e2897bbe831ce83c5dfd5267c302ee81cb1b))
* **deps:** update zookeeper:3.9 docker digest to e12cace ([#267](https://github.com/quokkify/q4j/issues/267)) ([05dc80c](https://github.com/quokkify/q4j/commit/05dc80c61fab8d0e547fa7bbbfdbb949a3ae08d3))
* **deps:** update zookeeper:3.9 docker digest to f0f1d5e ([#158](https://github.com/quokkify/q4j/issues/158)) ([7410567](https://github.com/quokkify/q4j/commit/7410567c1e268ae9cd38e6fd1f68fffa0afaf6dc))
* **main:** release 0.2.0 ([#405](https://github.com/quokkify/q4j/issues/405)) ([374d61e](https://github.com/quokkify/q4j/commit/374d61e4c29d013a12ab6489d6a5ed522ef548d9))
* **main:** release 0.2.1 ([#413](https://github.com/quokkify/q4j/issues/413)) ([0d703b1](https://github.com/quokkify/q4j/commit/0d703b1f06c9e4d123f3d9d56c6e11e0c6e89d7c))
* **main:** release 0.2.2 ([#415](https://github.com/quokkify/q4j/issues/415)) ([3120611](https://github.com/quokkify/q4j/commit/3120611f3c5cbb3be6095aa6973e0eb4e25ba069))
* **main:** release 0.2.3 ([#418](https://github.com/quokkify/q4j/issues/418)) ([1fbfb30](https://github.com/quokkify/q4j/commit/1fbfb30d20333eaa98f22ba00dfe96c60bac1955))
* **main:** release 0.3.0 ([#467](https://github.com/quokkify/q4j/issues/467)) ([a5f3247](https://github.com/quokkify/q4j/commit/a5f324724eb0effb025638fa7605b3235a0af06d))
* **main:** release 1.0.0 ([#348](https://github.com/quokkify/q4j/issues/348)) ([096a9ed](https://github.com/quokkify/q4j/commit/096a9ed1e8fb5ffb30058a030256515b0b4334d0))
* **main:** release 1.0.1 ([#354](https://github.com/quokkify/q4j/issues/354)) ([0544696](https://github.com/quokkify/q4j/commit/054469607611802489cfc95e59648d4f24d67132))
* **main:** release 1.0.2 ([#359](https://github.com/quokkify/q4j/issues/359)) ([9490226](https://github.com/quokkify/q4j/commit/9490226587a529b75556825924b89122ee48927b))
* **main:** release 1.0.3 ([#364](https://github.com/quokkify/q4j/issues/364)) ([49b4d3f](https://github.com/quokkify/q4j/commit/49b4d3fb5d490ca3bb1f2b1ac499671e1712d2a5))
* **main:** release 1.0.4 ([#372](https://github.com/quokkify/q4j/issues/372)) ([695cfd6](https://github.com/quokkify/q4j/commit/695cfd6df5374d6e5c49f7de309b71c0fc2920f3))
* **main:** release 1.0.5 ([#383](https://github.com/quokkify/q4j/issues/383)) ([960d72f](https://github.com/quokkify/q4j/commit/960d72f2cbfe754deb431e1be6cb336d9d866a48))
* **main:** release 1.0.6 ([#387](https://github.com/quokkify/q4j/issues/387)) ([ed63d02](https://github.com/quokkify/q4j/commit/ed63d02893d73542dc4c2b5e1379aa3fa54d499e))
* **release:** restart versioning at 0.1.0 ([6bf9825](https://github.com/quokkify/q4j/commit/6bf9825d0b4caf56c335f8bd048ac6c45b252672))
* remove Centrifugo from Docker environment ([#235](https://github.com/quokkify/q4j/issues/235)) ([aef1afd](https://github.com/quokkify/q4j/commit/aef1afdba5b87d4e68ffba6e19d54ba2bf06eded))
* replace hidden Unicode chars (U+00A0, U+202F) with regular spaces ([77df53e](https://github.com/quokkify/q4j/commit/77df53e085e913115ec9d4f78e388e0ae1c2a537))
* switch renovate to shared gradle preset ([573a860](https://github.com/quokkify/q4j/commit/573a860c3b8ecd2e926133263f16735d5d58c390))
* **template:** update shared project template ([#404](https://github.com/quokkify/q4j/issues/404)) ([444b081](https://github.com/quokkify/q4j/commit/444b081640e86ad2d1c7009ab06b89539cbe2c55))
* **toolkit:** adopt project-toolkit v2.10.1 ([#398](https://github.com/quokkify/q4j/issues/398)) ([588d24e](https://github.com/quokkify/q4j/commit/588d24e9f271cef09e570d85b5fe8a1c47eb5459))

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
