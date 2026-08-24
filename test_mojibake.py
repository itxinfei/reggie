import sys
sys.stdout.reconfigure(encoding='utf-8', errors='replace')

# 测试 mojibake 恢复: 乱码字符串 -> GBK编码 -> UTF-8解码
samples = [
    "璇疯緭鍏ヨ彍鍝佸悕绉",
    "涓嬪崟鎴愬姛锛佽鍗曞彿锛",
    "閹烘帡妲﹂崣",
    "璁㈠崟鏁版嵁瀵煎嚭鎴愬姛锛屾枃浠跺凡寮€濮嬩笅杞",
    "渚涘簲鍟",
    "鐢ㄥ埜鐘舵€",
    "浼樻儬鍒告ā鏉",
    "褰撳墠閫変腑鐨勬ā鏉",
    "杩澶",
    "瀵绗︽稉",
    "鍔犺浇澶辫触",
    "缃戠粶寮傚父",
    "璇疯緭鍏ラ棬搴楀悕绉",
    "鐎诡偂绻氭ウ鈥茬",
    "妤硅锕╃挒鍡氬帿",
    "缁ｆ娊銈",
    "閸欐儼浜ら惇",
    "娴滅儤鏆",
    "閻樿埖鈧",
    "閹靛婧€閸",
]
for s in samples:
    try:
        raw = s.encode('gbk')
        restored = raw.decode('utf-8')
        print(f"{s!r} -> {restored!r}")
    except Exception as e:
        print(f"{s!r} -> ERROR: {e}")
