export type Role = 'admin' | 'teacher' | 'student'

export interface User {
  id: string
  name: string
  role: Role
  department: string
  className?: string
}

export interface Lesson {
  id: string
  courseId: string
  chapterId: string
  title: string
  durationMinutes: number
  knowledgePoints: string[]
}

export interface Chapter {
  id: string
  courseId: string
  title: string
  summarySeed: string
}

export interface Course {
  id: string
  title: string
  category: string
  teacherId: string
  teacherName: string
  status: 'draft' | 'review' | 'published'
  coverColor: string
  students: number
  progressAverage: number
  description: string
}

export interface Enrollment {
  id: string
  courseId: string
  studentId: string
  progress: number
  status: 'active' | 'completed' | 'atRisk'
}

export interface Assignment {
  id: string
  courseId: string
  title: string
  dueDate: string
  totalScore: number
  status: 'open' | 'closed'
  rubric: string[]
}

export interface AssignmentSubmission {
  id: string
  assignmentId: string
  studentId: string
  content: string
  submittedAt: string
  score?: number
  teacherComment?: string
  aiDraft?: string
}

export interface Exam {
  id: string
  courseId: string
  title: string
  startAt: string
  durationMinutes: number
  totalScore: number
  status: 'scheduled' | 'running' | 'finished'
  difficulty: DifficultyMix
}

export interface DifficultyMix {
  easy: number
  medium: number
  hard: number
}

export interface GradeRecord {
  id: string
  courseId: string
  studentId: string
  assignmentScore: number
  examScore: number
  finalScore: number
  passed: boolean
}

export interface ForumReply {
  id: string
  threadId: string
  authorId: string
  authorName: string
  content: string
  accepted: boolean
}

export interface ForumThread {
  id: string
  courseId: string
  title: string
  authorName: string
  pinned: boolean
  replies: ForumReply[]
}

export interface KnowledgeItem {
  id: string
  courseId: string
  chapterId: string
  title: string
  content: string
}

export interface AiCitation {
  title: string
  source: string
}

export interface AiResult {
  id: string
  type: 'qa' | 'feedback' | 'risk' | 'summary' | 'paper' | 'teaching-package' | 'operations-brief'
  title: string
  content: string
  citations?: AiCitation[]
  confidence?: number
  confirmed: boolean
}

/* 运行模式：demo 为默认，real 需 VITE_API_MODE=real 显式开启 */
export type ApiMode = 'demo' | 'real'
