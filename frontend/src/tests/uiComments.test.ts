import { readFileSync } from 'node:fs'
import { resolve } from 'node:path'
import { describe, expect, it } from 'vitest'

const appShell = () => readFileSync(resolve(process.cwd(), 'src/layouts/AppShell.vue'), 'utf8')
const styles = () => readFileSync(resolve(process.cwd(), 'src/styles/index.css'), 'utf8')
const lessonWorkspace = () => readFileSync(resolve(process.cwd(), 'src/domains/student/LessonWorkspacePage.vue'), 'utf8')

describe('browser UI comment regressions', () => {
  it('keeps the console header free of the removed crumb and global search', () => {
    const source = appShell()

    expect(source).not.toContain('class="topbar-crumb"')
    expect(source).not.toContain('class="global-search"')
  })

  it('removes the decorative blue strip from the workspace sidebar', () => {
    expect(styles()).not.toMatch(/\.workspace-sidebar\s*\{[^}]*border-top\s*:/s)
  })

  it('keeps the lesson outline anchored while the lesson content scrolls', () => {
    const source = lessonWorkspace()

    expect(source).toContain('class="panel course-tree"')
    expect(source).toMatch(/\.course-tree\s*\{[^}]*position\s*:\s*sticky[^}]*top\s*:\s*92px[^}]*max-height\s*:/s)
  })

  it('renders courseware as one slide at a time and constrains fullscreen height', () => {
    const source = lessonWorkspace()

    expect(source).toContain("material.materialType.code === 'COURSEWARE'")
    expect(source).toContain("filesApi.previewObjectUrl(fileId, previewPage.value)")
    expect(source).toMatch(/\.material-stage:fullscreen\s*\{[^}]*height\s*:\s*100vh[^}]*overflow\s*:\s*hidden/s)
    expect(source).toMatch(/\.material-stage:fullscreen \.slide-viewer\s*\{[^}]*min-height\s*:\s*0[^}]*height\s*:\s*calc\(100vh - 96px\)/s)
  })

  it('lays out dashboard action buttons as a single icon-and-label row', () => {
    expect(styles()).toMatch(/\.app-btn-primary\s*,\s*\.app-btn-secondary\s*\{[^}]*display\s*:\s*inline-flex[^}]*align-items\s*:\s*center[^}]*white-space\s*:\s*nowrap/s)
  })
})
