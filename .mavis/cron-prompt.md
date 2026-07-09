# Voice-Driven-VCSM — 12h Cron Prompt

## Repo
- Fork: `tmdeveloper007/Voice-Driven-Virtual-Customer-Success-Manager`
- Upstream: `ArpitaVerma16/Voice-Driven-Virtual-Customer-Success-Manager`
- Token: `${GITHUB_TOKEN}` — use from session env

## Context
- Spring Boot 3.2.0, Java 339 source files, 46 test files
- Topics: `gssoc-2026`, `java`, `springboot`
- Labels: `gssoc:approved` (added by maintainer after merge — do NOT wait for it)
- **sanrishi already has 29 open PRs** in this repo — check files changed to avoid conflicts
- Issues: ~94 open, mostly unlabeled. Check the API for issue list each run.
- `gssoc:approved` label on merged PRs is the scoring signal (233 already merged with it)
- **No Java in sandbox** — cannot compile or run tests. Rely on regex-based targeted fixes.

## Pre-flight
1. `cd /workspace/Voice-Driven-VCSM`
2. `git remote -v` — confirm origin = fork, upstream = ArpitaVerma16
3. `git checkout main && git fetch upstream && git reset --hard upstream/main && git push origin main --force` (if behind >50 commits)
4. Fetch open PR list: `GET /repos/ArpitaVerma16/Voice-Driven-Virtual-Customer-Success-Manager/pulls?state=open`
5. Record files already targeted by open PRs (especially sanrishi's 29 PRs)
6. Set git identity: `git config user.email "mavis@agent.local" && git config user.name "Mavis Auto-PR"`
7. Embed token in origin remote: `git remote set-url origin "https://${GITHUB_TOKEN}@github.com/tmdeveloper007/Voice-Driven-Virtual-Customer-Success-Manager.git"`

## Issue Selection
1. Fetch open issues: `GET /repos/ArpitaVerma16/Voice-Driven-Virtual-Customer-Success-Manager/issues?state=open&per_page=100`
2. Filter: skip issues already targeted by open PRs (check PR body/head ref)
3. Skip issues with `invalid`, `wontfix`, `duplicate` labels
4. Pick issues that can be addressed with targeted Java code changes (see Fix Types below)
5. Limit: 5 new PRs per run (hard cap)

## Fix Types (proven patterns from upstream)
Apply ONE fix type per PR. Each fix = 1 branch + 1 PR.

### Type A — Remove @CrossOrigin("*") (security)
Scan all `*Controller.java` files. Replace `@CrossOrigin(origins = "*")` and `@CrossOrigin("*")` with nothing (remove the annotation entirely).

### Type B — Replace printStackTrace() with log.error()
In files containing `printStackTrace()`:
1. Add `@Slf4j` import + annotation to the class if not already present
2. Replace `e.printStackTrace()` → `log.error("Unexpected error", e)`
3. Replace `printStackTrace()` → `log.error("Unexpected error", e)`

### Type C — Replace System.out/err.println with log.info/error
In files containing `System.out.println` or `System.err.println`:
1. Add `@Slf4j` if not present
2. Replace `System.out.println(...)` → `log.info(...)`
3. Replace `System.err.println(...)` → `log.error(...)`

### Type D — Replace generic Exception catch with specific
Replace `catch (Exception e)` → `catch (RuntimeException e)` in service files only.

### Type E — Add @NotBlank validation to DTO fields
Scan `src/main/java/com/vcsm/model/*.java` for String fields without `@NotBlank`. Add `@NotBlank(message = "fieldName is required")` above String fields in request DTOs.

### Type F — Documentation improvements
Update README.md or add Javadoc. Pure docs, no code changes.

## PR Format
```markdown
## Problem
[Brief description of what the issue is about — extracted from issue body]

## Changes
[What files changed and why]

## Testing
Code review recommended — no local Java available
```

Title format: `fix: <short description> (#<issue_number>)` or `feat: <short description> (#<issue_number>)` or `docs: <short description> (#<issue_number>)`
Assign: `tmdeveloper007`
Labels: none (maintainer adds `gssoc:approved` after merge)

## Submission workflow
1. `git checkout -b fix/<issue_number>-<slug>`
2. Apply the targeted regex/file change
3. `git add . && git commit -m "fix: <title> (Fixes #<issue_number>)"`
4. `git push origin fix/<issue_number>-<slug> --force`
5. `POST /repos/ArpitaVerma16/Voice-Driven-Virtual-Customer-Success-Manager/pulls`
   - `head`: `tmdeveloper007:fix/<issue_number>-<slug>`
   - `base`: `main`
   - `title`: PR title
   - `body`: PR body
6. `git checkout main`

## Post-run
- Write summary to `/workspace/.mavis/vcsm-last-run.md`
- Append opened PR URLs to `/workspace/.mavis/vcsm-pr-log.md`

## Gotchas
- `sanrishi` has 29 open PRs — check `/repos/ArpitaVerma16/Voice-Driven-Virtual-Customer-Success-Manager/pulls?state=open` before each submission and skip any issue that touches the same file as an existing open PR
- The upstream has 2 solve_issues.py files — these are other people's bots. Don't be confused by them.
- The maintainer's `gssoc:approved` label is applied after merge, not before. Don't worry about labels on your PRs.
- PRs that touch fewer files and are more targeted get merged faster.
- Git identity MUST be set before commit (user.email, user.name)
- Origin remote MUST have token embedded for push auth: `git remote set-url origin "https://${GITHUB_TOKEN}@github.com/tmdeveloper007/..."`
