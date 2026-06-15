
local _M = {}

function _M.format(short_code, ip)
    local cjson = require "cjson"
    local log_data = {
        time = ngx.localtime(),
        code = short_code,
        ip = ip,
        user_agent = ngx.var.http_user_agent
    }
    return cjson.encode(log_data)
end

return _M
