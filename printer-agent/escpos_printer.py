# -*- coding: utf-8 -*-
"""Reggie 门店 PC 打印代理 - ESC/POS 小票打印引擎

将后端下发的 PrintLine 列表（JSON 数组）渲染为 ESC/POS 字节流，
通过 win32print 以 RAW 数据格式直写 Windows 本地小票打印机。

- 中文以 GBK 编码（Windows 小票机驱动默认代码页 936）
- 支持 80mm / 58mm 纸张，手动按显示宽度补空格对齐（兼容驱动对齐实现不一）
- QR 类型：text 为 http(s) 链接时打印二维码图片（需安装 qrcode + pillow），
  否则跳过（后端占位符如 QR_PLACEHOLDER 不打印）；BARCODE 按 CODE128 打印

用法：:

    printer = EscPosPrinter("EPSON TM-T88V", "80mm")
    printer.print_lines(lines)   # lines 为 dict 列表
"""

import logging
import unicodedata

ESC = b"\x1b"
GS = b"\x1d"

# ESC/POS 对齐：0=左 1=中 2=右
ALIGN_MAP = {"LEFT": 0, "CENTER": 1, "RIGHT": 2}

# 纸张宽度（半角字符数）
PAPER_WIDTH = {"80mm": 32, "58mm": 24}

log = logging.getLogger("printer-agent")


def _display_width(text):
    """显示宽度：全角（东亚宽字符）记 2，半角记 1（与 GBK 字节宽一致）"""
    return sum(2 if unicodedata.east_asian_width(ch) in "WF" else 1 for ch in text)


class EscPosPrinter(object):
    """ESC/POS 打印机封装"""

    def __init__(self, printer_name=None, paper_size="80mm"):
        self.printer_name = printer_name or ""
        self.paper_size = paper_size if paper_size in PAPER_WIDTH else "80mm"
        self.chars_per_line = PAPER_WIDTH[self.paper_size]

    # ------------------------------------------------------------------ #
    # 对外入口
    # ------------------------------------------------------------------ #
    def print_lines(self, lines):
        """打印一组 PrintLine（dict 列表），失败抛异常由调用方回执失败"""
        data = self.build_bytes(lines)
        self._write_raw(data)

    def build_bytes(self, lines):
        """将 PrintLine 渲染为 ESC/POS 字节流（可离线单测）"""
        buf = bytearray()
        buf += ESC + b"@"  # 初始化打印机
        for line in lines or []:
            if not isinstance(line, dict):
                continue
            self._render_line(buf, line)
        buf += GS + b"V" + b"B" + b"\x03"  # 走纸并切纸（留 3 行）
        buf += ESC + b"@"
        return bytes(buf)

    # ------------------------------------------------------------------ #
    # 单行渲染
    # ------------------------------------------------------------------ #
    def _render_line(self, buf, line):
        line_type = str(line.get("type") or "TEXT").upper()
        text = str(line.get("text") or "")
        align = str(line.get("align") or "LEFT").upper()
        bold = bool(line.get("bold"))
        font_size = int(line.get("fontSize") or 0)

        if line_type == "DIVIDER":
            self._feed_text(buf, "-" * self.chars_per_line, "LEFT", False, 0)
            return

        if line_type == "QR":
            # 仅真实链接打印二维码；占位符直接跳过
            if self._feed_qr(buf, text):
                return
            if text and text.upper().startswith("QR_"):
                return
            self._feed_text(buf, text, align, bold, font_size)
            return

        if line_type == "BARCODE":
            if self._feed_barcode(buf, text):
                return
            self._feed_text(buf, text, align, bold, font_size)
            return

        # TEXT / TABLE：按显示宽度折行后逐行输出
        for row in self._wrap(text):
            self._feed_text(buf, row, align, bold, font_size)

    def _feed_text(self, buf, text, align, bold, font_size):
        """输出一行文本（带缩放 / 加粗 / 对齐补空格）"""
        width_factor = {0: 1, 1: 1, 2: 2, 3: 4}.get(font_size, 1)
        scale = {0: 0x00, 1: 0x01, 2: 0x11, 3: 0x33}.get(font_size, 0x00)
        limit = max(1, self.chars_per_line // width_factor)

        buf += ESC + b"a" + bytes([ALIGN_MAP.get(align, 0)])
        buf += ESC + b"E" + (b"\x01" if bold else b"\x00")
        buf += GS + b"!" + bytes([scale])
        buf += self._pad(text, limit, align).encode("gbk", errors="replace")
        buf += b"\n"

    def _feed_qr(self, buf, text):
        """二维码图片打印（GS v 0 位图）。依赖 qrcode+pillow，失败返回 False 降级文本"""
        if not text.lower().startswith(("http://", "https://")):
            return False
        try:
            import qrcode
            from PIL import Image
        except ImportError:
            log.warning("未安装 qrcode/pillow，二维码行降级为文本")
            return False
        qr = qrcode.QRCode(border=1, error_correction=qrcode.constants.ERROR_CORRECT_M)
        qr.add_data(text)
        qr.make(fit=True)
        img = qr.make_image(fill_color="black", back_color="white").convert("1")
        # 放大到小票纸宽（80mm≈384px / 58mm≈288px）
        target = 384 if self.paper_size == "80mm" else 288
        scale = max(1, target // img.width)
        img = img.resize((img.width * scale, img.height * scale), Image.NEAREST)
        self._feed_bitmap(buf, img)
        buf += b"\n"
        return True

    def _feed_bitmap(self, buf, img):
        """按 GS v 0 指令打印 1 位位图，高度超过 255 行自动分块"""
        width, height = img.size
        rows_per_block = 255
        pixels = img.load()
        for y0 in range(0, height, rows_per_block):
            block = img.crop((0, y0, width, min(y0 + rows_per_block, height)))
            b_w, b_h = block.size
            bytes_per_row = (b_w + 7) // 8
            buf += GS + b"v" + b"0" + b"\x00"
            buf += bytes([bytes_per_row & 0xFF, (bytes_per_row >> 8) & 0xFF])
            buf += bytes([b_h & 0xFF, (b_h >> 8) & 0xFF])
            for y in range(b_h):
                row = bytearray()
                for x in range(0, b_w, 8):
                    byte = 0
                    for bit in range(8):
                        if x + bit < b_w and block.getpixel((x + bit, y)) == 0:
                            byte |= 1 << (7 - bit)
                    row.append(byte)
                buf += bytes(row)

    def _feed_barcode(self, buf, text):
        """CODE128 条码（GS k 69 带校验）。仅支持 ASCII 内容，失败返回 False"""
        raw = text.encode("ascii", errors="ignore")
        if not raw or len(raw) > 80:
            return False
        buf += GS + b"H" + b"\x02"  # HRI 字符打印在条码下方
        buf += GS + b"h" + b"\x50"  # 条码高度 80 点
        buf += GS + b"k" + b"\x45" + bytes([len(raw)]) + raw  # CODE128 自动校验
        buf += b"\n"
        return True

    # ------------------------------------------------------------------ #
    # 文本排版辅助
    # ------------------------------------------------------------------ #
    def _pad(self, text, limit, align):
        """按显示宽度补空格：左对齐不补、居中两侧均分、右对齐补前"""
        width = _display_width(text)
        if width >= limit:
            return text
        pad = limit - width
        if align == "CENTER":
            left = pad // 2
            return " " * left + text + " " * (pad - left)
        if align == "RIGHT":
            return " " * pad + text
        return text

    def _wrap(self, text, limit=None):
        """按显示宽度折行（兼容显式换行符）"""
        limit = limit or self.chars_per_line
        result = []
        for raw in text.split("\n"):
            line = ""
            for ch in raw:
                if _display_width(line + ch) > limit and line:
                    result.append(line)
                    line = ch
                else:
                    line += ch
            result.append(line)
        return result

    # ------------------------------------------------------------------ #
    # 打印机直写
    # ------------------------------------------------------------------ #
    def _write_raw(self, data):
        """通过 win32print 以 RAW 数据直写本地打印机"""
        try:
            import win32print
        except ImportError:
            raise RuntimeError("缺少 pywin32，请先执行 install.bat 安装依赖")
        printer_name = self.printer_name or self._default_printer()
        handle = win32print.OpenPrinter(printer_name)
        try:
            win32print.StartDocPrinter(handle, 1, ("reggie-print", None, "RAW"))
            try:
                win32print.StartPagePrinter(handle)
                win32print.WritePrinter(handle, data)
                win32print.EndPagePrinter(handle)
            finally:
                win32print.EndDocPrinter(handle)
        finally:
            win32print.ClosePrinter(handle)
        log.info("已打印 %d 字节到打印机 [%s]", len(data), printer_name)

    @staticmethod
    def _default_printer():
        import win32print
        return win32print.GetDefaultPrinter()

    @staticmethod
    def list_printers():
        """列出本机已安装打印机名称（供配置 config.json 使用）"""
        try:
            import win32print
            return [p[2] for p in win32print.EnumPrinters(2)]
        except ImportError:
            return []


if __name__ == "__main__":
    # 调试：python escpos_printer.py 打印测试小票
    logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(message)s")
    print("本机打印机列表：")
    for name in EscPosPrinter.list_printers():
        print("  -", name)
    samples = [
        {"text": "=== 测试打印 ===", "fontSize": 2, "bold": True, "align": "CENTER", "type": "TEXT"},
        {"text": "打印代理工作正常", "fontSize": 0, "bold": False, "align": "CENTER", "type": "TEXT"},
        {"text": "", "fontSize": 0, "bold": False, "align": "LEFT", "type": "DIVIDER"},
        {"text": "中文对齐：左侧内容", "fontSize": 0, "bold": False, "align": "LEFT", "type": "TEXT"},
        {"text": "合计: 88.50", "fontSize": 1, "bold": True, "align": "RIGHT", "type": "TEXT"},
    ]
    EscPosPrinter(None, "80mm").print_lines(samples)
