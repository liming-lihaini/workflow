// 节点类型配置 - 对应后端 NodeType 枚举
export const NODE_TYPES_CONFIG = {
  start: {
    label: '开始节点',
    color: '#00b42a',
    icon: '▶',
    group: '基础',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', default: '开始' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  end: {
    label: '结束节点',
    color: '#f53f3f',
    icon: '■',
    group: '基础',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', default: '结束' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  userTask: {
    label: '用户任务',
    color: '#1677ff',
    icon: '👤',
    group: '任务',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', required: true },
      { key: 'assigneeType', label: '分配方式', type: 'select', options: ['user', 'role', 'dept', 'expression'], default: 'user' },
      { key: 'assignee', label: '处理人', type: 'input', placeholder: '用户ID/角色Key/表达式' },
      { key: 'candidateUsers', label: '候选人', type: 'input', placeholder: '多个用逗号分隔' },
      { key: 'dueDate', label: '截止时间', type: 'input', placeholder: '如: 2024-12-31' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  serviceTask: {
    label: '服务任务',
    color: '#722ed1',
    icon: '⚙',
    group: '任务',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', required: true },
      { key: 'serviceType', label: '服务类型', type: 'select', options: ['http', 'java', 'script', 'webhook'], default: 'http' },
      { key: 'serviceUrl', label: '服务地址', type: 'input', placeholder: 'HTTP URL 或 Java 类名' },
      { key: 'inputParams', label: '输入参数', type: 'textarea', placeholder: 'JSON 格式' },
      { key: 'outputParams', label: '输出参数', type: 'textarea', placeholder: 'JSON 格式' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  scriptTask: {
    label: '脚本任务',
    color: '#13c2c2',
    icon: '📝',
    group: '任务',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', required: true },
      { key: 'scriptType', label: '脚本类型', type: 'select', options: ['javascript', 'groovy', 'python'], default: 'javascript' },
      { key: 'script', label: '脚本内容', type: 'textarea', placeholder: '输入脚本代码' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  exclusiveGateway: {
    label: '排他网关',
    color: '#ff7d00',
    icon: '✕',
    group: '网关',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', default: '排他网关' },
      { key: 'defaultFlow', label: '默认分支', type: 'input', placeholder: '默认连线的 targetNodeId' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  parallelGateway: {
    label: '并行网关',
    color: '#faad14',
    icon: '＋',
    group: '网关',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', default: '并行网关' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  inclusiveGateway: {
    label: '包容网关',
    color: '#eb2f96',
    icon: '⊕',
    group: '网关',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', default: '包容网关' },
      { key: 'defaultFlow', label: '默认分支', type: 'input', placeholder: '默认连线的 targetNodeId' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  subProcess: {
    label: '子流程',
    color: '#2f54eb',
    icon: '📋',
    group: '高级',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', required: true },
      { key: 'processKey', label: '子流程标识', type: 'input', placeholder: '关联的流程定义 processKey' },
      { key: 'inheritVariables', label: '继承变量', type: 'select', options: ['true', 'false'], default: 'true' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  },
  custom: {
    label: '自定义节点',
    color: '#86909c',
    icon: '⬡',
    group: '高级',
    schema: [
      { key: 'name', label: '节点名称', type: 'input', required: true },
      { key: 'handlerClass', label: '处理器类名', type: 'input', placeholder: '自定义 NodeHandler 全限定类名' },
      { key: 'customConfig', label: '自定义配置', type: 'textarea', placeholder: 'JSON 格式自定义配置' },
      { key: 'description', label: '描述', type: 'textarea' }
    ]
  }
}

// 节点分组
export const NODE_GROUPS = {
  '基础': ['start', 'end'],
  '任务': ['userTask', 'serviceTask', 'scriptTask'],
  '网关': ['exclusiveGateway', 'parallelGateway', 'inclusiveGateway'],
  '高级': ['subProcess', 'custom']
}

// 连线条件 Schema
export const EDGE_SCHEMA = [
  { key: 'condition', label: '条件表达式', type: 'input', placeholder: '如: ${days > 3}' },
  { key: 'name', label: '分支名称', type: 'input' },
  { key: 'priority', label: '优先级', type: 'select', options: ['1', '2', '3', '4', '5'], default: '1' }
]
