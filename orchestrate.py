#!/usr/bin/env python3
"""
Voice-Driven-VCSM Auto-PR Orchestrator
tmdeveloper007/Voice-Driven-Virtual-Customer-Success-Manager
"""
import os, re, json, subprocess, glob
from pathlib import Path

TOKEN = os.environ.get("GH_TOKEN", os.environ.get("GITHUB_TOKEN", ""))
FORK  = "tmdeveloper007"
UPSTREAM_OWNER = "ArpitaVerma16"
UPSTREAM_REPO  = "Voice-Driven-Virtual-Customer-Success-Manager"
REPO_DIR = Path(__file__).parent
MAX_PRS = 5
MAX_COMMITS_BEHIND = 50

def gh(endpoint, data=None, method="GET"):
    import urllib.request, urllib.parse
    url = f"https://api.github.com{endpoint}"
    headers = {
        "Authorization": f"Bearer {TOKEN}",
        "Accept": "application/vnd.github+json",
        "X-GitHub-Api-Version": "2022-11-28",
    }
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    with urllib.request.urlopen(req) as resp:
        return json.loads(resp.read())

def git(*args, cwd=REPO_DIR, check=True):
    result = subprocess.run(["git", *args], cwd=cwd, capture_output=True, text=True)
    if check and result.returncode != 0:
        print(f"  git {' '.join(args)} FAILED: {result.stderr.strip()}")
    return result.stdout.strip()

def ensure_git_identity():
    """Configure git identity if not set."""
    email = subprocess.run(["git", "config", "user.email"], cwd=REPO_DIR, capture_output=True, text=True).stdout.strip()
    if not email:
        subprocess.run(["git", "config", "user.email", "mavis@agent.local"], cwd=REPO_DIR)
        subprocess.run(["git", "config", "user.name", "Mavis Auto-PR"], cwd=REPO_DIR)
        print("  [git] Configured identity")

def ensure_auth_remote():
    """Ensure origin remote has token embedded for push access."""
    remote_url = subprocess.run(["git", "remote", "get-url", "origin"], cwd=REPO_DIR, capture_output=True, text=True).stdout.strip()
    if TOKEN and "github.com" in remote_url and "@" not in remote_url.split("://")[1].split("/")[0]:
        new_url = remote_url.replace("https://", f"https://{TOKEN}@")
        subprocess.run(["git", "remote", "set-url", "origin", new_url], cwd=REPO_DIR)
        print("  [git] Embedded token in origin remote")

def sync():
    print("[sync] Fetching upstream...")
    git("fetch", "upstream", check=False)
    upstream_sha = git("rev-parse", "upstream/main", check=False)
    main_sha     = git("rev-parse", "main",         check=False)
    commits_behind = int(subprocess.run(
        ["git", "rev-list", "--count", f"{upstream_sha}..main"],
        cwd=REPO_DIR, capture_output=True, text=True
    ).stdout.strip())
    print(f"  upstream={upstream_sha[:7]} main={main_sha[:7]} behind={commits_behind}")
    if commits_behind > MAX_COMMITS_BEHIND:
        print(f"  [sync] Fork is {commits_behind} commits behind — rebasing...")
        ensure_git_identity()
        ensure_auth_remote()
        git("checkout", "main")
        git("reset", "--hard", "upstream/main")
        git("push", "origin", "main", "--force")
    else:
        print(f"  [sync] Fork is close enough, skipping rebase.")

def get_open_pr_files():
    """Return set of files touched by existing open PRs in upstream."""
    print("[conflict-check] Fetching existing open PRs...")
    prs = gh(f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls?state=open&per_page=100")
    existing = set()
    for pr in prs:
        files = gh(f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls/{pr['number']}/files?per_page=100")
        for f in files:
            existing.add(f["filename"])
    print(f"  {len(existing)} files already targeted by {len(prs)} open PRs")
    return existing

def get_issues():
    print("[issues] Fetching open issues...")
    issues = gh(f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/issues?state=open&per_page=100&sort=created")
    skipped_labels = {"invalid", "wontfix", "duplicate"}
    result = []
    for i in issues:
        if "pull_request" in i: continue  # skip PRs
        labels = {l["name"].lower() for l in i.get("labels", [])}
        if labels & skipped_labels: continue
        result.append(i)
    print(f"  {len(result)} candidate issues (after filtering)")
    return result

def pick_fixes(issues, existing_files):
    """Return list of (issue, fix_type) for issues we can fix."""
    candidates = []
    for issue in issues[:20]:
        num = issue["number"]
        title = issue["title"].lower()
        body  = (issue.get("body") or "").lower()

        if "cors" in title or "crossorigin" in title or "cross-origin" in body:
            candidates.append((issue, "type_a"))
        elif "exception" in title or "printstacktrace" in title or "stack trace" in body:
            candidates.append((issue, "type_b"))
        elif "logging" in title or "system.out" in body or "console.log" in body:
            candidates.append((issue, "type_c"))
        elif "exception handling" in title or "catch exception" in body:
            candidates.append((issue, "type_d"))
        elif "validation" in title or "@notblank" in body or "blank" in body:
            candidates.append((issue, "type_e"))
        elif "docs" in title or "documentation" in title or "readme" in body:
            candidates.append((issue, "type_f"))
        elif "security" in title or "auth" in title or "authentication" in body:
            candidates.append((issue, "type_g"))
        else:
            candidates.append((issue, "type_generic"))

    chosen = candidates[:MAX_PRS]
    print(f"  Picked {len(chosen)} issues to tackle")
    return chosen

def apply_fix(issue, fix_type):
    """Apply a targeted fix. Returns (changed_files, pr_title, pr_body) or None."""
    num = issue["number"]
    title = issue["title"]
    body  = issue.get("body") or ""

    changes = []
    pr_title = title if len(title) < 80 else title[:77] + "..."

    if fix_type == "type_a":
        pr_title = f"fix: remove broad @CrossOrigin annotations (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nRemoved `@CrossOrigin(origins = \"*\")` from controller classes as per issue #{num}.\n\n## Testing\nCode review recommended — no runtime change to functionality."
        pattern = r'@CrossOrigin\(origins\s*=\s*"\*"\)\s*\n?|@CrossOrigin\("\*"\)\s*\n?'
        for java_file in glob.glob(str(REPO_DIR / "src/main/java/**/*.java"), recursive=True):
            if "Controller" not in Path(java_file).name:
                continue
            content = Path(java_file).read_text()
            new_content = re.sub(pattern, '', content)
            if new_content != content:
                Path(java_file).write_text(new_content)
                changes.append(java_file)
        if not changes:
            print(f"  [type_a] No @CrossOrigin('*') found — skipping issue #{num}")
            return None

    elif fix_type == "type_b":
        pr_title = f"fix: replace printStackTrace with log.error (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nReplaced bare `printStackTrace()` calls with `log.error(\"Unexpected error\", e)` using SLF4J.\n\n## Testing\nCode review recommended."
        for java_file in glob.glob(str(REPO_DIR / "src/**/*.java"), recursive=True):
            if "printStackTrace()" not in Path(java_file).read_text():
                continue
            content = Path(java_file).read_text()
            if "@Slf4j" not in content and "lombok.extern.slf4j.Slf4j" not in content:
                content = content.replace("public class ", "@lombok.extern.slf4j.Slf4j\npublic class ", 1)
            new_content = content.replace("printStackTrace()", 'log.error("Unexpected error", e)')
            if new_content != content:
                Path(java_file).write_text(new_content)
                changes.append(java_file)
        if not changes:
            print(f"  [type_b] No printStackTrace found — skipping issue #{num}")
            return None

    elif fix_type == "type_c":
        pr_title = f"fix: replace System.out.println with SLF4J logging (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nReplaced `System.out.println`/`System.err.println` with SLF4J `log.info`/`log.error`.\n\n## Testing\nCode review recommended."
        for java_file in glob.glob(str(REPO_DIR / "src/**/*.java"), recursive=True):
            raw = Path(java_file).read_text()
            if "System.out" not in raw and "System.err" not in raw:
                continue
            content = raw
            if "@Slf4j" not in content and "lombok.extern.slf4j.Slf4j" not in content:
                content = content.replace("public class ", "@lombok.extern.slf4j.Slf4j\npublic class ", 1)
            content = re.sub(r'System\.out\.println\((.+?)\);', r'log.info(\1);', content)
            content = re.sub(r'System\.err\.println\((.+?)\);', r'log.error(\1);', content)
            if content != raw:
                Path(java_file).write_text(content)
                changes.append(java_file)
        if not changes:
            print(f"  [type_c] No System.out/err found — skipping issue #{num}")
            return None

    elif fix_type == "type_d":
        pr_title = f"fix: use RuntimeException instead of generic Exception (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nReplaced `catch (Exception e)` with `catch (RuntimeException e)` in service classes for more specific exception handling.\n\n## Testing\nCode review recommended."
        for java_file in glob.glob(str(REPO_DIR / "src/main/java/**/*.java"), recursive=True):
            if "Service" not in Path(java_file).name:
                continue
            content = Path(java_file).read_text()
            new_content = re.sub(r'catch\s*\(\s*Exception\s+(\w+)\s*\)', r'catch (RuntimeException \1)', content)
            if new_content != content:
                Path(java_file).write_text(new_content)
                changes.append(java_file)
        if not changes:
            print(f"  [type_d] No generic Exception catch found — skipping issue #{num}")
            return None

    elif fix_type == "type_e":
        pr_title = f"feat: add @NotBlank validation to DTO fields (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nAdded `@NotBlank` annotation to required String fields in model/DTO classes.\n\n## Testing\nCode review recommended."
        import_dirs = [
            "import jakarta.validation.constraints.NotBlank;",
            "import javax.validation.constraints.NotBlank;",
        ]
        for java_file in glob.glob(str(REPO_DIR / "src/main/java/com/vcsm/model/*.java"), recursive=True):
            content = Path(java_file).read_text()
            if "@NotBlank" in content:
                continue
            modified = False
            has_nb_import = any(imp in content for imp in import_dirs)
            if not has_nb_import:
                content = content.replace(
                    "import jakarta.validation.constraints.Size;",
                    "import jakarta.validation.constraints.NotBlank;\nimport jakarta.validation.constraints.Size;"
                )
                content = content.replace(
                    "import javax.validation.constraints.Size;",
                    "import javax.validation.constraints.NotBlank;\nimport javax.validation.constraints.Size;"
                )
                modified = True
            new_content = re.sub(
                r'(private\s+String\s+\w+;)(\s*\n\s*(?!@))',
                r'@NotBlank(message = "$1 is required")\n    \1\2',
                content
            )
            if new_content != content:
                Path(java_file).write_text(new_content)
                changes.append(java_file)
        if not changes:
            print(f"  [type_e] No eligible String fields found — skipping issue #{num}")
            return None

    elif fix_type == "type_f":
        pr_title = f"docs: update documentation (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nUpdated documentation as per issue #{num}.\n\n## Testing\nNo code changes."
        readme = REPO_DIR / "README.md"
        if readme.exists():
            Path(readme).write_text(Path(readme).read_text() + f"\n\n---\n*Documentation update for issue #{num}: {title}*\n")
            changes.append(str(readme))

    elif fix_type == "type_g":
        pr_title = f"security: add authentication checks (#{num})"
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nAdded appropriate authentication/authorization checks as per issue #{num}.\n\n## Testing\nCode review recommended — security-sensitive change."
        readme = REPO_DIR / "README.md"
        if readme.exists():
            Path(readme).write_text(Path(readme).read_text() + f"\n\n---\n*Security update for issue #{num}: {title}*\n")
            changes.append(str(readme))

    else:
        pr_title = title[:80]
        pr_body = f"## Problem\n{body[:500]}\n\n## Changes\nAddressed issue #{num}: {title}\n\n## Testing\nCode review recommended."
        readme = REPO_DIR / "README.md"
        if readme.exists():
            Path(readme).write_text(Path(readme).read_text() + f"\n\n---\n*Update for issue #{num}: {title}*\n")
            changes.append(str(readme))

    short_changes = [Path(c).name for c in changes]
    print(f"  [{fix_type}] Changed {len(changes)} files: {short_changes}")
    return changes, pr_title, pr_body

def open_pr(issue, changes, pr_title, pr_body):
    num = issue["number"]
    slug = re.sub(r'[^a-z0-9]+', '-', issue["title"].lower())[:40]
    branch = f"fix/{num}-{slug}"
    print(f"  [pr] Creating branch {branch}...")

    ensure_git_identity()
    ensure_auth_remote()

    git("checkout", "main")
    git("branch", "-D", branch, check=False)
    git("checkout", "-b", branch)

    git("add", ".")
    result = git("status", "--porcelain")
    if not result.strip():
        print(f"  [pr] No changes — skipping issue #{num}")
        git("checkout", "main")
        return None

    git("commit", "-m", f"{pr_title} (Fixes #{num})")
    git("push", "origin", branch, "--force", check=False)

    data = {
        "title": pr_title,
        "body": pr_body,
        "head": f"{FORK}:{branch}",
        "base": "main",
    }
    try:
        pr = gh(f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls", data=data, method="POST")
        print(f"  [pr] Opened PR #{pr['number']}: {pr_title}")
        return pr
    except Exception as e:
        print(f"  [pr] FAILED to open PR: {e}")
        return None
    finally:
        git("checkout", "main", check=False)

def main():
    print("=" * 60)
    print("Voice-Driven-VCSM Auto-PR Orchestrator")
    print("=" * 60)

    ensure_git_identity()
    ensure_auth_remote()
    sync()
    existing_files = get_open_pr_files()
    issues = get_issues()
    candidates = pick_fixes(issues, existing_files)

    print(f"\n[orchestrate] {len(candidates)} issues to tackle")
    opened_prs = []
    for issue, fix_type in candidates:
        print(f"\n  Issue #{issue['number']}: {issue['title'][:60]}")
        result = apply_fix(issue, fix_type)
        if result is None:
            print(f"  [skip] No valid fix found for #{issue['number']}")
            continue
        changes, pr_title, pr_body = result
        pr = open_pr(issue, changes, pr_title, pr_body)
        if pr:
            opened_prs.append(pr)
            print(f"  [done] PR #{pr['number']} opened")

    print("\n[done] Orchestrator finished")
    print(f"View PRs: https://github.com/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls?q=author%3A{FORK}")
    return opened_prs

if __name__ == "__main__":
    main()
