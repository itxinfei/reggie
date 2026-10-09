-- 限流计数器（固定窗口）：原子 INCR + 首次 EXPIRE
-- KEYS[1] = 限流 key
-- ARGV[1] = 窗口大小（秒）
-- 返回当前窗口内的累计请求数；调用方与阈值比较，超限则拒绝（HTTP 429）。
-- 说明：key 在窗口结束后自动过期，下一窗口从 1 重新计数。
local current = redis.call('INCR', KEYS[1])
if current == 1 then
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[1]))
end
return current
