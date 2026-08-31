# Q4J Gradle agent pack

This repository-local pack uses the project-scoped Codex custom-agent format: one standalone TOML file per role in `.codex/agents/`. Each role declares `name`, `description`, and `developer_instructions`; audit and review roles additionally declare `sandbox_mode = "read-only"`.

## Roles

- `gradle-maintainer.toml` — narrowly scoped Gradle implementation.
- `gradle-dependency-auditor.toml` — read-only catalog and migration audit.
- `gradle-reviewer-ci-triage.toml` — read-only exact-head review and CI triage.
- `gradle-policy.md` — shared repository policy loaded by the coordinator alongside the selected role.

The coordinator loads the policy and exactly one role, then supplies a self-contained handoff. Implementers and reviewers are separate invocations. The coordinator owns commits, pushes, merges, and releases.

## Validation

Run `python3 tools/gradle-agent-pack/smoke.py`. It creates disposable fixtures, discovers and parses all three TOML roles, invokes a bounded local harness twice, and evaluates role-specific structured handoffs, permissions, and the historical PR #570 evidence without modifying the checkout.
