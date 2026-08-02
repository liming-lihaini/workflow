package com.flow.engine.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * 客户档案测试数据初始化器。
 * 当 t_customer 表为空时，按环境监测行业初始化 100 条客户数据（ISSUE-026）。
 * 在 DictDataInitializer(@Order 10) 之后执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(11)
public class CustomerDataInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;
    private static final int TARGET = 100;

    private static final String[] CITY_PREFIX = {
        "深圳市", "广州市", "北京市", "上海市", "杭州市", "南京市", "成都市", "武汉市",
        "西安市", "苏州市", "东莞市", "佛山市", "天津市", "重庆市", "青岛市", "宁波市"
    };
    private static final String[] NAME_CORE = {
        "华测", "谱尼", "中环", "绿洁", "蓝盾", "中节能", "碧水源", "环科院", "亿通",
        "天祥", "通标", "广电", "建研", "城检", "海珠", "南山", "福田", "宝安", "龙华", "龙岗"
    };
    private static final String[] NAME_SUFFIX = {
        "环保科技有限公司", "环境检测有限公司", "环境监测中心", "生态科技有限公司",
        "检测认证有限公司", "环境工程有限公司", "水质监测站", "大气监测有限公司"
    };
    private static final String[] CONTACTS = {
        "张伟", "李娜", "王强", "刘洋", "陈静", "杨帆", "赵磊", "黄敏", "周涛", "吴婷"
    };
    private static final String[] DISTRICT = {
        "南山区科技园路", "福田区深南大道", "宝安区新安街道", "龙岗区龙城大道",
        "海淀区中关村大街", "浦东新区张江路", "天河区珠江新城", "西湖区文三路"
    };

    @Override
    public void run(String... args) {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM t_customer", Integer.class);
        if (cnt != null && cnt >= TARGET) {
            log.info("[CustomerDataInitializer] t_customer 已存在 {} 条数据，跳过初始化", cnt);
            return;
        }
        if (cnt != null && cnt > 0) {
            log.info("[CustomerDataInitializer] t_customer 现有 {} 条，不足 {} 条，先清空再初始化", cnt, TARGET);
            jdbcTemplate.update("DELETE FROM t_customer");
        }
        log.info("[CustomerDataInitializer] 开始初始化 {} 条客户数据...", TARGET);
        Random rand = new Random(20260802L);
        int inserted = 0;
        for (int i = 1; i <= TARGET; i++) {
            String custNo = String.format("C%05d", i);
            String nameCore = NAME_CORE[(i - 1) % NAME_CORE.length];
            String nameSuffix = NAME_SUFFIX[(i * 3) % NAME_SUFFIX.length];
            String custName = nameCore + nameSuffix;
            String city = CITY_PREFIX[(i - 1) % CITY_PREFIX.length];
            String creditCode = String.format("91440300MA5%08dX", i);
            String contact = CONTACTS[i % CONTACTS.length];
            String tel = String.format("1%03d%04d%04d",
                    3 + (i % 6), 1000 + (i * 7 % 9000), 1000 + (i * 13 % 9000));
            String district = DISTRICT[(i - 1) % DISTRICT.length];
            String address = city + district + (i % 200 + 1) + "号";
            String invoiceTitle = custName;
            String taxNo = creditCode;
            int status = (i % 5 == 0) ? 0 : 1; // 约 20% 停用
            jdbcTemplate.update(
                    "INSERT INTO t_customer (cust_no, cust_name, credit_code, contact, tel, " +
                            "invoice_title, tax_no, city, address, status, create_time, update_time) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,datetime('now'),datetime('now'))",
                    custNo, custName, creditCode, contact, tel,
                    invoiceTitle, taxNo, city, address, status);
            inserted++;
        }
        log.info("[CustomerDataInitializer] 客户数据初始化完成: 新增 {} 条", inserted);
    }
}
