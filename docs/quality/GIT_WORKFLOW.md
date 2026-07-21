# Tavall Studios Code Review, Git Hygiene, and Engineering Issue Workflow

> **Status:** Active  
> **Applies to:** Contributors, maintainers, repository owners, automation, and AI-assisted development  
> **Purpose:** Protect production, preserve professional traceability, and keep engineering decisions understandable after their original context fades.

## 1. Applicability and precedence

This document is the shared Tavall Studios repository workflow.

Repository-specific files such as `AGENTS.md`, `CONTRIBUTING.md`, release procedures, synchronization contracts, and deployment runbooks may impose stricter requirements. Stricter repository-specific rules take precedence when they protect public source-of-truth history, automated synchronization, release integrity, security, or production safety.

A repository-specific supplement may not silently remove accountable review, truthful validation reporting, or production traceability. Any intentional exception must be explicit and attributable to an authorized maintainer or repository owner.

## 2. Core policy

GitHub is the authoritative record for repository review.

Ordinary contributor changes may not enter `main` without accountable review recorded on GitHub.

Accountable pull-request review uses one of two paths:

1. **Independent review**
   - The current change is presented through a GitHub pull request.
   - At least one qualified human other than the author approves the current diff.
   - Reviewable changes after approval require renewed approval.
2. **Owner self-promotion**
   - This path is limited to an authorized repository owner when they authored the pull request and no separate qualified reviewer is available.
   - The owner reviews the complete current diff and posts an **Owner Self-Review** record on the pull request.
   - The record names scope, validation, untested paths, risks, rollback, and the merge decision.
   - Reviewable changes after the record require a renewed Owner Self-Review.

Pull-request promotion requires configured checks to pass unless an authorized owner explicitly accepts an override, blocking conversations to be resolved, requested changes to be cleared, and the change to reach `main` through the GitHub merge flow.

GitHub does not allow an author to approve their own pull request. Owner self-promotion therefore uses a visible review record and, when required, repository-owner or ruleset-bypass merge control instead of fictional self-approval.

Automated reviews, AI reviews, status checks, comments, or reactions support review but satisfy neither independent human approval nor Owner Self-Review.

Authorized repository owners retain direct commit, push, force-push, check-bypass, and merge-bypass authority where repository-specific synchronization or protection rules do not impose a stricter path.

When an owner changes `main` directly:

- the pushed change should be surfaced for human review on GitHub by configured review automation when available;
- review may occur after the push but before the corresponding production deployment;
- the owner may explicitly override review when they accept responsibility for the change;
- the change must be reconciled into affected active staging or synchronization branches;
- direct owner authority does not extend to ordinary contributors or automation merely because those actors have write access.

A commit reaching `main` does not automatically mean that a production service has been updated.

### Operational flows

Ordinary contributor work:

```text
working/* -> staging/* -> main pull request
-> independent human review
-> manual production deployment
```

Owner-authored pull-request work:

```text
working/* -> staging/* -> main pull request
-> Owner Self-Review or independent human review
-> manual production deployment
```

Trusted owner direct work, when repository-specific rules permit it:

```text
owner push to main
-> GitHub human review of the pushed change
-> manual production deployment
```

An authorized owner may explicitly override review when necessary. Deployment remains a separate decision.

## 3. Branch model

The canonical branch flow is:

```text
working/* -> staging/* -> main
```

A repository-specific contribution or synchronization workflow may substitute personal-fork branches, `sync/**`, `upstream/**`, or another documented integration path while preserving the same review and production boundaries.

`main` is the authoritative production source. Deployment from `main` is manual unless another production document explicitly changes that behavior.

### 3.1 Working branches

Ordinary changes begin on a focused branch such as:

```text
working/<short-description>
```

Examples:

```text
working/runtime-audit-recovery
working/docs-quality-foundation
working/release-validation
```

A working branch should contain one coherent feature, fix, refactor, documentation change, or investigation outcome. Split unrelated systems into separate branches.

Working-branch pull requests normally target the appropriate `staging/*` branch. Use draft pull requests while implementation, validation, or direction is incomplete.

An authorized repository owner may work directly on `main` when another branch provides no meaningful review, integration, synchronization, or safety benefit and repository-specific rules permit it.

### 3.2 Staging branches

Staging branches collect a reviewable release or integration scope:

```text
staging/<release-or-scope>
```

A staging branch must remain coherent enough to validate as one integrated change. It is not a permanent junk drawer for every branch that happened to compile on a Tuesday.

Pull requests from `staging/*` into `main` are production-promotion pull requests and receive the full production review described here.

### 3.3 `main`

`main` is production and must normally:

- reject direct pushes from ordinary contributors;
- require pull requests for ordinary contributor changes;
- require independent approval or a current Owner Self-Review for pull-request changes;
- dismiss stale approvals after reviewable changes;
- require renewed accountable review after the latest reviewable push;
- require blocking conversations to be resolved;
- require configured checks unless an authorized owner records an override;
- block deletion and force pushes by ordinary contributors;
- restrict merges to trusted maintainers;
- preserve explicit owner bypass authority where stricter repository-specific rules do not prohibit it.

Direct owner changes should be surfaced for human review before the corresponding production deployment unless the owner explicitly overrides review.

### 3.4 Repository owner authority

Repository owner authority is determined by repository or organization access controls and is not enumerated in this public workflow.

When GitHub requires an explicit account or team for mechanical ownership, that assignment belongs in `.github/CODEOWNERS`, organization roles, repository permissions, ruleset bypass configuration, or an access-controlled operational record.

An authorized repository owner may, subject to stricter repository-specific synchronization rules:

- use Owner Self-Review for an authored pull request;
- merge an owner-authored pull request through an explicit ruleset bypass;
- commit or push directly to `main`;
- force-push shared branches when recovery or history repair genuinely requires it;
- bypass a failed or incomplete check while accepting the resulting risk;
- alter release scope;
- authorize production deployment.

Before rewriting shared history, an owner should, when practical, preserve the previous branch state, inspect the rewritten range, identify affected pull requests and branches, communicate the rewrite, and verify that contributors and automation can recover cleanly.

Owner authority exists to preserve operational control, not to force every owner action through ceremony that contributes no safety.

### 3.5 Hotfixes

A production hotfix should:

1. start from the current `main` state;
2. contain only the minimum safe correction;
3. use a pull request when that path remains useful and safe;
4. receive independent approval or Owner Self-Review when using a pull request;
5. run the checks available during the incident;
6. be reconciled into every affected active staging or synchronization branch;
7. link an incident, blocker, or follow-up issue when work remains.

An authorized repository owner may apply the correction directly to `main` where repository-specific rules permit it. Urgency changes the size of the process, not whether engineering judgment exists.

## 4. Code review flow

### 4.1 Before implementation

- Read `AGENTS.md`, this workflow, `CODE_ARCHITECTURE.md`, and relevant quality, design, progression, and operational documents.
- Search open and closed issues for blockers, accepted directions, rejected approaches, and investigations.
- Inspect the existing implementation before proposing new classes or systems.
- Identify owning modules, expected tests, migration risk, failure behavior, and rollback.
- Create or link an issue when the work crosses the issue threshold in Section 7.

### 4.2 During implementation

- Keep the change focused.
- Commit at meaningful working checkpoints.
- Avoid unrelated cleanup.
- Update linked issues when evidence changes the direction.
- Keep tests and documentation with the behavior they explain.
- Treat generated code as untrusted until its APIs and behavior are verified.
- Extend established systems instead of creating parallel implementations.

### 4.3 Before requesting review

The author must:

- review the complete diff;
- remove temporary logging, debug code, dead code, and accidental generated files;
- run relevant automated checks;
- perform required bot or manual verification;
- document untested paths honestly;
- synchronize with the target branch and resolve obvious conflicts;
- update documentation and progression evidence when behavior changed;
- link issues accurately with `Closes #...`, `Fixes #...`, or `Refs #...`.

Use `Closes` or `Fixes` only when the pull request fully resolves the issue. Use `Refs` when the work contributes without completing it.

### 4.4 Pull request state

Keep a pull request in **Draft** while implementation, validation, or its description is incomplete.

Mark it ready only when scope is complete, the author reviewed the diff, validation is recorded, risks and gaps are disclosed, documentation is current, and the change is reasonably reviewable as one coherent boundary.

Large coherent systems are allowed. Do not split one system into artificial fragments merely to satisfy an arbitrary line-count preference.

### 4.5 Accountable review

Reviewers evaluate:

- correctness against requested behavior;
- compatibility with `CODE_ARCHITECTURE.md` and relevant quality documents;
- system ownership and duplicate-system risk;
- failure and recovery behavior;
- persistence and migration safety;
- concurrency and lifecycle behavior;
- permission and security boundaries;
- performance and operational impact;
- logging and audit behavior;
- test quality and missing validation;
- naming and maintainability;
- unrelated work hidden in the diff;
- fabricated APIs or shallow assumptions in generated code.

Independent approval means the reviewer accepts responsibility for the reviewed state. It is not a ceremonial green button awarded because the diff projected confidence.

Owner Self-Review uses this pull-request comment:

```text
Owner Self-Review

Scope:
- What is being promoted.

Validation:
- Checks, tests, harnesses, and manual verification actually completed.

Untested:
- Known validation not performed.

Risks:
- Material production, data, security, migration, or operational risks.

Rollback:
- Exact revert or recovery approach.

Decision:
- Ready for merge on the current reviewed pull-request state.
```

GitHub records the pull-request head and change history. Contributors are not required to manually locate and paste a commit SHA into the review record. A later reviewable change invalidates the Owner Self-Review.

### 4.6 Changes after review

Review is required again when new commits alter executable or operational behavior, conflict resolution changes the diff, a rebase introduces meaningful changes, generated files or dependency locks change, migration or deployment instructions change, scope expands, or automated fixes modify the implementation.

Approval applies to the current reviewable state, not an older and more emotionally convenient diff.

### 4.7 Automated review and `AGENTS.md`

Codex automatic GitHub review is the advisory automated reviewer. It should run when a pull request is opened, updated with reviewable changes, or moved from draft to ready.

Codex repository access is controlled by the installed integration. This workflow does not require reducing its repository permissions.

When reviewing, Codex:

- follows repository guidance in `AGENTS.md`;
- reviews against this workflow, `CODE_ARCHITECTURE.md`, and linked quality and system documents;
- inspects existing implementations and linked issues before proposing replacements;
- prioritizes correctness, production safety, and architectural consistency over style preferences;
- does not satisfy independent human approval or Owner Self-Review.

When Codex authors or pushes a correction, that correction becomes a new reviewable change and invalidates stale human or owner review.

Repository-level `AGENTS.md` should link this workflow, `CODE_ARCHITECTURE.md`, relevant quality documents, and owning system documents. Its review guidance should direct Codex to flag meaningful risks involving runtime failures, lifecycle and concurrency defects, unsafe platform APIs, dependency incompatibility, persistence and cache consistency, permissions and trust boundaries, cross-service state, resource leaks, hot paths, duplicated systems, unsafe defaults, missing tests, fabricated APIs, and incomplete rollback or logging.

Each meaningful automated finding should include severity, affected code, a realistic failure scenario, why it matters, and a concrete correction. Speculative formatting preferences are not defects unless they create a real correctness, compatibility, or maintainability problem.

### 4.8 Merge guidance

- Use squash merge for working-to-staging pull requests when intermediate history has no long-term value.
- Preserve meaningful working commits when their history is intentional.
- Use a merge commit for staging-to-main promotion when preserving the release boundary is useful.
- Delete merged working branches unless intentionally retained.
- Do not rewrite shared branch history without coordination.
- Repository-specific synchronization workflows take precedence for mirrored public modules.
- Authorized owners retain final merge-method discretion.

## 5. Git hygiene

### 5.1 Atomic commits

Each commit represents one understandable change. It should contain related code, tests, and documentation, remain reviewable within its coherent boundary, leave the branch usable or document an intentional intermediate state, and be revertible without unrelated work.

Avoid mixing formatting with behavior changes, unrelated refactors with features, dependency upgrades with unrelated work, separate fixes without a shared boundary, or generated output without its source or explanation.

Atomic does not mean artificially tiny. A commit may be large when one coherent system boundary genuinely requires it.

### 5.2 Commit messages

Every commit uses one or more typed subject lines followed by the structured body:

```text
Type: Capitalized concise action

Reason:
- Why the change is needed.

Changes:
- What changed.

Validation:
- What was run or inspected.
```

Allowed subject types:

- `Build`
- `Added`
- `Changed`
- `Removed`
- `Fixed`
- `Refactor`
- `Clean`
- `Test`
- `Docs`
- `License`
- `TODO`
- `Misc`

`Meta` is not an allowed type.

Multiple typed subject lines may appear when every line describes the same coherent boundary.

Subject rules:

- Use `Type: Action`, not Conventional Commit syntax.
- Capitalize the first word after the colon.
- Describe the result, not the act of editing files.
- Avoid vague subjects such as `Update`, `Changes`, `Stuff`, `Fix`, `Final`, or `WIP`.
- Use `Refactor`, not the legacy misspelling `Refractor`, for new commits.
- State validation truthfully. Use `Not run: <reason>` when validation was not performed.

### 5.3 Working tree discipline

Before committing, inspect `git status` and the staged diff, stage only intended files, verify the branch, remove temporary artifacts, confirm environment files are ignored, check line-ending churn, and verify that no secrets or sensitive data are included.

Never commit API keys, tokens, passwords, private keys, production credentials, unredacted user data, sensitive database dumps, or local-only environment state.

If a secret reaches Git history, rotate and invalidate it. Deleting the visible line is not remediation. It is putting a blanket over a fire.

### 5.4 Synchronization and history

Update local knowledge of target branches before significant work. Use fast-forward pulls where possible. Rebase personal working branches when it improves clarity. Never rewrite a branch other contributors or synchronization automation use without coordination.

Use `--force-with-lease` when force-pushing a personal branch. Force pushes to shared staging, synchronization, or production branches follow repository-specific rules and owner authority.

### 5.5 Scope control

Do not make unrelated drive-by changes. Leave unrelated work unchanged, record it in an issue when it meets the threshold, and handle it separately. A small cleanup may remain only when it directly supports the current change and does not obscure review.

### 5.6 AI-assisted work

The human contributor remains responsible for every committed line regardless of who or what typed it.

Provide relevant repository documents and issues, require repository discovery, verify referenced APIs and dependencies, run tests rather than trusting generated claims, inspect the complete diff, disclose uncertainty, treat AI-authored fixes as new reviewable changes, and preserve established architecture and ownership.

## 6. Production promotion and deployment

A staging-to-main pull request must explain release scope, included issues and working pull requests, important behavior changes, migrations and configuration, automated and manual evidence, risks and untested paths, rollback, and post-deployment verification.

Before merging:

- [ ] The target and source branches are correct.
- [ ] The current diff has independent approval or a valid Owner Self-Review.
- [ ] Stale review was renewed.
- [ ] Checks pass or an authorized owner override is documented.
- [ ] Blocking conversations are resolved.
- [ ] Issues and included pull requests are linked.
- [ ] Migration, configuration, rollback, and post-deployment steps are documented.
- [ ] Automated output is not being treated as independent review.
- [ ] Deployment remains a separate authorized action.

### 6.1 Deployment remains separate

Merging into `main` does not automatically authorize or perform deployment.

The production source may be identified through a pull request, merge record, release, tag, build metadata, artifact metadata, or deployment tooling. Contributors are not expected to manually hunt through Git history for a commit SHA when GitHub or the build system already records the source revision.

Before deploying:

- [ ] The production source and artifact are identified.
- [ ] Review and validation status are known.
- [ ] Untested paths are documented.
- [ ] Configuration, secrets, and migrations are ready.
- [ ] Backups, rollback artifacts, or recovery procedures exist where appropriate.
- [ ] The deployment owner and affected services are known.
- [ ] Post-deployment checks and monitoring are defined.
- [ ] Active staging or synchronization branches include direct production corrections that must not be overwritten later.

After deployment, verify service health, migrations, expected behavior, warnings and errors, and dependent systems. Record the deployed source and begin rollback when acceptance checks fail.

## 7. GitHub Issues as engineering context

Issues are a durable engineering record, not merely a task list.

Create or update an issue when a challenge blocks progress, may change architecture, exposes a limitation, requires investigation, affects multiple systems, creates production or security risk, records a temporary compromise, has important tradeoffs, or cannot be responsibly explained only in a commit or pull request.

Do not create issues for every tiny task, routine formatting change, temporary note, or trivial correction fully explained by one small pull request.

Direction-setting issues should document summary, current behavior, why it matters, evidence, constraints, options, current direction, acceptance criteria, related work, and AI implementation context.

Record rejected approaches, link implementation work, and close issues only when acceptance criteria are satisfied.

## 8. Author checklist

- [ ] Branch and target are correct.
- [ ] Complete diff was self-reviewed.
- [ ] Scope is focused.
- [ ] Relevant issues, architecture, and quality documents were reviewed.
- [ ] Tests and checks were actually run and recorded.
- [ ] Manual or bot verification is documented where required.
- [ ] Gaps, configuration, migration, rollback, and recovery are documented.
- [ ] No secrets or accidental artifacts are included.
- [ ] Generated code and APIs were verified.
- [ ] The pull request explains what changed and why.
- [ ] Codex review completed when available and meaningful findings were addressed or explained.

## 9. Reviewer checklist

- [ ] Review is independent, or the author is an authorized owner using valid Owner Self-Review.
- [ ] Requested behavior is implemented correctly.
- [ ] Architecture and repository-specific rules are respected.
- [ ] No duplicate or parallel system was introduced.
- [ ] Failure, recovery, data, security, permission, lifecycle, performance, and operational behavior were considered.
- [ ] Tests are meaningful and passing.
- [ ] Documentation and migration notes are sufficient.
- [ ] Issue decisions were followed or intentionally amended.
- [ ] Codex findings and blocking conversations were inspected.
- [ ] Review applies to the current diff.
- [ ] Deployment remains separate from merge approval.

## 10. Recommended repository configuration

### `working/*`

- Allow contributor pushes and draft pull requests.
- Run relevant checks.
- Allow `--force-with-lease` on personal branches.
- Delete after merge unless intentionally retained.

### `staging/*`

- Require pull requests from ordinary working branches.
- Allow trusted owner integration where repository-specific rules permit it.
- Run configured checks and automatic Codex review.
- Block deletion while active and discourage history rewrites.

### `main`

- Require pull requests for ordinary contributors.
- Require independent approval or Owner Self-Review for pull-request promotion.
- While there is only one qualified maintainer, set required approving reviews to zero or grant that owner an explicit ruleset bypass.
- Do not require CODEOWNERS approval until another qualified code owner can approve owner-authored pull requests.
- Dismiss stale review, require conversation resolution, and run configured checks.
- Block deletion and force pushes by ordinary actors.
- Preserve explicitly authorized owner bypass authority where repository-specific synchronization rules permit it.
- Surface direct owner pushes for review through repository automation when available.
- Use CODEOWNERS to identify responsibility without creating an impossible self-approval requirement.

### Codex and `AGENTS.md`

- Enable automatic Codex pull-request review.
- Link this workflow, `CODE_ARCHITECTURE.md`, and relevant quality and system documents from `AGENTS.md`.
- Keep repository-specific review priorities in `AGENTS.md`.
- Treat Codex-authored fixes as new reviewable changes.
- Do not treat automated review as independent approval.

## 11. Adoption and exceptions

Changes to this workflow should be introduced through a documentation pull request and reviewed before enforcement rules change, unless an authorized owner intentionally applies the organization-wide canonical policy directly.

Any exception must be explicit, narrow, attributable to an authorized maintainer or owner, and reviewable on GitHub when practical.

Automation cannot grant itself an exception or convert automated output into independent human acceptance.

An undocumented contributor exception is not flexibility. It is policy decay wearing business casual.
