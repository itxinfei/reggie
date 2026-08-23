
import re, os

base = r"D:\MyCodeeggie\src\main\java\comeggie"
all_java = []
for root, dirs, files in os.walk(base):
    for f in files:
        if f.endswith(".java"):
            p = os.path.join(root, f).replace(chr(92), "/")
            all_java.append(p)

file_contents = {}
file_lines = {}
for p in all_java:
    try:
        c = open(p, encoding="utf-8").read()
        file_contents[p] = c
        file_lines[p] = c.split(chr(10))
    except:
        pass

def extract_interface_methods(content):
    methods = []
    for i, line in enumerate(content.split(chr(10))):
        stripped = line.strip()
        if not stripped.endswith(";"):
            continue
        body = stripped[:-1].strip()
        m = re.match(r"^(public\s+)?(\w[\w<>\.\[\],\s]+?)\s+(\w+)\s*\(([^)]*)\)\s*(?:throws\s+[\w,\.\s]+)?$", body)
        if not m:
            continue
        ret = m.group(2).strip()
        name = m.group(3)
        params_raw = m.group(4).strip()
        if name in ("if","else","for","while","switch","return","class","interface","enum","new","try","catch","finally"):
            continue
        if ret in ("public","static","final","abstract","default","synchronized","native","transient","volatile"):
            continue
        methods.append({"name":name,"return_type":ret,"params":params_raw,"line":i+1,"signature":f"{ret} {name}({params_raw})"})
    return methods

print("extraction ok")
