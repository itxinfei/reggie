#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""Reggie 门店 PC 打印代理主程序

流程：读取 config.json -> 注册/加载终端 token -> 定时心跳并拉取打印任务
-> 后台打印线程调用本地打印机 -> 回执后端任务状态。

后端契约（见 PrinterAgentController）：
    POST {server}/printer/agent/register            注册，返回 {terminalId, token, status}
    POST {server}/printer/agent/heartbeat           心跳+拉任务，请求头 X-Terminal-Code/X-Terminal-Token
    POST {server}/printer/agent/task/{id}/callback  打印回执，body {success, errorMsg}
统一响应 R：code=1 成功，data 为数据体；鉴权失败返回 HTTP 422。

运行：python printer_agent.py         常驻运行
      python printer_agent.py --once  调试：执行一次注册+心跳后退出
"""

import argparse
import json
import logging
import logging.handlers
import os
import queue
import sys
import threading
import time
import uuid

import requests

from escpos_printer import EscPosPrinter

VERSION = "1.0.0"
REQUEST_TIMEOUT = 15


def _app_dir():
    """程序主目录：PyInstaller 打包后为 exe 所在目录，开发时为脚本所在目录。
    勿用 __file__ 直接取目录——onefile 模式下它指向临时解压目录 _MEIxxx。"""
    if getattr(sys, "frozen", False):
        return os.path.dirname(os.path.abspath(sys.executable))
    return os.path.dirname(os.path.abspath(__file__))


BASE_DIR = _app_dir()
CONFIG_FILE = os.path.join(BASE_DIR, "config.json")
TOKEN_FILE = os.path.join(BASE_DIR, "token.json")
LOG_FILE = os.path.join(BASE_DIR, "printer-agent.log")

log = logging.getLogger("printer-agent")


class ApiError(Exception):
    """后端接口错误：HTTP 状态码 + R 响应体"""

    def __init__(self, message, status_code=None, body=None):
        super(ApiError, self).__init__(message)
        self.status_code = status_code
        self.body = body or {}

    @property
    def is_auth_error(self):
        """鉴权失败（终端未注册 / token 失效）需要重新注册"""
        msg = str(self.body.get("msg") or "") + " " + str(self)
        return ("鉴权" in msg) or ("未注册" in msg) or self.status_code in (401, 403)


class Agent(object):
    """打印代理主体：注册 -> 心跳拉任务 -> 后台打印 -> 回执"""

    def __init__(self, cfg):
        self.cfg = cfg
        self.server = str(cfg.get("server_url") or "").rstrip("/")
        self.store_code = str(cfg.get("store_code") or "").strip()
        if not self.server or not self.store_code:
            raise ValueError("config.json 缺少 server_url 或 store_code")
        self.terminal_code = self._load_or_create_code()
        self.terminal_id = None
        self.token = None
        self.printer = None
        self.task_queue = queue.Queue()

    # ------------------------------------------------------------------ #
    # token 持久化
    # ------------------------------------------------------------------ #
    def _load_token(self):
        try:
            with open(TOKEN_FILE, "r", encoding="utf-8") as fh:
                return json.load(fh) or {}
        except (IOError, ValueError):
            return {}

    def _save_token(self):
        with open(TOKEN_FILE, "w", encoding="utf-8") as fh:
            json.dump({
                "terminalCode": self.terminal_code,
                "terminalId": self.terminal_id,
                "token": self.token,
                "storeCode": self.store_code,
            }, fh, ensure_ascii=False, indent=2)

    def _reset_token(self):
        self.token = None
        self.terminal_id = None
        try:
            os.remove(TOKEN_FILE)
        except OSError:
            pass

    def _load_or_create_code(self):
        data = self._load_token()
        if data.get("terminalCode"):
            return data["terminalCode"]
        return "T-" + uuid.uuid4().hex[:12].upper()

    # ------------------------------------------------------------------ #
    # 注册
    # ------------------------------------------------------------------ #
    def ensure_registered(self):
        """无有效 token 时调用 register；已注册则复用缓存 token"""
        cached = self._load_token()
        if cached.get("token") and cached.get("terminalCode") == self.terminal_code:
            self.token = cached["token"]
            self.terminal_id = cached.get("terminalId")
            log.info("使用已缓存终端凭据 code=%s id=%s", self.terminal_code, self.terminal_id)
            return

        payload = {
            "storeCode": self.store_code,
            "terminalCode": self.terminal_code,
            "name": str(self.cfg.get("terminal_name") or self.terminal_code),
            "printerName": str(self.cfg.get("printer_name") or ""),
            "paperSize": str(self.cfg.get("paper_size") or "80mm"),
            "clientVersion": VERSION,
        }
        body = self._post("/printer/agent/register", json=payload)
        data = body.get("data") or {}
        self.token = data.get("token")
        self.terminal_id = data.get("terminalId")
        self._save_token()
        if data.get("status") == 0:
            log.warning("终端已注册但处于停用状态：请在后台「打印终端」页启用，否则不会派发任务")
        log.info("注册成功 code=%s terminalId=%s", self.terminal_code, self.terminal_id)

    # ------------------------------------------------------------------ #
    # HTTP
    # ------------------------------------------------------------------ #
    def _post(self, path, **kwargs):
        url = self.server + path
        resp = requests.post(url, timeout=REQUEST_TIMEOUT, **kwargs)
        if resp.status_code >= 400:
            raise ApiError("HTTP %s: %s" % (resp.status_code, resp.text[:200]),
                           resp.status_code, self._safe_body(resp))
        try:
            body = resp.json()
        except ValueError:
            raise ApiError("响应非 JSON: %s" % resp.text[:200])
        if body.get("code") != 1:
            raise ApiError(str(body.get("msg") or "业务失败"), resp.status_code, body)
        return body

    @staticmethod
    def _safe_body(resp):
        try:
            return resp.json()
        except ValueError:
            return {}

    def _auth_headers(self):
        return {"X-Terminal-Code": self.terminal_code, "X-Terminal-Token": self.token or ""}

    # ------------------------------------------------------------------ #
    # 心跳 / 回执
    # ------------------------------------------------------------------ #
    def heartbeat(self):
        params = {"version": VERSION}
        if self.cfg.get("printer_name"):
            params["printerName"] = str(self.cfg["printer_name"])
        body = self._post("/printer/agent/heartbeat", headers=self._auth_headers(), params=params)
        tasks = body.get("data") or []
        for task in tasks:
            self.task_queue.put(task)
        if tasks:
            log.info("心跳拉取任务 %d 条", len(tasks))

    def _callback(self, task_id, success, error_msg=""):
        """打印回执；回执请求失败仅记日志（任务超时后由后端回收重派）"""
        try:
            payload = {"success": success, "errorMsg": error_msg}
            self._post("/printer/agent/task/%s/callback" % task_id,
                       headers=self._auth_headers(), json=payload)
        except Exception as exc:
            log.error("回执失败 task=%s：%s", task_id, exc)

    # ------------------------------------------------------------------ #
    # 后台打印线程
    # ------------------------------------------------------------------ #
    def _worker(self):
        while True:
            task = self.task_queue.get()
            task_id = task.get("id")
            try:
                lines = json.loads(task.get("content") or "[]")
                self.printer.print_lines(lines)
                self._callback(task_id, True)
                log.info("任务 %s 打印成功", task_id)
            except Exception as exc:
                log.error("任务 %s 打印失败：%s", task_id, exc)
                self._callback(task_id, False, str(exc)[:300])
            finally:
                self.task_queue.task_done()

    # ------------------------------------------------------------------ #
    # 主循环
    # ------------------------------------------------------------------ #
    def run(self):
        self.ensure_registered()
        self.printer = EscPosPrinter(
            str(self.cfg.get("printer_name") or ""),
            str(self.cfg.get("paper_size") or "80mm"))
        threading.Thread(target=self._worker, name="print-worker", daemon=True).start()
        interval = max(1, int(self.cfg.get("poll_interval") or 3))
        log.info("打印代理启动 server=%s store=%s code=%s 轮询=%ss",
                 self.server, self.store_code, self.terminal_code, interval)
        while True:
            try:
                if not self.token:
                    self.ensure_registered()
                self.heartbeat()
            except ApiError as exc:
                if exc.is_auth_error:
                    log.warning("鉴权失败（%s），重新注册", exc)
                    self._reset_token()
                else:
                    log.error("接口错误：%s", exc)
            except requests.RequestException as exc:
                log.error("网络异常（%s），%ss 后重试", exc, interval)
            except Exception:
                log.exception("心跳处理异常")
            time.sleep(interval)


def load_config(path):
    with open(path, "r", encoding="utf-8") as fh:
        return json.load(fh) or {}


def setup_logging(level_name):
    level = getattr(logging, str(level_name).upper(), logging.INFO)
    root = logging.getLogger("printer-agent")
    root.setLevel(level)
    fmt = logging.Formatter("%(asctime)s %(levelname)s [%(threadName)s] %(message)s")
    console = logging.StreamHandler()
    console.setFormatter(fmt)
    file_handler = logging.handlers.RotatingFileHandler(
        LOG_FILE, maxBytes=5 * 1024 * 1024, backupCount=3, encoding="utf-8")
    file_handler.setFormatter(fmt)
    root.addHandler(console)
    root.addHandler(file_handler)


def main():
    parser = argparse.ArgumentParser(description="Reggie 门店 PC 打印代理")
    parser.add_argument("--config", default=CONFIG_FILE, help="配置文件路径")
    parser.add_argument("--once", action="store_true", help="调试：单次注册+心跳后退出")
    args = parser.parse_args()

    cfg = load_config(args.config)
    setup_logging(cfg.get("log_level", "INFO"))
    agent = Agent(cfg)
    try:
        if args.once:
            agent.ensure_registered()
            agent.heartbeat()
            log.info("单次执行完成，本地待打印任务数=%d", agent.task_queue.qsize())
            return
        agent.run()
    except KeyboardInterrupt:
        log.info("收到中断，打印代理退出")
    except Exception:
        log.exception("打印代理启动失败，请检查 config.json 与后端连接")


if __name__ == "__main__":
    main()
