
#!/bin/bash

mysql -uroot -p db_shortlink << EOF
delete from short_link where status != 1 or expire_at < now() limit 1000;
EOF

echo "过期数据清理完成"
