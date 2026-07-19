import type { ApiMode } from '@/types/domain'

export function getApiMode(env: Record<string, string | undefined> = import.meta.env): ApiMode {
  return env.VITE_API_MODE === 'real' ? 'real' : 'demo'
}

export function createTraceId() {
  return `trace-${Date.now().toString(36)}-${Math.random().toString(36).slice(2, 7)}`
}

export class RuntimeError extends Error {
  constructor(
    message: string,
    public readonly traceId = createTraceId(),
    public readonly code = 'RUNTIME_ERROR',
    public readonly status?: number,
  ) {
    super(message)
    this.name = 'RuntimeError'
  }
}

export async function demoDelay<T>(value: T, ms = 90): Promise<T> {
  await new Promise<void>((resolve) => window.setTimeout(resolve, ms))
  return structuredClone(value)
}
