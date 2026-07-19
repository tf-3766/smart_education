<template>
  <div>
    <div class="page-header"><div><h1 class="page-title">学期选课时间</h1><p class="page-subtitle">按学期统一设置选课窗口；教师建课时选课时间留空则继承本学期设置。</p></div></div>
    <div v-if="message" class="toast">{{ message }}</div>
    <AsyncState :loading="state.loading.value" :error="state.error.value" :retry="load" />

    <section class="panel">
      <h2 class="panel-title">新增 / 更新学期窗口</h2>
      <div class="form-grid push-top">
        <div><label class="field-label" for="te-term">学期</label><select id="te-term" v-model="form.term" class="select"><option value="">请选择学期</option><option v-for="term in termOptions" :key="term" :value="term">{{ term }}</option></select></div>
        <div><label class="field-label" for="te-open">选课开始</label><input id="te-open" v-model="form.enrollmentOpenAt" class="input" type="datetime-local" /></div>
        <div><label class="field-label" for="te-close">选课截止</label><input id="te-close" v-model="form.enrollmentCloseAt" class="input" type="datetime-local" /></div>
      </div>
      <p class="muted" style="margin: 8px 0 0; font-size: 12.5px">两端留空表示该端不限时。同一学期已存在则覆盖更新。</p>
      <p v-if="formError" class="form-error" role="alert">{{ formError }}</p>
      <div class="form-actions"><AppButton variant="primary" :loading="state.loading.value" :disabled="!form.term" @click="save">保存学期窗口</AppButton></div>
    </section>

    <section class="panel flush">
      <div class="panel-head"><h2>已配置学期</h2><span class="count">共 {{ windows.length }} 个</span></div>
      <div class="table-scroll"><table class="table">
        <thead><tr><th>学期</th><th>选课开始</th><th>选课截止</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="w in windows" :key="w.windowId"><td class="cell-strong">{{ w.term }}</td><td>{{ w.enrollmentOpenAt ? formatTime(w.enrollmentOpenAt) : '不限' }}</td><td>{{ w.enrollmentCloseAt ? formatTime(w.enrollmentCloseAt) : '不限' }}</td><td class="cell-actions"><button class="text-link" @click="edit(w)">编辑</button></td></tr>
          <tr v-if="!windows.length"><td colspan="4" class="list-empty">尚未配置任何学期选课窗口</td></tr>
        </tbody>
      </table></div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { formatDateTime } from '@/utils/datetime'
import { computed, onMounted, reactive, ref } from 'vue'
import AppButton from '@/components/AppButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import { termEnrollmentWindowsApi } from '@/services/api'
import type { TermEnrollmentWindowVO } from '@/services/api/types'
import { usePageState } from '@/services/pageState'

const state = usePageState()
const windows = ref<TermEnrollmentWindowVO[]>([])
const message = ref('')
const formError = ref('')
const form = reactive({ term: '', enrollmentOpenAt: '', enrollmentCloseAt: '' })
const formatTime = formatDateTime

// 与教师建课学期选项一致：「YYYY 春季/秋季」，范围取当年 ±1。
const termOptions = computed(() => {
  const year = new Date().getFullYear()
  const options: string[] = []
  for (let y = year - 1; y <= year + 1; y += 1) options.push(`${y} 春季`, `${y} 秋季`)
  return options
})

function flash(text: string) { message.value = text; window.setTimeout(() => (message.value = ''), 2200) }

async function load() {
  const rows = await state.run(() => termEnrollmentWindowsApi.list())
  if (rows) windows.value = rows
}

function edit(w: TermEnrollmentWindowVO) {
  form.term = w.term
  form.enrollmentOpenAt = w.enrollmentOpenAt ? w.enrollmentOpenAt.slice(0, 16) : ''
  form.enrollmentCloseAt = w.enrollmentCloseAt ? w.enrollmentCloseAt.slice(0, 16) : ''
  formError.value = ''
}

async function save() {
  formError.value = ''
  if (form.enrollmentOpenAt && form.enrollmentCloseAt && new Date(form.enrollmentOpenAt) >= new Date(form.enrollmentCloseAt)) {
    formError.value = '选课截止时间需晚于开始时间'; return
  }
  const result = await state.run(() => termEnrollmentWindowsApi.upsert({
    term: form.term,
    enrollmentOpenAt: form.enrollmentOpenAt ? new Date(form.enrollmentOpenAt).toISOString() : null,
    enrollmentCloseAt: form.enrollmentCloseAt ? new Date(form.enrollmentCloseAt).toISOString() : null,
  }))
  if (result) { flash('学期选课窗口已保存'); await load() }
  else formError.value = state.error.value?.message ?? '保存失败，请稍后重试。'
}

onMounted(load)
</script>
