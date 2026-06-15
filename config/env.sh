
#!/bin/bash

export REDIS_HOST="localhost"
export REDIS_PORT="6379"
export REDIS_PASSWORD=""

export MYSQL_HOST="localhost"
export MYSQL_PORT="3306"
export MYSQL_USER="admin"
export MYSQL_PASSWORD="password"
export MYSQL_DB="db_shortlink"

export KAFKA_HOST="localhost"
export KAFKA_PORT="9092"

export APP_PORT="8080"
export APP_ENV="dev"

start_redis() {
    redis-server /etc/redis/redis.conf
}

start_mysql() {
    systemctl start mysqld
}

start_kafka() {
    cd /opt/kafka && bin/kafka-server-start.sh config/server.properties &
}

start_app() {
    java -jar /opt/short-link-service.jar --spring.profiles.active=$APP_ENV &
}

stop_app() {
    pkill -f short-link-service
}
