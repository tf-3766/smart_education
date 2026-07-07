<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChatLineSquare,
  DataAnalysis,
  Document,
  House,
  MagicStick,
  PieChart,
  Reading,
  Setting,
  Tickets,
  User,
  Warning,
} from '@element-plus/icons-vue'
import type { NavItem, UserRole } from '../../types/ui'

const props = defineProps<{ role: UserRole }>()
const emit = defineEmits<{ navigate: [] }>()
const route = useRoute()
const router = useRouter()

const menus = computed<Record<UserRole, NavItem[]>>(() => ({
  student: [
    { label: '学习首页', path: '/student/dashboard', icon: House },
    { label: '我的课程', path: '/student/courses/data-structures/lessons/binary-tree', icon: Reading },
    { label: '学习任务', path: '/student/dashboard#tasks', icon: Tickets },
    { label: '成绩与进度', path: '/student/dashboard#progress', icon: DataAnalysis },
    { label: '互动交流', path: '/student/dashboard#announcements', icon: ChatLineSquare },
    { label: 'AI 学习助手', path: '/student/courses/data-structures/lessons/binary-tree#ai', icon: MagicStick },
  ],
  teacher: [
    { label: '教学工作台', path: '/teacher/dashboard', icon: House },
    { label: '课程管理', path: '/teacher/dashboard#courses', icon: Reading },
    { label: '作业与批改', path: '/teacher/assignments/tree-traversal/grading/li', icon: Document },
    { label: '考试与题库', path: '/teacher/exams/data-structures-final/paper', icon: Tickets },
    { label: '学情与预警', path: '/teacher/dashboard#warnings', icon: Warning },
    { label: '课程互动', path: '/teacher/dashboard#activity', icon: ChatLineSquare },
  ],
  admin: [
    { label: '数据看板', path: '/admin/dashboard', icon: DataAnalysis },
    { label: '用户管理', path: '/admin/dashboard#users', icon: User },
    { label: '课程治理', path: '/admin/dashboard#courses', icon: Reading },
    { label: '内容治理', path: '/admin/dashboard#reviews', icon: Document },
    { label: '数据统计', path: '/admin/dashboard#statistics', icon: PieChart },
    { label: 'AI 管理', path: '/admin/dashboard#alerts', icon: MagicStick },
    { label: '系统设置', path: '/admin/dashboard#settings', icon: Setting },
  ],
}))

const isActive = (path: string) => {
  const clean = path.split('#')[0]
  if (clean.includes('/assignments/')) return route.path.includes('/assignments/')
  if (clean.includes('/exams/')) return route.path.includes('/exams/')
  return route.path === clean
}

const navigate = async (path: string) => {
  await router.push(path)
  emit('navigate')
}
</script>

<template>
  <aside class="app-sidebar">
    <nav class="side-nav" aria-label="主导航">
      <button
        v-for="item in menus[role]"
        :key="item.label"
        class="side-nav-item"
        :class="{ active: isActive(item.path) }"
        type="button"
        @click="navigate(item.path)"
      >
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.label }}</span>
      </button>
    </nav>
    <div class="sidebar-foot">
      <div class="sidebar-foot-mark">校</div>
      <div>
        <strong>2026 春季学期</strong>
        <span>在线教学平台</span>
      </div>
    </div>
  </aside>
</template>
