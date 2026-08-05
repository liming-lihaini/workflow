package com.flow.engine.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flow.engine.entity.EmsSampleParamConfig;
import com.flow.engine.entity.EmsSampleParamItem;
import com.flow.engine.service.EmsSampleParamConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 采样参数配置数据初始化器（TRD 5.1）
 * 依据环境监测行业 HJ 标准，初始化 20 条常用「检测类别 + 检测项目」的现场结构化必填采样参数配置。
 * 幂等：仅当 t_sample_param_config 为空时执行，避免重复写入。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(30)
public class SampleParamConfigInitializer implements CommandLineRunner {

    private final EmsSampleParamConfigService configService;

    @Override
    public void run(String... args) {
        long cnt = configService.count();
        if (cnt > 0) {
            log.info("[采样参数配置] 已存在 {} 条数据，跳过初始化", cnt);
            return;
        }

        List<EmsSampleParamConfig> all = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 有组织废气 - 颗粒物（GB 16297-1996 / HJ 836）
        all.add(build("有组织废气", "颗粒物", "GB 16297-1996",
                "颗粒物最高允许排放浓度按排气筒高度执行对应限值；低于 20mg/m³ 采用 HJ 836 低浓度法",
                "等速采样；使用玻璃纤维滤筒；采样断面优先选在垂直管段",
                item("flue_area", "烟道截面积", "number", "m²", 1, "实测烟道内径计算"),
                item("flue_temp", "烟气温度", "number", "℃", 1, "测点烟气温度"),
                item("flue_press", "烟气静压", "number", "kPa", 1, "测点静压"),
                item("flue_mois", "烟气含湿量", "number", "%", 1, "干湿球法或重量法"),
                item("sample_flow", "等速采样流量", "number", "L/min", 1, "颗粒物必须等速采样"),
                item("sample_time", "采样时长", "number", "min", 1, "单次采样≥3min")));

        // 2. 有组织废气 - 二氧化硫
        all.add(build("有组织废气", "二氧化硫(SO2)", "GB 16297-1996",
                "按排气筒高度执行对应排放浓度限值",
                "使用多孔玻板吸收管；采样后避光冷藏",
                item("flue_area", "烟道截面积", "number", "m²", 1, "实测烟道内径计算"),
                item("flue_mois", "烟气含湿量", "number", "%", 1, "重量法测定"),
                item("sample_flow", "采样流量", "number", "L/min", 1, "固定流量采样"),
                item("sample_time", "采样时长", "number", "min", 1, "累计采样时间"),
                item("stack_h", "排气筒高度", "number", "m", 1, "现场核实排气筒高度")));

        // 3. 有组织废气 - 氮氧化物
        all.add(build("有组织废气", "氮氧化物(NOx)", "GB 16297-1996",
                "按排气筒高度执行对应排放浓度限值",
                "使用氧化瓶+吸收液；采样后避光",
                item("flue_area", "烟道截面积", "number", "m²", 1, "实测烟道内径计算"),
                item("flue_temp", "烟气温度", "number", "℃", 1, "测点烟气温度"),
                item("sample_flow", "采样流量", "number", "L/min", 1, "固定流量采样"),
                item("sample_time", "采样时长", "number", "min", 1, "累计采样时间"),
                item("stack_h", "排气筒高度", "number", "m", 1, "现场核实排气筒高度")));

        // 4. 有组织废气 - 油烟（GB 18483-2002）
        all.add(build("有组织废气", "油烟", "GB 18483-2002",
                "饮食业单位油烟最高允许排放浓度 2.0 mg/m³",
                "等速采样；使用金属滤筒；按基准风量折算",
                item("sample_vol", "采样体积", "number", "m³", 1, "标准状态干烟气体积"),
                item("sample_time", "采样时长", "number", "min", 1, "连续采样"),
                item("base_air", "基准风量", "number", "m³/h", 1, "按灶头数折算")));

        // 5. 无组织废气 - 总悬浮颗粒物 TSP（GB 16297-1996）
        all.add(build("无组织废气", "总悬浮颗粒物(TSP)", "GB 16297-1996",
                "无组织排放监控浓度限值 1.0 mg/m³（周界外浓度最高点）",
                "中流量采样器；采样头距地 1.5m；避免风口",
                item("sample_flow", "采样流量", "number", "m³/min", 1, "中流量 TSP 采样"),
                item("sample_time", "采样时长", "number", "min", 1, "建议≥45min"),
                item("weather", "气象条件", "text", "", 1, "风速、风向现场记录")));

        // 6. 无组织废气 - 苯系物（HJ 584-2010）
        all.add(build("无组织废气", "苯系物", "HJ 584-2010",
                "按标准执行对应限值",
                "活性炭吸附管采样；采样后密封避光冷藏",
                item("sample_flow", "采样流量", "number", "L/min", 1, "吸附管采样流量"),
                item("sample_time", "采样时长", "number", "min", 1, "累计采样时间"),
                item("weather", "气象条件", "text", "", 1, "风速风向现场记录")));

        // 7. 废水/地表水 - 化学需氧量 COD（HJ 828-2017）
        all.add(build("废水/地表水", "化学需氧量(COD)", "HJ 828-2017",
                "按 GB 8978 污水综合排放标准执行对应限值",
                "硫酸调 pH≤2 固定；棕色玻璃瓶；冷藏",
                item("water_temp", "水样温度", "number", "℃", 1, "现场测定水温"),
                item("ph", "pH值", "number", "", 1, "现场 pH 检测"),
                item("sample_depth", "采样深度", "number", "m", 1, "水面下 0.5m"),
                item("sample_vol", "采样体积", "number", "mL", 1, "≥500mL"),
                item("fix_agent", "固定剂", "text", "", 1, "加硫酸至 pH≤2")));

        // 8. 废水/地表水 - 氨氮（HJ 535-2009）
        all.add(build("废水/地表水", "氨氮", "HJ 535-2009",
                "按 GB 8978 执行对应限值",
                "现场过滤；硫酸调 pH≤2；冷藏保存",
                item("water_temp", "水样温度", "number", "℃", 1, "现场测定水温"),
                item("ph", "pH值", "number", "", 1, "现场 pH 检测"),
                item("sample_vol", "采样体积", "number", "mL", 1, "≥250mL"),
                item("store_temp", "保存温度", "number", "℃", 1, "2~5℃ 冷藏")));

        // 9. 废水/地表水 - 总磷（GB 11893-89）
        all.add(build("废水/地表水", "总磷", "GB 11893-89",
                "按 GB 8978 执行对应限值",
                "硫酸调 pH≤2；冷藏；聚乙烯瓶",
                item("ph", "pH值", "number", "", 1, "现场 pH 检测"),
                item("sample_vol", "采样体积", "number", "mL", 1, "≥200mL"),
                item("fix_agent", "固定剂", "text", "", 1, "加硫酸固定")));

        // 10. 废水/地表水 - 总氮（HJ 636-2012）
        all.add(build("废水/地表水", "总氮", "HJ 636-2012",
                "按 GB 8978 执行对应限值",
                "硫酸调 pH≤2；避光冷藏",
                item("ph", "pH值", "number", "", 1, "现场 pH 检测"),
                item("sample_vol", "采样体积", "number", "mL", 1, "≥200mL"),
                item("fix_agent", "固定剂", "text", "", 1, "加硫酸固定"),
                item("light_proof", "避光保存", "bool", "", 1, "棕色瓶或铝箔包裹")));

        // 11. 废水/地表水 - 石油类（HJ 637-2018）
        all.add(build("废水/地表水", "石油类", "HJ 637-2018",
                "按 GB 8978 执行对应限值",
                "采样瓶必须全满、单独采样、禁止预先用水样冲洗；现场萃取或 4℃ 冷藏 24h 内萃取",
                item("sample_vol", "采样体积", "number", "mL", 1, "≥500mL 满瓶"),
                item("light_proof", "避光冷藏", "bool", "", 1, "4℃ 避光保存"),
                item("separate", "单独采样", "bool", "", 1, "严禁混合其他项目")));

        // 12. 废水/地表水 - 挥发酚（HJ 503-2009）
        all.add(build("废水/地表水", "挥发酚", "HJ 503-2009",
                "按 GB 8978 执行对应限值",
                "加磷酸调 pH≤4；加 CuSO4 抑制微生物；冷藏",
                item("ph", "pH值", "number", "", 1, "加磷酸至 pH≤4"),
                item("sample_vol", "采样体积", "number", "mL", 1, "≥250mL"),
                item("fix_agent", "固定剂", "text", "", 1, "磷酸+硫酸铜"),
                item("store_temp", "保存温度", "number", "℃", 1, "2~5℃ 冷藏")));

        // 13. 地下水 - pH（GB/T 14848-2017）
        all.add(build("地下水", "pH", "GB/T 14848-2017",
                "地下水质量标准 Ⅰ~Ⅴ 类 pH 限值 6.5~8.5",
                "现场测定；采样前静置；聚乙烯瓶",
                item("water_temp", "水温", "number", "℃", 1, "现场测水温"),
                item("sample_depth", "采样深度", "number", "m", 1, "水位以下 0.5m"),
                item("sample_vol", "采样体积", "number", "mL", 1, "≥500mL")));

        // 14. 地下水 - 重金属（总铅/总镉，HJ 700-2014）
        all.add(build("地下水", "总铅", "HJ 700-2014",
                "按 GB/T 14848 各类限值执行",
                "硝酸酸化至 pH≤2；冷藏；聚乙烯瓶",
                item("sample_vol", "采样体积", "number", "mL", 1, "≥500mL"),
                item("fix_agent", "酸化剂", "text", "", 1, "硝酸酸化 pH≤2"),
                item("store_temp", "保存温度", "number", "℃", 1, "2~5℃ 冷藏")));

        // 15. 土壤 - 重金属（GB 15618-2018 / HJ 803）
        all.add(build("土壤", "重金属", "GB 15618-2018",
                "农用地土壤污染风险筛选值/管制值按 pH 分档",
                "四分法缩分；风干过筛；记录采样层位",
                item("sample_depth", "采样深度", "number", "cm", 1, "0~20cm 耕作层"),
                item("sample_weight", "采样重量", "number", "kg", 1, "≥2kg 新鲜样"),
                item("air_dry", "风干处理", "bool", "", 1, "阴凉通风风干"),
                item("sieve", "过筛孔径", "number", "mm", 1, "2mm 尼龙筛")));

        // 16. 土壤 - 挥发性有机物 VOCs（HJ 605-2011）
        all.add(build("土壤", "挥发性有机物(VOCs)", "HJ 605-2011",
                "按 GB 36600 建设用地标准执行",
                "使用 VOC 专用采样瓶；少扰动；满瓶密封；4℃ 冷藏避光",
                item("sample_weight", "采样重量", "number", "g", 1, "≥5g 新鲜样"),
                item("seal", "密封状态", "bool", "", 1, "顶空瓶满瓶密封"),
                item("store_temp", "保存温度", "number", "℃", 1, "4℃ 冷藏"),
                item("light_proof", "避光保存", "bool", "", 1, "铝箔包裹避光")));

        // 17. 厂界噪声 - 等效 A 声级（GB 12348-2008）
        all.add(build("厂界噪声", "等效A声级", "GB 12348-2008",
                "昼间 60dB(A) / 夜间 50dB(A)（2 类声环境功能区）按功能区类别执行",
                "传声器距地面 1.2m；避开反射面；同时测背景噪声",
                item("measure_h", "测量高度", "number", "m", 1, "距地 1.2m"),
                item("measure_time", "测量时段", "text", "", 1, "昼间/夜间分别测量"),
                item("bg_noise", "背景噪声", "number", "dB(A)", 1, "同步测定背景值"),
                item("weather", "气象条件", "text", "", 1, "无雨雪、风速<5m/s"),
                item("point_pos", "测点位置", "text", "", 1, "厂界外 1m")));

        // 18. 室内空气 - 甲醛（GB/T 18883-2022）
        all.add(build("室内空气", "甲醛", "GB/T 18883-2022",
                "1 小时平均浓度限值 0.08 mg/m³",
                "采样前关闭门窗 12h；大型气泡吸收管；室温记录",
                item("close_time", "封闭时间", "number", "h", 1, "采样前封闭≥12h"),
                item("sample_time", "采样时间", "number", "min", 1, "连续采样 45min"),
                item("sample_flow", "采样流量", "number", "L/min", 1, "0.5L/min"),
                item("room_temp", "室内温度", "number", "℃", 1, "现场记录温湿度")));

        // 19. 室内空气 - 苯、甲苯、二甲苯（HJ 583-2010）
        all.add(build("室内空气", "苯、甲苯、二甲苯", "HJ 583-2010",
                "1 小时平均限值 苯 0.11 / 甲苯 0.20 / 二甲苯 0.20 mg/m³",
                "Tenax TA 吸附管；采样前封闭 12h；记录温湿度",
                item("close_time", "封闭时间", "number", "h", 1, "采样前封闭≥12h"),
                item("sample_time", "采样时间", "number", "min", 1, "连续采样 45min"),
                item("sample_flow", "采样流量", "number", "L/min", 1, "0.5L/min"),
                item("room_temp", "室内温度", "number", "℃", 1, "现场记录温湿度")));

        // 20. 固体废物 - 浸出液重金属（GB 5085.3-2007 / HJ 557-2010）
        all.add(build("固体废物", "浸出液重金属", "GB 5085.3-2007",
                "浸出毒性鉴别标准按项目分别执行",
                "硫酸硝酸法浸提；转速 30r/min；浸提 18h；测 pH",
                item("sample_weight", "样品重量", "number", "g", 1, "干基称取 100g"),
                item("extract_agent", "浸提剂", "text", "", 1, "硫酸硝酸混合液 pH≈3.2"),
                item("extract_time", "浸提时间", "number", "h", 1, "18±2h"),
                item("rotate_speed", "转速", "number", "r/min", 1, "30±2 r/min"),
                item("ph", "浸提液 pH", "number", "", 1, "浸提前后测 pH")));

        // 批量写入（每个配置级联写入明细）
        int n = 0;
        for (EmsSampleParamConfig c : all) {
            configService.saveConfig(c, c.getItems());
            n++;
        }
        log.info("[采样参数配置] 初始化完成，共写入 {} 条配置", n);
    }

    /** 构造主表 + 暂存明细（明细在 saveConfig 内回填 configId） */
    private EmsSampleParamConfig build(String type, String item, String standard,
                                       String limit, String remark, EmsSampleParamItem... items) {
        EmsSampleParamConfig c = new EmsSampleParamConfig();
        c.setType(type);
        c.setItem(item);
        c.setStandard(standard);
        c.setLimitValue(limit);
        c.setRemark(remark);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        List<EmsSampleParamItem> list = new ArrayList<>();
        int idx = 0;
        for (EmsSampleParamItem it : items) {
            it.setSortNo(idx++);
            list.add(it);
        }
        c.setItems(list);
        return c;
    }

    private EmsSampleParamItem item(String code, String name, String type, String unit, int required, String tip) {
        EmsSampleParamItem it = new EmsSampleParamItem();
        it.setCode(code);
        it.setName(name);
        it.setParamType(type);
        it.setUnit(unit);
        it.setRequired(required);
        it.setTip(tip);
        return it;
    }
}
