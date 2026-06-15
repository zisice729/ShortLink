
local redis = require "resty.redis"
local red = redis:new()

local redis_host = "127.0.0.1"
local redis_port = 6379
local timeout = 100

red:set_timeout(timeout)
local ok, err = red:connect(redis_host, redis_port)
if not ok then
    ngx.location.capture("/fallback/link")
    return
end

local short_code = ngx.var[1]
local bloom_key = "short_bloom"
local short_key = "short:" .. short_code

local exists, err = red:bf_exists(bloom_key, short_code)
if exists == 0 then
    ngx.status = 404
    ngx.say("Not Found")
    return
end

local res, err = red:hgetall(short_key)
if not res or #res == 0 then
    ngx.location.capture("/fallback/link")
    return
end

local url = nil
local status = 0
local expire_at = 0
for i = 1, #res, 2 do
    if res[i] == "long_url" then
        url = res[i+1]
    elseif res[i] == "status" then
        status = tonumber(res[i+1])
    elseif res[i] == "expire_at" then
        expire_at = tonumber(res[i+1])
    end
end

local now = ngx.time()
if status ~= 1 or now > expire_at then
    ngx.status = 410
    ngx.say("Gone")
    return
end

ngx.redirect(url, ngx.HTTP_MOVED_TEMPORARILY)

red:set_keepalive(10000, 100)
