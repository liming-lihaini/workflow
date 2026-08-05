import { ref } from 'vue'
import { getUsers } from '../api/system'

/**
 * 用户名 -> 真实姓名 映射工具。
 * 用于在流程列表、详情页将后端返回的 username/登录名解析为真实姓名展示。
 * 后端 /system/users 返回全量用户，本地构建 username->realName 映射；
 * 单用户解析失败降级为原用户名显示。
 */
export function useUserMap() {
  const userMap = ref({})

  async function buildUserMap() {
    try {
      const res = await getUsers()
      const data = res.data || res
      const records = Array.isArray(data) ? data : (data.list || data.records || [])
      const next = {}
      records.forEach(u => {
        if (u.username) next[u.username] = u.realName || u.username
      })
      userMap.value = next
    } catch {
      // 解析失败不阻塞页面，保留原用户名显示
    }
  }

  // 取真实姓名，缺失时降级为原用户名
  function realName(username) {
    if (!username) return '-'
    return userMap.value[username] || username
  }

  return { userMap, buildUserMap, realName }
}

