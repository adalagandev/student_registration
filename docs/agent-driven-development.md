# Agent-Driven Development: Auto-Delegation & Authorship Stamps

How to make your custom subagents (`service-warden`, `exception-warden`,
`test-warden`) **write the code in their domain**, **run automatically when you
prompt for a feature**, and **stamp their name** on what they author.

> **TL;DR** — There is no single "always route X to agent Y" switch in Claude Code.
> You get there by combining two levers you *do* control: each agent's `description`
> frontmatter (drives auto-delegation) and a written policy in `CLAUDE.md` (makes it
> expected, not occasional). The name stamp must be *instructed*, not hooked. None of
> this is 100% deterministic — delegation is model-driven and heuristic.

---

## 1. Making agents auto-run on a feature prompt

The only built-in auto-delegation mechanism is the **`description` field** in each
`.claude/agents/*.md` frontmatter. Claude's main loop reads those descriptions and
delegates based on them. Trigger phrases like `MUST BE USED` and `use PROACTIVELY`
bias it strongly — but nothing guarantees delegation on every prompt.

Two caveats:

- **Reviewer vs. author.** The current agent descriptions are written for *reviewing*
  ("MUST BE USED before merging…"). To make an agent the **author**, its description
  must say it *writes / implements* in its domain, not just reviews after the fact.
- **Session-specific restriction.** Some harness configurations restrict the main
  agent from spawning subagents unless explicitly asked. That is a harness rule, **not**
  something `CLAUDE.md` overrides. In a normal Claude Code CLI session, description-driven
  delegation is active and these settings take effect.

## 2. Yes — modify `CLAUDE.md` (your most reliable lever)

`CLAUDE.md` is loaded every session and its instructions override default behavior, so
a written **delegation policy** is what turns "sometimes delegates" into "expected to
delegate." Use it *together with* the agent descriptions. Add:

```markdown
## Agent-driven development

Feature/change work is delegated to the specialist subagent that owns the
domain — that agent WRITES the code, the main agent only coordinates:

- Service / business-logic classes (service/, use-cases) → service-warden
- API error/exception handling, status mapping, validation → exception-warden
- Unit/integration tests → test-warden

A feature spanning domains is split so each agent authors its own layer.

### Authorship stamp
Every class or function an agent authors or substantially rewrites carries a
tag on the line directly above its declaration, e.g.:

    // @agent: service-warden
    public class OrderService { ... }

Keep the tag on edits; change it only when a different agent takes the code over.
```

## 3. The name stamp

**Hooks cannot do this.** A hook runs a shell command on an event; it can't know which
agent authored a function, and it can't force delegation either. The stamp must be
**instructed**, in two places so it reflects the *actual* author:

1. The **`CLAUDE.md` convention** above (visible to everyone), and
2. A line in **each agent's body** (`.claude/agents/<name>.md`), for example:

   > *When you write or refactor code, stamp each class/function you author with
   > `// @agent: <your-name>` on the line directly above its declaration, per the
   > CLAUDE.md authorship convention.*

A hook is still useful as a **guardrail** — e.g. a `PostToolUse` check that warns if a
new class in `service/` lacks a stamp — but it can't be the source of truth.

---

## What to change, at a glance

| Goal | Where | What |
|---|---|---|
| Agents author their domain | `.claude/agents/*.md` frontmatter `description` | Reword from "reviews" to "writes/implements/reviews"; keep `MUST BE USED` / `use PROACTIVELY` |
| Delegation happens automatically | `CLAUDE.md` | Add the **Agent-driven development** policy section (domain → agent map) |
| Name stamped on code | `CLAUDE.md` + each agent body | Add the **Authorship stamp** convention + a per-agent instruction |
| (Optional) enforce the stamp | `.claude/settings.json` hook | `PostToolUse` check that warns when a new class/function lacks `// @agent:` |

## Honest bottom line

- ✅ `CLAUDE.md` policy + agent `description` rewrite → agents author their domains and
  auto-delegate **most of the time**.
- ✅ `CLAUDE.md` convention + per-agent instruction → name stamps.
- ⚠️ **Not 100% deterministic** — delegation is heuristic. If it ever misses, a one-line
  reminder ("have service-warden do it") re-routes it.

## Domain → agent reference

| Agent | Owns | Example prompts that should route to it |
|---|---|---|
| `service-warden` | Service / business-logic classes, use-cases, DI, DTO/domain mapping, transactions | "Add an OrderService", "implement the refund flow", "split up UserService" |
| `persistence-warden` | Persistence / data-access layer: ORM entities, repositories/DAOs, query methods, schema/migration mechanics | "Add a nickname column to Student", "list a student's documents newest-first", "make Document a real @ManyToOne" |
| `controller-warden` | API controllers / routers / endpoint handlers: routes, status codes, request/response DTOs, endpoint validation & security | "Add a StudentController", "new endpoint for registrations", "this endpoint returns 200 with an error body" |
| `exception-warden` | API error/exception handling, status-code mapping, validation errors, exception middleware | "Add error handling to POST /orders", "why does this return 200 with an error body?" |
| `frontend-warden` | React frontend (`frontend/src/`): components, immutable state, the `api.js` seam, controlled inputs, CSS-token theming | "Add a phone column to the student table", "wire up a delete-student button", "re-theme to light mode" |
| `test-warden` | Unit/integration tests, flaky/slow test diagnosis, test refactors, regression tests for bug fixes | "Add tests for OrderService", "cover the refund flow", "add a regression test" |
