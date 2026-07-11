import re
path = r'D:\MyCode\reggie\src\main\java\com\reggie\module\ai\service\impl\AiProviderConfigServiceImpl.java'
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the specific line that builds testUrl without normalization
old_line = '        String testUrl = config.getBaseUrl() + "/chat/completions";'
new_line = '        String normalizedUrl = config.getBaseUrl() == null ? "" : config.getBaseUrl().replaceAll("/+$", "");\n        String testUrl = normalizedUrl + "/chat/completions";'

if old_line not in content:
    print('OLD LINE NOT FOUND')
    start = content.find('testUrl = config.getBaseUrl()')
    print(repr(content[start:start+200]))
else:
    content = content.replace(old_line, new_line, 1)
    # Also improve the 404 error message
    content = content.replace(
        'return "FAIL: API地址不存在 (HTTP 404)，请检查 baseUrl 配置";',
        'return "FAIL: API地址不存在 (HTTP 404)，请求路径：" + testUrl + "，请检查 baseUrl 末尾是否有多余斜杠，和模型名称是否正确";',
        1
    )
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print('DONE')

