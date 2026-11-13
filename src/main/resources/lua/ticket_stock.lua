-- KEYS[i]  = key tồn kho của loại vé thứ i, dạng {event:<eventId>}:ticket:<ticketId>:available
-- ARGV[i]  = số lượng muốn giữ cho đúng loại vé ở KEYS[i] (cùng thứ tự với KEYS)
-- Trả về: "OK" nếu giữ thành công cho TẤT CẢ loại vé
--         "MISS" nếu có key chưa có trong cache (Java sẽ warm-up rồi gọi lại)
--         "1", "2", ... là chỉ số (1-based) của loại vé đầu tiên không đủ vé
local n = #KEYS
local available = {}

for i = 1, n do
  local v = redis.call('GET', KEYS[i])
  if v == false then
    return 'MISS'
  end
  available[i] = tonumber(v)
end

for i = 1, n do
  if available[i] < tonumber(ARGV[i]) then
    return tostring(i)
  end
end

for i = 1, n do
  redis.call('DECRBY', KEYS[i], ARGV[i])
end

return 'OK'