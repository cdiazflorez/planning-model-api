create user 'admin'@'%' identified by 'admin';
grant all privileges on *.* to 'admin'@'%' with grant option;
flush privileges;
