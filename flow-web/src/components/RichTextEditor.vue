<template>
  <div class="rich-text-editor" :class="{ 'is-readonly': readonly }">
    <div v-if="!readonly" class="rte-toolbar">
      <button type="button" class="rte-btn" title="加粗" @mousedown.prevent @click="exec('bold')"><b>B</b></button>
      <button type="button" class="rte-btn" title="斜体" @mousedown.prevent @click="exec('italic')"><i>I</i></button>
      <button type="button" class="rte-btn" title="下划线" @mousedown.prevent @click="exec('underline')"><u>U</u></button>
      <button type="button" class="rte-btn" title="删除线" @mousedown.prevent @click="exec('strikeThrough')"><s>S</s></button>
      <span class="rte-divider"></span>
      <button type="button" class="rte-btn" title="标题" @mousedown.prevent @click="exec('formatBlock', '<h3>')">H</button>
      <button type="button" class="rte-btn" title="正文" @mousedown.prevent @click="exec('formatBlock', '<p>')">¶</button>
      <span class="rte-divider"></span>
      <button type="button" class="rte-btn" title="无序列表" @mousedown.prevent @click="exec('insertUnorderedList')">•≡</button>
      <button type="button" class="rte-btn" title="有序列表" @mousedown.prevent @click="exec('insertOrderedList')">1≡</button>
      <span class="rte-divider"></span>
      <button type="button" class="rte-btn" title="字体颜色" @mousedown.prevent @click="pickColor">A<span class="color-bar"></span></button>
      <button type="button" class="rte-btn" title="清除格式" @mousedown.prevent @click="exec('removeFormat')">Tx</button>
      <span class="rte-divider"></span>
      <button type="button" class="rte-btn" title="撤销" @mousedown.prevent @click="exec('undo')">↺</button>
      <button type="button" class="rte-btn" title="重做" @mousedown.prevent @click="exec('redo')">↻</button>
      <input ref="colorInput" type="color" class="rte-color-input" @input="applyColor" />
    </div>
    <div
      ref="editorRef"
      class="rte-content"
      :contenteditable="!readonly"
      :data-placeholder="placeholder"
      @input="handleInput"
      @blur="handleInput"
    ></div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted } from 'vue'

const props = defineProps({
  /** HTML 内容 v-model:value */
  value: { type: String, default: '' },
  placeholder: { type: String, default: '请输入内容' },
  readonly: { type: Boolean, default: false }
})

const emit = defineEmits(['update:value'])

const editorRef = ref(null)
const colorInput = ref(null)
let internalChange = false

onMounted(() => {
  if (editorRef.value) editorRef.value.innerHTML = props.value || ''
})

// 外部值变化时同步到编辑区（避免光标跳动，仅在非内部输入时更新）
watch(() => props.value, (val) => {
  if (internalChange) { internalChange = false; return }
  if (editorRef.value && editorRef.value.innerHTML !== (val || '')) {
    editorRef.value.innerHTML = val || ''
  }
})

function handleInput() {
  if (!editorRef.value) return
  internalChange = true
  emit('update:value', editorRef.value.innerHTML)
}

function exec(command, arg = null) {
  editorRef.value?.focus()
  document.execCommand(command, false, arg)
  handleInput()
}

function pickColor() { colorInput.value?.click() }
function applyColor(e) { exec('foreColor', e.target.value) }
</script>

<style scoped>
.rich-text-editor { border: 1px solid #d9d9d9; border-radius: 6px; background: #fff; transition: border-color 0.2s; }
.rich-text-editor:focus-within { border-color: #1677ff; box-shadow: 0 0 0 2px rgba(22,119,255,0.1); }
.rich-text-editor.is-readonly { border: none; }
.rte-toolbar { display: flex; align-items: center; flex-wrap: wrap; gap: 2px; padding: 4px 6px; border-bottom: 1px solid #f0f0f0; background: #fafafa; border-radius: 6px 6px 0 0; position: relative; }
.rte-btn { min-width: 26px; height: 26px; padding: 0 5px; border: none; background: transparent; border-radius: 4px; cursor: pointer; font-size: 13px; color: #555; line-height: 1; display: inline-flex; align-items: center; justify-content: center; }
.rte-btn:hover { background: #e6f0ff; color: #1677ff; }
.rte-divider { width: 1px; height: 16px; background: #e0e0e0; margin: 0 4px; }
.color-bar { display: inline-block; width: 10px; height: 3px; background: linear-gradient(90deg, #f5222d, #faad14, #52c41a, #1677ff); margin-left: 2px; border-radius: 1px; }
.rte-color-input { position: absolute; width: 0; height: 0; opacity: 0; pointer-events: none; }
.rte-content { min-height: 120px; max-height: 400px; overflow-y: auto; padding: 8px 12px; font-size: 14px; line-height: 1.7; color: #333; outline: none; }
.rich-text-editor.is-readonly .rte-content { min-height: auto; padding: 4px 0; }
.rte-content:empty::before { content: attr(data-placeholder); color: #bbb; pointer-events: none; }
.rte-content :deep(h3) { margin: 6px 0; font-size: 16px; }
.rte-content :deep(p) { margin: 4px 0; }
.rte-content :deep(ul), .rte-content :deep(ol) { padding-left: 22px; margin: 4px 0; }
</style>
