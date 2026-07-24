import { reactive, readonly } from 'vue'

export interface ConfirmDialogOptions {
  title?: string
  confirmLabel?: string
  cancelLabel?: string
}

const dialogState = reactive({
  open: false,
  title: '请确认操作',
  message: '',
  confirmLabel: '确认',
  cancelLabel: '取消',
})

let resolveCurrent: ((confirmed: boolean) => void) | null = null
let hostCount = 0

export const confirmDialogState = readonly(dialogState)

export function confirmDialog(message: string, options: ConfirmDialogOptions = {}): Promise<boolean> {
  if (hostCount === 0) return Promise.resolve(window.confirm(message))

  resolveCurrent?.(false)

  dialogState.title = options.title ?? '请确认操作'
  dialogState.message = message
  dialogState.confirmLabel = options.confirmLabel ?? '确认'
  dialogState.cancelLabel = options.cancelLabel ?? '取消'
  dialogState.open = true

  return new Promise<boolean>((resolve) => {
    resolveCurrent = resolve
  })
}

export function registerConfirmDialogHost() {
  hostCount += 1
  return () => {
    hostCount = Math.max(0, hostCount - 1)
  }
}

export function settleConfirmDialog(confirmed: boolean) {
  if (!dialogState.open) return

  dialogState.open = false
  const resolve = resolveCurrent
  resolveCurrent = null
  resolve?.(confirmed)
}
