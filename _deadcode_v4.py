import re, os

base = r"D:\MyCode\reggie\src\main\java\com\reggie"
all_java = []
for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith(".java"):
            p = os.path.join(root, f).replace("\\", "/")
            all_java.append(p)

file_contents = {}
for p in all_java:
    try:
        file_contents[p] = open(p, encoding='utf-8').read()
    except:
        pass

def extract_interface_methods(content):
    methods = []
    for i, line in enumerate(content.split('\n')):
        stripped = line.strip()
        if not stripped.endswith(';'):
            continue
        body = stripped[:-1].strip()
        m = re.match(r'^(public\s+)?(\w[\w<>.\[\],\s]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,.\s]+)?$', body)
        if not m:
            continue
        ret = m.group(2).strip()
        name = m.group(3)
        params_raw = m.group(4).strip()
        if name in ('if','else','for','while','switch','return','class','interface','enum','new','try','catch','finally'):
            continue
        if ret in ('public','static','final','abstract','default','synchronized','native','transient','volatile'):
            continue
        methods.append({'name':name,'return_type':ret,'params':params_raw,'line':i+1,'signature':f"{ret} {name}({params_raw})"})
    return methods

def extract_impl_methods(content):
    methods = []
    lines = content.split('\n')
    class_name = None
    for j in range(len(lines)):
        cm = re.search(r'\bclass\s+(\w+)', lines[j])
        if cm:
            class_name = cm.group(1)
            break
    for i, line in enumerate(lines):
        stripped = line.strip()
        has_override = False
        for k in range(max(0,i-3),i):
            if '@Override' in lines[k].strip():
                has_override = True
                break
        m = re.match(r'^(\w[\w<>.\[\],\s]*?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,.\s]+)?\s*\{?\s*$', stripped)
        if not m:
            m = re.match(r'^(\w[\w<>.\[\],\s]*?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,.\s]+)?\s*$', stripped)
        if m:
            ret = m.group(1).strip()
            name = m.group(2)
            params_raw = m.group(3).strip()
            if name in ('if','else','for','while','switch','return','class','interface','enum'):
                continue
            if class_name and name == class_name:
                continue
            methods.append({'name':name,'return_type':ret,'params':params_raw,'line':i+1,'override':has_override,'signature':f"{ret} {name}({params_raw})"})
    return methods

impl_files = [p for p in all_java if "/impl/" in p and p.endswith("ServiceImpl.java")]
iface_files = [p for p in all_java if "/service/" in p and "/impl/" not in p and "/mapper/" not in p and "/model/" not in p and "/dto/" not in p]

iface_map = {}
for p in iface_files:
    content = file_contents.get(p, '')
    m = re.search(r'\binterface\s+(\w+)', content)
    if m:
        iface_map[m.group(1)] = p

paired = {}
for p in impl_files:
    content = file_contents.get(p, '')
    m = re.search(r'implements\s+(\w+Service)', content)
    if m:
        svc = m.group(1)
        if svc in iface_map:
            paired[p] = iface_map[svc]

test_files = set(p for p in all_java if "/test/" in p.lower())

# Cat1: Interface method declared + Impl exists + NO callers anywhere except self/interface (test excluded but controllers COUNT)
cat1 = []
for impl_file, iface_file in paired.items():
    iface_content = file_contents.get(iface_file, '')
    iface_methods = extract_interface_methods(iface_content)
    for im in iface_methods:
        name = im['name']
        has_external = False
        for p, content in file_contents.items():
            if p == impl_file or p == iface_file:
                continue
            if p in test_files:
                continue
            pat = r'\b' + re.escape(name) + r'\s*\('
            if re.search(pat, content):
                has_external = True
                break
        if not has_external:
            cat1.append({'impl_file':os.path.basename(impl_file),'iface_file':os.path.basename(iface_file),'method':name,'return_type':im['return_type'],'params':im['params'],'line':im['line'],'signature':im['signature']})

# Cat2
cat2 = []
for impl_file, iface_file in paired.items():
    iface_content = file_contents.get(iface_file, '')
    impl_content = file_contents.get(impl_file, '')
    iface_methods = extract_interface_methods(iface_content)
    impl_methods = extract_impl_methods(impl_content)
    iface_declared = set((m['name'], m['params']) for m in iface_methods)
    for im in impl_methods:
        if im['override']:
            key = (im['name'], im['params'])
            if key not in iface_declared:
                cat2.append({'impl_file':os.path.basename(impl_file),'method':im['name'],'return_type':im['return_type'],'params':im['params'],'line':im['line'],'signature':im['signature']})

# Cat3
cat3 = []
for impl_file in impl_files:
    impl_content = file_contents.get(impl_file, '')
    impl_methods = extract_impl_methods(impl_content)
    for im in impl_methods:
        if not im['override']:
            name = im['name']
            has_ref = False
            for p, content in file_contents.items():
                if p == impl_file:
                    continue
                pat = r'\b' + re.escape(name) + r'\s*\('
                if re.search(pat, content):
                    has_ref = True
                    break
            if not has_ref:
                cat3.append({'impl_file':os.path.basename(impl_file),'method':im['name'],'return_type':im['return_type'],'params':im['params'],'line':im['line'],'signature':im['signature']})

print(f"CAT1 (interface+impl, NO caller anywhere excl test): {len(cat1)}")
for c in cat1:
    print(f"  [{c['line']}] {c['iface_file']} / {c['impl_file']} | {c['signature']}")

print(f"\nCAT2 (@Override but interface not declare): {len(cat2)}")
for c in cat2:
    print(f"  [{c['line']}] {c['impl_file']} | {c['signature']}")

print(f"\nCAT3 (non-@Override method, zero external ref): {len(cat3)}")
for c in cat3:
    print(f"  [{c['line']}] {c['impl_file']} | {c['signature']}")
