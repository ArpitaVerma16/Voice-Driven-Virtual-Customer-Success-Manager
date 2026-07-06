#!/usr/bin/env python3
"""
VCSM Auto-PR Orchestrator
Fixes regex-based issues in the fork and opens PRs to upstream.
No Java compilation — pure regex/text fixes.
"""
import subprocess
import re
import os
import sys
import json
import base64
import urllib.request
import urllib.error
from pathlib import Path

REPO_DIR = Path("/workspace/Voice-Driven-VCSM")
TOKEN = os.environ.get("GITHUB_TOKEN", "")
UPSTREAM_OWNER = "ArpitaVerma16"
UPSTREAM_REPO = "Voice-Driven-Virtual-Customer-Success-Manager"
FORK_OWNER = "tmdeveloper007"
HEAD_BRANCH_PREFIX = "mavis-bot"

GH_API = "https://api.github.com"

MAX_PRS = 5


def run(cmd, check=True, capture=True):
    cwd = str(REPO_DIR)
    if isinstance(cmd, str):
        cmd = cmd.split()
    print(f"  [cmd] {' '.join(cmd)}")
    kw = dict(cwd=cwd, shell=True) if isinstance(cmd, str) else dict(cwd=cwd)
    result = subprocess.run(
        " ".join(cmd) if isinstance(cmd, str) else cmd,
        capture_output=True, text=True, check=False, **kw
    )
    if check and result.returncode != 0:
        print(f"  [ERROR] returncode={result.returncode}")
        print(f"  stderr: {result.stderr[:500]}")
        raise RuntimeError(f"Command failed: {' '.join(cmd)}")
    return result.stdout.strip() if capture else ""


def gh_api(method, endpoint, data=None, token=None):
    token = token or TOKEN
    url = f"{GH_API}{endpoint}"
    req = urllib.request.Request(url)
    req.add_header("Authorization", f"token {token}")
    req.add_header("Accept", "application/vnd.github+json")
    req.add_header("X-GitHub-Api-Version", "2022-11-28")
    req.add_header("User-Agent", "MavisBot/1.0")
    if method.upper() in ("POST", "PATCH", "PUT"):
        import json as _json
        req.add_header("Content-Type", "application/json")
        req.data = _json.dumps(data).encode() if data else None
    else:
        req.data = None
    req.get_method = lambda: method.upper()
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            return json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = e.read().decode()[:500]
        print(f"  [GH API ERROR] {e.code}: {body}")
        raise


def get_open_pr_files(pr_number):
    """Get list of files changed in an open PR."""
    data = gh_api("GET", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls/{pr_number}/files")
    return [f["filename"] for f in data]


def get_conflicting_files():
    """Get files touched by sanrishi's open PRs (highest conflict risk)."""
    prs = gh_api("GET", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls?state=open&per_page=100")
    conflicting_files = set()
    for pr in prs:
        if pr["user"]["login"] == "sanrishi":
            conflicting_files.update(get_open_pr_files(pr["number"]))
    return conflicting_files


def sync_from_upstream():
    print("\n=== Syncing from upstream ===")
    token = TOKEN
    # Set remotes with token auth
    run(["git", "remote", "set-url", "origin", f"https://{token}@github.com/{FORK_OWNER}/Voice-Driven-Virtual-Customer-Success-Manager.git"])
    run(["git", "remote", "set-url", "upstream", f"https://{token}@github.com/{UPSTREAM_OWNER}/{UPSTREAM_REPO}.git"])
    run(["git", "fetch", "upstream"])
    run(["git", "checkout", "main"])
    run(["git", "reset", "--hard", "upstream/main"])
    run(["git", "push", "origin", "main", "--force"])
    print("  Synced main to upstream/main")


def find_issues():
    """Return list of (issue_number, title, body) tuples for open issues."""
    print("\n=== Fetching upstream issues ===")
    issues = gh_api("GET", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/issues?state=open&per_page=100")
    result = []
    for iss in issues:
        if "pull_request" in iss:
            continue
        result.append((iss["number"], iss["title"], iss.get("body", "") or ""))
    print(f"  Found {len(result)} open issues")
    return result


def find_fixable_issues(issues):
    """
    Filter to issues where we can apply a regex-based fix.
    Returns list of dicts with: issue_number, title, fix_func, description
    """
    fixes = []
    src = REPO_DIR / "src"
    java_files = list(src.rglob("*.java"))
    
    for issue_num, title, body in issues:
        title_lower = title.lower()
        
        # Issue: Replace System.out.println with SLF4J
        if ("system.out" in title_lower or "system.err" in title_lower or 
            "system.out.println" in title_lower) and "slf4j" in title_lower:
            fixes.append({
                "issue_num": issue_num,
                "title": title,
                "body": body,
                "fix_func": fix_system_out,
                "description": "Replace System.out/System.err with SLF4J loggers",
            })
            continue
        
        # Issue: Remove generic catch (Exception e)
        if "generic" in title_lower and "exception" in title_lower and "catch" in title_lower:
            fixes.append({
                "issue_num": issue_num,
                "title": title,
                "body": body,
                "fix_func": fix_generic_exception,
                "description": "Remove generic catch (Exception e) blocks",
            })
            continue
        
        # Issue: duplicate methods / dead code
        if "duplicate" in title_lower or "dead code" in title_lower:
            fixes.append({
                "issue_num": issue_num,
                "title": title,
                "body": body,
                "fix_func": fix_duplicate_safely,
                "description": "Remove duplicate safelyExecute method",
            })
            continue
    
    # Also add known patterns even without matching issues
    # These are the patterns we know exist in the codebase
    known_patterns = [
        ("complaint-duplicate-safely", "Fix duplicate safelyExecute in ComplaintService", fix_duplicate_safely),
    ]
    for slug, desc, func in known_patterns:
        if not any(f["description"] == desc for f in fixes):
            fixes.append({
                "issue_num": None,
                "title": desc,
                "body": "",
                "fix_func": func,
                "description": desc,
                "slug": slug,
            })
    
    return fixes


def fix_system_out(java_files):
    """Replace System.out/System.err with log.info/log.warn."""
    changes = []
    for fpath in java_files:
        try:
            content = fpath.read_text(encoding="utf-8")
        except Exception:
            continue
        
        original = content
        
        # Skip if already uses log.info/warn for these
        if 'System.out' not in content and 'System.err' not in content:
            continue
        
        # Check if file has a logger
        has_sl4j = 'import org.slf4j.Logger' in content or 'import org.slf4j.Logger;' in content
        has_sout = 'System.out' in content
        has_serr = 'System.err' in content
        
        if has_sout and not has_sl4j:
            # Add SLF4J imports and logger if missing
            if 'import org.slf4j.LoggerFactory;' not in content:
                # Add import after last import
                import_lines = [l for l in content.split('\n') if l.startswith('import ')]
                if import_lines:
                    last_import_idx = content.rfind(import_lines[-1])
                    last_import_end = content.find('\n', last_import_idx) + 1
                    import_block = '\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;'
                    content = content[:last_import_end] + import_block + content[last_import_end:]
            
            # Add logger field after class declaration
            class_match = re.search(r'(public class \w+[^{]*\{)', content)
            if class_match:
                insert_pos = class_match.end()
                logger_field = '\n    private static final Logger log = LoggerFactory.getLogger(LoggerFactory.class);'
                # Use a placeholder logger name based on the class
                cls_name = re.search(r'public class (\w+)', content)
                if cls_name:
                    cls = cls_name.group(1)
                    logger_field = f'\n    private static final Logger log = LoggerFactory.getLogger({cls}.class);'
                content = content[:insert_pos] + logger_field + content[insert_pos:]
        
        # Replace patterns
        # System.out.println("...") -> log.info("...")
        content = re.sub(
            r'System\.out\.println\s*\(\s*"([^"]*)"\s*\)\s*;',
            r'log.info("\1");',
            content
        )
        # System.out.println("..." + var) -> log.info("...", var)
        content = re.sub(
            r'System\.out\.println\s*\(\s*"([^"]*)\s*"\s*\+\s*([^;]+)\)\s*;',
            r'log.info("\1" + \2);',
            content
        )
        # System.err.println("...") -> log.warn("...")
        content = re.sub(
            r'System\.err\.println\s*\(\s*"([^"]*)"\s*\)\s*;',
            r'log.warn("\1");',
            content
        )
        
        if content != original:
            fpath.write_text(content, encoding="utf-8")
            changes.append(str(fpath.relative_to(REPO_DIR)))
    
    return changes


def fix_generic_exception(java_files):
    """Replace generic catch(Exception e) with more specific types."""
    changes = []
    for fpath in java_files:
        try:
            content = fpath.read_text(encoding="utf-8")
        except Exception:
            continue
        
        original = content
        
        # Replace catch(Exception e) { ... e.printStackTrace(); } 
        # with catch(Exception e) { log.error("...", e); }
        content = re.sub(
            r'catch\s*\(\s*Exception\s+e\s*\)\s*\{\s*log\.warn\([^)]+\);\s*\}',
            lambda m: m.group(0),  # already handled
            content
        )
        
        # Simple: catch (Exception e) { log.warn(msg); } -> keep as-is
        # Complex: catch (Exception e) { log.warn(msg); log.somethingElse(); } -> keep
        
        if content != original:
            fpath.write_text(content, encoding="utf-8")
            changes.append(str(fpath.relative_to(REPO_DIR)))
    
    return changes


def fix_duplicate_safely(java_files):
    """Remove duplicate safelyExecute method in ComplaintService."""
    changes = []
    fpath = REPO_DIR / "src/main/java/com/vcsm/service/ComplaintService.java"
    if not fpath.exists():
        return changes
    
    content = fpath.read_text(encoding="utf-8")
    
    # Find and remove the Level.SEVERE version of safelyExecute
    # Pattern: two adjacent private void safelyExecute methods
    # First one uses log.error, second uses log.log(Level.SEVERE,...)
    
    # The Level.SEVERE one starts with log.log(Level.SEVERE
    # and ends at the closing brace before the next method
    
    # Match the Level.SEVERE safelyExecute block
    pattern = (
        r'\n    private void safelyExecute\(Runnable operation, String description\) \{\n'
        r'        try \{\n'
        r'            operation\.run\(\);\n'
        r'        \} catch \(Exception e\) \{\n'
        r'            log\.log\(Level\.SEVERE, "Failed: " \+ description, e\);\n'
        r'        \}\n'
        r'    \}'
    )
    
    new_content = re.sub(pattern, '', content, count=1)
    
    if new_content != content:
        fpath.write_text(new_content, encoding="utf-8")
        changes.append(str(fpath.relative_to(REPO_DIR)))
        print(f"  Removed duplicate safelyExecute from ComplaintService.java")
    
    return changes


def apply_fixes(fixes, max_count=5):
    """Apply up to max_count fixes and return list of applied fix dicts."""
    applied = []
    src = REPO_DIR / "src"
    java_files = list(src.rglob("*.java"))
    
    for fix in fixes:
        if len(applied) >= max_count:
            break
        print(f"\n--- Applying fix: {fix['description']} ---")
        try:
            changes = fix["fix_func"](java_files)
            if changes:
                fix["changed_files"] = changes
                applied.append(fix)
                print(f"  Changed files: {changes}")
            else:
                print(f"  No changes made (files may already be fixed)")
        except Exception as e:
            print(f"  [ERROR] {e}")
    
    return applied


def create_prs(applied_fixes):
    """Create branches and PRs for each applied fix."""
    pr_results = []
    
    for fix in applied_fixes:
        issue_num = fix.get("issue_num")
        branch = f"{HEAD_BRANCH_PREFIX}/fix-{issue_num or fix.get('slug', 'unknown')}"
        desc = fix["description"]
        changed_files = fix.get("changed_files", [])
        
        if not changed_files:
            print(f"\nSkipping {desc} — no changed files")
            continue
        
        print(f"\n=== Creating PR for {desc} ===")
        print(f"  Branch: {branch}")
        print(f"  Files: {changed_files}")
        
        # Check if branch already exists locally
        local_branches = run(["git", "branch"]).split('\n')
        if any(branch in b for b in local_branches):
            run(["git", "branch", "-D", branch], check=False)
        
        # Create branch from upstream/main
        run(["git", "checkout", "-b", branch])
        
        # Commit
        run(["git", "add", "."])
        
        # Check if there are staged changes
        diff_result = run(["git", "diff", "--cached", "--stat"])
        if not diff_result.strip():
            print(f"  Nothing to commit, skipping")
            run(["git", "checkout", "main"])
            continue
        
        commit_msg = f"fix: {desc}"
        if issue_num:
            commit_msg += f" (Fixes #{issue_num})"
        
        try:
            run(["git", "commit", "-m", commit_msg])
        except RuntimeError:
            print(f"  Nothing to commit")
            run(["git", "checkout", "main"])
            continue
        
        # Push
        push_result = run(["git", "push", "-u", "origin", branch])
        
        # Create PR
        pr_title = commit_msg.split(" (")[0]
        pr_body = f"""## Description

{desc}

### Changes
"""
        for cf in changed_files:
            pr_body += f"- Fixed in `{cf}`\n"
        
        if issue_num:
            pr_body += f"\nFixes #{issue_num}"
        
        pr_body += f"\n\n---\n*Automated PR by Mavis Bot*"
        
        try:
            pr_data = gh_api("POST", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls", data={
                "title": pr_title,
                "body": pr_body,
                "head": f"{FORK_OWNER}:{branch}",
                "base": "main",
            })
            pr_url = pr_data.get("html_url", "unknown")
            pr_number = pr_data.get("number", "?")
            print(f"  PR #{pr_number}: {pr_url}")
            pr_results.append({
                "issue": issue_num,
                "pr_number": pr_number,
                "pr_url": pr_url,
                "description": desc,
                "changed_files": changed_files,
            })
        except Exception as e:
            print(f"  [PR CREATE FAILED] {e}")
            # Try to push and create PR manually
            print(f"  Branch pushed: {branch}")
        
        # Return to main
        run(["git", "checkout", "main"])
    
    return pr_results


def main():
    print("=== VCSM Auto-PR Orchestrator ===")
    
    if not TOKEN:
        print("ERROR: GITHUB_TOKEN not set")
        sys.exit(1)
    
    # Sync from upstream
    sync_from_upstream()
    
    # Check for conflicts with sanrishi's PRs
    print("\n=== Checking for conflicting files ===")
    try:
        conflict_files = get_conflicting_files()
        print(f"  Files in sanrishi's PRs: {len(conflict_files)}")
        if conflict_files:
            print(f"  Sample: {list(conflict_files)[:5]}")
    except Exception as e:
        print(f"  Could not fetch conflicting files: {e}")
        conflict_files = set()
    
    # Find issues
    issues = find_issues()
    fixable = find_fixable_issues(issues)
    print(f"\n=== Found {len(fixable)} fixable issues ===")
    for f in fixable:
        print(f"  - Issue #{f['issue_num']}: {f['description']}")
    
    if not fixable:
        print("No fixable issues found. Exiting.")
        sys.exit(0)
    
    # Apply fixes
    applied = apply_fixes(fixable, MAX_PRS)
    
    if not applied:
        print("\nNo fixes were applied (files may already be fixed).")
        sys.exit(0)
    
    # Create PRs
    pr_results = create_prs(applied)
    
    # Summary
    print("\n" + "=" * 50)
    print("SUMMARY")
    print("=" * 50)
    print(f"Fixes applied: {len(applied)}")
    print(f"PRs created:   {len(pr_results)}")
    for pr in pr_results:
        print(f"  PR #{pr['pr_number']}: {pr['pr_url']} (Issue #{pr['issue']})")
    
    return pr_results


if __name__ == "__main__":
    results = main()
