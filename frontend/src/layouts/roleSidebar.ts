import {
  Archive,
  BarChart3,
  BookOpen,
  CalendarClock,
  ClipboardCheck,
  FileQuestion,
  Gauge,
  LayoutGrid,
  MessageSquare,
  ShieldCheck,
  Sparkles,
  Users,
  type LucideIcon,
} from 'lucide-vue-next'
import type { Role } from '@/types/domain'

export type SidebarPrimaryKey = 'all' | 'resources'

export interface RoleSidebarItem {
  label: string
  to: string
  icon?: LucideIcon
  meta?: string
  matchPrefixes?: string[]
}

export interface RoleSidebarPrimaryItem {
  key: SidebarPrimaryKey
  label: string
  icon: LucideIcon
}

export interface RoleSidebarSecondaryMenu {
  title: string
  items: RoleSidebarItem[]
}

export interface RoleSidebarDetailParent {
  label: string
  icon?: LucideIcon
  children: RoleSidebarItem[]
}

export type RoleSidebarDetailItem = RoleSidebarItem | RoleSidebarDetailParent

export interface RoleSidebarGroup {
  title?: string
  items: RoleSidebarDetailItem[]
}

export interface RoleSidebarConfig {
  navigationLabel: string
  description: string
  primary: RoleSidebarPrimaryItem[]
  secondary: Record<SidebarPrimaryKey, RoleSidebarSecondaryMenu>
  detailGroups: RoleSidebarGroup[]
}

const studentItems: RoleSidebarItem[] = [
  { label: '学习首页', to: '/student/dashboard', icon: Gauge },
  { label: '我的课程', to: '/student/courses', icon: BookOpen, matchPrefixes: ['/student/lessons/'] },
  { label: '选课中心', to: '/student/enroll', icon: LayoutGrid },
  { label: '学习任务', to: '/student/assignments', icon: ClipboardCheck },
  { label: '考试安排', to: '/student/exams', icon: FileQuestion },
  { label: '成绩进度', to: '/student/grades', icon: BarChart3 },
  { label: '课程互动', to: '/student/forum', icon: MessageSquare },
]

const teacherItems: RoleSidebarItem[] = [
  { label: '教学工作台', to: '/teacher/dashboard', icon: Gauge },
  { label: '课程管理', to: '/teacher/courses', icon: BookOpen },
  { label: '作业批改', to: '/teacher/assignments', icon: ClipboardCheck },
  { label: '考试题库', to: '/teacher/exams', icon: FileQuestion },
  { label: '学情预警', to: '/teacher/warnings', icon: BarChart3 },
  { label: '课程互动', to: '/teacher/forum', icon: MessageSquare },
]

const adminItems: RoleSidebarItem[] = [
  { label: '数据看板', to: '/admin/dashboard', icon: Gauge },
  { label: '用户管理', to: '/admin/users', icon: Users },
  { label: '课程治理', to: '/admin/course-reviews', icon: ShieldCheck },
  { label: '学期选课时间', to: '/admin/term-enrollment', icon: CalendarClock },
  { label: '内容治理', to: '/admin/content', icon: MessageSquare },
  { label: '数据统计', to: '/admin/statistics', icon: BarChart3 },
  { label: 'AI 设置', to: '/admin/ai', icon: Sparkles },
]

const sidebars: Record<Role, RoleSidebarConfig> = {
  student: {
    navigationLabel: '学习入口',
    description: '常用学习功能会显示在这里。您可以从全部学习功能中打开课程、任务和成绩工具。',
    primary: [
      { key: 'all', label: '全部学习功能', icon: LayoutGrid },
      { key: 'resources', label: '我的学习资源', icon: BookOpen },
    ],
    secondary: {
      all: { title: '全部学习功能', items: studentItems },
      resources: {
        title: '我的学习资源',
        items: [
          { ...studentItems[1], meta: '2 门在学' },
          { ...studentItems[3], meta: '2 项待完成' },
          { ...studentItems[4], meta: '1 场近期考试' },
        ],
      },
    },
    detailGroups: [
      {
        title: '学习管理',
        items: [
          studentItems[0],
          studentItems[1],
          studentItems[2],
          { label: '任务与考试', icon: ClipboardCheck, children: studentItems.slice(3, 5) },
        ],
      },
      { title: '学习记录', items: studentItems.slice(5) },
    ],
  },
  teacher: {
    navigationLabel: '教学入口',
    description: '常用教学功能会显示在这里。您可以从全部教学功能中打开课程、作业和学情工具。',
    primary: [
      { key: 'all', label: '全部教学功能', icon: LayoutGrid },
      { key: 'resources', label: '我的教学资源', icon: Archive },
    ],
    secondary: {
      all: { title: '全部教学功能', items: teacherItems },
      resources: {
        title: '我的教学资源',
        items: [
          { ...teacherItems[1], meta: '3 门课程' },
          { ...teacherItems[2], meta: '2 项待批改' },
          { ...teacherItems[3], meta: '3 套题库' },
        ],
      },
    },
    detailGroups: [
      {
        title: '教学管理',
        items: [
          teacherItems[0],
          teacherItems[1],
          { label: '作业与考试', icon: ClipboardCheck, children: teacherItems.slice(2, 4) },
        ],
      },
      { title: '教学支持', items: teacherItems.slice(4) },
    ],
  },
  admin: {
    navigationLabel: '管理入口',
    description: '常用管理功能会显示在这里。您可以从全部管理功能中打开用户、课程和数据工具。',
    primary: [
      { key: 'all', label: '全部管理功能', icon: LayoutGrid },
      { key: 'resources', label: '我的管理事项', icon: Users },
    ],
    secondary: {
      all: { title: '全部管理功能', items: adminItems },
      resources: {
        title: '我的管理事项',
        items: [
          { ...adminItems[1], meta: '6 位用户' },
          { ...adminItems[2], meta: '1 项待审核' },
          { ...adminItems[4], meta: '3 项待处理' },
        ],
      },
    },
    detailGroups: [
      {
        title: '平台管理',
        items: [
          adminItems[0],
          adminItems[1],
          { label: '课程与内容', icon: ShieldCheck, children: adminItems.slice(2, 5) },
        ],
      },
      { title: '数据中心', items: adminItems.slice(5) },
    ],
  },
}

/**
 * 「用户管理」等入口仅超级管理员可用（后端 admin:manage）。普通被授权的管理员传入
 * { superAdmin: false } 时过滤掉这些入口，避免出现点进去必然 403 的死路。
 */
function filterAdminSidebar(
  config: RoleSidebarConfig,
  allow: (item: RoleSidebarItem) => boolean,
): RoleSidebarConfig {
  const filterFlat = (items: RoleSidebarItem[]) => items.filter(allow)
  const filterDetail = (items: RoleSidebarDetailItem[]): RoleSidebarDetailItem[] =>
    items
      .map((item) => (isDetailParent(item) ? { ...item, children: item.children.filter(allow) } : item))
      .filter((item) => (isDetailParent(item) ? item.children.length > 0 : allow(item)))
  return {
    ...config,
    secondary: {
      all: { ...config.secondary.all, items: filterFlat(config.secondary.all.items) },
      resources: { ...config.secondary.resources, items: filterFlat(config.secondary.resources.items) },
    },
    detailGroups: config.detailGroups.map((group) => ({ ...group, items: filterDetail(group.items) })),
  }
}

export function getRoleSidebar(role: Role, options: { superAdmin?: boolean } = {}) {
  const config = sidebars[role]
  if (role === 'admin' && options.superAdmin === false) {
    return filterAdminSidebar(config, (item) => item.to !== '/admin/users')
  }
  return config
}

export function isDetailParent(item: RoleSidebarDetailItem): item is RoleSidebarDetailParent {
  return 'children' in item
}

export function flattenDetailItems(groups: RoleSidebarGroup[]) {
  return groups.flatMap((group) => group.items.flatMap((item) => isDetailParent(item) ? item.children : [item]))
}
