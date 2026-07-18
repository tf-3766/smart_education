// 3.2 文件上传与访问接口
import { demoDelay } from '../runtime'
import { del, fileContentUrl, get, isRealMode, TOKEN_STORAGE_KEY, upload } from './client'
import { conflict, currentUser, db, nextId, notFound, nowIso, persist } from './demo/db'
import type { FilePurpose, StoredFileVO } from './types'

function toVO(row: typeof db.files[number]): StoredFileVO {
  const { ownerId: _ownerId, ...vo } = row
  return { ...vo }
}

export const filesApi = {
  async upload(file: File, purpose: FilePurpose = 'GENERAL'): Promise<StoredFileVO> {
    if (isRealMode()) {
      const form = new FormData()
      form.append('file', file)
      form.append('purpose', purpose)
      return upload<StoredFileVO>('/api/v1/files', form)
    }
    if (purpose === 'AVATAR' && !['image/jpeg', 'image/png', 'image/webp', 'image/gif'].includes(file.type)) {
      conflict('头像仅接受 JPEG、PNG、WebP 和 GIF。', 'FILE_TYPE_NOT_ALLOWED')
    }
    const fileId = nextId()
    const row = {
      fileId,
      originalName: file.name,
      objectKey: `${purpose.toLowerCase()}/${fileId}/${file.name}`,
      accessUrl: `/api/v1/files/${fileId}/content`,
      fileSize: file.size,
      mimeType: file.type || 'application/octet-stream',
      sha256: `demo-sha256-${fileId}`,
      purpose,
      ownerId: currentUser('STUDENT').userId,
      uploadedAt: nowIso(),
      version: 0,
    }
    db.files.push(row)
    persist()
    return demoDelay(toVO(row))
  },

  async getMeta(fileId: string): Promise<StoredFileVO> {
    if (isRealMode()) return get<StoredFileVO>(`/api/v1/files/${fileId}`)
    const row = db.files.find((item) => item.fileId === fileId) ?? notFound('文件不存在。')
    return demoDelay(toVO(row))
  },

  /** 文件流地址；演示模式返回占位地址，真实模式指向网关。 */
  contentUrl(fileId: string): string {
    if (isRealMode()) return fileContentUrl(fileId)
    return db.files.find((item) => item.fileId === fileId)?.accessUrl ?? `/api/v1/files/${fileId}/content`
  },

  /**
   * 以带鉴权的方式拉取文件内容并返回可直接用于 <img>/下载 的对象 URL。
   * 文件内容接口需 Authorization 头，<img src> 无法携带，故用 fetch+blob。
   * 用完请 URL.revokeObjectURL 释放。演示模式回退占位地址。
   */
  async contentObjectUrl(fileId: string): Promise<string> {
    if (!isRealMode()) return this.contentUrl(fileId)
    const token = sessionStorage.getItem(TOKEN_STORAGE_KEY)
    const response = await fetch(fileContentUrl(fileId), {
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    })
    if (!response.ok) throw new Error(`文件加载失败（HTTP ${response.status}）`)
    return URL.createObjectURL(await response.blob())
  },

  async remove(fileId: string): Promise<void> {
    if (isRealMode()) return del(`/api/v1/files/${fileId}`)
    const index = db.files.findIndex((item) => item.fileId === fileId)
    if (index < 0) notFound('文件不存在。')
    const referenced = db.users.some((user) => user.avatarFileId === fileId)
      || db.materials.some((material) => material.fileId === fileId)
      || db.assignments.some((assignment) => assignment.attachments.some((attachment) => attachment.fileId === fileId))
      || db.submissions.some((submission) => submission.fileId === fileId)
    if (referenced) conflict('文件仍被业务数据引用，解除引用后再删除。', 'FILE_IN_USE')
    db.files.splice(index, 1)
    persist()
    return demoDelay(undefined)
  },
}
