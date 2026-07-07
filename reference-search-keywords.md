# 在线教育辅助教学系统：视觉参考搜索关键词与采集模板

> 适用平台：Figma Community、Dribbble、Behance  
> 依据文档：`design-research.md`、`sitemap.md`、`ui-spec.md`、`wireframes.md`  
> 当前用途：收集布局、导航、组件、信息密度和交互表达参考；不用于直接复制页面、组件代码或品牌素材。

## 1. 搜索使用说明

### 1.1 项目视觉定位

搜索时优先寻找以下方向：

- 高校 LMS、EdTech SaaS、校园学习工作台，而不是少儿教育、营销落地页或培训机构官网。
- 现代、清晰、正式、易读，以蓝色为主、青绿色为辅助，AI 只使用少量紫色识别。
- 学生端突出待办、继续学习、进度和支持性引导。
- 教师端突出队列、分栏、表格、连续处理和人工确认。
- 管理员端突出指标、筛选、趋势、异常队列和下钻入口。
- AI 必须依附课程、章节、提交、预警或试卷上下文，并展示来源、生成状态和人工确认操作。

### 1.2 三个平台的关键词组合方式

不同平台的内容特点不同，建议使用以下公式组合关键词：

| 平台 | 推荐组合方式 | 示例 |
|---|---|---|
| Figma Community | `业务类型 + 页面类型 + UI Kit / Design System / Dashboard` | `LMS student dashboard UI kit` |
| Dribbble | `页面类型 + 核心组件 + web app / dashboard / sidebar` | `online course dashboard sidebar` |
| Behance | `产品类型 + UX UI case study + web platform / SaaS` | `EdTech LMS UX UI case study` |

建议先使用宽泛关键词收集整体方向，再加上 `desktop`、`responsive`、`higher education`、`enterprise`、`SaaS`、`accessibility` 等限定词缩小范围。

---

## 2. 学生端首页 / 学习工作台

对应页面：学习首页、继续学习、本周待办、考试提醒、学习进度、成绩趋势、风险提醒、AI 学习建议。

### 2.1 Figma Community 搜索关键词

- `LMS student dashboard UI kit`
- `education dashboard design system`
- `student learning portal dashboard`
- `higher education student dashboard`
- `online learning dashboard web app`
- `university student portal UI kit`
- `learning management system dashboard`
- `EdTech dashboard desktop UI`
- `course progress dashboard UI kit`
- `student task dashboard Figma`

### 2.2 Dribbble 搜索关键词

- `student dashboard web app`
- `LMS dashboard design`
- `online education dashboard`
- `learning progress dashboard`
- `student portal dashboard`
- `course task dashboard`
- `education SaaS dashboard`
- `university learning portal`
- `student performance dashboard`
- `learning analytics student dashboard`

### 2.3 Behance 搜索关键词

- `LMS student dashboard UX UI case study`
- `online education platform UX case study`
- `university student portal redesign`
- `EdTech web platform case study`
- `learning management system UX design`
- `higher education dashboard UX UI`
- `student learning experience platform`
- `education SaaS product design case study`
- `student progress tracking UX`
- `digital campus learning platform design`

### 2.4 重点观察

- 顶部栏与左侧导航如何控制信息密度，当前菜单是否容易识别。
- 欢迎区是否直接呈现“下一步行动”，而不是只放装饰性文案。
- “继续学习”卡片如何展示课程、当前章节、进度和主操作。
- 待办、作业、考试如何按截止时间和紧急程度排序。
- 进度、成绩、学习时长是否使用少量易读图表，而非图表墙。
- 风险提醒是否包含原因、证据和改进入口，是否避免制造焦虑。
- AI 建议是否从风险或进度卡进入，并保留“生成—结果—加入计划”闭环。

---

## 3. 我的课程与课程详情页

对应页面：我的课程、选课状态、课程卡片、课程详情、章节目录、课程公告、近期作业和考试。

### 3.1 Figma Community 搜索关键词

- `LMS course catalog UI kit`
- `my courses dashboard Figma`
- `online course detail page UI`
- `course management student portal`
- `education course card design system`
- `course overview page UI kit`
- `university course portal Figma`
- `course curriculum page design`
- `learning platform course details`
- `course enrollment UI kit`

### 3.2 Dribbble 搜索关键词

- `my courses page web app`
- `online course detail page`
- `LMS course catalog dashboard`
- `course card UI education`
- `course overview dashboard`
- `course curriculum interface`
- `student course portal`
- `course enrollment web app`
- `education course details sidebar`
- `learning platform course page`

### 3.3 Behance 搜索关键词

- `online course platform UX UI case study`
- `LMS course catalog UX design`
- `course detail page UX case study`
- `higher education course portal redesign`
- `online learning platform product design`
- `course enrollment experience UX`
- `university LMS redesign case study`
- `education marketplace course page UX`
- `digital learning course experience`
- `course curriculum information architecture`

### 3.4 重点观察

- 我的课程如何区分进行中、未开始、已结课和可选课程。
- 课程卡片是否保留课程名、教师、状态、进度和唯一主操作。
- 课程详情头部如何组织课程基本信息、教师、学习进度和操作按钮。
- 课程内局部导航采用 Tabs、锚点还是二级侧栏，是否容易返回课程列表。
- 章节目录如何显示解锁、完成、学习中和未发布状态。
- 作业、考试、公告如何成为课程上下文中的入口，而不是重复建立另一套页面。
- 窄屏下课程信息与近期事项如何从左右双栏改为上下排列。

---

## 4. 章节学习页、课程播放器与 AI 答疑侧栏

对应页面：章节树、课程播放器、正文资料、学习完成状态、知识点摘要、课程资料引用、AI 智能答疑。

### 4.1 Figma Community 搜索关键词

- `online course player UI kit`
- `LMS lesson page Figma`
- `course learning interface desktop`
- `video learning platform UI kit`
- `course curriculum sidebar player`
- `e-learning lesson workspace`
- `AI tutor sidebar UI kit`
- `AI assistant learning interface`
- `course content chat sidebar`
- `knowledge base AI chat Figma`
- `document citation AI assistant UI`
- `learning content split view UI`

### 4.2 Dribbble 搜索关键词

- `online course player dashboard`
- `lesson learning interface`
- `video course player sidebar`
- `course curriculum navigation`
- `e-learning content viewer`
- `AI tutor sidebar`
- `AI learning assistant chat`
- `contextual AI assistant panel`
- `AI chat citations interface`
- `course assistant drawer`
- `split view learning workspace`
- `knowledge summary education UI`

### 4.3 Behance 搜索关键词

- `online course player UX UI case study`
- `e-learning lesson experience design`
- `LMS course player redesign`
- `video learning platform UX case study`
- `AI tutor learning experience case study`
- `AI education assistant product design`
- `contextual AI sidebar UX`
- `AI chat source citation interface`
- `knowledge assistant web app case study`
- `digital classroom learning experience`
- `course content consumption UX`
- `learning workspace UX UI design`

### 4.4 重点观察

- 三栏关系是否清晰：左侧章节目录、中间学习内容、右侧 AI 答疑。
- 章节目录是否固定、可折叠，并显示当前课时和完成状态。
- 播放器、正文、附件、笔记和完成按钮之间的视觉顺序。
- 内容区是否保持适合阅读的宽度，AI 侧栏是否会过度压缩正文。
- AI 顶部是否明确显示当前课程、章节和可用资料范围。
- AI 回答是否展示课程资料引用、页码、时间点或章节定位。
- 流式生成、停止、重试、无依据和服务异常如何表达。
- AI 结果是否提供复制、反馈、重新生成和继续追问。
- 手机端是否把 AI 侧栏改为全屏，并将章节目录改为抽屉。

---

## 5. 教师工作台、课程管理、作业批改与成绩分析

对应页面：教师工作台、负责课程、待发布、待批改、课程编辑、提交队列、评分量规、AI 评语、成绩与学情分析。

### 5.1 Figma Community 搜索关键词

- `teacher dashboard UI kit`
- `LMS instructor dashboard Figma`
- `education admin teacher portal`
- `course management dashboard UI kit`
- `assignment grading dashboard`
- `student submission review interface`
- `grading rubric UI Figma`
- `teacher gradebook dashboard`
- `student performance analytics UI kit`
- `learning analytics teacher dashboard`
- `split view review workspace`
- `instructor course builder UI`

### 5.2 Dribbble 搜索关键词

- `teacher dashboard web app`
- `instructor LMS dashboard`
- `course management interface`
- `assignment grading dashboard`
- `grading workspace UI`
- `student submission review`
- `rubric scoring interface`
- `teacher gradebook design`
- `student performance analytics`
- `learning analytics dashboard teacher`
- `review queue split view`
- `course content management dashboard`

### 5.3 Behance 搜索关键词

- `teacher portal UX UI case study`
- `LMS instructor experience redesign`
- `course management SaaS UX`
- `assignment grading workflow case study`
- `online assessment platform UX UI`
- `teacher gradebook product design`
- `student performance analytics case study`
- `learning analytics platform UX`
- `education management system UX case study`
- `instructor course builder product design`
- `grading rubric user experience`
- `teacher workflow dashboard design`

### 5.4 重点观察

- 工作台是否优先展示今天待办、待批改、待发布和风险学生。
- 负责课程、学生数量、待批改数量等指标是否能够直接下钻。
- 课程管理页面如何平衡表格、筛选、状态和快捷操作。
- 批改工作台是否采用“学生队列—提交预览—评分与评语”三栏结构。
- 文件、代码、图片或 PDF 提交的预览区如何切换。
- 评分量规、总分和教师评语是否处于同一操作上下文。
- 是否支持“保存草稿—发布成绩—下一份”的连续处理流程。
- AI 评语是否标为草稿，并提供生成、编辑、复制、重新生成、采用和发布前确认。
- 成绩分析是否同时提供总体分布、趋势、异常学生和具体证据。

---

## 6. 管理员后台、用户管理、课程审核与数据看板

对应页面：全局数据看板、用户管理、课程分类、课程审核、公告管理、异常提醒、AI 运行管理。

### 6.1 Figma Community 搜索关键词

- `education admin dashboard UI kit`
- `LMS admin dashboard Figma`
- `university management dashboard`
- `user management admin panel UI`
- `course approval workflow UI`
- `content moderation dashboard Figma`
- `education analytics dashboard UI kit`
- `SaaS admin dashboard design system`
- `enterprise admin panel Figma`
- `audit log dashboard UI kit`
- `system monitoring admin dashboard`
- `role permission management UI`

### 6.2 Dribbble 搜索关键词

- `education admin dashboard`
- `LMS admin panel`
- `university management system dashboard`
- `user management table UI`
- `course approval dashboard`
- `content moderation interface`
- `education analytics dashboard`
- `enterprise SaaS admin panel`
- `system status dashboard`
- `audit log interface`
- `role permissions dashboard`
- `data dashboard table design`

### 6.3 Behance 搜索关键词

- `education admin dashboard UX UI case study`
- `LMS administration platform design`
- `university management system UX`
- `enterprise admin panel UX case study`
- `user management SaaS product design`
- `course approval workflow UX`
- `content moderation dashboard case study`
- `education analytics platform design`
- `system monitoring dashboard UX UI`
- `role based access control UX`
- `audit management platform design`
- `data intensive dashboard case study`

### 6.4 重点观察

- 看板首屏是否只保留关键指标、趋势、角色分布和待处理异常。
- 指标卡是否能下钻到用户、课程、审核或系统状态列表。
- 图表是否带时间范围、口径、单位和更新时间。
- 用户管理表格如何组合搜索、筛选、状态、批量操作和行操作。
- 用户详情是否区分基本信息、角色权限、状态和操作记录。
- 课程审核是否采用“待审队列—课程信息—审核记录—通过/驳回”流程。
- 审核驳回是否要求填写原因，高风险操作是否二次确认。
- 公告和内容治理是否有发布范围、状态、时间及操作留痕。
- 管理员 AI 页面是否只展示用量、失败率、索引、安全事件和审计，不暴露学生私人会话正文。

---

## 7. AI 学习助手、AI 评语、AI 预警与 AI 组卷建议

这类参考应优先寻找“AI 嵌入业务流程”的产品界面，避免只收集通用聊天机器人页面。

### 7.1 Figma Community 搜索关键词

#### AI 学习助手

- `AI tutor UI kit`
- `AI learning assistant Figma`
- `education chatbot interface`
- `contextual AI chat sidebar`
- `AI answer citation UI`
- `knowledge base assistant UI kit`

#### AI 评语与教师草稿

- `AI writing assistant editor UI`
- `AI feedback generator dashboard`
- `AI grading assistant interface`
- `AI draft review workflow`
- `human in the loop AI UI`
- `AI copilot review panel Figma`

#### AI 风险预警

- `student risk analytics dashboard`
- `early warning system UI`
- `AI risk explanation card`
- `learning intervention dashboard`
- `predictive analytics education UI`
- `evidence based alert UI kit`

#### AI 智能组卷

- `AI question generator UI`
- `exam builder UI kit`
- `question bank dashboard Figma`
- `AI assessment generator interface`
- `test builder recommendation panel`
- `exam blueprint dashboard UI`

### 7.2 Dribbble 搜索关键词

#### AI 学习助手

- `AI tutor interface`
- `AI education assistant`
- `learning assistant sidebar`
- `AI chat with citations`
- `context aware AI assistant`
- `knowledge assistant web app`

#### AI 评语与教师草稿

- `AI feedback assistant`
- `AI writing copilot editor`
- `grading assistant dashboard`
- `AI draft approval workflow`
- `human review AI interface`
- `AI content suggestion panel`

#### AI 风险预警

- `student risk dashboard`
- `learning early warning system`
- `AI risk insights card`
- `student intervention dashboard`
- `predictive learning analytics`
- `evidence based alert design`

#### AI 智能组卷

- `AI exam generator`
- `question bank dashboard`
- `test builder interface`
- `assessment generator dashboard`
- `exam blueprint UI`
- `AI question recommendation`

### 7.3 Behance 搜索关键词

#### AI 学习助手

- `AI tutor UX UI case study`
- `AI education assistant product design`
- `conversational learning experience UX`
- `AI knowledge assistant case study`
- `contextual AI copilot UX`
- `AI chat citation experience design`

#### AI 评语与教师草稿

- `AI feedback generator UX case study`
- `AI writing assistant product design`
- `human in the loop AI workflow UX`
- `AI copilot review experience`
- `AI grading assistant case study`
- `generative AI draft approval UX`

#### AI 风险预警

- `student early warning system UX`
- `predictive learning analytics case study`
- `AI risk explanation dashboard`
- `student intervention platform UX`
- `education risk analytics product design`
- `explainable AI alert interface`

#### AI 智能组卷

- `AI assessment generator UX case study`
- `exam builder product design`
- `question bank management UX`
- `AI test generation platform`
- `assessment blueprint dashboard UX`
- `teacher AI assessment copilot`

### 7.4 重点观察

| AI 场景 | 必须观察的界面结构 | 本项目应保留的人工控制 |
|---|---|---|
| AI 学习助手 | 上下文栏、消息列表、快捷问题、输入区、生成状态、引用来源、错误重试 | 学生主动提问、复制、反馈、重问；不能直接修改正式课程内容 |
| 章节摘要 | 当前章节、摘要草稿、来源、编辑状态、版本与发布状态 | 教师编辑并发布；学生只看已发布版本 |
| AI 评语 | 学生提交、评分量规、教师原评语、AI 草稿、差异和操作区 | 教师编辑、采用、保存、发布；AI 不自动打分或发布 |
| AI 风险预警 | 风险等级、触发依据、时间范围、解释、建议动作、处理状态 | 学生加入计划；教师采纳、修改或忽略干预建议 |
| AI 智能组卷 | 考试约束、知识点覆盖、难度分布、候选题、推荐理由、试卷预览 | 教师替换、排除、调整分值并确认入卷 |

---

## 8. 收集参考时的页面观察清单

每张参考图至少从以下维度记录，而不是只记录“好看”：

### 8.1 全局布局

- 顶部栏高度、侧栏宽度、内容最大宽度和页面留白。
- 一级导航、课程内导航、面包屑和返回路径的层级关系。
- 页面标题区、筛选区、内容区、操作区的先后顺序。
- 主任务是否在首屏可见，次要信息是否被合理折叠。

### 8.2 卡片、表格与状态

- 哪些内容适合卡片，哪些高密度内容使用表格或队列。
- 卡片是否出现多层嵌套、过多阴影或同尺寸堆叠。
- 状态是否同时使用文字、图标和颜色，而不是只依赖颜色。
- 截止时间、分数、风险原因和审核原因是否足够醒目。

### 8.3 数据展示

- 指标卡是否有明确口径、时间范围和下钻入口。
- 图表类型是否与问题匹配：趋势用折线，构成用条形或环图，完成度用进度条。
- 是否提供空状态、加载状态、异常状态和数据更新时间。
- 是否避免 3D 图表、过多小图和无业务意义的同比装饰。

### 8.4 AI 交互

- AI 入口对应哪一个真实业务任务和当前上下文。
- 生成前是否说明使用的数据范围；生成中是否允许停止。
- 结果是否标记为 AI 草稿或 AI 建议，并显示来源或证据。
- 是否提供编辑、复制、重新生成、采纳、保存和撤销。
- AI 失败时是否保留用户输入和原业务数据，是否提供重试或返回原流程。

### 8.5 响应式与可访问性

- 桌面双栏或三栏在平板、手机上如何折叠。
- 表格是横向滚动、冻结关键列，还是改为卡片摘要。
- 点击区域、字号、颜色对比和键盘焦点是否清晰。
- 截止、错误、风险和考试状态是否有非颜色提示。

---

## 9. 需要避开的视觉风格

- 大面积紫蓝渐变、霓虹发光、玻璃拟态和持续动态背景。
- 过于像加密货币、金融交易或游戏控制台的深色科技风。
- 大幅 3D 插画、机器人吉祥物或装饰图片挤压教学内容。
- 每个模块都套卡片、卡片中再嵌套卡片的“仪表盘模板感”。
- 一个页面混用多个高饱和主色，或把不同菜单随机染色。
- 大量圆角药丸、悬浮按钮和无明确主次的实心按钮。
- 只有漂亮图表、没有数据口径、异常队列和下钻动作的假看板。
- 通用 ChatGPT 克隆界面：没有课程上下文、来源、业务落点和人工确认。
- 只显示风险分数或“可能挂科”，却不显示依据和改进行动。
- AI 自动打分、自动发布评语或自动加入试题的不可控设计。
- 为了展示动效而加入弹跳、翻转、视差、数字滚动和长时间假加载。
- 典型营销落地页、少儿教育卡通风、培训机构宣传页和移动学习打卡 App 风格。

---

## 10. 许可证、著作权与复制风险

### 10.1 平台风险判断

| 平台 | 默认风险判断 | 收集时的正确做法 |
|---|---|---|
| Figma Community | “可复制到草稿”不等于可自由商用；每个文件、插件、字体、图标和图片可能有不同许可证 | 打开资源说明和作者主页，记录许可证、使用限制、作者与链接；许可证不明时只作为内部结构参考 |
| Dribbble | 页面截图和作品通常受作者著作权保护，点赞、保存或公开展示不构成使用授权 | 只记录布局思想和交互模式，不直接截取插画、图标、品牌、字体或完整页面用于产品 |
| Behance | 案例研究、品牌资产、摄影、插画和 UI 页面通常均有明确作者；下载附件也不自动代表可商用 | 保存项目链接与作者信息，确认作品说明；只抽象信息架构和设计原则，不照搬成套视觉 |

### 10.2 禁止直接复制的内容

- 完整页面结构、组件尺寸、颜色、文案和图标的逐像素复刻。
- 参考作品中的 Logo、学校标识、课程封面、头像、摄影、插画和品牌名称。
- 未确认许可证的字体、图标库、图表素材、3D 模型和图片。
- Figma Community 文件中的代码片段、组件实现或设计系统变量整套搬运。
- Dribbble、Behance 中的截图直接作为项目背景、课程封面或答辩材料主视觉。
- 从 Canvas、Moodle、Open edX 等具体产品复制品牌化页面、主题或组件代码。

### 10.3 推荐的安全借鉴方式

- 借鉴“模式”而非“成品”：例如三栏批改工作台、跨课程待办、课程内章节树。
- 每个页面至少组合 2–3 个不同来源，再按本项目业务重新组织。
- 使用 `ui-spec.md` 中既定色彩、字体、间距、圆角和状态语义，不跟随参考图任意换皮。
- 使用 Element Plus 或已确认许可证的统一图标库重新实现组件。
- 所有课程、用户、学校和统计文案改为本项目自己的业务内容。
- 在采集表中记录来源、许可证和“只借鉴什么”，保留设计决策依据。

---

## 11. 参考收集表格模板

建议在 Markdown、Excel、Notion 或飞书表格中维护。每张图一行；同一作品包含多个关键页面时可拆成多行。

| 编号 | 页面类别 | 平台 | 作品名称 | 作者 / 团队 | 来源链接 | 收集日期 | 许可证 / 权利说明 | 重点借鉴点 | 主要布局结构 | 值得参考的组件 / 交互 | 对应项目页面 / 路由 | 与视觉规范一致性 | 复制风险 | 是否采用 | 采用范围 / 改造方式 | 本地截图文件名 | 备注 |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| REF-001 | 学生首页 | Figma |  |  |  |  |  |  |  |  | `/student/dashboard` | 高 / 中 / 低 | 低 / 中 / 高 | 采用 / 部分采用 / 不采用 / 待定 |  |  |  |
| REF-002 | 章节学习 + AI | Dribbble |  |  |  |  |  |  |  |  | `/student/courses/:courseId/lessons/:lessonId` | 高 / 中 / 低 | 低 / 中 / 高 | 采用 / 部分采用 / 不采用 / 待定 |  |  |  |
| REF-003 | 作业批改 | Behance |  |  |  |  |  |  |  |  | `/teacher/assignments/:assignmentId/grading/:submissionId` | 高 / 中 / 低 | 低 / 中 / 高 | 采用 / 部分采用 / 不采用 / 待定 |  |  |  |
| REF-004 | 管理数据看板 |  |  |  |  |  |  |  |  |  | `/admin/dashboard` | 高 / 中 / 低 | 低 / 中 / 高 | 采用 / 部分采用 / 不采用 / 待定 |  |  |  |

### 11.1 “重点借鉴点”推荐写法

不要只写“配色好看”或“布局高级”，建议写成可执行描述：

- “借鉴首页把待办与继续学习放在首屏，统计卡缩小为第二优先级。”
- “借鉴课程详情的左侧章节树和右侧近期任务，但颜色、字号和组件按本项目规范重做。”
- “借鉴批改页三栏结构与发布后自动进入下一份的流程，不复制原页面图标和评分样式。”
- “借鉴 AI 回答下方的来源折叠区，并增加本项目要求的章节定位和无依据状态。”
- “借鉴审核页的历史记录时间线，审批按钮和权限提示按本项目流程重新设计。”

### 11.2 参考图采用前的快速评分

可按 1–5 分记录以下项目，总分低于 18 分的参考不建议进入主方案：

| 评分项 | 判断问题 | 分值 |
|---|---|---:|
| 业务匹配 | 是否适合高校 LMS，而不是营销页或少儿产品？ | 1–5 |
| 信息架构 | 是否能支持本项目规定的数据和操作？ | 1–5 |
| 视觉一致 | 是否接近现代、清晰、正式的校园学习工作台？ | 1–5 |
| AI 闭环 | AI 是否有上下文、来源、生成状态和人工确认？ | 1–5 |
| 可实现性 | 是否适合 Vue 3 + Element Plus，且不过度依赖特效？ | 1–5 |
| 合规安全 | 来源和许可证是否清楚，是否能做到只借鉴模式？ | 1–5 |

---

## 12. 建议的首轮收集数量

避免一次保存上百张风格冲突的图片。首轮建议控制在 30–40 张：

| 类别 | 建议数量 | 优先目标 |
|---|---:|---|
| 学生首页 | 5–6 张 | 待办优先、继续学习、风险与 AI 入口 |
| 我的课程 / 课程详情 | 4–5 张 | 课程卡、章节目录、课程内导航 |
| 章节学习 / 播放器 / AI | 6–7 张 | 三栏学习空间、引用来源、响应式 AI |
| 教师工作台 / 批改 / 分析 | 6–7 张 | 队列、三栏批改、量规、连续处理 |
| 管理员后台 / 审核 / 看板 | 5–6 张 | 指标、趋势、异常队列、审核流程 |
| AI 评语 / 预警 / 组卷 | 5–6 张 | AI 草稿、证据、建议、人工确认 |

每类最终只保留 2–3 张“主参考”，其余作为局部组件或反例参考。这样能避免学生端、教师端和管理员端被三套互不相干的视觉风格带偏。
