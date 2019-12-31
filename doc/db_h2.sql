-- 菜单
CREATE TABLE sys_menu (
  menu_id bigint NOT NULL AUTO_INCREMENT,
  parent_id bigint ,
  name varchar(50) ,
  url varchar(200) ,
  perms varchar(500) ,
  type int ,
  icon varchar(50) ,
  order_num int ,
  PRIMARY KEY (menu_id)
);

-- 系统用户
CREATE TABLE sys_user (
  user_id bigint NOT NULL AUTO_INCREMENT,
  username varchar(50) NOT NULL ,
  password varchar(100) ,
  salt varchar(20) ,
  email varchar(100) ,
  mobile varchar(100) ,
  status tinyint ,
  create_user_id bigint(20) ,
  create_time datetime ,
  PRIMARY KEY (user_id),
  UNIQUE INDEX (username)
);

-- 系统用户Token
CREATE TABLE sys_user_token (
  user_id bigint(20) NOT NULL,
  token varchar(100) NOT NULL ,
  expire_time datetime DEFAULT NULL ,
  update_time datetime DEFAULT NULL ,
  PRIMARY KEY (user_id),
  UNIQUE KEY token0 (token)
);

-- 角色
CREATE TABLE sys_role (
  role_id bigint NOT NULL AUTO_INCREMENT,
  role_name varchar(100) ,
  remark varchar(100) ,
  create_user_id bigint(20) ,
  create_time datetime ,
  PRIMARY KEY (role_id)
);

-- 用户与角色对应关系
CREATE TABLE sys_user_role (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint ,
  role_id bigint ,
  PRIMARY KEY (id)
);

-- 角色与菜单对应关系
CREATE TABLE sys_role_menu (
  id bigint NOT NULL AUTO_INCREMENT,
  role_id bigint ,
  menu_id bigint ,
  PRIMARY KEY (id)
);

-- 系统配置信息
CREATE TABLE sys_config (
    id bigint NOT NULL AUTO_INCREMENT,
    key varchar(50) ,
    value varchar(2000) ,
    status tinyint DEFAULT 1 ,
    remark varchar(500) ,
    PRIMARY KEY (id),
    UNIQUE INDEX (key)
);

-- 系统日志
CREATE TABLE sys_log (
  id bigint(20) NOT NULL AUTO_INCREMENT,
  username varchar(50) ,
  operation varchar(50) ,
  method varchar(200) ,
  params varchar(5000) ,
  time bigint NOT NULL ,
  ip varchar(64) ,
  create_date datetime ,
  PRIMARY KEY (id)
);

-- 初始数据 
INSERT INTO sys_user (user_id, username, password, salt, email, mobile, status, create_user_id, create_time) VALUES ('1', 'admin', '9ec9750e709431dad22365cabc5c625482e574c74adaebba7dd02f1129e4ce1d', 'YzcmCZNvbXocrsz9dm8e', 'root@renren.io', '13612345678', '1', '1', '2016-11-11 11:11:11');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('1', '0', '系统管理', NULL, NULL, '0', 'fa fa-cog', '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('2', '1', '管理员列表', 'modules/sys/user.html', NULL, '1', 'fa fa-user', '1');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('3', '1', '角色管理', 'modules/sys/role.html', NULL, '1', 'fa fa-user-secret', '2');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('4', '1', '菜单管理', 'modules/sys/menu.html', NULL, '1', 'fa fa-th-list', '3');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('5', '1', 'SQL监控', 'druid/sql.html', NULL, '1', 'fa fa-bug', '4');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('15', '2', '查看', NULL, 'sys:user:list,sys:user:info', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('16', '2', '新增', NULL, 'sys:user:save,sys:role:select', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('17', '2', '修改', NULL, 'sys:user:update,sys:role:select', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('18', '2', '删除', NULL, 'sys:user:delete', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('19', '3', '查看', NULL, 'sys:role:list,sys:role:info', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('20', '3', '新增', NULL, 'sys:role:save,sys:menu:list', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('21', '3', '修改', NULL, 'sys:role:update,sys:menu:list', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('22', '3', '删除', NULL, 'sys:role:delete', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('23', '4', '查看', NULL, 'sys:menu:list,sys:menu:info', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('24', '4', '新增', NULL, 'sys:menu:save,sys:menu:select', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('25', '4', '修改', NULL, 'sys:menu:update,sys:menu:select', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('26', '4', '删除', NULL, 'sys:menu:delete', '2', NULL, '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('27', '1', '参数管理', 'modules/sys/config.html', 'sys:config:list,sys:config:info,sys:config:save,sys:config:update,sys:config:delete', '1', 'fa fa-sun-o', '6');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('29', '1', '系统日志', 'modules/sys/log.html', 'sys:log:list', '1', 'fa fa-file-text-o', '7');

-- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------
-- 性能测试相关SQL -------------------------------------------------------------------------------------------------------------
-- ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------

-- 性能测试菜单
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('31', '0', '压力测试', NULL, NULL, '0', 'fa fa-bolt', '0');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('32', '31', '用例管理', 'modules/test/stressTest.html', 'test:stress', '1', 'fa fa-briefcase', '1');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('33', '31', '脚本文件管理', 'modules/test/stressTestFile.html', 'test:stress', '1', 'fa fa-file-text-o', '2');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('34', '31', '性能测试报告管理', 'modules/test/stressTestReports.html', 'test:stress', '1', 'fa fa-area-chart', '3');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('35', '31', '调试报告管理', 'modules/test/debugTestReports.html', 'test:debug', '1', 'fa fa-area-chart', '4');
INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('36', '31', '分布式节点管理', 'modules/test/stressTestSlave.html', 'test:stress', '1', 'fa fa-cloud', '5');

-- 性能测试用例表
CREATE TABLE test_stress_case (
  case_id bigint NOT NULL AUTO_INCREMENT,
  case_name varchar(50) NOT NULL ,
  project varchar(50) ,
  module varchar(50) ,
  status tinyint NOT NULL DEFAULT 0 ,
  operator varchar(20) ,
  remark varchar(300) ,
  priority int ,
  case_dir varchar(200) ,
  add_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  add_by bigint(20) ,
  update_time timestamp NOT NULL  AS CURRENT_TIMESTAMP ,
  update_by bigint(20) ,
  PRIMARY KEY (case_id),
  UNIQUE INDEX (case_name)
);

-- 性能测试用例文件表
CREATE TABLE test_stress_case_file (
  file_id bigint NOT NULL AUTO_INCREMENT,
  case_id bigint NOT NULL ,
  slave_id bigint ,
  origin_name varchar(200) NOT NULL ,
  file_name varchar(200) ,
  file_md5 varchar(100) ,
  status tinyint NOT NULL DEFAULT 0 ,
  report_status tinyint NOT NULL DEFAULT 0 ,
  webchart_status tinyint NOT NULL DEFAULT 0 ,
  debug_status tinyint NOT NULL DEFAULT 0 ,
  duration int NOT NULL DEFAULT 10800 ,
  add_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  add_by bigint(20) ,
  update_time timestamp NOT NULL  AS CURRENT_TIMESTAMP ,
  update_by bigint(20) ,
  PRIMARY KEY (file_id),
  UNIQUE INDEX (origin_name)
);

-- 性能测试报告文件表
CREATE TABLE test_stress_case_reports (
  report_id bigint NOT NULL AUTO_INCREMENT,
  case_id bigint NOT NULL ,
  file_id bigint NOT NULL ,
  origin_name varchar(200) NOT NULL ,
  report_name varchar(200) NOT NULL ,
  file_size bigint ,
  status tinyint NOT NULL DEFAULT 0 ,
  remark varchar(300) ,
  add_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  add_by bigint(20) ,
  update_time timestamp NOT NULL  AS CURRENT_TIMESTAMP ,
  update_by bigint(20) ,
  PRIMARY KEY (report_id)
);

-- 调试/接口测试报告文件表
CREATE TABLE test_debug_case_reports (
  report_id bigint NOT NULL AUTO_INCREMENT,
  case_id bigint NOT NULL ,
  file_id bigint NOT NULL ,
  origin_name varchar(200) NOT NULL ,
  report_name varchar(200) NOT NULL ,
  file_size bigint ,
  status tinyint NOT NULL DEFAULT 0 ,
  remark varchar(300) ,
  add_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  add_by bigint(20) ,
  update_time timestamp NOT NULL  AS CURRENT_TIMESTAMP ,
  update_by bigint(20) ,
  PRIMARY KEY (report_id)
);

-- 性能测试分布式节点表
CREATE TABLE test_stress_slave (
  slave_id bigint NOT NULL AUTO_INCREMENT,
  slave_name varchar(50) NOT NULL ,
  ip varchar(50) NOT NULL ,
  jmeter_port INT NOT NULL DEFAULT 1099 ,
  user_name varchar(100) ,
  passwd varchar(100) ,
  ssh_port int NOT NULL DEFAULT 22 ,
  home_dir varchar(200) ,
  status tinyint NOT NULL DEFAULT 0 ,
  weight int NOT NULL DEFAULT 100 ,
  add_time timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ,
  add_by bigint(20) ,
  update_time timestamp NOT NULL  AS CURRENT_TIMESTAMP ,
  update_by bigint(20) ,
  PRIMARY KEY (slave_id),
  UNIQUE INDEX (ip)
);

-- 让本机master配置后也可以参与性能测试，默认是禁用master主节点
INSERT INTO test_stress_slave (slave_id, slave_name, ip, jmeter_port, user_name, passwd, ssh_port, home_dir, status, add_time, add_by, update_time, update_by) VALUES ('1', 'LocalHost', '127.0.0.1', '0', NULL, NULL, '22', '', '0', '2018-06-18 18:18:18', NULL, '2018-06-18 18:18:18', NULL);

-- 数据库中配置性能压测配置信息。key不要变。
INSERT INTO sys_config (id, key, value, status, remark) VALUES ('2', 'MASTER_JMETER_HOME_KEY', 'D:\\software\\apache-jmeter-4.0', '1', '本地Jmeter_home绝对路径');
INSERT INTO sys_config (id, key, value, status, remark) VALUES ('3', 'MASTER_JMETER_CASES_HOME_KEY', 'D:\\E\\stressTestCases', '1', '本地保存用例数据的绝对路径，不要随意切换会导致文件找不到错误。');
INSERT INTO sys_config (id, key, value, status, remark) VALUES ('4', 'MASTER_JMETER_USE_SCRIPT_KEY', 'false', '1', 'false:在服务器进程内启动Jmeter压测。true:启动Jmeter_home中的命令压测');
INSERT INTO sys_config (id, key, value, status, remark) VALUES ('5', 'MASTER_JMETER_REPLACE_FILE_KEY', 'true', '1', '上传文件时，遇到同名文件是替换还是报错，默认是替换为true');
INSERT INTO sys_config (id, key, value, status, remark) VALUES ('6', 'MASTER_JMETER_GENERATE_REPORT_KEY', 'true', '1', 'true:本地web程序进程生成测试报告，可以多线程并发生成。false:使用Jmeter_home中的命令生成测试报告。');
INSERT INTO sys_config (id, key, value, status, remark) VALUES ('7', 'SCRIPT_SCHEDULER_DURATION_KEY', 'true', '1', 'true:脚本限时执行生效，具体时间由脚本单独配置，是默认值 false:取消脚本限时执行');


-- 还没有完全实现的测试场景组装功能
-- INSERT INTO sys_menu (menu_id, parent_id, name, url, perms, type, icon, order_num) VALUES ('37', '31', '测试场景组装', 'modules/test/stressTestAssembly.html', 'test:stress', '1', 'fa fa-clipboard', '6');

