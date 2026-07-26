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
import urllib.request
import urllib.error
from pathlib import Path

REPO_DIR = Path("/workspace/Voice-Driven-VCSM")
TOKEN = os.environ.get("GH_TOKEN", os.environ.get("GITHUB_TOKEN", ""))
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
    data = gh_api("GET", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls/{pr_number}/files")
    return [f["filename"] for f in data]


def get_conflicting_files():
    prs = gh_api("GET", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/pulls?state=open&per_page=100")
    conflicting_files = set()
    for pr in prs:
        if pr["user"]["login"] == "sanrishi":
            conflicting_files.update(get_open_pr_files(pr["number"]))
    return conflicting_files


def sync_from_upstream():
    print("\n=== Syncing from upstream ===")
    token = TOKEN
    # Set remotes with token auth before any operation
    run(["git", "remote", "set-url", "origin", f"https://{token}@github.com/{FORK_OWNER}/Voice-Driven-Virtual-Customer-Success-Manager.git"])
    run(["git", "remote", "set-url", "upstream", f"https://{token}@github.com/{UPSTREAM_OWNER}/{UPSTREAM_REPO}.git"])
    run(["git", "fetch", "upstream"])
    # Preserve orchestrate.py (only on fork, not upstream)
    import shutil, tempfile
    orch_src = REPO_DIR / "orchestrate.py"
    orch_bak = None
    if orch_src.exists():
        orch_bak = tempfile.NamedTemporaryFile(delete=False, suffix=".py")
        shutil.copy(orch_src, orch_bak.name)
        orch_bak.close()
    run(["git", "checkout", "main"])
    run(["git", "reset", "--hard", "upstream/main"])
    # Restore orchestrate.py if it existed
    if orch_bak:
        shutil.copy(orch_bak.name, orch_src)
        os.unlink(orch_bak.name)
        run(["git", "add", "-f", "orchestrate.py"])
        run(["git", "commit", "-m", "Add orchestrate.py orchestrator"])
        run(["git", "push", "-f", "origin", "main"])
        print("  Preserved and re-committed orchestrate.py")
    print("  Synced main to upstream/main")


def find_issues():
    print("\n=== Fetching upstream issues ===")
    issues = gh_api("GET", f"/repos/{UPSTREAM_OWNER}/{UPSTREAM_REPO}/issues?state=open&per_page=100")
    result = []
    for iss in issues:
        if "pull_request" in iss:
            continue
        result.append((iss["number"], iss["title"], iss.get("body", "") or ""))
    print(f"  Found {len(result)} open issues")
    return result


def fix_system_out(java_files, conflict_files=None):
    conflict_files = conflict_files or set()
    """Replace System.out/System.err with log.info/warn in files that need it."""
    changes = []
    for fpath in java_files:
        try:
            content = fpath.read_text(encoding="utf-8")
        except Exception:
            continue
        original = content
        
        if 'System.out' not in content and 'System.err' not in content:
            continue
        
        has_sl4j = 'import org.slf4j.Logger' in content or 'import org.slf4j.Logger;' in content
        has_sout = 'System.out' in content
        has_serr = 'System.err' in content
        
        if (has_sout or has_serr) and not has_sl4j:
            if 'import org.slf4j.LoggerFactory;' not in content:
                import_lines = [l for l in content.split('\n') if l.startswith('import ')]
                if import_lines:
                    last_import_idx = content.rfind(import_lines[-1])
                    last_import_end = content.find('\n', last_import_idx) + 1
                    import_block = '\nimport org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;'
                    content = content[:last_import_end] + import_block + content[last_import_end:]
            
            class_match = re.search(r'(public class \w+[^{]*\{)', content)
            if class_match:
                insert_pos = class_match.end()
                cls_name = re.search(r'public class (\w+)', content)
                if cls_name:
                    cls = cls_name.group(1)
                    logger_field = f'\n    private static final Logger log = LoggerFactory.getLogger({cls}.class);'
                    content = content[:insert_pos] + logger_field + content[insert_pos:]
        
        content = re.sub(
            r'System\.out\.println\s*\(\s*"([^"]*)"\s*\)\s*;',
            r'log.info("\1");',
            content
        )
        content = re.sub(
            r'System\.out\.println\s*\(\s*"([^"]*)\s*"\s*\+\s*([^;]+)\)\s*;',
            r'log.info("\1" + \2);',
            content
        )
        content = re.sub(
            r'System\.err\.println\s*\(\s*"([^"]*)"\s*\)\s*;',
            r'log.warn("\1");',
            content
        )
        
        if content != original:
            fpath.write_text(content, encoding="utf-8")
            changes.append(str(fpath.relative_to(REPO_DIR)))
    
    return changes


def fix_duplicate_safely(java_files):
    """Remove duplicate safelyExecute method using Level.SEVERE in ComplaintService."""
    changes = []
    fpath = REPO_DIR / "src/main/java/com/vcsm/service/ComplaintService.java"
    if not fpath.exists():
        return changes
    
    content = fpath.read_text(encoding="utf-8")
    original = content
    
    # Find the Level.SEVERE method using simple string search
    idx = content.find('log.log(Level.SEVERE')
    if idx == -1:
        return changes  # Already fixed
    
    # Find the start of this method (go back to find 'private void safelyExecute')
    start = content.rfind('\n    private void safelyExecute', 0, idx)
    if start == -1:
        return changes
    
    # Find the end: after the closing brace, look for the next method
    end_braces = content.find('}', idx)
    end = content.find('\n    private boolean', end_braces)
    if end == -1:
        return changes
    
    method_block = content[start:end]
    new_content = content[:start] + content[end:]
    
    # Clean up double blank lines
    new_content = re.sub(r'\n{3,}', '\n\n', new_content)
    
    if new_content != original:
        fpath.write_text(new_content, encoding="utf-8")
        changes.append(str(fpath.relative_to(REPO_DIR)))
        print(f"  Removed duplicate safelyExecute (Level.SEVERE version)")
    
    return changes




def fix_empty_catches(java_files):
    """Remove empty catch(Exception ignored) blocks that silently swallow exceptions."""
    changes = []
    for fpath in java_files:
        rel = str(fpath.relative_to(REPO_DIR))
        try:
            content = fpath.read_text(encoding='utf-8')
        except Exception:
            continue
        
        import re
        original = content
        # Match: } catch (Exception <name>) { }
        content = re.sub(r'\} catch \(Exception \w+\) \{ \}', '}', content)
        # Also handle multi-line: }\ncatch (Exception x) {\n}
        content = re.sub(r'\}\s*catch \(Exception \w+\) \{\s*\}', '}', content)
        
        if content != original:
            fpath.write_text(content, encoding='utf-8')
            changes.append(rel)
    return changes


def fix_missing_logger(java_files):
    """Add SLF4J logger to GlobalExceptionHandler which uses log.error without a logger field."""
    changes = []
    fpath = REPO_DIR / 'src/main/java/com/vcsm/config/GlobalExceptionHandler.java'
    if not fpath.exists():
        return changes
    
    rel = str(fpath.relative_to(REPO_DIR))
    content = fpath.read_text(encoding='utf-8')
    
    if 'log.error' in content or 'log.warn' in content or 'log.info' in content:
        if 'LoggerFactory.getLogger' not in content:
            import_lines = [l for l in content.split('\n') if l.startswith('import ')]
            if import_lines:
                last_import = import_lines[-1]
                pos = content.rfind(last_import)
                end = content.find('\n', pos) + 1
                content = content[:end] + 'import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;\n' + content[end:]
            
            class_match = re.search(r'(public class \w+[^\{]*\{)', content)
            if class_match:
                insert_pos = class_match.end()
                cls = re.search(r'public class (\w+)', content)
                if cls:
                    logger_field = f'\n    private static final Logger log = LoggerFactory.getLogger({cls.group(1)}.class);'
                    content = content[:insert_pos] + logger_field + content[insert_pos:]
    
    if content != fpath.read_text(encoding='utf-8'):
        fpath.write_text(content, encoding='utf-8')
        changes.append(rel)
    return changes


def find_fixable_issues(issues):
    fixes = []
    
    for issue_num, title, body in issues:
        title_lower = title.lower()
        
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
        
        if "generic" in title_lower and "exception" in title_lower and "catch" in title_lower:
            fixes.append({
                "issue_num": issue_num,
                "title": title,
                "body": body,
                "fix_func": lambda f: [],  # complex, skip for now
                "description": "Remove generic catch (Exception e) blocks",
            })
            continue
    
    # Always try duplicate safelyExecute
    fixes.append({
        "issue_num": 523,
        "title": "Remove duplicate safelyExecute method",
        "body": "",
        "fix_func": fix_duplicate_safely,
        "description": "Remove duplicate safelyExecute in ComplaintService",
        "slug": "complaint-duplicate-safely",
    })
    
    # Standalone fixes (no GitHub issue required)
    fixes.append({
        "issue_num": None,
        "title": "Remove empty catch blocks in IotAlertController",
        "body": "",
        "fix_func": fix_empty_catches,
        "description": "Remove empty catch(Exception ignored) blocks in IotAlertController",
        "slug": "iot-alert-empty-catches",
    })
    
    fixes.append({
        "issue_num": None,
        "title": "Add missing SLF4J logger to GlobalExceptionHandler",
        "body": "",
        "fix_func": fix_missing_logger,
        "description": "Add missing SLF4J Logger field to GlobalExceptionHandler",
        "slug": "global-exception-logger",
    })
    
    return fixes


def apply_fixes(fixes, max_count=5):
    applied = []
    src = REPO_DIR / "src"
    java_files = list(src.rglob("*.java"))
    
    print("\n=== Checking for conflicting files ===")
    try:
        conflict_files = get_conflicting_files()
        print(f"  Files in sanrishi's open PRs: {len(conflict_files)}")
    except Exception as e:
        print(f"  Could not fetch conflicting files: {e}")
        conflict_files = set()
    
    for fix in fixes:
        if len(applied) >= max_count:
            break
        print(f"\n--- Applying fix: {fix['description']} ---")
        try:
            if fix["fix_func"].__name__ == "fix_system_out":
                changes = fix["fix_func"](java_files, conflict_files)
            else:
                changes = fix["fix_func"](java_files)
            if changes:
                fix["changed_files"] = changes
                applied.append(fix)
                print(f"  Changed files: {changes}")
            else:
                print(f"  No changes made")
        except Exception as e:
            print(f"  [ERROR] {e}")
    
    return applied


def create_prs(applied_fixes):
    pr_results = []
    
    for fix in applied_fixes:
        issue_num = fix.get("issue_num")
        slug = fix.get("slug", f"issue-{issue_num or 'unknown'}")
        branch = f"{HEAD_BRANCH_PREFIX}/fix-{slug}"
        desc = fix["description"]
        changed_files = fix.get("changed_files", [])
        
        if not changed_files:
            print(f"\nSkipping {desc} — no changed files")
            continue
        
        print(f"\n=== Creating PR for {desc} ===")
        print(f"  Branch: {branch}")
        print(f"  Files: {changed_files}")
        
        # Delete existing branch if it exists
        local_branches = run(["git", "branch"]).split('\n')
        if any(branch in b for b in local_branches):
            run(["git", "branch", "-D", branch])
        
        run(["git", "checkout", "-b", branch])
        run(["git", "add", "."])
        
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
        
        run(["git", "push", "-u", "origin", branch, "--force"])
        
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
        
        run(["git", "checkout", "main"])
    
    return pr_results


def main():
    print("=== VCSM Auto-PR Orchestrator ===")
    
    if not TOKEN:
        print("ERROR: GITHUB_TOKEN not set")
        sys.exit(1)
    
    sync_from_upstream()
    
    issues = find_issues()
    fixable = find_fixable_issues(issues)
    print(f"\n=== Found {len(fixable)} fixable issues ===")
    for f in fixable:
        print(f"  - Issue #{f['issue_num']}: {f['description']}")
    
    if not fixable:
        print("No fixable issues found. Exiting.")
        sys.exit(0)
    
    applied = apply_fixes(fixable, MAX_PRS)
    
    if not applied:
        print("\nNo fixes were applied.")
        sys.exit(0)
    
    pr_results = create_prs(applied)
    
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
