# Reggie 门店 PC 打印代理

门店 PC 上的本地打印客户端。安装在**打印机所在的 Windows 电脑**（通常为收银台主机），
心跳轮询后端拉取打印任务，调用 **Windows 本地小票打印机**静默出票，无需打印弹窗。

对应后端模块：`PrinterAgentController`（`/printer/agent/register|heartbeat|task/{id}/callback`）。

## 目录结构

```
printer-agent/
├── printer_agent.py    # 主程序（注册/心跳/拉任务/打印线程/回执）
├── escpos_printer.py   # ESC/POS 小票打印引擎（GBK 中文 / 对齐 / 放大 / QR）
├── config.json         # 运行配置（见下）
├── requirements.txt    # Python 依赖
├── install.bat         # 一键安装依赖
├── start.bat           # 前台启动
├── README.md
└── token.json          # 运行后自动生成（终端码+token 持久化，勿删）
```

## 快速开始（exe 版，门店推荐）

门店电脑 **无需安装 Python**，直接使用打包好的绿色软件：

1. 将 `dist/` 目录整个复制到门店电脑（`ReggiePrintAgent.exe` + `config.json` + 运维脚本）；
2. 用记事本修改 `config.json`（`server_url` = 后端地址，`store_code` = 门店编码；`printer_name` 留空用默认打印机）；
3. 双击 `ReggiePrintAgent.exe` —— **无窗口后台静默运行**（首次启动自解压 1~3 秒属正常，请耐心等待）；
4. 打开后台 **打印终端** 页，将新注册终端设为 **启用**（新终端默认停用）。

**运维脚本**（均在 `dist/` 目录）：

| 脚本 | 作用 |
| --- | --- |
| `安装开机自启.bat` | 注册开机自启，门店开机后自动后台运行（推荐） |
| `卸载开机自启.bat` | 取消开机自启 |
| `停止代理.bat` | 停止后台运行的代理 |

**如何确认在运行**：查看同目录 `printer-agent.log`，末尾有 `注册成功` / `心跳` 日志；或在任务管理器中能看到 `ReggiePrintAgent.exe` 进程。

> exe 为 PyInstaller 单文件打包（约 33MB，含 requests / pywin32 / 二维码打印全部依赖，内置应用图标）；
> `token.json`（终端凭据）、`printer-agent.log` 生成在 exe 同目录。
> **注意**：若本目录是从开发机复制而来且自带 `token.json`，请先删除该文件再运行——否则门店机会复用开发机的终端身份（每台机器应使用各自注册的终端）。
>
> 重新打包（需 Python + PyInstaller）：`python -m PyInstaller --onefile --clean --noupx --windowed --icon assets/reggie-agent.ico --name ReggiePrintAgent printer_agent.py`

## 安装（源码版，开发调试）

1. 安装 **Python 3.8+**（勾选 *Add Python to PATH*）；
2. 双击 `install.bat` 安装依赖（requests + pywin32；qrcode/pillow 为可选，装了可打印二维码）；
3. 修改 `config.json`（见下）；
4. 双击 `start.bat` 启动；
5. 打开后台 **打印终端** 页，将新注册终端设为 **启用**（新终端默认停用，启用后才会派发任务）。

## 配置说明（config.json）

| 配置项 | 说明 |
|---|---|
| `server_url` | 后端地址，如 `http://192.168.1.10:8080`（**不要**加 `/api` 后缀） |
| `store_code` | 本机对应门店的 `store_code`（后台门店管理可查） |
| `terminal_name` | 终端名称，便于后台识别，如 `收银台-01` |
| `printer_name` | Windows 打印机名，**留空 = 使用系统默认打印机**。可在打印机"属性"里查看名称 |
| `paper_size` | `80mm` 或 `58mm` |
| `poll_interval` | 心跳拉任务间隔（秒），默认 3 |
| `log_level` | `INFO` / `DEBUG` / `WARNING` |

## 验证

```bat
:: 列出本机打印机 + 打印一张测试小票
python escpos_printer.py

:: 单次注册+心跳调试（后端可达性、token 是否有效）
python printer_agent.py --once
```

后台 **打印终端** 页点「测试打印」也可派发一条 TEST 任务验证链路。

## 常见问题

- **不打印**：① 后台终端未启用（日志有 "处于停用状态" 提示）；② `printer_name` 填错（留空用默认打印机最稳）；③ 后端地址不通（看 `printer-agent.log` 网络异常）。
- **中文乱码**：代理按 GBK 编码输出（Windows 小票机驱动默认 936）。若乱码，确认打印机驱动/固件默认编码为 GBK/GB2312，或联系打印机厂商设置。
- **鉴权失败自动重连**：token 失效 / 后台删除终端时，代理会自动重新注册（日志见 "重新注册"）。
- **代理掉线不丢单**：已领取但未回执的任务 60 秒后被后端回收重新派发；回执失败同样会重派，不会丢票。
- **网络打印机**：`printer_name` 填网络打印机的共享名即可（如 `\\192.168.1.5\EPSON`），走 Windows 打印机队列。

## 开机自启动（可选）

Win+R 输入 `shell:startup`，把 `start.bat` 的快捷方式放进去即可开机自动运行。
正式环境建议改用 NSSM 注册为 Windows 服务（`nssm install PrinterAgent ...`）实现免窗口运行与自动拉起。
