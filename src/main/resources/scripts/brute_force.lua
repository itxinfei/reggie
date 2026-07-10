-- 暴力破解防护Lua脚本：原子性操作
-- KEYS[1] = 失败计数Key (login:failure:xxx)
-- KEYS[2] = 锁定Key (login:locked:xxx)
-- ARGV[1] = 过期时间（秒）
-- ARGV[2] = 最大失败次数阈值
-- 返回值 = 当前失败次数（如果已锁定则返回-1）

local failureKey = KEYS[1]
local lockedKey = KEYS[2]
local expire = tonumber(ARGV[1])
local maxAttempts = tonumber(ARGV[2])

-- 先检查是否已被锁定
local locked = redis.call('EXISTS', lockedKey)
if locked == 1 then
    -- 已锁定，清理失败计数并返回-1
    redis.call('DEL', failureKey)
    return -1
end

-- 原子性执行increment
local count = redis.call('INCR', failureKey)

-- 如果是第一次设置，添加过期时间
if count == 1 then
    redis.call('EXPIRE', failureKey, expire)
end

-- 检查是否达到锁定阈值
if count >= maxAttempts then
    -- 原子性设置锁定并清理计数
    redis.call('SET', lockedKey, '1', 'EX', expire)
    redis.call('DEL', failureKey)
end

return count