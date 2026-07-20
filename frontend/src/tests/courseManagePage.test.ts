import { describe, expect, it, vi } from 'vitest'
import CourseManagePage from '@/domains/teacher/CourseManagePage.vue'
import { db } from '@/services/api/demo/db'
import { clickByText, freshDemo, mountPage, settle } from './pageTestUtils'

describe('教师课程管理页 · 学期输入', () => {
  it('学期为下拉选择，选项统一为「YYYY 春季/秋季」格式', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')

    const select = wrapper.get('#cm-term')
    expect(select.element.tagName).toBe('SELECT')
    const options = select.findAll('option').map((o) => o.text())
    // 每个非空选项都必须符合规范格式，杜绝自由文本。
    const terms = options.filter((t) => t !== '未设置')
    expect(terms.length).toBeGreaterThan(0)
    for (const term of terms) expect(term).toMatch(/^\d{4} (春季|秋季)$/)
    // 覆盖当年及相邻年份。
    const year = new Date().getFullYear()
    expect(terms).toContain(`${year} 春季`)
    expect(terms).toContain(`${year} 秋季`)
  })

  it('可设置选课时间窗，创建后持久化到课程（ISO）', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')

    await wrapper.get('#cm-code').setValue('WINDOW01')
    await wrapper.get('#cm-name').setValue('时间窗测试课')
    // 选一个无管理员统一窗口的学期，方可自定义选课时间。
    await wrapper.get('#cm-term').setValue('')
    await wrapper.get('#cm-enroll-open').setValue('2026-08-01T09:00')
    await wrapper.get('#cm-enroll-close').setValue('2026-09-01T09:00')
    await clickByText(wrapper, 'button', '创建草稿')
    await settle(800)

    const created = db.courses.find((course) => course.courseCode === 'WINDOW01')
    expect(created).toBeTruthy()
    expect(created!.enrollmentOpenAt).toBeTruthy()
    expect(created!.enrollmentCloseAt).toBeTruthy()
    expect(new Date(created!.enrollmentCloseAt!).getTime()).toBeGreaterThan(new Date(created!.enrollmentOpenAt!).getTime())
    // 成功创建后弹窗应自动关闭。
    expect(wrapper.find('.modal-panel').exists()).toBe(false)
  })

  it('选定有管理员窗口的学期时，自动套用其选课时间且隐藏自定义输入', async () => {
    freshDemo()
    const seeded = db.termEnrollmentWindows[0]
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')
    await settle(300)

    // 选中已配置窗口的学期。
    await wrapper.get('#cm-term').setValue(seeded.term)
    await settle()
    // 自定义选课时间输入被只读说明取代。
    expect(wrapper.find('#cm-enroll-open').exists()).toBe(false)
    expect(wrapper.get('.modal-panel').text()).toContain('由管理员统一设置')

    await wrapper.get('#cm-code').setValue('AUTOWIN')
    await wrapper.get('#cm-name').setValue('自动套用窗口课')
    await clickByText(wrapper, 'button', '创建草稿')
    await settle(800)
    const created = db.courses.find((c) => c.courseCode === 'AUTOWIN')!
    // 直接套用窗口原始时间（无 datetime-local 转换，精确一致）。
    expect(created.enrollmentOpenAt).toBe(seeded.enrollmentOpenAt)
    expect(created.enrollmentCloseAt).toBe(seeded.enrollmentCloseAt)
  })

  it('选课截止不晚于开始时拦截创建', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')

    await wrapper.get('#cm-code').setValue('BADWIN')
    await wrapper.get('#cm-name').setValue('非法时间窗')
    await wrapper.get('#cm-term').setValue('')
    await wrapper.get('#cm-enroll-open').setValue('2026-09-01T09:00')
    await wrapper.get('#cm-enroll-close').setValue('2026-08-01T09:00')
    await clickByText(wrapper, 'button', '创建草稿')
    await settle(300)

    expect(db.courses.find((course) => course.courseCode === 'BADWIN')).toBeFalsy()
    expect(wrapper.text()).toContain('选课截止时间需晚于开始时间')
  })

  it('课程编码含中文/空格时前端拦截并给出明确提示', async () => {
    freshDemo()
    const before = db.courses.length
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')

    await wrapper.get('#cm-code').setValue('童飞 2 号')
    await wrapper.get('#cm-name').setValue('童飞')
    await clickByText(wrapper, 'button', '创建草稿')
    await settle(300)

    // 前端就拦下，不发请求、不建课，并在弹窗内清楚说明编码规则。
    expect(db.courses.length).toBe(before)
    expect(wrapper.get('.modal-panel').text()).toContain('课程编码只能包含字母、数字')
  })

  it('可从内置课程库选用模板回填，新建自定义课程沉淀进内置库', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')
    await settle(300)

    // 选择内置模板 → 编码/名称/简介自动回填
    const tplSelect = wrapper.get('#cm-template')
    const osOption = tplSelect.findAll('option').find((o) => o.text().includes('操作系统原理'))!
    await tplSelect.setValue(osOption.attributes('value'))
    expect((wrapper.get('#cm-code').element as HTMLInputElement).value).toBe('OS301')
    expect((wrapper.get('#cm-name').element as HTMLInputElement).value).toBe('操作系统原理')

    // 改为自定义新课并创建 → 该课程进入内置库
    await tplSelect.setValue('')
    await wrapper.get('#cm-code').setValue('NEW999')
    await wrapper.get('#cm-name').setValue('全新自定义课')
    await clickByText(wrapper, 'button', '创建草稿')
    await settle(800)
    expect(db.courseTemplates.some((t) => t.courseCode === 'NEW999' && t.name === '全新自定义课')).toBe(true)
  })

  it('创建失败时保持弹窗打开并在弹窗内提示错误', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    await clickByText(wrapper, 'button', '新建课程')

    await wrapper.get('#cm-code').setValue('PY101') // 与种子课程 21001 编码重复
    await wrapper.get('#cm-name').setValue('重复编码课')
    await clickByText(wrapper, 'button', '创建草稿')
    await settle(500)

    // 弹窗仍打开，且错误提示出现在弹窗内部（而非仅在被遮罩挡住的页面顶部）。
    const panel = wrapper.get('.modal-panel')
    expect(panel.text()).toContain('课程编码已存在')
  })

  it('可编辑课程信息，未编辑字段原样保留（后端 update 为整体覆盖）', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)

    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('Python 程序设计'))!
    await row.findAll('button').find((b) => b.text() === '编辑')!.trigger('click')
    await settle(700)

    // 编辑弹窗回填当前值。
    expect((wrapper.get('#ce-name').element as HTMLInputElement).value).toBe('Python 程序设计')
    await wrapper.get('#ce-name').setValue('Python 程序设计（修订）')
    await wrapper.get('#ce-credit').setValue(4)
    await clickByText(wrapper, 'button', '保存修改')
    await settle(800)

    const updated = db.courses.find((course) => course.courseId === '21001')!
    expect(updated.name).toBe('Python 程序设计（修订）')
    expect(updated.credit).toBe(4)
    // 弹窗中不可编辑的字段必须原样透传，不能被整体覆盖清空。
    expect(updated.categoryId).toBe('1')
    expect(updated.department).toBe('计算机学院')
    expect(updated.startAt).toBeTruthy()
    expect(updated.endAt).toBeTruthy()
    expect(wrapper.find('.modal-panel').exists()).toBe(false)
    expect(wrapper.text()).toContain('课程信息已更新')
  })

  it('编辑时选课截止早于开始被拦截且弹窗保持打开', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)

    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('Python 程序设计'))!
    await row.findAll('button').find((b) => b.text() === '编辑')!.trigger('click')
    await settle(700)

    await wrapper.get('#ce-enroll-open').setValue('2026-09-01T09:00')
    await wrapper.get('#ce-enroll-close').setValue('2026-08-01T09:00')
    await clickByText(wrapper, 'button', '保存修改')
    await settle(300)

    const panel = wrapper.get('.modal-panel')
    expect(panel.text()).toContain('选课截止时间需晚于开始时间')
  })

  it('课程团队：邀请协作教师为待确认，可撤回邀请，主讲不可移除', async () => {
    freshDemo()
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    // 人工智能导论（21002）仅有主讲李明，便于验证邀请。
    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('人工智能导论'))!
    await row.findAll('button').find((b) => b.text() === '课程团队')!.trigger('click')
    await settle()
    const panel = wrapper.get('.modal-panel')
    expect(panel.text()).toContain('李明')
    expect(panel.text()).toContain('主讲不可移除')
    // 邀请协作教师（陈若溪，用户 ID 3）——建 PENDING，需对方确认。
    await wrapper.get('#cm-team-id').setValue('3')
    await clickByText(wrapper, 'button', '邀请')
    await settle()
    expect(wrapper.get('.modal-panel').text()).toContain('陈若溪')
    expect(wrapper.get('.modal-panel').text()).toContain('待确认')
    const rel = db.courseTeachers.find((t) => t.courseId === '21002' && t.teacherId === '3')!
    expect(rel.role).toBe('COLLABORATOR')
    expect(rel.status).toBe('PENDING')
    // 撤回邀请（pending 行的操作按钮为「撤回邀请」）。
    const confirmSpy = vi.spyOn(window, 'confirm').mockReturnValue(true)
    const collabRow = wrapper.get('.modal-panel').findAll('tbody tr').find((tr) => tr.text().includes('陈若溪'))!
    await collabRow.findAll('button').find((b) => b.text() === '撤回邀请')!.trigger('click')
    await settle()
    confirmSpy.mockRestore()
    expect(db.courseTeachers.some((t) => t.courseId === '21002' && t.teacherId === '3')).toBe(false)
  })

  it('被邀请教师在顶部确认区接受后才正式加入团队', async () => {
    freshDemo()
    // 负责人李明(id2)邀请陈若溪(id3)协作 21002。
    const { wrapper } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    const row = wrapper.findAll('tbody tr').find((r) => r.text().includes('人工智能导论'))!
    await row.findAll('button').find((b) => b.text() === '课程团队')!.trigger('click')
    await settle()
    await wrapper.get('#cm-team-id').setValue('3')
    await clickByText(wrapper, 'button', '邀请')
    await settle()
    expect(db.courseTeachers.find((t) => t.courseId === '21002' && t.teacherId === '3')!.status).toBe('PENDING')

    // 切换登录态为被邀请人陈若溪(id3)，重挂载页面。
    db.session.userId = '3'
    const { wrapper: w2 } = await mountPage(CourseManagePage, { path: '/teacher/courses' })
    await settle(500)
    // 顶部出现待确认邀请；此时其课程列表尚不含 21002（PENDING 不计入）。
    expect(w2.text()).toContain('待我确认的协作邀请')
    expect(db.courseTeachers.find((t) => t.courseId === '21002' && t.teacherId === '3')!.status).toBe('PENDING')
    // 接受邀请 → 转为 ACTIVE。
    await clickByText(w2, 'button', '接受')
    await settle(500)
    expect(db.courseTeachers.find((t) => t.courseId === '21002' && t.teacherId === '3')!.status).toBe('ACTIVE')
    expect(w2.text()).not.toContain('待我确认的协作邀请')
  })
})
