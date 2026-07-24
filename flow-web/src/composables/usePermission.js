import { useUserStore } from '../stores/user'

/**
 * 权限控制组合式函数
 * 用法: const { hasPerm } = usePermission()
 *       v-if="hasPerm('process:definition:create')"
 */
export function usePermission() {
  const userStore = useUserStore()

  /** 判断是否拥有指定权限 */
  const hasPerm = (key) => userStore.hasPermission(key)

  /** 判断是否为管理员 */
  const isAdmin = () => userStore.isAdmin

  return { hasPerm, isAdmin }
}
