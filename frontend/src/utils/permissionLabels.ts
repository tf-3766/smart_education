// 权限码 → 中文名称。后端 permissions 为形如 `admin:access` 的编码，
// 前端统一映射为中文展示；未收录的编码回退为原码，保证不丢信息。
const PERMISSION_LABELS: Record<string, string> = {
  'admin:access': '管理控制台',
  'admin:manage': '用户与权限管理',
  'auth:profile:read': '查看个人资料',
  'teacher:access': '教师工作台',
  'student:access': '学生工作台',
}

export function permissionLabel(code: string): string {
  return PERMISSION_LABELS[code] ?? code
}

// 角色码 → 中文名称。
const ROLE_LABELS: Record<string, string> = {
  STUDENT: '学生',
  TEACHER: '教师',
  ADMIN: '管理员',
  SUPER_ADMIN: '超级管理员',
}

export function roleLabel(code: string): string {
  return ROLE_LABELS[code] ?? code
}
