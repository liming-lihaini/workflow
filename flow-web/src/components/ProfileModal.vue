<template>
  <a-modal
    :open="open"
    title="修改个人信息"
    :width="520"
    :confirm-loading="submitting"
    ok-text="保存"
    cancel-text="取消"
    @ok="handleSubmit"
    @cancel="$emit('update:open', false)"
  >
    <a-form :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }" style="margin-top: 16px">
      <a-form-item label="用户名">
        <a-input :value="form.username" disabled />
      </a-form-item>
      <a-form-item label="姓名">
        <a-input v-model:value="form.realName" placeholder="请输入姓名" />
      </a-form-item>
      <a-form-item label="邮箱">
        <a-input v-model:value="form.email" placeholder="请输入邮箱" />
      </a-form-item>
      <a-form-item label="手机号">
        <a-input v-model:value="form.phone" placeholder="请输入手机号" />
      </a-form-item>

      <a-divider style="margin: 12px 0">
        <a-checkbox v-model:checked="changePwd">修改登录密码</a-checkbox>
      </a-divider>

      <template v-if="changePwd">
        <a-form-item label="原密码" required>
          <a-input-password v-model:value="pwdForm.oldPassword" placeholder="请输入原密码" />
        </a-form-item>
        <a-form-item label="新密码" required>
          <a-input-password v-model:value="pwdForm.newPassword" placeholder="至少6位" />
        </a-form-item>
        <a-form-item label="确认新密码" required>
          <a-input-password v-model:value="pwdForm.confirmPassword" placeholder="再次输入新密码" />
        </a-form-item>
      </template>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, watch } from 'vue'
import { message } from 'ant-design-vue'
import { getProfile, updateProfile, changePassword } from '../api/profile'

const props = defineProps({
  open: { type: Boolean, default: false }
})
const emit = defineEmits(['update:open'])

const submitting = ref(false)
const changePwd = ref(false)
const form = reactive({ username: '', realName: '', email: '', phone: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirmPassword: '' })

watch(() => props.open, async (val) => {
  if (!val) return
  changePwd.value = false
  Object.assign(pwdForm, { oldPassword: '', newPassword: '', confirmPassword: '' })
  try {
    const res = await getProfile()
    const data = res.data || {}
    form.username = data.username
    form.realName = data.realName
    form.email = data.email
    form.phone = data.phone
  } catch {
    // 拦截器已提示
  }
})

async function handleSubmit() {
  if (changePwd.value) {
    if (!pwdForm.oldPassword) return message.warning('请输入原密码')
    if (!pwdForm.newPassword || pwdForm.newPassword.length < 6) return message.warning('新密码长度不能少于6位')
    if (pwdForm.newPassword !== pwdForm.confirmPassword) return message.warning('两次输入的新密码不一致')
  }
  submitting.value = true
  try {
    await updateProfile({ realName: form.realName, email: form.email, phone: form.phone })
    if (changePwd.value) {
      await changePassword({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
      message.success('个人信息已保存，密码修改成功')
    } else {
      message.success('个人信息已保存')
    }
    emit('update:open', false)
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}
</script>
