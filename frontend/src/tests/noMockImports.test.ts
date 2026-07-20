import { existsSync, readFileSync, readdirSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const roleRoots = ['student', 'teacher', 'admin'].map((role) => resolve(process.cwd(), 'src/domains', role))

const legacyMockFiles = [
  'src/mocks/data.ts',
  'src/services/mockRuntime.ts',
  ...['ai', 'analytics', 'assignment', 'auth', 'course', 'enrollment', 'exam', 'forum', 'grade']
    .map((name) => `src/services/${name}Service.ts`),
]

function vueFiles(directory: string): string[] {
  return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
    const path = resolve(directory, entry.name)
    return entry.isDirectory() ? vueFiles(path) : entry.name.endsWith('.vue') ? [path] : []
  })
}

describe('real backend page boundary', () => {
  it('legacy mock modules are fully removed', () => {
    const remaining = legacyMockFiles.filter((path) => existsSync(resolve(process.cwd(), path)))
    expect(remaining).toEqual([])
  })

  it('no source file imports mock data or legacy mock services', () => {
    const sourceRoot = resolve(process.cwd(), 'src')
    const files = (function walk(directory: string): string[] {
      return readdirSync(directory, { withFileTypes: true }).flatMap((entry) => {
        const path = resolve(directory, entry.name)
        if (entry.isDirectory()) return entry.name === 'tests' ? [] : walk(path)
        return path.endsWith('.vue') || path.endsWith('.ts') ? [path] : []
      })
    })(sourceRoot)
    const violations = files
      .map((path) => ({ path, source: readFileSync(path, 'utf8') }))
      .filter(({ source }) => source.includes('@/mocks/') || /@\/services\/(?:auth|course|enrollment|assignment|exam|grade|forum|analytics|ai)Service|services\/mockRuntime/.test(source))
      .map(({ path }) => path.replace(`${process.cwd()}/`, ''))
    expect(violations).toEqual([])
  })

  it('does not import mock data or legacy mock services from role pages', () => {
    const violations = roleRoots.flatMap(vueFiles)
      .map((path) => ({ path, source: readFileSync(path, 'utf8') }))
      .filter(({ source }) => source.includes('@/mocks/data') || /@\/services\/(?:auth|course|enrollment|assignment|exam|grade|forum|analytics|ai)Service/.test(source))
      .map(({ path }) => path.replace(`${process.cwd()}/`, ''))

    expect(violations).toEqual([])
  })

  it('does not contain user-facing demo-operation labels in role pages', () => {
    const violations = roleRoots.flatMap(vueFiles)
      .map((path) => ({ path, source: readFileSync(path, 'utf8') }))
      .filter(({ source }) => /演示|示例数据/.test(source))
      .map(({ path }) => path.replace(`${process.cwd()}/`, ''))

    expect(violations).toEqual([])
  })

  it('teacher warnings page uses the real warning lifecycle', () => {
    const source = readFileSync(resolve(process.cwd(), 'src/domains/teacher/WarningsPage.vue'), 'utf8')
    expect(source).toContain('warningsApi.generate')
    expect(source).toContain('warningsApi.teacherList')
    expect(source).toContain('warningsApi.handle')
    expect(source).not.toMatch(/const\s+warnings\s*[:=]/)
  })
})
