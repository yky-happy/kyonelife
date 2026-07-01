-- 访客事件增加 IP 归属地（省 市），由后端 ip2region 离线库解析后写入
ALTER TABLE event_log
    ADD COLUMN ip_location VARCHAR(64) DEFAULT NULL COMMENT 'IP归属地(省 市)' AFTER ip;
