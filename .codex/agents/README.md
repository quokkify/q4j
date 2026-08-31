# Q4J Gradle agent pack

This repository uses a small, repository-local Markdown contract for specialist agents. It is intentionally independent of user-level Hermes/Codex configuration and is safe to load in any harness that can enumerate `.codex/agents/*.md` files.

Each role file has YAML front matter (`name`, `description`, `mode`) followed by the same sections: Trigger, Input handoff, Allowed actions, Prohibited actions, Investigation, Verification, Stop/escalate, Evidence handoff, Secret hygiene, and Definition of done. `mode: write` is used only by the maintainer; `mode: read-only` is mandatory for audit and review roles.

## Roles

- `gradle-maintainer.md` — implements narrowly scoped Gradle changes.
- `gradle-dependency-auditor.md` — audits catalogs, aliases, constraints, and migration completeness without editing.
- `gradle-reviewer-ci-triage.md` — performs exact-head, read-only review and separates code failures from unavailable services.
- `gradle-policy.md` — shared policy referenced by every role; keep common guardrails here rather than copying them into role prompts.

## Loading and invocation

The calling harness should load `gradle-policy.md` first, then exactly one role file, then provide the self-contained handoff packet described by that role. Agents must work from the current checkout and must not infer module paths or commands that are not present in the repository. A coordinator, not an implementer or reviewer, owns commits, pushes, merges, and releases.

For a new task, choose the maintainer for build/configuration edits, the auditor for a migration audit, and the reviewer for a read-only review or CI failure triage. For a change requiring both implementation and review, run separate invocations and pass the implementation evidence to the reviewer; never combine those responsibilities.

## Validation

Run the repository-local parser/contract smoke test:

```bash
python3 tools/gradle-agent-pack/smoke.py
```

It validates front matter, required sections, role boundaries, referenced repository files, and two bounded scenarios. It does not modify PR #570, the current checkout, or external services.
