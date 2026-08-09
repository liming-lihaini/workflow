验证结果
Webhook 配置已注册（API 查询确认）。
直接调用接口 POST /api/v1/webhooks/retain-status 成功将 LY20260808001 改为"销毁审批中"，再恢复"留样中"，测试数据无残留。
说明：

nodeId 当前为空（流程级，任意节点完成都会触发）。如需限定到 LYXHSQ 的某个具体审批节点，可在前端 Webhook 配置中填入该节点的实际 nodeId。
status 默认写为"销毁审批中"，可在配置中改为其他状态（如"已销毁"）。
url 使用 http://localhost:8080 自回调；部署到其它环境时需改为对应的外部可访问地址（或改为相对内部地址配置）。

EmsRetainService 新增 updateStatusByRetainNo(retainNo, status)（按 retainNo 更新 t_retain.status 及 updateTime）。

2. 配置到 LYXHSQ 流程节点
DatabaseMigration.initRetainDisposeWebhook() 在 wf_webhook 表插入记录（已验证 id=4）：

webhookKey=retain_dispose_status
processKey=LYXHSQ，triggerEvents=["NODE_COMPLETED"]
url=http://localhost:8080/api/v1/webhooks/retain-status
payloadTemplate={"retainNo":"${formData.retainNo}","status":"销毁审批中"}


服务任务
{
  "serviceType": "http",
  "url": "http://localhost:8080/api/v1/webhooks/retain-status/${status}",
  "method": "POST",
  "body": "{\"retainNo\":\"${retainNo}\"}",
  "resultVariable": "httpResp"
}


{"retainNo":"${formData.retainNo}","status":"销毁审批中"}，