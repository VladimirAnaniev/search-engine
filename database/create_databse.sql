CREATE USER 'essential'@'localhost' IDENTIFIED BY 'trustno1';
CREATE DATABASE `search_engine` CHARACTER SET utf8 COLLATE utf8_bin;
GRANT ALL ON `search_engine`.* TO 'essential'@'localhost';
FLUSH PRIVILEGES;

# Confirm the database has been created and can be accessed:
# mysql -u essential search_engine -p
