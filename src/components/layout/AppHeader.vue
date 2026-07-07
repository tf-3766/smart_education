<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Menu, QuestionFilled, Reading, Search } from '@element-plus/icons-vue'
import type { UserRole } from '../../types/ui'

const props = defineProps<{
  role: UserRole
  compact?: boolean
}>()

const emit = defineEmits<{
  menu: []
}>()

const router = useRouter()

const profile = computed(() => {
  if (props.role === 'teacher') return { name: '张老师', role: '教师', avatar: '张' }
  if (props.role === 'admin') return { name: '系统管理员', role: '管理员', avatar: '管' }
  return { name: '李同学', role: '学生', avatar: '李' }
})

const switchRole = (role: UserRole) => {
  const target = role === 'student' ? '/student/dashboard' : role === 'teacher' ? '/teacher/dashboard' : '/admin/dashboard'
  router.push(target)
}
</script>

<template>
  <header class="app-header" :class="{ 'app-header--compact': compact }">
    <div class="brand-wrap">
      <button class="mobile-menu" type="button" aria-label="打开菜单" @click="emit('menu')">
        <el-icon><Menu /></el-icon>
      </button>
      <button class="brand" type="button" @click="router.push(`/${role}/dashboard`)" aria-label="返回工作台">
        <span class="brand-mark"><el-icon><Reading /></el-icon></span>
        <span>智学课堂</span>
      </button>
    </div>

    <div v-if="!compact" class="global-search">
      <el-icon><Search /></el-icon>
      <span>搜索课程、任务、资料</span>
      <kbd>Ctrl K</kbd>
    </div>
    <slot name="center" />

    <div class="header-actions">
      <button class="header-icon" type="button" aria-label="通知">
        <el-icon><Bell /></el-icon>
        <span class="notification-dot">3</span>
      </button>
      <button class="header-icon header-help" type="button" aria-label="帮助">
        <el-icon><QuestionFilled /></el-icon>
      </button>
      <el-dropdown trigger="click" @command="switchRole">
        <button class="profile-button" type="button">
          <span class="avatar" :class="`avatar--${role}`">{{ profile.avatar }}</span>
          <span class="profile-copy">
            <strong>{{ profile.name }}</strong>
            <small>{{ profile.role }}</small>
          </span>
          <span class="profile-caret">⌄</span>
        </button>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="student">切换到学生端</el-dropdown-item>
            <el-dropdown-item command="teacher">切换到教师端</el-dropdown-item>
            <el-dropdown-item command="admin">切换到管理员端</el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>
