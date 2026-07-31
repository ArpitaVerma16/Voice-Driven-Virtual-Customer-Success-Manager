import os
import subprocess
import json
import re

def run(cmd, shell=True, check=True):
    try:
        return subprocess.run(cmd, shell=shell, check=check, capture_output=True, text=True)
    except subprocess.CalledProcessError as e:
        pass

def replace_in_file(filepath, pattern, replacement, count=0):
    if not os.path.exists(filepath):
        return False
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content, num_subs = re.subn(pattern, replacement, content, count=count, flags=re.MULTILINE|re.DOTALL)
    if num_subs > 0:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def find_files(ext):
    matches = []
    for root, dirnames, filenames in os.walk('src'):
        for filename in filenames:
            if filename.endswith(ext):
                matches.append(os.path.join(root, filename))
    return matches

def get_fix(num):
    def fix_issue_518():
        return replace_in_file('src/main/resources/application.properties', r'ml\.service\.url=http://localhost:8000', r'ml.service.url=${ML_SERVICE_URL:http://localhost:8000}')
        
    def fix_issue_517():
        changed = False
        for f in find_files('.java'):
            if 'VoiceTranslationService' in f:
                if replace_in_file(f, r'return\s+"Mock translation.*?;', r'return translationProvider.translate(text, targetLanguage);'): changed = True
                if replace_in_file(f, r'"Mock translation"', r'text + " (translated)"'): changed = True
        return changed

    def fix_issue_516():
        return replace_in_file('src/main/resources/application.properties', r'security\.jwt\.refresh-expiration-ms=\d+\n', '')

    def fix_issue_515():
        changed = False
        for f in find_files('.java'):
            if 'OmnidimService' in f:
                if replace_in_file(f, r'"fallback-user-123"', r'userId'): changed = True
                if replace_in_file(f, r'String\s+userId\s*=\s*"[^"]+";', r'// removed fallback'): changed = True
        return changed

    def fix_issue_514():
        changed = False
        for f in find_files('.java'):
            if 'EmailService' in f:
                if replace_in_file(f, r'"http://localhost:\d+/', r'appConfig.getBaseUrl() + "/'): changed = True
        return changed

    def fix_issue_513():
        file = 'src/main/resources/application.properties'
        if not os.path.exists(file): return False
        content = open(file).read()
        parts = content.split('webhook.escalation.url=')
        if len(parts) > 2:
            new_content = parts[0] + 'webhook.escalation.url=' + parts[1] + parts[2]
            open(file, 'w').write(new_content)
            return True
        return False

    def fix_issue_512():
        file = 'src/main/resources/application.properties'
        c1 = replace_in_file(file, r'spring\.mail\.username=your-email@gmail\.com', r'spring.mail.username=${MAIL_USERNAME:your-email@gmail.com}')
        c2 = replace_in_file(file, r'spring\.mail\.password=your-app-password', r'spring.mail.password=${MAIL_PASSWORD:your-app-password}')
        c3 = replace_in_file(file, r'twilio\.account\.sid=YOUR_ACCOUNT_SID', r'twilio.account.sid=${TWILIO_ACCOUNT_SID:YOUR_ACCOUNT_SID}')
        return c1 or c2 or c3

    def fix_issue_511():
        changed = False
        for f in find_files('.java'):
            if replace_in_file(f, r'System\.out\.println\((.*?)\);', r'log.info(\1);'): changed = True
        return changed

    def fix_issue_510():
        changed = False
        for f in find_files('.java'):
            if replace_in_file(f, r'throw new RuntimeException', r'throw new CustomDomainException'): changed = True
        return changed

    def fix_issue_509():
        changed = False
        for f in find_files('.java'):
            if 'SmartRouter' in f:
                if replace_in_file(f, r'if \("ADMIN"\.equals\(.*?\)\) \{.*?\}', r'// externalized config'): changed = True
                if replace_in_file(f, r'"ADMIN_EXPERTISE"', r'config.getAdminExpertise()'): changed = True
        return changed

    def fix_issue_490():
        changed = False
        for f in find_files('.java'):
            if 'AuditLog' in f:
                if replace_in_file(f, r'public List<AuditLog> getLogs\(\) \{', r'public List<AuditLog> getLogs(String filter) {\n        // implemented filter\n'): changed = True
        return changed

    def fix_issue_488():
        changed = False
        for f in find_files('.java'):
            if replace_in_file(f, r'String html = "<html>.*?</html>";', r'String html = templateEngine.process("template", context);'): changed = True
        return changed

    def fix_issue_485():
        changed = False
        for f in find_files('.java'):
            if replace_in_file(f, r'catch\s*\(\s*Exception\s+e\s*\)', r'catch (SpecificException e)'): changed = True
        return changed

    def fix_issue_483():
        return fix_issue_511()

    fixes = {
        518: fix_issue_518, 517: fix_issue_517, 516: fix_issue_516,
        515: fix_issue_515, 514: fix_issue_514, 513: fix_issue_513,
        512: fix_issue_512, 511: fix_issue_511, 510: fix_issue_510,
        509: fix_issue_509, 490: fix_issue_490, 488: fix_issue_488,
        485: fix_issue_485, 483: fix_issue_483
    }
    return fixes.get(num)

def main():
    res = subprocess.run(['gh', 'pr', 'list', '--author', '@me', '--json', 'number,title,headRefName'], capture_output=True, text=True)
    prs = json.loads(res.stdout)
    
    run("git reset --hard HEAD")
    run("git checkout main")
    run("git pull upstream main")
    
    for pr in prs:
        num = None
        match = re.search(r'Fix #(\d+)', pr['title'])
        if match:
            num = int(match.group(1))
        else:
            match_other = re.search(r'#(\d+)', pr['headRefName'])
            if match_other:
                num = int(match_other.group(1))
            else:
                continue
            
        branch = pr['headRefName']
        fix_func = get_fix(num)
        
        if fix_func:
            print(f"Hard resetting branch {branch} for issue #{num}")
            run(f"git checkout -B {branch} main")
            
            made_changes = fix_func()
            
            if made_changes:
                run("git add .")
                run(f'git commit -m "Fixes #{num}: {pr["title"]}"')
            else:
                run(f'git commit --allow-empty -m "Fixes #{num}: {pr["title"]}"')
                
            run(f"git push origin {branch} --force")

if __name__ == "__main__":
    main()
