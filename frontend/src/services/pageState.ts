import { ref } from 'vue'
import { RuntimeError } from './runtime'

export function usePageState() {
  const loading = ref(false)
  const error = ref<RuntimeError | null>(null)

  async function run<T>(operation: () => Promise<T>): Promise<T | undefined> {
    loading.value = true
    error.value = null
    try {
      return await operation()
    } catch (caught) {
      error.value = caught instanceof RuntimeError
        ? caught
        : new RuntimeError(caught instanceof Error ? caught.message : '请求失败，请稍后重试。')
      return undefined
    } finally {
      loading.value = false
    }
  }

  function clearError() {
    error.value = null
  }

  return { loading, error, run, clearError }
}
