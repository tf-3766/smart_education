#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
AI 冒烟脚本：一键跑通全部 6 个 AI 接口并打印每步结果，用于调试与验收。

用法（后端已启动时）：
    python3 tests/integration/ai_smoke.py

AI 模型由后端环境变量统一配置（DASHSCOPE_API_KEY / DASHSCOPE_MODEL），脚本不会读取、保存或发送密钥。

可选环境变量：
    GATEWAY   网关地址，默认 http://localhost:18080
    STUDENT / TEACHER / ADMIN  形如 user:pass，默认 student:123456 / teacher:t123456 / admin:admin123

退出码：全部 PASS/SKIP 为 0；出现 FAIL 为 1。
"""
import json
import os
import sys
import urllib.request
import urllib.error

GATEWAY = os.environ.get("GATEWAY", "http://localhost:18080").rstrip("/")
CREDS = {
    "student": os.environ.get("STUDENT", "student:123456"),
    "teacher": os.environ.get("TEACHER", "teacher:t123456"),
    "admin": os.environ.get("ADMIN", "admin:admin123"),
    "ordinary_admin": os.environ.get("ORDINARY_ADMIN", "admin_ops:admin123"),
}

USE_COLOR = sys.stdout.isatty()
def _c(code, s): return f"\033[{code}m{s}\033[0m" if USE_COLOR else s
def green(s): return _c("32", s)
def red(s): return _c("31", s)
def yellow(s): return _c("33", s)
def dim(s): return _c("90", s)

RESULTS = {"PASS": 0, "FAIL": 0, "SKIP": 0}
def report(kind, title, detail=""):
    RESULTS[kind] = RESULTS.get(kind, 0) + 1
    tag = {"PASS": green("[PASS]"), "FAIL": red("[FAIL]"), "SKIP": yellow("[SKIP]")}[kind]
    print(f"{tag} {title}" + (f"  {dim('· ' + detail)}" if detail else ""))


def http(method, path, token=None, body=None, stream=False, timeout=90):
    """返回 (status_code, 解析后的对象 或 SSE 事件列表 或 原始文本)。异常返回 (0, str)。"""
    url = GATEWAY + path
    headers = {"Content-Type": "application/json", "X-Trace-Id": f"smoke-{os.getpid()}"}
    if token:
        headers["Authorization"] = "Bearer " + token
    if stream:
        headers["Accept"] = "text/event-stream"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        resp = urllib.request.urlopen(req, timeout=timeout)
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            return e.code, json.loads(raw)
        except Exception:
            return e.code, raw
    except Exception as e:
        return 0, f"{type(e).__name__}: {e}"

    if stream:
        events = []
        cur = {}
        try:
            for line in resp:
                line = line.decode("utf-8", "replace").rstrip("\n")
                if line == "":
                    if cur:
                        events.append(cur)
                        cur = {}
                    continue
                if line.startswith("event:"):
                    cur["event"] = line[6:].strip()
                elif line.startswith("data:"):
                    cur["data"] = cur.get("data", "") + line[5:].strip()
                if cur.get("event") in ("done", "error") and "data" in cur:
                    completed = cur
                    events.append(completed)
                    cur = {}
                    if completed.get("event") == "error":
                        break
        except Exception as e:
            events.append({"event": "_read_error", "data": f"{type(e).__name__}: {e}"})
        return resp.status, events

    raw = resp.read().decode("utf-8", "replace")
    try:
        return resp.status, json.loads(raw)
    except Exception:
        return resp.status, raw


def data_of(obj):
    return obj.get("data") if isinstance(obj, dict) else None

def records_of(obj):
    d = data_of(obj)
    if isinstance(d, dict) and isinstance(d.get("records"), list):
        return d["records"]
    if isinstance(d, list):
        return d
    return []

def login(role):
    user, pw = CREDS[role].split(":", 1)
    code, obj = http("POST", "/api/v1/auth/login", body={"username": user, "password": pw})
    if code == 200 and isinstance(obj, dict) and data_of(obj):
        return data_of(obj).get("accessToken")
    return None


def select_teacher_demo_context(contexts):
    """优先选择可教学且拥有最多演示证据的课程，避免列表首项恰好是下线课程。"""
    if not contexts:
        return None

    active_statuses = {"PUBLISHED", "IN_PROGRESS", "ONGOING"}

    def score(item):
        return (
            100 if item.get("status") in active_statuses else 0,
            8 if item.get("lessonId") else 0,
            4 if item.get("submissionId") else 0,
            2 if item.get("warningId") else 0,
            1 if item.get("assignmentId") else 0,
        )

    return max(contexts, key=score)


def discover_teacher_demo_context(token):
    contexts = []
    courses = records_of(http("GET", "/api/v1/teacher/courses?page=1&size=100", token=token)[1])
    for course in courses:
        course_id = course.get("courseId")
        status = course.get("courseStatus") or course.get("status") or {}
        status_code = status.get("code") if isinstance(status, dict) else status
        lesson_id = None
        chapters = data_of(http("GET", f"/api/v1/teacher/courses/{course_id}/chapters", token=token)[1]) or []
        for chapter in chapters if isinstance(chapters, list) else []:
            lessons = data_of(http(
                "GET", f"/api/v1/teacher/chapters/{chapter.get('chapterId')}/lessons", token=token)[1]) or []
            if lessons:
                lesson_id = lessons[0].get("lessonId")
                break

        assignment_id = submission_id = None
        assignments = records_of(http(
            "GET", f"/api/v1/teacher/courses/{course_id}/assignments?page=1&size=100", token=token)[1])
        for assignment in assignments:
            if assignment_id is None:
                assignment_id = assignment.get("assignmentId")
            submissions = records_of(http(
                "GET",
                f"/api/v1/teacher/assignments/{assignment.get('assignmentId')}/submissions?page=1&size=100",
                token=token)[1])
            if submissions:
                submission_id = submissions[0].get("submissionId")
                break

        warnings = records_of(http(
            "GET", f"/api/v1/teacher/courses/{course_id}/warnings?page=1&size=100", token=token)[1])
        contexts.append({
            "courseId": course_id,
            "status": status_code,
            "lessonId": lesson_id,
            "assignmentId": assignment_id,
            "submissionId": submission_id,
            "warningId": warnings[0].get("warningId") if warnings else None,
        })
    return select_teacher_demo_context(contexts)


def parse_sse(events):
    """归纳 SSE 事件：返回 (事件类型集合, provider, delta文本, 引用数, 错误)。"""
    kinds, provider, delta, citations, err = set(), None, "", 0, None
    for ev in events:
        k = ev.get("event"); kinds.add(k)
        try:
            d = json.loads(ev.get("data", "")) if ev.get("data") else {}
        except Exception:
            d = {}
        payload = d.get("data", d) if isinstance(d, dict) else {}
        if k == "meta" and isinstance(payload, dict):
            provider = payload.get("provider")

        elif k == "delta":
            delta += str(payload)
        elif k == "citation":
            citations += 1
        elif k in ("error", "_read_error"):
            err = payload.get("message") if isinstance(payload, dict) else str(payload) or d.get("message")
    return kinds, provider, delta, citations, err


def main():
    print(dim(f"网关 {GATEWAY} · AI 配置由后端环境变量统一提供"))
    print(dim("提示：modelConfigured=false 时，AI 正文为「模型尚未配置」占位属正常。\n"))

    # 0. 登录
    toks = {r: login(r) for r in ("student", "teacher", "admin")}
    for r, t in toks.items():
        if t:
            report("PASS", f"登录 {r}")
        else:
            report("FAIL", f"登录 {r}", "检查后端/账号密码/基础设施容器是否都在")
    if not all(toks.values()):
        print(red("\n登录失败，后续 AI 用例无法执行——先排查后端与基础设施。"))
        return summary()

    stok, ttok, atok = toks["student"], toks["teacher"], toks["admin"]

    # 1. AI 服务状态（管理员）
    code, obj = http("GET", "/api/v1/ai/admin/status", token=atok)
    st = data_of(obj) if code == 200 else None
    if st:
        caps = f"modelConfigured={st.get('modelConfigured')} vectorStoreConfigured={st.get('vectorStoreConfigured')}"
        report("PASS", "① AI 服务状态", f"service={st.get('serviceStatus')} provider={st.get('provider')} model={st.get('model')} {caps}")
    else:
        report("FAIL", "① AI 状态/测试连接", f"HTTP {code} {obj}")

    # 2. 课程答疑 SSE（学生）——必须选「可学习」课程；已下线课程 AI 会 FORBIDDEN
    sc = records_of(http("GET", "/api/v1/student/courses", token=stok)[1])
    def _learnable(x):
        st = x.get("courseStatus") or x.get("status") or {}
        code = st.get("code") if isinstance(st, dict) else st
        return code in ("PUBLISHED", "IN_PROGRESS", "ONGOING")
    course = next((x for x in sc if _learnable(x)), None)
    if not sc:
        report("SKIP", "② 课程答疑(SSE)", "该学生没有已选课程，先去选一门课")
    elif not course:
        report("SKIP", "② 课程答疑(SSE)", "已选课程都不是可学习状态（AI 答疑要求已发布/进行中），先发布或选一门在读课程")
    else:
        cid = course.get("courseId")
        code, events = http("POST", f"/api/v1/ai/courses/{cid}/qa/stream", token=stok,
                            body={"question": "这门课主要讲什么？", "conversationId": "smoke-conv-1"},
                            stream=True, timeout=90)
        kinds, provider, delta, cites, err = parse_sse(events)
        if err:
            report("FAIL", "② 课程答疑(SSE)", f"error: {err}")
        elif "done" in kinds or delta:
            seq = "→".join([k for k in ("meta", "delta", "citation", "done") if k in kinds])
            report("PASS", "② 课程答疑(SSE)", f"事件 {seq} · provider={provider} · 引用×{cites} · 正文{len(delta)}字")
        else:
            report("FAIL", "② 课程答疑(SSE)", f"未收到 done/delta，事件={sorted(kinds)} HTTP={code}")

    # 教师侧 ID 发现：扫描全部课程，优先选有课时、提交和预警的在教课程。
    teacher_context = discover_teacher_demo_context(ttok) or {}
    tcid = teacher_context.get("courseId")

    # 3. 课时摘要（教师）
    lid = teacher_context.get("lessonId")
    if not lid:
        report("SKIP", "③ 课时摘要草稿", "教师课程下没有可用课时")
    else:
        code, obj = http("POST", f"/api/v1/ai/lessons/{lid}/summary-draft", token=ttok,
                         body={"courseId": tcid})
        d = data_of(obj) if code == 200 else None
        if d:
            report("PASS", "③ 课时摘要草稿", f"provider={d.get('provider')} status={d.get('status')} 正文{len(d.get('content') or '')}字")
        else:
            report("FAIL", "③ 课时摘要草稿", f"HTTP {code} {trunc(obj)}")

    # 4. 批改评语（教师）— 需要一条提交
    subid = teacher_context.get("submissionId")
    if not subid:
        report("SKIP", "④ 批改评语草稿", "没有可批改的学生提交")
    else:
        code, obj = http("POST", f"/api/v1/ai/submissions/{subid}/comment-draft", token=ttok,
                         body={"instruction": "语气鼓励一些"})
        d = data_of(obj) if code == 200 else None
        if d:
            report("PASS", "④ 批改评语草稿", f"provider={d.get('provider')} 正文{len(d.get('content') or '')}字")
        else:
            report("FAIL", "④ 批改评语草稿", f"HTTP {code} {trunc(obj)}")

    # 5. 预警解读（教师）— 需要一条预警
    wid = teacher_context.get("warningId")
    if not wid:
        report("SKIP", "⑤ 预警解读草稿", "该课程暂无预警（可先在教师端生成预警再跑）")
    else:
        code, obj = http("POST", f"/api/v1/ai/warnings/{wid}/explanation", token=ttok,
                         body={"instruction": None})
        d = data_of(obj) if code == 200 else None
        if d:
            report("PASS", "⑤ 预警解读草稿", f"provider={d.get('provider')} 正文{len(d.get('content') or '')}字")
        else:
            report("FAIL", "⑤ 预警解读草稿", f"HTTP {code} {trunc(obj)}")

    # 6. 组卷建议（教师）
    if not tcid:
        report("SKIP", "⑥ 组卷建议草稿", "教师没有课程")
    else:
        code, obj = http("POST", "/api/v1/ai/exams/paper-suggestions", token=ttok,
                         body={"courseId": tcid, "questionCount": 5, "totalScore": 100, "requirements": "覆盖第一章"})
        d = data_of(obj) if code == 200 else None
        if d:
            report("PASS", "⑥ 组卷建议草稿", f"provider={d.get('provider')} 正文{len(d.get('content') or '')}字")
        else:
            report("FAIL", "⑥ 组卷建议草稿", f"HTTP {code} {trunc(obj)}")

    return summary()


def trunc(obj, n=160):
    s = json.dumps(obj, ensure_ascii=False) if isinstance(obj, (dict, list)) else str(obj)
    return s[:n]


def summary():
    print()
    line = f"汇总：{green(str(RESULTS['PASS'])+' PASS')} · {red(str(RESULTS['FAIL'])+' FAIL')} · {yellow(str(RESULTS['SKIP'])+' SKIP')}"
    print(line)
    print(dim("说明：provider=fallback 与占位正文表示后端未配置模型；请在后端环境变量中设置 DASHSCOPE_API_KEY 后重启服务。"))
    if RESULTS["FAIL"]:
        print(dim("排错：拿响应里的 traceId 去 backend/logs/edu-ai-service-local.log 与 edu-biz-service-local.log 里 grep。"))
    return 1 if RESULTS["FAIL"] else 0


if __name__ == "__main__":
    sys.exit(main())
