-- 限流Lua脚本：原子性的increment+expire操作
-- KEYS[1] = 限流Key
-- ARGV[1] = 过期时间（秒）
-- 返回值 = increment后的计数值

local key = KEYS[1]
local expire = tonumber(ARGV[1])

-- 原子性执行increment
local count = redis.call('INCR', key)

-- 如果是第一次设置，添加过期时间
if count == 1 then
    redis.call('EXPIRE', key, expire)
end

return count