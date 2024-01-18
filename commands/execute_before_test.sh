#!/bin/bash

echo "[mysql] Starting"
# Start MySQL in the background.
mysqld --initialize-insecure && mysqld --user=root --init-file=<(echo "ALTER USER 'root'@'localhost' IDENTIFIED BY 'root';") --console &

# Wait for MySQL to be ready.
echo "Checking DB connection ..."
while ! mysqladmin ping --silent &> /dev/null; do
    echo "No ping, wait 1s"
    sleep 1
done

# Wait for MySQL to accept connections.
while ! mysql -u root -proot -e "SELECT 1" &> /dev/null; do
    echo "No select allowed, wait 1s"
    sleep 1
done
echo "DB is up ..."

echo "applying init SQLs"
cat /mysql-init/*.sql
cat /mysql-init/*.sql | mysql -u root -proot

echo "applying migrations"
echo "CREATE DATABASE IF NOT EXISTS ${DATABASE_NAME}; USE ${DATABASE_NAME}; " | cat - /mysql-migrations/*.sql
for file in $(ls /mysql-migrations/*.sql); do
    echo "CREATE DATABASE IF NOT EXISTS ${DATABASE_NAME}; USE ${DATABASE_NAME}; " | cat - $file | mysql -f -u root -proot &> /dev/null
done;
