-- =====================================================================
-- 测试数据初始化脚本 (SQLite)
-- 包含: 3个一级部门 + 10个二级部门 + 20个三级部门 + 每部门5个用户(水浒传人物)
-- 使用固定ID避免冲突: 部门 100-139, 用户 100-264
-- 幂等设计: 通过 NOT EXISTS 判断避免重复插入
-- =====================================================================

-- ==================== 一级部门 (3个) ====================
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 100, 0, '总经办', 'DEPT-ZJB', 'company', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 100);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 101, 0, '技术研发中心', 'DEPT-JSZX', 'center', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 101);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 102, 0, '运营管理部', 'DEPT-YYGL', 'center', 3, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 102);

-- ==================== 二级部门 (10个) ====================
-- 总经办下属
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 110, 100, '战略发展部', 'DEPT-ZLFZ', 'dept', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 110);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 111, 100, '人力资源部', 'DEPT-RLZY', 'dept', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 111);

-- 技术研发中心下属
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 112, 101, '平台架构部', 'DEPT-PTJG', 'dept', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 112);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 113, 101, '应用开发部', 'DEPT-YYKF', 'dept', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 113);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 114, 101, '质量保障部', 'DEPT-ZLBZ', 'dept', 3, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 114);

-- 运营管理部下属
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 115, 102, '市场营销部', 'DEPT-SCYX', 'dept', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 115);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 116, 102, '客户服务部', 'DEPT-KHFW', 'dept', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 116);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 117, 102, '供应链部', 'DEPT-GYL', 'dept', 3, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 117);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 118, 102, '财务管理部', 'DEPT-CWGL', 'dept', 4, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 118);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 119, 102, '行政管理部', 'DEPT-XZGL', 'dept', 5, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 119);

-- ==================== 三级部门 (20个) ====================
-- 战略发展部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 120, 110, '市场战略组', 'DEPT-SCZL', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 120);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 121, 110, '投资分析组', 'DEPT-TZFX', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 121);

-- 人力资源部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 122, 111, '招聘培训组', 'DEPT-ZPPX', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 122);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 123, 111, '薪酬绩效组', 'DEPT-XCJX', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 123);

-- 平台架构部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 124, 112, '基础架构组', 'DEPT-JCJG', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 124);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 125, 112, '数据平台组', 'DEPT-SJPT', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 125);

-- 应用开发部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 126, 113, '前端开发组', 'DEPT-QDKF', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 126);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 127, 113, '后端开发组', 'DEPT-HDKF', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 127);

-- 质量保障部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 128, 114, '测试工程组', 'DEPT-CSGC', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 128);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 129, 114, '安全审计组', 'DEPT-AQSJ', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 129);

-- 市场营销部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 130, 115, '品牌推广组', 'DEPT-PPTG', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 130);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 131, 115, '渠道运营组', 'DEPT-QDYY', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 131);

-- 客户服务部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 132, 116, '售前咨询组', 'DEPT-SQZX', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 132);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 133, 116, '售后支持组', 'DEPT-SHZC', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 133);

-- 供应链部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 134, 117, '采购管理组', 'DEPT-CGGL', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 134);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 135, 117, '仓储物流组', 'DEPT-CCWL', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 135);

-- 财务管理部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 136, 118, '会计核算组', 'DEPT-KJHS', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 136);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 137, 118, '资金管理组', 'DEPT-ZJGL', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 137);

-- 行政管理部下属 (2)
INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 138, 119, '后勤保障组', 'DEPT-HQBZ', 'group', 1, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 138);

INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time)
SELECT 139, 119, '企业文化组', 'DEPT-QYWH', 'group', 2, 1, datetime('now'), datetime('now')
WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = 139);

-- ==================== 用户数据 (33部门 × 5人 = 165人) ====================
-- 密码统一为: 123456 (实际存储明文，测试环境)
-- username格式: 拼音, realName: 水浒人物名

-- 部门100: 总经办
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 100, 'songjiang', '123456', '宋江', 100, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 100);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 101, 'lujunyi', '123456', '卢俊义', 100, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 101);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 102, 'wuyong', '123456', '吴用', 100, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 102);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 103, 'gongsunsheng', '123456', '公孙胜', 100, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 103);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 104, 'guansheng', '123456', '关胜', 100, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 104);

-- 部门101: 技术研发中心
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 105, 'linchong', '123456', '林冲', 101, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 105);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 106, 'qinming', '123456', '秦明', 101, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 106);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 107, 'huyanzhuo', '123456', '呼延灼', 101, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 107);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 108, 'huarong', '123456', '花荣', 101, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 108);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 109, 'chaijin', '123456', '柴进', 101, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 109);

-- 部门102: 运营管理部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 110, 'liba', '123456', '李应', 102, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 110);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 111, 'zhumiao', '123456', '朱仝', 102, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 111);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 112, 'luzhishen', '123456', '鲁智深', 102, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 112);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 113, 'wusong', '123456', '武松', 102, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 113);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 114, 'dongsong', '123456', '董平', 102, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 114);

-- 部门110: 战略发展部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 115, 'zhangqing', '123456', '张清', 110, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 115);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 116, 'yangzhi', '123456', '杨志', 110, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 116);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 117, 'xuning', '123456', '徐宁', 110, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 117);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 118, 'suochao', '123456', '索超', 110, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 118);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 119, 'daimu', '123456', '戴宗', 110, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 119);

-- 部门111: 人力资源部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 120, 'liutang', '123456', '刘唐', 111, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 120);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 121, 'likui', '123456', '李逵', 111, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 121);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 122, 'shijin', '123456', '史进', 111, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 122);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 123, 'mulei', '123456', '穆弘', 111, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 123);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 124, 'leizheng', '123456', '雷横', 111, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 124);

-- 部门112: 平台架构部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 125, 'lishun', '123456', '李俊', 112, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 125);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 126, 'ruanxiaor', '123456', '阮小二', 112, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 126);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 127, 'zhangheng', '123456', '张横', 112, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 127);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 128, 'ruanxiaowu', '123456', '阮小五', 112, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 128);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 129, 'zhangshun', '123456', '张顺', 112, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 129);

-- 部门113: 应用开发部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 130, 'ruanxiaoqi', '123456', '阮小七', 113, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 130);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 131, 'yangxiong', '123456', '杨雄', 113, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 131);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 132, 'shiuxiu', '123456', '石秀', 113, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 132);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 133, 'jieting', '123456', '解珍', 113, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 133);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 134, 'jiebao', '123456', '解宝', 113, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 134);

-- 部门114: 质量保障部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 135, 'yanqing', '123456', '燕青', 114, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 135);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 136, 'huangxin', '123456', '黄信', 114, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 136);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 137, 'sunli', '123456', '孙立', 114, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 137);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 138, 'xuanzan', '123456', '宣赞', 114, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 138);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 139, 'haosiwen', '123456', '郝思文', 114, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 139);

-- 部门115: 市场营销部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 140, 'hansiwen', '123456', '韩滔', 115, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 140);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 141, 'pengqi', '123456', '彭玘', 115, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 141);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 142, 'shanyanqi', '123456', '单廷珪', 115, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 142);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 143, 'weidingguo', '123456', '魏定国', 115, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 143);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 144, 'xiaorang', '123456', '萧让', 115, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 144);

-- 部门116: 客户服务部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 145, 'peixuan', '123456', '裴宣', 116, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 145);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 146, 'ouupeng', '123456', '欧鹏', 116, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 146);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 147, 'dengfei', '123456', '邓飞', 116, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 147);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 148, 'yanshun', '123456', '燕顺', 116, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 148);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 149, 'yanglin', '123456', '杨林', 116, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 149);

-- 部门117: 供应链部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 150, 'lingzhen', '123456', '凌振', 117, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 150);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 151, 'jiangjing', '123456', '蒋敬', 117, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 151);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 152, 'lvfang', '123456', '吕方', 117, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 152);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 153, 'guosheng', '123456', '郭盛', 117, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 153);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 154, 'anquansheng', '123456', '安道全', 117, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 154);

-- 部门118: 财务管理部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 155, 'huangfu', '123456', '皇甫端', 118, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 155);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 156, 'wangdingliu', '123456', '王定六', 118, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 156);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 157, 'yubaosi', '123456', '郁保四', 118, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 157);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 158, 'duanqi', '123456', '段琪', 118, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 158);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 159, 'baixiuying', '123456', '白秀英', 118, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 159);

-- 部门119: 行政管理部
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 160, 'songwan', '123456', '宋万', 119, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 160);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 161, 'duqian', '123456', '杜迁', 119, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 161);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 162, 'zoushen', '123456', '邹渊', 119, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 162);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 163, 'zourun', '123456', '邹润', 119, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 163);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 164, 'zhugui', '123456', '朱贵', 119, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 164);

-- 部门120: 市场战略组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 165, 'shiwen', '123456', '施恩', 120, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 165);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 166, 'likui2', '123456', '李忠', 120, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 166);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 167, 'zhoutong', '123456', '周通', 120, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 167);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 168, 'tanglong', '123456', '汤隆', 120, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 168);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 169, 'duhui', '123456', '杜兴', 120, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 169);

-- 部门121: 投资分析组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 170, 'zhengshou', '123456', '郑天寿', 121, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 170);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 171, 'taozong', '123456', '陶宗旺', 121, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 171);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 172, 'caozheng', '123456', '曹正', 121, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 172);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 173, 'songqing', '123456', '宋清', 121, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 173);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 174, 'gongming', '123456', '龚旺', 121, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 174);

-- 部门122: 招聘培训组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 175, 'dingesheng', '123456', '丁得胜', 122, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 175);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 176, 'muxian', '123456', '穆春', 122, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 176);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 177, 'xuelong', '123456', '薛永', 122, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 177);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 178, 'kongming2', '123456', '孔明', 122, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 178);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 179, 'kongliang', '123456', '孔亮', 122, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 179);

-- 部门123: 薪酬绩效组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 180, 'zhangqing2', '123456', '张青', 123, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 180);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 181, 'sunerniang', '123456', '孙二娘', 123, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 181);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 182, 'gudage', '123456', '顾大嫂', 123, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 182);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 183, 'zhangheng2', '123456', '张横', 123, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 183);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 184, 'baiyi', '123456', '白胜', 123, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 184);

-- 部门124: 基础架构组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 185, 'shiqian', '123456', '时迁', 124, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 185);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 186, 'lihuzi', '123456', '李衮', 124, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 186);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 187, 'baohua', '123456', '鲍旭', 124, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 187);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 188, 'fanrui', '123456', '樊瑞', 124, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 188);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 189, 'xiangchong', '123456', '项充', 124, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 189);

-- 部门125: 数据平台组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 190, 'kongming3', '123456', '马麟', 125, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 190);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 191, 'tongwei', '123456', '童威', 125, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 191);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 192, 'tongmeng', '123456', '童猛', 125, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 192);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 193, 'mengkuang', '123456', '孟康', 125, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 193);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 194, 'houjian', '123456', '侯健', 125, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 194);

-- 部门126: 前端开发组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 195, 'chenzhen', '123456', '陈达', 126, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 195);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 196, 'yangchun', '123456', '杨春', 126, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 196);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 197, 'zhengtu', '123456', '郑屠', 126, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 197);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 198, 'cuidaocheng', '123456', '崔道成', 126, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 198);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 199, 'qiuxiaoyi', '123456', '丘小乙', 126, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 199);

-- 部门127: 后端开发组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 200, 'luqian', '123456', '陆谦', 127, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 200);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 201, 'fuchai', '123456', '富安', 127, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 201);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 202, 'dongchaofu', '123456', '董超', 127, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 202);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 203, 'xueba', '123456', '薛霸', 127, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 203);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 204, 'gaoqiu', '123456', '高俅', 127, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 204);

-- 部门128: 测试工程组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 205, 'caijing', '123456', '蔡京', 128, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 205);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 206, 'tongguan', '123456', '童贯', 128, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 206);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 207, 'yangshi', '123456', '杨戬', 128, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 207);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 208, 'gao衙nei', '123456', '高衙内', 128, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 208);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 209, 'ximenqing', '123456', '西门庆', 128, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 209);

-- 部门129: 安全审计组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 210, 'jiangzhong', '123456', '蒋忠', 129, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 210);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 211, 'zhangduhu', '123456', '张都监', 129, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 211);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 212, 'huangwenbing', '123456', '黄文炳', 129, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 212);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 213, 'jiaming', '123456', '贾氏', 129, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 213);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 214, 'ligu', '123456', '李固', 129, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 214);

-- 部门130: 品牌推广组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 215, 'wanglun', '123456', '王伦', 130, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 215);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 216, 'chaigai', '123456', '晁盖', 130, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 216);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 217, 'wuyongshi', '123456', '吴学究', 130, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 217);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 218, 'ruanxiaosan', '123456', '阮小三', 130, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 218);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 219, 'linchong2', '123456', '林娘子', 130, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 219);

-- 部门131: 渠道运营组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 220, 'zhongli', '123456', '钟离老人', 131, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 220);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 221, 'songchaofeng', '123456', '宋太公', 131, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 221);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 222, 'lichaoofeng', '123456', '李太公', 131, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 222);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 223, 'kongtaigong', '123456', '孔太公', 131, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 223);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 224, 'shitaigong', '123456', '史太公', 131, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 224);

-- 部门132: 售前咨询组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 225, 'wangjiao', '123456', '王进', 132, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 225);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 226, 'luanchi', '123456', '栾廷玉', 132, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 226);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 227, 'sunxin', '123456', '孙新', 132, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 227);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 228, 'zhangsun', '123456', '张孙氏', 132, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 228);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 229, 'yuehe', '123456', '乐和', 132, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 229);

-- 部门133: 售后支持组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 230, 'xiezhen2', '123456', '邹渊', 133, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 230);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 231, 'dengyun', '123456', '邓元觉', 133, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 231);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 232, 'shibao', '123456', '石宝', 133, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 232);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 233, 'fangla', '123456', '方腊', 133, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 233);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 234, 'fangtian', '123456', '方天定', 133, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 234);

-- 部门134: 采购管理组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 235, 'wangyin', '123456', '王寅', 134, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 235);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 236, 'pangwanchun', '123456', '庞万春', 134, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 236);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 237, 'lvgong', '123456', '厉天闰', 134, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 237);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 238, 'sizhengfang', '123456', '司行方', 134, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 238);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 239, 'baoliguang', '123456', '包道乙', 134, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 239);

-- 部门135: 仓储物流组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 240, 'zhengbiao', '123456', '郑彪', 135, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 240);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 241, 'gaolian', '123456', '高廉', 135, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 241);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 242, 'murong', '123456', '慕容知府', 135, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 242);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 243, 'liangzhongjie', '123456', '梁中杰', 135, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 243);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 244, 'caizhong', '123456', '蔡知府', 135, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 244);

-- 部门136: 会计核算组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 245, 'chengwanzhi', '123456', '程万里', 136, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 245);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 246, 'liushimeng', '123456', '刘世盟', 136, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 246);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 247, 'wenrenyi', '123456', '闻人达', 136, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 247);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 248, 'wangzheng', '123456', '王正', 136, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 248);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 249, 'liuzan', '123456', '刘赞', 136, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 249);

-- 部门137: 资金管理组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 250, 'huangxin2', '123456', '黄信', 137, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 250);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 251, 'wusong2', '123456', '武大郎', 137, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 251);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 252, 'panjinlian', '123456', '潘金莲', 137, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 252);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 253, 'wangpo', '123456', '王婆', 137, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 253);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 254, 'yanqinxi', '123456', '阎婆惜', 137, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 254);

-- 部门138: 后勤保障组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 255, 'tanglonger', '123456', '唐牛儿', 138, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 255);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 256, 'niuer', '123456', '牛二', 138, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 256);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 257, 'zhangjiao', '123456', '张教头', 138, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 257);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 258, 'jinyu', '123456', '锦儿', 138, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 258);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 259, 'yingguan', '123456', '迎儿', 138, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 259);

-- 部门139: 企业文化组
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 260, 'meixiang', '123456', '梅香', 139, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 260);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 261, 'chunxiang', '123456', '春香', 139, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 261);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 262, 'laoer', '123456', '李老儿', 139, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 262);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 263, 'polaopo', '123456', '潘老婆', 139, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 263);
INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) SELECT 264, 'songpo', '123456', '宋婆', 139, 1, 1, datetime('now'), datetime('now') WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = 264);

-- ==================== 用户兼职记录 (主部门) ====================
-- post_id = 0 表示默认岗位（与sys_user.post_id保持一致）
INSERT INTO sys_user_post (user_id, dept_id, post_id, is_main)
SELECT u.id, u.dept_id, 0, 1 FROM sys_user u WHERE u.id BETWEEN 100 AND 264
AND NOT EXISTS (SELECT 1 FROM sys_user_post WHERE user_id = u.id AND is_main = 1);
