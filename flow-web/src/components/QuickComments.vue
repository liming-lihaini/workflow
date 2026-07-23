<template>
  <div class="quick-comments">
    <div class="qc-label">
      <span>快捷意见</span>
      <a-button type="link" size="small" @click="showAddInput = !showAddInput">
        + 自定义
      </a-button>
    </div>
    <div class="qc-tags">
      <a-tag
        v-for="(item, idx) in allComments"
        :key="idx"
        class="qc-tag"
        :color="item.isCustom ? 'orange' : 'default'"
        @click="$emit('select', item.text)"
      >
        {{ item.text }}
        <CloseOutlined
          v-if="item.isCustom"
          class="qc-tag-close"
          @click.stop="removeCustom(idx)"
        />
      </a-tag>
    </div>
    <div v-if="showAddInput" class="qc-add">
      <a-input
        v-model:value="newComment"
        placeholder="输入自定义意见，回车保存"
        size="small"
        style="width: 200px"
        @pressEnter="addCustom"
      />
      <a-button size="small" type="primary" @click="addCustom" :disabled="!newComment.trim()">
        添加
      </a-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { CloseOutlined } from '@ant-design/icons-vue'

defineEmits(['select'])

const STORAGE_KEY = 'flow_quick_comments'

const presetComments = [
  { text: '同意', isCustom: false },
  { text: '同意，请继续办理', isCustom: false },
  { text: '已阅', isCustom: false },
  { text: '退回修改', isCustom: false },
  { text: '不同意', isCustom: false },
  { text: '请补充材料', isCustom: false },
]

const customComments = ref([])
const showAddInput = ref(false)
const newComment = ref('')

const allComments = computed(() => {
  return [...presetComments, ...customComments.value.map(t => ({ text: t, isCustom: true }))]
})

function loadCustom() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) customComments.value = JSON.parse(saved)
  } catch { customComments.value = [] }
}

function saveCustom() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(customComments.value))
}

function addCustom() {
  const text = newComment.value.trim()
  if (!text) return
  if (customComments.value.includes(text)) {
    newComment.value = ''
    return
  }
  customComments.value.push(text)
  saveCustom()
  newComment.value = ''
  showAddInput.value = false
}

function removeCustom(idx) {
  customComments.value.splice(idx - presetComments.length, 1)
  saveCustom()
}

onMounted(loadCustom)
</script>

<style scoped>
.quick-comments { margin-bottom: 8px; }
.qc-label {
  display: flex; align-items: center; justify-content: space-between;
  font-size: 12px; color: #888; margin-bottom: 6px;
}
.qc-tags { display: flex; flex-wrap: wrap; gap: 4px; }
.qc-tag {
  cursor: pointer; user-select: none; font-size: 12px;
  transition: all 0.2s;
}
.qc-tag:hover { opacity: 0.8; transform: scale(1.03); }
.qc-tag-close { font-size: 10px; margin-left: 2px; }
.qc-add { display: flex; gap: 6px; margin-top: 6px; align-items: center; }
</style>
