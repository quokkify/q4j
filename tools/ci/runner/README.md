# Self-hosted runner (Docker + DinD)

This runner uses Docker-in-Docker to avoid exposing the host Docker socket.

## Setup

1. Copy `.env.example` to `.env` and fill values.
2. Get a registration token (valid for 1 hour):
   ```bash
   gh api -X POST repos/ylazakovich/quokkify/actions/runners/registration-token -q .token
   ```
3. Start runners (3 in parallel by default):
   ```bash
   ./tools/ci/runner/start_runner.sh
   ```
   Optional: set a custom count.
   ```bash
   RUNNER_COUNT=5 ./tools/ci/runner/start_runner.sh
   ```

## Stop

```bash
./tools/ci/runner/stop_runner.sh
```

Notes:

- Runner labels: `self-hosted, linux, compose, internal`
- Runner is ephemeral: it unregisters after each job run.
- `start_runner.sh` uses `RUNNER_COUNT` (default `3`) and scales the `runner` service.
