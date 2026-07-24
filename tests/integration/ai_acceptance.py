#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""最终 AI 演示验收：覆盖三角色边界、全局问答和安全草稿自动流。

先启动 Biz、AI、Gateway，再运行：
    python tests/integration/ai_acceptance.py

脚本只读取数据并生成 AI 草稿/建议，不确认或执行正式业务动作。
"""

import json
import os
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parents[2]
if str(PROJECT_ROOT) not in sys.path:
    sys.path.insert(0, str(PROJECT_ROOT))

from tests.integration.ai_smoke import (
    RESULTS,
    data_of,
    dim,
    discover_teacher_demo_context,
    http,
    login,
    parse_sse,
    records_of,
    report,
    summary,
    trunc,
)


def validate_capability_boundaries(capabilities_by_role):
    """返回角色能力契约问题；空列表表示前后端所依赖的权限边界成立。"""
    problems = []
    student = capabilities_by_role.get("student", [])
    teacher = capabilities_by_role.get("teacher", [])
    admin = capabilities_by_role.get("admin", [])
    ordinary_admin = capabilities_by_role.get("ordinary_admin", [])

    if not student or any(item.get("mode") != "ANSWER" for item in student):
        problems.append("学生能力必须全部为 ANSWER")

    teacher_ids = {item.get("capabilityId") for item in teacher}
    teacher_modes = {item.get("mode") for item in teacher}
    if not {"ANSWER", "DRAFT", "ACTION"}.issubset(teacher_modes):
        problems.append("教师必须同时具备问答、草稿和待确认动作")
    if any(str(item or "").startswith("admin.") for item in teacher_ids):
        problems.append("教师能力中不得出现管理员能力")

    admin_ids = {item.get("capabilityId") for item in admin}
    required_admin = {
        "admin.platform-overview.query",
        "admin.teacher-registration.batch-precheck",
        "admin.teacher-registration.review",
    }
    if not required_admin.issubset(admin_ids):
        problems.append("超级管理员缺少平台问答、教师预审或教师审核能力")
    if any(str(item or "").startswith(("student.", "teacher.")) for item in admin_ids):
        problems.append("管理员能力中不得出现学生或教师专属能力")

    ordinary_ids = {item.get("capabilityId") for item in ordinary_admin}
    if not ordinary_admin or "admin.platform-overview.query" not in ordinary_ids:
        problems.append("普通管理员缺少平台治理问答能力")
    if ordinary_ids.intersection({
        "admin.user-governance.query",
        "admin.teacher-registration.batch-precheck",
        "admin.teacher-registration.review",
    }):
        problems.append("普通管理员不得获得教师注册预审或审核能力")
    return problems


def capabilities(token, course_id=None):
    suffix = f"?courseId={course_id}" if course_id else ""
    code, obj = http("GET", f"/api/v1/ai/capabilities{suffix}", token=token)
    data = data_of(obj)
    return data if code == 200 and isinstance(data, list) else []


def assistant_answer(token, question, page_path, page_title):
    code, events = http(
        "POST",
        "/api/v1/ai/assistant/stream",
        token=token,
        body={
            "question": question,
            "pagePath": page_path,
            "pageTitle": page_title,
            "conversationId": f"acceptance-{page_title.encode('utf-8').hex()[:20]}",
        },
        stream=True,
        timeout=150,
    )
    kinds, provider, delta, citations, error = parse_sse(events)
    return code, kinds, provider, delta, citations, error


def event_payload(events, event_type, capability_id=None):
    """提取 SSE 结构化事件中的业务数据。"""
    for event in events:
        if event.get("event") != event_type:
            continue
        try:
            envelope = json.loads(event.get("data") or "{}")
        except json.JSONDecodeError:
            continue
        payload = envelope.get("data", envelope) if isinstance(envelope, dict) else None
        if not isinstance(payload, dict):
            continue
        if capability_id is None or payload.get("capabilityId") == capability_id:
            return payload
    return None


def tool_names(events):
    names = set()
    for event in events:
        payload = event_payload([event], "tool")
        if payload and payload.get("toolName"):
            names.add(payload["toolName"])
    return names


def valid_single_choice_questions(questions, expected_count=3):
    if len(questions) != expected_count:
        return False
    for question in questions:
        options = question.get("options") or []
        if question.get("questionType") != "SINGLE_CHOICE" or len(options) < 2:
            return False
        if sum(1 for option in options if option.get("correct") is True) != 1:
            return False
    return True


def submitted_ids(token, course_id, limit=50):
    result = []
    assignments = records_of(http(
        "GET", f"/api/v1/teacher/courses/{course_id}/assignments?page=1&size=100", token=token)[1])
    for assignment in assignments:
        assignment_id = assignment.get("assignmentId")
        submissions = records_of(http(
            "GET", f"/api/v1/teacher/assignments/{assignment_id}/submissions?page=1&size=100", token=token)[1])
        for submission in submissions:
            status = submission.get("submissionStatus") or submission.get("status") or {}
            status_code = status.get("code") if isinstance(status, dict) else status
            grade_status = submission.get("gradeStatus") or {}
            grade_code = grade_status.get("code") if isinstance(grade_status, dict) else grade_status
            if status_code == "SUBMITTED" and grade_code != "PUBLISHED":
                result.append(submission.get("submissionId"))
            if len(result) >= limit:
                return result
    return result


def pending_teacher_ids(token):
    users = records_of(http("GET", "/api/v1/admin/users?page=1&size=100", token=token)[1])
    return [item.get("userId") for item in users
            if item.get("userStatus") == "PENDING" and "TEACHER" in (item.get("roles") or [])]


def expect_draft(title, code, obj):
    draft = data_of(obj) if code == 200 else None
    if draft and draft.get("status") in {"DRAFT", "FRAMEWORK_ONLY"}:
        report("PASS", title, f"status={draft.get('status')} requestId={draft.get('requestId')}")
        return draft
    report("FAIL", title, f"HTTP {code} {trunc(obj)}")
    return None


def valid_strong_confirmation_plan(action):
    return bool(
        action and action.get("status") == "WAITING_CONFIRMATION"
        and action.get("riskLevel") == "HIGH"
        and action.get("confirmationPolicy") == "STRONG_CONFIRM"
        and action.get("requiresConfirmation") is True
        and action.get("actionId") and action.get("preview")
    )


def main():
    print(dim("最终 AI 演示验收：只读问答 + 安全草稿，不执行正式写操作"))
    tokens = {role: login(role) for role in ("student", "teacher", "ordinary_admin", "admin")}
    for role, token in tokens.items():
        report("PASS" if token else "FAIL", f"角色登录：{role}")
    if not all(tokens.values()):
        return summary()

    student, teacher = tokens["student"], tokens["teacher"]
    ordinary_admin, admin = tokens["ordinary_admin"], tokens["admin"]
    context = discover_teacher_demo_context(teacher) or {}
    course_id = context.get("courseId")
    if not course_id:
        report("FAIL", "教师演示课程发现", "没有可用课程")
        return summary()
    report("PASS", "教师演示课程发现", f"courseId={course_id}")

    role_caps = {
        "student": capabilities(student, course_id),
        "teacher": capabilities(teacher, course_id),
        "ordinary_admin": capabilities(ordinary_admin, course_id),
        "admin": capabilities(admin, course_id),
    }
    problems = validate_capability_boundaries(role_caps)
    if problems:
        report("FAIL", "三角色能力与权限矩阵", "；".join(problems))
    else:
        report("PASS", "三角色能力与权限矩阵",
               "学生仅问答；教师问答/草稿/动作；普通管理员不含用户治理；超级管理员强确认")

    negative_checks = [
        ("学生禁止生成课时摘要", student, "POST", f"/api/v1/ai/lessons/{context.get('lessonId')}/summary-draft",
         {"courseId": course_id}),
        ("教师禁止读取管理员 AI 状态", teacher, "GET", "/api/v1/ai/admin/status", None),
        ("管理员禁止生成教师课时摘要", admin, "POST", f"/api/v1/ai/lessons/{context.get('lessonId')}/summary-draft",
         {"courseId": course_id}),
        ("普通管理员禁止读取用户治理明细", ordinary_admin, "GET", "/api/v1/admin/users?page=1&size=10", None),
    ]
    for title, token, method, path, body in negative_checks:
        code, obj = http(method, path, token=token, body=body)
        report("PASS" if code == 403 else "FAIL", title, f"HTTP {code}" if code == 403 else f"HTTP {code} {trunc(obj)}")

    questions = [
        ("学生全局学习问答", student, "请汇总我的课程、待办作业、考试、已发布成绩和学习预警，只查询不修改数据", "/student/dashboard", "学生工作台"),
        ("教师全局教学问答", teacher, "请汇总我的课程、作业、考试、待批改提交和学生预警，只查询不修改数据", "/teacher/dashboard", "教师工作台"),
        ("管理员全局运营问答", admin, "请汇总平台用户、课程、运营指标、待审核教师和 AI 服务状态，只查询不执行操作", "/admin/dashboard", "管理工作台"),
    ]
    for title, token, question, page_path, page_title in questions:
        code, kinds, provider, delta, cites, error = assistant_answer(token, question, page_path, page_title)
        ok = code == 200 and not error and "done" in kinds and bool(delta)
        report("PASS" if ok else "FAIL", title,
               f"provider={provider} 正文{len(delta)}字 引用{cites} 事件={','.join(sorted(kinds))}"
               if ok else f"HTTP {code} error={error} events={sorted(kinds)}")

    bank_title = f"最终验收AI题库-{os.getpid()}"
    code, authoring_events = http(
        "POST", "/api/v1/ai/assistant/stream", token=teacher,
        body={
            "question": (
                f"根据已发布测试课程的章节、课时和资料正文创建3道单选题草稿题库，"
                f"题库名为{bank_title}。必须先检查课程授权和内容，只创建草稿，不要发布。"
            ),
            "pagePath": "/teacher/exams",
            "pageTitle": "考试题库",
            "conversationId": f"acceptance-authoring-{os.getpid()}",
        }, stream=True, timeout=240)
    authoring = event_payload(authoring_events, "action", "course.question-bank.create")
    bank_id = authoring.get("resourceId") if authoring else None
    bank_code, bank_obj = http("GET", f"/api/v1/teacher/question-banks/{bank_id}", token=teacher) if bank_id else (0, None)
    question_code, question_obj = http(
        "GET", f"/api/v1/teacher/question-banks/{bank_id}/questions?page=1&size=100", token=teacher) if bank_id else (0, None)
    bank = data_of(bank_obj) if bank_code == 200 else None
    generated_questions = records_of(question_obj) if question_code == 200 else []
    authoring_tools = tool_names(authoring_events)
    authoring_ok = (
        code == 200 and authoring and authoring.get("status") == "DRAFT_CREATED"
        and bank and bank.get("source") == "AI" and bank.get("status") == "DRAFT"
        and bank.get("name") == bank_title and valid_single_choice_questions(generated_questions)
        and f"bankId={bank_id}" in (authoring.get("href") or "")
        and {"inspectAuthorizedCourseForAuthoring", "generateQuestionBankForCourse"}.issubset(authoring_tools)
    )
    report("PASS" if authoring_ok else "FAIL", "教师一句话生成题库并深链定位",
           f"bankId={bank_id} source=AI status=DRAFT questions=3 tools={sorted(authoring_tools)} href={authoring.get('href')}"
           if authoring_ok else f"HTTP {code} action={trunc(authoring)} bank={trunc(bank)} questions={len(generated_questions)} tools={sorted(authoring_tools)}")

    warning_id = context.get("warningId")
    if warning_id:
        expect_draft("教师预警干预计划", *http(
            "POST", f"/api/v1/ai/warnings/{warning_id}/intervention-plan", token=teacher,
            body={"instruction": "给出学生提醒、补救材料、补交任务和一周后复查安排"}, timeout=150))
    else:
        report("FAIL", "教师预警干预计划", "演示课程暂无预警；请重新导入最终演示数据")

    expect_draft("教师一键教学包计划", *http(
        "POST", f"/api/v1/ai/courses/{course_id}/teaching-package-plan", token=teacher,
        body={"instruction": "按资料梳理、教案、摘要、作业、题目、公告六阶段输出"}, timeout=150))

    submissions = submitted_ids(teacher, course_id)
    if submissions:
        code, obj = http(
            "POST", "/api/v1/ai/submissions/batch-grading-draft", token=teacher,
            body={
                "submissionIds": submissions[:50],
                "rubric": "概念准确40分、推理过程30分、示例与表达30分；总分不得越界",
                "reviewThreshold": 0.75,
                "instruction": "严格识别空答案、过短答案和低置信度结果",
            }, timeout=180)
        draft = data_of(obj) if code == 200 else None
        ok = draft and draft.get("status") == "DRAFT" and draft.get("totalCount") == len(submissions[:50])
        report("PASS" if ok else "FAIL", "教师批量辅助批改",
               f"共{draft.get('totalCount')}份，复核{draft.get('reviewCount')}份，仅返回建议"
               if ok else f"HTTP {code} {trunc(obj)}")
    else:
        report("FAIL", "教师批量辅助批改", "没有未发布成绩的已提交答案；请重新导入最终演示数据")

    expect_draft("管理员每日运营简报", *http(
        "POST", "/api/v1/ai/admin/operations-brief", token=admin,
        body={"instruction": "输出核心指标、异常信号、建议动作并标注需要人工确认的项目"}, timeout=150))

    pending = pending_teacher_ids(admin)
    code, obj = http(
        "POST", "/api/v1/ai/admin/governance-review-draft", token=admin,
        body={
            "teacherUserIds": pending[:50],
            "courseIds": [course_id],
            "criteria": "核对待审核状态、课程资料与课时完整度；只给建议，不自动审批",
        }, timeout=180)
    governance = data_of(obj) if code == 200 else None
    ok = governance and governance.get("totalCount", 0) >= 1 and governance.get("reviewCount", 0) >= 1
    report("PASS" if ok else "FAIL", "管理员教师预审与课程合规检查",
           f"检查{governance.get('totalCount')}项，人工复核{governance.get('reviewCount')}项，不执行审批"
           if ok else f"HTTP {code} {trunc(obj)}")

    if pending:
        target_user_id = pending[0]
        code, action_events = http(
            "POST", "/api/v1/ai/assistant/stream", token=admin,
            body={
                "question": (
                    f"为待审核教师 userId={target_user_id} 生成通过审核的待确认计划。"
                    "只生成风险和预览，不要执行；必须要求输入确认执行。"
                ),
                "pagePath": "/admin/users",
                "pageTitle": "用户管理",
                "conversationId": f"acceptance-admin-action-{os.getpid()}",
            }, stream=True, timeout=240)
        action = event_payload(action_events, "action", "admin.teacher-registration.review")
        action_id = action.get("actionId") if action else None
        plan_ok = code == 200 and valid_strong_confirmation_plan(action)
        report("PASS" if plan_ok else "FAIL", "管理员高风险计划与强确认预览",
               f"actionId={action_id} status=WAITING_CONFIRMATION policy=STRONG_CONFIRM"
               if plan_ok else f"HTTP {code} action={trunc(action)}")

        if action_id:
            reject_code, reject_obj = http(
                "POST", f"/api/v1/assistant-actions/{action_id}/confirm", token=admin,
                body={"confirmationText": "确认"})
            report("PASS" if reject_code in {400, 409, 422} else "FAIL", "强确认口令错误时拒绝执行",
                   f"HTTP {reject_code}" if reject_code in {400, 409, 422} else f"HTTP {reject_code} {trunc(reject_obj)}")
            cancel_code, cancel_obj = http(
                "POST", f"/api/v1/assistant-actions/{action_id}/cancel", token=admin)
            cancelled = data_of(cancel_obj) if cancel_code == 200 else None
            still_pending = target_user_id in pending_teacher_ids(admin)
            cancel_ok = cancelled and cancelled.get("status") == "CANCELLED" and still_pending
            report("PASS" if cancel_ok else "FAIL", "待确认动作取消且业务状态不变",
                   f"actionId={action_id} status=CANCELLED teacher=PENDING"
                   if cancel_ok else f"HTTP {cancel_code} action={trunc(cancelled)} stillPending={still_pending}")
        else:
            report("FAIL", "强确认口令错误时拒绝执行", "未创建待确认动作")
            report("FAIL", "待确认动作取消且业务状态不变", "未创建待确认动作")
    else:
        report("FAIL", "管理员高风险计划与强确认预览", "没有待审核教师；请重新导入最终演示数据")
        report("FAIL", "强确认口令错误时拒绝执行", "没有可用动作")
        report("FAIL", "待确认动作取消且业务状态不变", "没有可用动作")

    return summary()


if __name__ == "__main__":
    RESULTS.update({"PASS": 0, "FAIL": 0, "SKIP": 0})
    sys.exit(main())
