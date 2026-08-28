# RUNBOOK — local infra & Selenide table tests

Recipes for bringing up the local `web` profile and running the Selenide table tests on a
developer machine (macOS/Apple Silicon tested).

## Prerequisites

- Docker Desktop running.
- JDK 21 (the Gradle toolchain requires it; auto-provisioning is disabled).
  - macOS with Homebrew: `brew install openjdk@21`.
  - Symlink so Gradle auto-detects it:
    ```bash
    ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk \
      ~/Library/Java/JavaVirtualMachines/openjdk-21.jdk
    ```
  - Verify with `/usr/libexec/java_home -V`.

## 1. Bring up the `web` profile

⚠️ **Unset `CI` first.** If `CI` leaks into the shell (it often does in dev setups), the infra
scripts switch to CI mode: they build `ci-nginx`, allocate random ports, and write
`NGINX_BASE_URL=http://nginx:80` (unreachable from a host JVM).

```bash
cd <repo>
env -u CI ./tools/environment/scripts/infra/run_app.sh web
```

This:
- starts `nginx` (host port `80`) + Selenium Grid (`selenium-hub` ports `4442-4444`);
- renders `tools/environment/assets/selenium-grid/config.toml` (substitutes `__NETWORK__`);
- waits for grid readiness and writes `.nginx.env` / `.selenium-grid.env` (gitignored, see F8).

Check readiness:

```bash
curl -s http://localhost:4444/wd/hub/status   # expect "ready": true with a chrome slot
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:80/table/
```

If the grid shows `Selenium Grid not ready` with the node log
`Make sure that a driver is available on $PATH`, the node container was created with a truncated
`config.toml` (the rendering bug fixed in this branch) — re-create the node so it loads the fresh
config:

```bash
docker compose -f tools/environment/docker/docker-compose.yml \
  -f tools/environment/docker/docker-compose.local.yml \
  up -d --force-recreate selenium-node-docker selenium-hub
```

To stop: `env -u CI ./tools/environment/scripts/infra/stop_app.sh web`.

## 2. Run the table tests

The browser runs **inside the Selenium node container**, so the app URL must be reachable from
that container. `http://host.docker.internal:80` works (the node adds
`host.docker.internal → host-gateway`); `localhost` does **not** (it resolves to the browser
container itself).

```bash
CI= \
BASE_URL=http://host.docker.internal:80 \
NGINX_BASE_URL=http://host.docker.internal:80 \
BROWSER_REMOTE_URL=http://localhost:4444/wd/hub \
./gradlew :integrations:selenide:test \
  --tests 'dev.quokkify.test.UiTableTest' \
  --tests 'dev.quokkify.test.TableRowWaitTest' \
  --tests 'dev.quokkify.test.UiHorizontalTableTest' \
  --tests 'dev.quokkify.test.TableModelContractTest' \
  --tests 'dev.quokkify.test.TableQueryContractTest' \
  --tests 'dev.quokkify.test.TableAssertionsActionsContractTest' \
  --no-daemon --console=plain
```

- `BASE_URL` reaches the fixture app (nginx `/table/`, `/table-model-contract/`,
  `/horizontal_table/`).
- `BROWSER_REMOTE_URL` = the Selenium Grid (`http://localhost:4444/wd/hub` in local mode).
- `NGINX_BASE_URL` is read directly by the neutral-model contract tests.

### Timing-sensitive flakiness

`TableRowWaitTest` and `TableQueryContractTest` contain sub-second / short-timeout assertions
that flake under load and under amd64-on-arm64 emulation (fail in a full run, pass in isolation).
Re-run the failing test alone to distinguish a real failure from contention:

```bash
# repeat the gradle command, but with a single --tests filter for the failing method
```

## Known gaps / follow-ups

- Selenium hub/node images are **amd64-only** → Rosetta emulation on Apple Silicon (slow,
  timing-flaky). Multi-arch images would help.
- Local runs dirty the tree (`config.toml` rendered in place, `assets/selenium-grid/assets/**`
  session artifacts) — see `integrations/selenide/AUDIT.md` F8.