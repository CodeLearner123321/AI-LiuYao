-- 这是一段时间窗口的lua脚本，在一定的时间窗口内，数据量不能超过最大数。如果超过返回0如果没超过将该数据放入并返回1
-- KEYS[1]  限流 key (zset)
-- ARGV[1]  当前时间戳（毫秒）
-- ARGV[2]  窗口大小（毫秒）
-- ARGV[3]  最大请求数
-- ARGV[4]  当前请求的唯一 member（例如 UUID）

local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local maxReq = tonumber(ARGV[3])
local member = ARGV[4]

-- 1) 清理窗口外的旧记录
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 2) 统计当前窗口内的数量
local count = redis.call('ZCARD', key)

-- 3) 超过限制则拒绝
if count >= maxReq then
    return 0
end
return 1
