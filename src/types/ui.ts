import type { Component } from 'vue'

export type UserRole = 'student' | 'teacher' | 'admin'

export interface NavItem {
  label: string
  path: string
  icon: Component
}

export interface MetricItem {
  label: string
  value: string
  detail: string
  tone: 'blue' | 'teal' | 'orange' | 'purple'
  icon: Component
}
