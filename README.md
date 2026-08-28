# Q4J

Generated with `quokkify/project-toolkit` at `v2.14.0`. Run `copier update` to apply future template changes; Renovate updates workflow version references independently.

## Allure reports

The project-owned `Run tests` workflow at `.github/workflows/test.yml` must upload 24-50 dedicated Allure result artifacts whose names start with `allure-results-`. Keep JUnit, logs, and other build outputs outside that prefix. After validation, a read-only job streams and extracts selected source-run ZIPs under `runner.temp`, checks identity, count, central-directory, collision, and expanded-resource limits before extraction, copies only fully validated regular files into a symlink-checked workspace directory, and generates the Allure 3 HTML report. A separate job with only pull-request comment permission publishes a static trusted link to the HTML artifact. A freshness-checked Pages-only job applies the same bounded extraction gate and publishes the report below `allure/pr-N` at `https://quokkify.github.io/q4j`; fork and Dependabot PRs are excluded from Pages.
