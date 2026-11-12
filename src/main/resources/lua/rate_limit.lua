-- KEYS[1]: key, vd rl:create-order:<userId>
-- ARGV[1]: cửa sổ (giây)   ARGV[2]: tối đa   ARGV[3]: mã riêng của request này
-- Trả về 0: cho qua. Số > 0: số giây phải chờ

local key    = KEYS[1]
local window = tonumber(ARGV[1]) * 1000          -- đổi ra mili giây
local limit  = tonumber(ARGV[2])

local t   = redis.call('TIME')                   -- giờ của Redis: {giây, micro giây}
local now = t[1] * 1000 + math.floor(t[2] / 1000)

-- 1. Bỏ các request đã cũ hơn 60 giây
redis.call('ZREMRANGEBYSCORE', key, 0, now - window)

-- 2. Đếm số request còn trong 60 giây gần nhất
local count = redis.call('ZCARD', key)

if count >= limit then
  -- Request cũ nhất rời khỏi cửa sổ thì có chỗ trống
  local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
  return math.ceil((tonumber(oldest[2]) + window - now) / 1000)
end

-- 3. Ghi request này, score = thời điểm
redis.call('ZADD', key, now, ARGV[3])
redis.call('PEXPIRE', key, window)
return 0