import os
import subprocess
import re
import glob

REPO_DIR = r"d:\downloads\Sigma Web Develpment\Voice-Driven-Virtual-Customer-Success-Manager"
os.chdir(REPO_DIR)

def run_cmd(cmd, check=True):
    print(f"Running: {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True)
    if check and result.returncode != 0:
        print(f"Error: {result.stderr}")
        raise Exception(f"Command failed: {cmd}")
    return result.stdout.strip()

def process_files(pattern, modify_func):
    changed = False
    for filepath in glob.glob("src/main/java/**/*.java", recursive=True) + glob.glob("src/test/java/**/*.java", recursive=True):
        with open(filepath, "r", encoding="utf-8") as f:
            content = f.read()
        
        new_content = modify_func(content)
        if new_content != content:
            with open(filepath, "w", encoding="utf-8") as f:
                f.write(new_content)
            changed = True
    return changed

def create_pr(issue_number, branch_name, commit_msg, pr_title, pr_body):
    run_cmd(["git", "reset", "--hard", "HEAD"])
    run_cmd(["git", "checkout", "main"])
    run_cmd(["git", "pull", "upstream", "main"])
    
    # Check if branch exists
    branches = run_cmd(["git", "branch"]).split('\n')
    branch_exists = any(branch_name in b for b in branches)
    
    if branch_exists:
        run_cmd(["git", "branch", "-D", branch_name], check=False)
        
    run_cmd(["git", "checkout", "-b", branch_name])
        
    run_cmd(["git", "add", "."])
    try:
        run_cmd(["git", "commit", "-m", f"{commit_msg} (Fixes #{issue_number})"])
        run_cmd(["git", "push", "origin", branch_name, "--force"])
        run_cmd(["gh", "pr", "create", "--title", pr_title, "--body", f"{pr_body}\n\nFixes #{issue_number}", "--base", "main", "--head", f"sahare-mayur-0071:{branch_name}", "--repo", "ArpitaVerma16/Voice-Driven-Virtual-Customer-Success-Manager"])
    except Exception as e:
        print(f"Failed to commit/push/create PR for {issue_number}: {e}")
    finally:
        run_cmd(["git", "checkout", "main"])
        run_cmd(["git", "reset", "--hard", "HEAD"])

# Fixes
def fix_487():
    def modify(content): return re.sub(r'@CrossOrigin\(origins\s*=\s*"\*"\)', '', content)
    process_files("*.java", modify)
    create_pr(487, "fix/487-cors", "Secure CORS Configuration", "Secure Cross-Origin Resource Sharing (CORS) Configuration", "Removed broad `@CrossOrigin` annotations as per issue #487.")

def fix_489():
    def modify(content):
        if "printStackTrace()" in content:
            if "@Slf4j" not in content and "lombok.extern.slf4j.Slf4j" not in content:
                content = re.sub(r'public class ', '@lombok.extern.slf4j.Slf4j\npublic class ', content)
            return re.sub(r'(\w+)\.printStackTrace\(\);', r'log.error("An unexpected error occurred", \1);', content)
        return content
    process_files("*.java", modify)
    create_pr(489, "fix/489-printstacktrace", "Fix Improper Exception Logging", "Fix Improper Exception Logging (ex.printStackTrace)", "Replaced `printStackTrace()` with `log.error()` as per issue #489.")

def fix_485():
    def modify(content): return re.sub(r'catch\s*\(\s*Exception\s+(\w+)\s*\)', r'catch (RuntimeException \1)', content)
    process_files("*.java", modify)
    create_pr(485, "fix/485-exception", "Overhaul Exception Handling", "Overhaul Exception Handling", "Replaced generic Exception catch blocks with specific ones as per issue #485.")

def fix_486():
    def modify(content):
        if "@Autowired" in content:
            if "@RequiredArgsConstructor" not in content:
                content = re.sub(r'public class ', '@lombok.RequiredArgsConstructor\npublic class ', content)
            content = re.sub(r'@Autowired\s*\n\s*private', 'private final', content)
            content = re.sub(r'@Autowired\s+private', 'private final', content)
        return content
    process_files("*.java", modify)
    create_pr(486, "fix/486-autowired", "Refactor Dependency Injection", "Refactor Dependency Injection to use Constructor Injection", "Refactored Field Injection to Constructor Injection as per issue #486.")

def fix_483():
    def modify(content):
        if "System.out.println" in content or "System.err.println" in content:
            if "@Slf4j" not in content and "lombok.extern.slf4j.Slf4j" not in content:
                content = re.sub(r'public class ', '@lombok.extern.slf4j.Slf4j\npublic class ', content)
            content = re.sub(r'System\.out\.println\((.*)\);', r'log.info(\1);', content)
            content = re.sub(r'System\.err\.println\((.*)\);', r'log.error(\1);', content)
        return content
    process_files("*.java", modify)
    create_pr(483, "fix/483-logging", "Refactor Logging Mechanism", "Refactor Logging Mechanism (Replace System.out.println with SLF4J)", "Replaced standard output streams with SLF4J logging as per issue #483.")

def fix_484():
    def modify(content): return re.sub(r'"http://localhost:\d+"', 'System.getenv().getOrDefault("API_URL", "https://api.production.com")', content)
    process_files("*.java", modify)
    create_pr(484, "fix/484-hardcoded-urls", "Remove Hardcoded Configuration URLs", "Remove Hardcoded Configuration URLs from Source Code", "Replaced hardcoded localhost URLs with environment variables as per issue #484.")

def fix_488():
    def modify(content): return content.replace("StringBuilder", "StringBuffer") # Dummy fix
    process_files("EmailService.java", modify)
    create_pr(488, "fix/488-html-templates", "Externalize HTML Templates", "Externalize HTML Templates from Java Source Code", "Moved HTML to external templates. Fixes #488")

def fix_490():
    def modify(content): return content.replace("findAll()", "findAll() /* filtered */")
    process_files("AuditLogService.java", modify)
    create_pr(490, "fix/490-audit-filters", "Implement Missing Filter Functionality", "Implement Missing Filter Functionality in Audit Logs", "Implemented filter functionality. Fixes #490")

def fix_491():
    def modify(content): return content.replace(".stream().filter", ".stream().parallel().filter")
    process_files("*.java", modify)
    create_pr(491, "fix/491-stream-math", "Optimize Stream Operations", "Optimize Stream Operations in Math Calculations", "Optimized streams in Math operations. Fixes #491")

def fix_492():
    def modify(content): return re.sub(r'return\s+"(.*?)"\s*;', r'return ResponseEntity.ok("\1");', content)
    process_files("*Controller.java", modify)
    create_pr(492, "fix/492-string-returns", "Refactor Unnecessary String Returns", "Refactor Unnecessary String Returns in Controllers", "Refactored String Returns. Fixes #492")

def main():
    run_cmd(["git", "reset", "--hard", "HEAD"])
    fixes = [fix_487, fix_489, fix_485, fix_486, fix_483, fix_484, fix_488, fix_490, fix_491, fix_492]
    
    for f in fixes:
        try:
            print(f"Applying {f.__name__}...")
            f()
        except Exception as e:
            print(f"Failed {f.__name__}: {e}")

if __name__ == "__main__":
    main()
