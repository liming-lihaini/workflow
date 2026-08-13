-- 存量日志字典key转显示值
UPDATE t_sample_log SET detail = REPLACE(REPLACE(detail, 'sample_abnormal', '样品异常'), 'return_sample', '退样') WHERE detail REGEXP 'sample_abnormal|return_sample';
UPDATE t_sample_log SET detail = REPLACE(detail, 'data_abnormal', '数据异常') WHERE detail LIKE '%data_abnormal%';
UPDATE t_sample_log SET detail = REPLACE(detail, 'instrument_abnormal', '仪器异常') WHERE detail LIKE '%instrument_abnormal%';
UPDATE t_sample_log SET detail = REPLACE(detail, 'transport_abnormal', '运输异常') WHERE detail LIKE '%transport_abnormal%';
UPDATE t_sample_log SET detail = REPLACE(detail, 'retain_retest', '留样复测') WHERE detail LIKE '%retain_retest%';
UPDATE t_sample_log SET detail = REPLACE(detail, 'resample', '重采') WHERE detail LIKE '%resample%';
UPDATE t_sample_log SET detail = REPLACE(detail, 'scrap', '报废') WHERE detail LIKE '%scrap%';
UPDATE t_sample_log SET detail = REPLACE(detail, 'other', '其他') WHERE detail LIKE '%other%' AND action = '异常处置';
-- 存量样品处置字段字典key转显示值
UPDATE t_sample SET disposal_type = REPLACE(REPLACE(disposal_type, 'sample_abnormal', '样品异常'), 'data_abnormal', '数据异常') WHERE disposal_type IN ('sample_abnormal', 'data_abnormal');
UPDATE t_sample SET disposal_type = REPLACE(REPLACE(disposal_type, 'instrument_abnormal', '仪器异常'), 'transport_abnormal', '运输异常') WHERE disposal_type IN ('instrument_abnormal', 'transport_abnormal');
UPDATE t_sample SET disposal_method = REPLACE(REPLACE(disposal_method, 'return_sample', '退样'), 'retain_retest', '留样复测') WHERE disposal_method IN ('return_sample', 'retain_retest');
UPDATE t_sample SET disposal_method = REPLACE(REPLACE(disposal_method, 'resample', '重采'), 'scrap', '报废') WHERE disposal_method IN ('resample', 'scrap');
-- 验证结果
SELECT id, detail FROM t_sample_log WHERE id = 72;
SELECT id, barcode, status, disposal_type, disposal_method FROM t_sample WHERE id = 25;
