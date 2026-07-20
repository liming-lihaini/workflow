<template>
  <div class="designer-wrap">
    <div class="page-header">
      <span class="page-title">流程设计器</span>
      <a-button @click="handleOpenNew">新窗口打开</a-button>
    </div>
    <div class="designer-iframe">
      <iframe
        :src="designerUrl"
        frameborder="0"
        allowfullscreen
      />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const designerUrl = computed(() => {
  // 开发环境：React 设计器运行在 3001 端口
  if (import.meta.env.DEV) {
    return 'http://localhost:3001'
  }
  // 生产环境：独立部署地址或相对路径
  return '/designer'
})

function handleOpenNew() {
  window.open(designerUrl.value, '_blank')
}
</script>

<style scoped>
.designer-wrap {
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}

.designer-iframe {
  flex: 1;
  border-radius: var(--radius-lg);
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.designer-iframe iframe {
  width: 100%;
  height: 100%;
}
</style>
