package com.flow.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 测试数据初始化器 - 水浒传人物部门与用户数据
 * 在 schema.sql 和 PermissionDataInitializer 之后执行
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(13)
public class TestDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE id BETWEEN 100 AND 264", Long.class);
        if (count != null && count > 0) {
            log.info("[TestDataInitializer] 测试数据已存在({}条)，跳过初始化", count);
            return;
        }
        log.info("[TestDataInitializer] 开始初始化测试数据(水浒传人物)...");
        initDepts();
        initUsers();
        initUserPosts();
        log.info("[TestDataInitializer] 测试数据初始化完成: 33个部门 + 165个用户");
    }

    private void initDepts() {
        // 一级部门 (3个)
        dept(100, 0, "总经办", "DEPT-ZJB", "company", 1);
        dept(101, 0, "技术研发中心", "DEPT-JSZX", "center", 2);
        dept(102, 0, "运营管理部", "DEPT-YYGL", "center", 3);
        // 二级部门 (10个)
        dept(110, 100, "战略发展部", "DEPT-ZLFZ", "dept", 1);
        dept(111, 100, "人力资源部", "DEPT-RLZY", "dept", 2);
        dept(112, 101, "平台架构部", "DEPT-PTJG", "dept", 1);
        dept(113, 101, "应用开发部", "DEPT-YYKF", "dept", 2);
        dept(114, 101, "质量保障部", "DEPT-ZLBZ", "dept", 3);
        dept(115, 102, "市场营销部", "DEPT-SCYX", "dept", 1);
        dept(116, 102, "客户服务部", "DEPT-KHFW", "dept", 2);
        dept(117, 102, "供应链部", "DEPT-GYL", "dept", 3);
        dept(118, 102, "财务管理部", "DEPT-CWGL", "dept", 4);
        dept(119, 102, "行政管理部", "DEPT-XZGL", "dept", 5);
        // 三级部门 (20个)
        dept(120, 110, "市场战略组", "DEPT-SCZL", "group", 1);
        dept(121, 110, "投资分析组", "DEPT-TZFX", "group", 2);
        dept(122, 111, "招聘培训组", "DEPT-ZPPX", "group", 1);
        dept(123, 111, "薪酬绩效组", "DEPT-XCJX", "group", 2);
        dept(124, 112, "基础架构组", "DEPT-JCJG", "group", 1);
        dept(125, 112, "数据平台组", "DEPT-SJPT", "group", 2);
        dept(126, 113, "前端开发组", "DEPT-QDKF", "group", 1);
        dept(127, 113, "后端开发组", "DEPT-HDKF", "group", 2);
        dept(128, 114, "测试工程组", "DEPT-CSGC", "group", 1);
        dept(129, 114, "安全审计组", "DEPT-AQSJ", "group", 2);
        dept(130, 115, "品牌推广组", "DEPT-PPTG", "group", 1);
        dept(131, 115, "渠道运营组", "DEPT-QDYY", "group", 2);
        dept(132, 116, "售前咨询组", "DEPT-SQZX", "group", 1);
        dept(133, 116, "售后支持组", "DEPT-SHZC", "group", 2);
        dept(134, 117, "采购管理组", "DEPT-CGGL", "group", 1);
        dept(135, 117, "仓储物流组", "DEPT-CCWL", "group", 2);
        dept(136, 118, "会计核算组", "DEPT-KJHS", "group", 1);
        dept(137, 118, "资金管理组", "DEPT-ZJGL", "group", 2);
        dept(138, 119, "后勤保障组", "DEPT-HQBZ", "group", 1);
        dept(139, 119, "企业文化组", "DEPT-QYWH", "group", 2);
    }

    private void initUsers() {
        // 部门100: 总经办
        user(100, "songjiang", "宋江", 100);
        user(101, "lujunyi", "卢俊义", 100);
        user(102, "wuyong", "吴用", 100);
        user(103, "gongsunsheng", "公孙胜", 100);
        user(104, "guansheng", "关胜", 100);
        // 部门101: 技术研发中心
        user(105, "linchong", "林冲", 101);
        user(106, "qinming", "秦明", 101);
        user(107, "huyanzhuo", "呼延灼", 101);
        user(108, "huarong", "花荣", 101);
        user(109, "chaijin", "柴进", 101);
        // 部门102: 运营管理部
        user(110, "liying", "李应", 102);
        user(111, "zhutong", "朱仝", 102);
        user(112, "luzhishen", "鲁智深", 102);
        user(113, "wusong", "武松", 102);
        user(114, "dongping", "董平", 102);
        // 部门110: 战略发展部
        user(115, "zhangqing", "张清", 110);
        user(116, "yangzhi", "杨志", 110);
        user(117, "xuning", "徐宁", 110);
        user(118, "suochao", "索超", 110);
        user(119, "daizong", "戴宗", 110);
        // 部门111: 人力资源部
        user(120, "liutang", "刘唐", 111);
        user(121, "likui", "李逵", 111);
        user(122, "shijin", "史进", 111);
        user(123, "muhong", "穆弘", 111);
        user(124, "leiheng", "雷横", 111);
        // 部门112: 平台架构部
        user(125, "lijun", "李俊", 112);
        user(126, "ruanxiaoer", "阮小二", 112);
        user(127, "zhangheng", "张横", 112);
        user(128, "ruanxiaowu", "阮小五", 112);
        user(129, "zhangshun", "张顺", 112);
        // 部门113: 应用开发部
        user(130, "ruanxiaoqi", "阮小七", 113);
        user(131, "yangxiong", "杨雄", 113);
        user(132, "shixiu", "石秀", 113);
        user(133, "xiezhen", "解珍", 113);
        user(134, "xiebao", "解宝", 113);
        // 部门114: 质量保障部
        user(135, "yanqing", "燕青", 114);
        user(136, "huangxin", "黄信", 114);
        user(137, "sunli", "孙立", 114);
        user(138, "xuanzan", "宣赞", 114);
        user(139, "haosiwen", "郝思文", 114);
        // 部门115: 市场营销部
        user(140, "hantao", "韩滔", 115);
        user(141, "pengqi", "彭玘", 115);
        user(142, "shantinggui", "单廷珪", 115);
        user(143, "weidingguo", "魏定国", 115);
        user(144, "xiaorang", "萧让", 115);
        // 部门116: 客户服务部
        user(145, "peixuan", "裴宣", 116);
        user(146, "oupeng", "欧鹏", 116);
        user(147, "dengfei", "邓飞", 116);
        user(148, "yanshun", "燕顺", 116);
        user(149, "yanglin", "杨林", 116);
        // 部门117: 供应链部
        user(150, "lingzhen", "凌振", 117);
        user(151, "jiangjing", "蒋敬", 117);
        user(152, "lvfang", "吕方", 117);
        user(153, "guosheng", "郭盛", 117);
        user(154, "andaoquan", "安道全", 117);
        // 部门118: 财务管理部
        user(155, "huangfuduan", "皇甫端", 118);
        user(156, "wangdingliu", "王定六", 118);
        user(157, "yubaosi", "郁保四", 118);
        user(158, "duanqi", "段琪", 118);
        user(159, "baixiuying", "白秀英", 118);
        // 部门119: 行政管理部
        user(160, "songwan", "宋万", 119);
        user(161, "duqian", "杜迁", 119);
        user(162, "zouyuan", "邹渊", 119);
        user(163, "zourun", "邹润", 119);
        user(164, "zhugui", "朱贵", 119);
        // 部门120: 市场战略组
        user(165, "shien", "施恩", 120);
        user(166, "lizhong", "李忠", 120);
        user(167, "zhoutong", "周通", 120);
        user(168, "tanglong", "汤隆", 120);
        user(169, "duxing", "杜兴", 120);
        // 部门121: 投资分析组
        user(170, "zhengtianshou", "郑天寿", 121);
        user(171, "taozongwang", "陶宗旺", 121);
        user(172, "caozheng", "曹正", 121);
        user(173, "songqing", "宋清", 121);
        user(174, "gongwang", "龚旺", 121);
        // 部门122: 招聘培训组
        user(175, "dingdesheng", "丁得胜", 122);
        user(176, "muchun", "穆春", 122);
        user(177, "xueyong", "薛永", 122);
        user(178, "kongming", "孔明", 122);
        user(179, "kongliang", "孔亮", 122);
        // 部门123: 薪酬绩效组
        user(180, "zhangqing2", "张青", 123);
        user(181, "sunerniang", "孙二娘", 123);
        user(182, "gudasao", "顾大嫂", 123);
        user(183, "caifu", "蔡福", 123);
        user(184, "baisheng", "白胜", 123);
        // 部门124: 基础架构组
        user(185, "shiqian", "时迁", 124);
        user(186, "ligun", "李衮", 124);
        user(187, "baoxu", "鲍旭", 124);
        user(188, "fanrui", "樊瑞", 124);
        user(189, "xiangchong", "项充", 124);
        // 部门125: 数据平台组
        user(190, "malin", "马麟", 125);
        user(191, "tongwei", "童威", 125);
        user(192, "tongmeng", "童猛", 125);
        user(193, "mengkuang", "孟康", 125);
        user(194, "houjian", "侯健", 125);
        // 部门126: 前端开发组
        user(195, "chenda", "陈达", 126);
        user(196, "yangchun", "杨春", 126);
        user(197, "zhuwu", "朱武", 126);
        user(198, "cuiping", "崔萍", 126);
        user(199, "zhangchuan", "张川", 126);
        // 部门127: 后端开发组
        user(200, "luqian", "陆谦", 127);
        user(201, "fuan", "富安", 127);
        user(202, "dongchao", "董超", 127);
        user(203, "xueba", "薛霸", 127);
        user(204, "gaoqiu", "高俅", 127);
        // 部门128: 测试工程组
        user(205, "caijing", "蔡京", 128);
        user(206, "tongguan", "童贯", 128);
        user(207, "yangjian", "杨戬", 128);
        user(208, "gaoyanei", "高衙内", 128);
        user(209, "ximenqing", "西门庆", 128);
        // 部门129: 安全审计组
        user(210, "jiangzhong", "蒋忠", 129);
        user(211, "zhangdujian", "张都监", 129);
        user(212, "huangwenbing", "黄文炳", 129);
        user(213, "jiaming", "贾氏", 129);
        user(214, "ligu", "李固", 129);
        // 部门130: 品牌推广组
        user(215, "wanglun", "王伦", 130);
        user(216, "chaogai", "晁盖", 130);
        user(217, "sunxin", "孙新", 130);
        user(218, "yuehe", "乐和", 130);
        user(219, "wangjiao", "王进", 130);
        // 部门131: 渠道运营组
        user(220, "luantingyu", "栾廷玉", 131);
        user(221, "fangla", "方腊", 131);
        user(222, "fangtianding", "方天定", 131);
        user(223, "wangyin", "王寅", 131);
        user(224, "pangwanchun", "庞万春", 131);
        // 部门132: 售前咨询组
        user(225, "dengyuanjue", "邓元觉", 132);
        user(226, "shibao", "石宝", 132);
        user(227, "lvtianduo", "厉天闰", 132);
        user(228, "sixingfang", "司行方", 132);
        user(229, "baodaoyi", "包道乙", 132);
        // 部门133: 售后支持组
        user(230, "zhengbiao", "郑彪", 133);
        user(231, "gaolian", "高廉", 133);
        user(232, "murong", "慕容知府", 133);
        user(233, "liangzhongshu", "梁中书", 133);
        user(234, "caizhifu", "蔡知府", 133);
        // 部门134: 采购管理组
        user(235, "chengwanli", "程万里", 134);
        user(236, "liushimeng", "刘世盟", 134);
        user(237, "wenrenda", "闻人达", 134);
        user(238, "wangzheng", "王正", 134);
        user(239, "liuzan", "刘赞", 134);
        // 部门135: 仓储物流组
        user(240, "wudalang", "武大郎", 135);
        user(241, "panjinlian", "潘金莲", 135);
        user(242, "wangpo", "王婆", 135);
        user(243, "yanqinxi", "阎婆惜", 135);
        user(244, "tangniuer", "唐牛儿", 135);
        // 部门136: 会计核算组
        user(245, "niuer", "牛二", 136);
        user(246, "zhangjiaotou", "张教头", 136);
        user(247, "jiner", "锦儿", 136);
        user(248, "yingguan", "迎儿", 136);
        user(249, "meixiang", "梅香", 136);
        // 部门137: 资金管理组
        user(250, "chunxiang", "春香", 137);
        user(251, "liniangzi", "林娘子", 137);
        user(252, "songtaigong", "宋太公", 137);
        user(253, "litaigong", "李太公", 137);
        user(254, "kongtaigong", "孔太公", 137);
        // 部门138: 后勤保障组
        user(255, "shitaigong", "史太公", 138);
        user(256, "songpo", "宋婆", 138);
        user(257, "yanpo", "阎婆", 138);
        user(258, "panlaopo", "潘老婆", 138);
        user(259, "lilaopo", "李老婆", 138);
        // 部门139: 企业文化组
        user(260, "zhongsan", "张三", 139);
        user(261, "lisi", "李四", 139);
        user(262, "wangwu", "王五", 139);
        user(263, "zhaoliu", "赵六", 139);
        user(264, "sunqi", "孙七", 139);
    }

    private void initUserPosts() {
        jdbcTemplate.update(
                "INSERT INTO sys_user_post (user_id, dept_id, post_id, is_main) " +
                "SELECT u.id, u.dept_id, 0, 1 FROM sys_user u WHERE u.id BETWEEN 100 AND 264 " +
                "AND NOT EXISTS (SELECT 1 FROM sys_user_post WHERE user_id = u.id AND is_main = 1)");
    }

    private void dept(int id, int parentId, String name, String code, String type, int sort) {
        jdbcTemplate.update(
                "INSERT INTO sys_dept (id, parent_id, dept_name, dept_code, dept_type, sort_order, status, create_time, update_time) " +
                "SELECT ?, ?, ?, ?, ?, ?, 1, datetime('now'), datetime('now') " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_dept WHERE id = ?)",
                id, parentId, name, code, type, sort, id);
    }

    private void user(int id, String username, String realName, int deptId) {
        jdbcTemplate.update(
                "INSERT INTO sys_user (id, username, password, real_name, dept_id, status, security_level, create_time, update_time) " +
                "SELECT ?, ?, '123456', ?, ?, 1, 1, datetime('now'), datetime('now') " +
                "WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE id = ?)",
                id, username, realName, deptId, id);
    }
}
