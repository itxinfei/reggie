#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""评估 git HEAD 版本的乱码可逆性"""
import subprocess, sys, re

sys.stdout.reconfigure(encoding='utf-8', errors='replace')

MARKS = "涓鍟鏂閿鎴寮绉璇緭閫缃椤鏈鐨鍒鐢鍦鏄绠鐞鍝鍏椋缂哄潡杩愯溅瀵煎嚭楄〃褰撳墠妯″瀷嗘瀽掗槦涘簲嗙敤鎴锋暟鎹圭洰閰嶇疆鍨嬶璧疯绛夌瓑閺堝鐔ヨ閸涖剙宕熼幋鐘虹叀鏌?€氬挋楠屾嫹鐡掑懏妞?閻熸岸"
M = set(MARKS)

def git(*a):
    r = subprocess.run(["git", "--no-pager"] + list(a), capture_output=True, cwd=r"d:\MyCode\reggie")
    return r.stdout

def analyze(data):
    try:
        s = data.decode('utf-8')
    except Exception:
        return "NOT-UTF8", 0, 0
    # 统计乱码片段
    segs = re.findall(r'[涓鍟鏂閿鎴寮绉璇緭閫缃椤鏈鐨鍒锟]{2,}', s)
    ok_cnt = 0
    bad = []
    for seg in segs:
        try:
            rev = seg.encode('gbk').decode('utf-8')
            if any('\u4e00' <= c <= '\u9fff' for c in rev):
                ok_cnt += 1
            else:
                bad.append(seg)
        except Exception:
            bad.append(seg)
    return s, ok_cnt, len(segs), bad[:5]

files = ["dining/qrcode-order.html","dining/queue-list.html","export/index.html",
"food/list.html","inventory/material-warning.html","inventory/smart-replenish.html",
"member-center/coupon-detail.html","member-center/coupon-expiring.html","member-center/coupon-issue.html",
"report/daily.html","retention/retention.html","store/dashboard.html","store/list.html","urgency/urgency.html"]

for f in files:
    head = git("show", "HEAD:src/main/resources/backend/page/" + f)
    try:
        s = head.decode('utf-8')
    except Exception:
        print(f"  {f}: 非UTF-8")
        continue
    segs = re.findall(r'[涓鍟鏂閿鎴寮绉璇緭閫缃椤鏈鐨鍒锟]{2,}', s)
    ok_cnt = 0
    bad = []
    for seg in segs:
        try:
            rev = seg.encode('gbk').decode('utf-8')
            if any('\u4e00' <= c <= '\u9fff' for c in rev):
                ok_cnt += 1
            else:
                bad.append(seg)
        except Exception:
            bad.append(seg)
    print(f"{f}: 乱码段={len(segs)} 可逆={ok_cnt} 不可逆={len(bad)}")
    for b in bad[:6]:
        print(f"    X {b}")
