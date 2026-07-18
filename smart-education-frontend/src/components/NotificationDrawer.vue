<template>
  <Teleport to="body">
    <div v-if="open" class="inbox-layer" @keydown.esc.prevent="$emit('close')">
      <button class="inbox-backdrop" aria-label="关闭站内信" @click="$emit('close')" />
      <aside ref="drawer" id="notificationDrawer" class="inbox-drawer" role="dialog" aria-modal="true" aria-labelledby="inboxTitle" tabindex="-1">
        <header class="inbox-header">
          <h2 id="inboxTitle">站内信</h2>
          <div class="inbox-header-actions">
            <button @click="showAllMessages">查看更多</button>
            <button :aria-expanded="subscriptionsOpen" aria-controls="inboxSubscriptions" @click="toggleSubscriptions">订阅管理</button>
            <button class="inbox-icon-button" aria-label="关闭站内信" @click="$emit('close')"><X :size="22" /></button>
          </div>
        </header>

        <div class="inbox-controls">
          <div class="inbox-filters">
            <div class="inbox-segmented" aria-label="消息状态">
              <button :class="{ active: notifications.statusFilter === 'all' }" :aria-pressed="notifications.statusFilter === 'all'" @click="notifications.statusFilter = 'all'">全部</button>
              <button :class="{ active: notifications.statusFilter === 'unread' }" :aria-pressed="notifications.statusFilter === 'unread'" @click="notifications.statusFilter = 'unread'">未读</button>
            </div>
            <label class="inbox-category-select">
              <span class="sr-only">消息分类</span>
              <select v-model="notifications.categoryFilter" aria-label="消息分类">
                <option value="all">全部 / 全部</option>
                <option value="course">教学 / 课程消息</option>
                <option value="assignment">教学 / 作业消息</option>
                <option value="exam">教学 / 考试消息</option>
                <option value="warning">风险 / 预警消息</option>
                <option value="system">平台 / 系统消息</option>
              </select>
              <ChevronDown :size="18" />
            </label>
            <button class="inbox-read-all" :disabled="notifications.unreadCount === 0 || notifications.loading" @click="void notifications.markAllRead()">
              <CircleCheck :size="18" />
              <span>全部已读</span>
            </button>
          </div>
          <div v-if="subscriptionsOpen" id="inboxSubscriptions" class="inbox-subscriptions">
            <strong>订阅类型</strong>
            <label v-for="option in subscriptionOptions" :key="option.value">
              <input
                type="checkbox"
                :checked="notifications.subscribedCategories.includes(option.value)"
                :disabled="notifications.preferencesLoading"
                @change="onSubscriptionChange(option.value, $event)"
              />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>

        <div class="inbox-list" aria-live="polite">
          <div v-if="notifications.loading" class="inbox-empty">
            <span>消息加载中...</span>
          </div>
          <div v-else-if="notifications.error" class="inbox-empty inbox-error">
            <strong>消息加载失败</strong>
            <span>{{ notifications.error }}</span>
            <button @click="void notifications.load()">重试</button>
          </div>
          <template v-else>
            <article
              v-for="item in notifications.visibleItems"
              :key="item.id"
              class="inbox-message"
              :class="{ unread: !item.read }"
            >
              <button class="inbox-message-open" @click="void openMessage(item)">
                <strong :title="item.title">{{ item.title }}</strong>
                <span v-if="item.content" class="inbox-message-content">{{ item.content }}</span>
                <span class="inbox-message-meta">
                  <span class="inbox-message-tag">{{ item.categoryLabel }}</span>
                  <time :datetime="item.timestamp" :title="item.timestamp">{{ formatTimestamp(item.timestamp) }}</time>
                </span>
              </button>
              <button class="inbox-message-archive" :aria-label="`归档${item.title}`" title="归档" @click="void notifications.archive(item.id)">
                <Archive :size="17" />
              </button>
            </article>
            <div v-if="notifications.visibleItems.length === 0" class="inbox-empty">
              <CircleCheck :size="32" />
              <strong>{{ notifications.statusFilter === 'unread' ? '暂无未读消息' : '暂无消息' }}</strong>
              <span>{{ notifications.statusFilter === 'unread' ? '所有站内信都已处理' : '当前筛选条件下没有消息' }}</span>
            </div>
            <button v-if="notifications.hasMore" class="inbox-load-more" :disabled="notifications.loading" @click="void notifications.loadMore()">
              加载更多
            </button>
          </template>
        </div>
      </aside>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'
import { Archive, ChevronDown, CircleCheck, X } from 'lucide-vue-next'
import { useRouter } from 'vue-router'
import { notificationTarget, useNotificationStore } from '@/stores/notifications'
import type { InboxNotification, NotificationCategory } from '@/stores/notifications'
import { useSessionStore } from '@/stores/session'

const props = defineProps<{ open: boolean }>()
const emit = defineEmits<{ close: [] }>()

const router = useRouter()
const session = useSessionStore()
const notifications = useNotificationStore()
const subscriptionsOpen = ref(false)
const drawer = ref<HTMLElement | null>(null)
let lastFocused: HTMLElement | null = null
const subscriptionOptions: { value: NotificationCategory; label: string }[] = [
  { value: 'course', label: '课程消息' },
  { value: 'assignment', label: '作业消息' },
  { value: 'exam', label: '考试消息' },
  { value: 'warning', label: '预警消息' },
  { value: 'system', label: '系统消息' },
]

watch(() => props.open, (open) => {
  if (open) {
    lastFocused = document.activeElement as HTMLElement | null
    void notifications.load()
    void nextTick(() => drawer.value?.focus())
  } else {
    lastFocused?.focus?.()
    lastFocused = null
  }
}, { immediate: true })

function formatTimestamp(timestamp: string) {
  const date = new Date(timestamp)
  if (Number.isNaN(date.getTime())) return timestamp
  return date.toLocaleString('zh-CN', { hour12: false })
}

function toggleSubscriptions() {
  subscriptionsOpen.value = !subscriptionsOpen.value
  if (subscriptionsOpen.value) void notifications.loadPreferences()
}

function onSubscriptionChange(category: NotificationCategory, event: Event) {
  void notifications.setCategorySubscription(category, (event.target as HTMLInputElement).checked)
}

async function openMessage(item: InboxNotification) {
  await notifications.markRead(item.id)
  const target = notificationTarget(item, session.currentRole)
  if (!target) return
  emit('close')
  await router.push(target)
}

function showAllMessages() {
  notifications.statusFilter = 'all'
  notifications.categoryFilter = 'all'
  subscriptionsOpen.value = false
}
</script>
