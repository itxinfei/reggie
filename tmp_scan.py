import re, os, sys

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

base_dir = "D:/MyCode/reggie/src/main/java/com/reggie"
java_files = []
for root, dirs, files in os.walk(base_dir):
    for f in files:
        if f.endswith('.java'):
            java_files.append(os.path.join(root, f))

results = []

for fpath in java_files:
    try:
        with open(fpath, 'r', encoding='utf-8', errors='replace') as f:
            content = f.read()
            lines = content.split('\n')
    except:
        continue

    i = 0
    while i < len(lines):
        line = lines[i]
        m = re.search(r'catch\s*\(([^)]*)\)\s*\{', line)
        if not m:
            i += 1
            continue

        exc_type = m.group(1).strip()
        brace_depth = 1
        start_line = i + 1
        block_lines = [line[m.end():]]
        j = i + 1
        while j < len(lines) and brace_depth > 0:
            bl = lines[j]
            block_lines.append(bl)
            depth_change = bl.count('{') - bl.count('}')
            brace_depth += depth_change
            j += 1

        block_text = '\n'.join(block_lines)

        has_log = bool(re.search(r'log\.(error|warn|info|debug)', block_text))
        has_throw = bool(re.search(r'\bthrow\b', block_text))
        has_custom_exc = bool(re.search(r'CustomException', block_text))
        is_catching_custom = 'CustomException' in exc_type

        if has_log and not has_throw and not has_custom_exc and not is_catching_custom:
            sep = chr(92)
            rel = fpath[len(base_dir)+1:].replace(sep, '/')
            results.append((rel, start_line, exc_type, block_text.strip()[:200]))

        i = j

controllers = [r for r in results if '/controller/' in r[0]]
services = [r for r in results if '/service/' in r[0] and 'ServiceImpl' in r[0]]
service_ifaces = [r for r in results if '/service/' in r[0] and 'ServiceImpl' not in r[0]]
others = [r for r in results if r not in controllers and r not in services and r not in service_ifaces]

print('=== CONTROLLERS ({} findings) ==='.format(len(controllers)))
for rel, line, exc, snippet in controllers:
    print('{}:{}  [{}] {}'.format(rel, line, exc, snippet))

print()
print('=== SERVICE IMPLEMENTATIONS ({} findings) ==='.format(len(services)))
for rel, line, exc, snippet in services:
    print('{}:{}  [{}] {}'.format(rel, line, exc, snippet))

print()
print('=== SERVICE INTERFACES ({} findings) ==='.format(len(service_ifaces)))
for rel, line, exc, snippet in service_ifaces:
    print('{}:{}  [{}] {}'.format(rel, line, exc, snippet))

print()
print('=== OTHER ({} findings) ==='.format(len(others)))
for rel, line, exc, snippet in others:
    print('{}:{}  [{}] {}'.format(rel, line, exc, snippet))

print()
print('Total: {}  Controllers: {}  Services: {}  Other: {}'.format(
    len(results), len(controllers), len(services) + len(service_ifaces), len(others)))