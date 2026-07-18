import { getAiKey } from './aiKey'

// AI 调用失败时的统一提示：未配置 BYO 密钥时给出明确引导，
// 否则透出后端返回的具体原因，避免只显示笼统的「暂不可用」。
export function aiErrorMessage(err: unknown): string {
  if (!getAiKey()) {
    return 'AI 未配置大模型密钥：请在「AI 设置」中填写密钥后重试。'
  }
  return err instanceof Error && err.message ? err.message : 'AI 暂不可用，请稍后重试。'
}
