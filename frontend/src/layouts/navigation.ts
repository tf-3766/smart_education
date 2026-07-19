import { BarChart3, BookOpen, CalendarClock, ClipboardCheck, FileQuestion, LayoutDashboard, MessageSquare, ShieldCheck, Users, type LucideIcon } from 'lucide-vue-next'
import type { Role } from '@/types/domain'

export interface NavigationItem { label: string; to: string; icon: LucideIcon }
export interface NavigationSection { title: '工作台' | '教学业务' | '管理'; items: NavigationItem[] }

const items: Record<Role, NavigationItem[]> = {
  student: [
    { label: '学习首页', to: '/student/dashboard', icon: LayoutDashboard },
    { label: '我的课程', to: '/student/courses', icon: BookOpen },
    { label: '学习任务', to: '/student/assignments', icon: ClipboardCheck },
    { label: '考试安排', to: '/student/exams', icon: FileQuestion },
    { label: '成绩进度', to: '/student/grades', icon: BarChart3 },
    { label: '课程互动', to: '/student/forum', icon: MessageSquare },
  ],
  teacher: [
    { label: '教学工作台', to: '/teacher/dashboard', icon: LayoutDashboard },
    { label: '课程管理', to: '/teacher/courses', icon: BookOpen },
    { label: '作业批改', to: '/teacher/assignments', icon: ClipboardCheck },
    { label: '考试题库', to: '/teacher/exams', icon: FileQuestion },
    { label: '学情预警', to: '/teacher/warnings', icon: BarChart3 },
    { label: '课程互动', to: '/teacher/forum', icon: MessageSquare },
  ],
  admin: [
    { label: '数据看板', to: '/admin/dashboard', icon: LayoutDashboard },
    { label: '用户管理', to: '/admin/users', icon: Users },
    { label: '课程治理', to: '/admin/course-reviews', icon: ShieldCheck },
    { label: '学期选课时间', to: '/admin/term-enrollment', icon: CalendarClock },
    { label: '内容治理', to: '/admin/content', icon: MessageSquare },
    { label: '数据统计', to: '/admin/statistics', icon: BarChart3 },
  ],
}

export function getNavigationSections(role: Role): NavigationSection[] {
  const roleItems = items[role]
  const workbench = roleItems.filter((item) => item.to.endsWith('/dashboard'))
  const remaining = roleItems.filter((item) => !workbench.includes(item))
  return role === 'admin'
    ? [{ title: '工作台', items: workbench }, { title: '管理', items: remaining }]
    : [{ title: '工作台', items: workbench }, { title: '教学业务', items: remaining }]
}
