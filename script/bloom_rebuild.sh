
#!/bin/bash

redis-cli DEL short_bloom
redis-cli BF.RESERVE short_bloom 0.001 100000000

mysql -uroot -p db_shortlink -e "select short_code from short_link where status=1 and expire_at > now()" \
| awk '{print $1}' \
| xargs -I {} redis-cli BF.ADD short_bloom {}

echo "布隆过滤器重建完成"
