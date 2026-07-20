// AI 调用失败时优先展示后端返回的业务原因；未知错误使用统一提示。
export function aiErrorMessage(err: unknown): string {
  return err instanceof Error && err.message ? err.message : 'AI 暂不可用，请稍后重试。'
}