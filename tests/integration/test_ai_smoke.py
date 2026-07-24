import unittest

from tests.integration.ai_acceptance import (
    validate_capability_boundaries,
    valid_single_choice_questions,
    valid_strong_confirmation_plan,
)
from tests.integration.ai_smoke import select_teacher_demo_context


class AiSmokeSelectionTest(unittest.TestCase):
    def test_prefers_active_course_with_demo_evidence_over_first_offline_course(self):
        contexts = [
            {
                "courseId": 21004,
                "status": "OFFLINE",
                "lessonId": None,
                "submissionId": None,
                "warningId": None,
            },
            {
                "courseId": 21001,
                "status": "PUBLISHED",
                "lessonId": 23001,
                "submissionId": 32001,
                "warningId": 36001,
            },
        ]

        selected = select_teacher_demo_context(contexts)

        self.assertEqual(21001, selected["courseId"])

    def test_role_capability_boundaries_reject_student_writes_and_admin_leaks(self):
        problems = validate_capability_boundaries({
            "student": [
                {"capabilityId": "student.learning-overview.query", "mode": "ANSWER"},
            ],
            "teacher": [
                {"capabilityId": "teacher.teaching-overview.query", "mode": "ANSWER"},
                {"capabilityId": "course.question-bank.create", "mode": "DRAFT"},
                {"capabilityId": "course.submission.grade", "mode": "ACTION"},
            ],
            "admin": [
                {"capabilityId": "admin.platform-overview.query", "mode": "ANSWER"},
                {"capabilityId": "admin.teacher-registration.batch-precheck", "mode": "DRAFT"},
                {"capabilityId": "admin.teacher-registration.review", "mode": "ACTION"},
            ],
            "ordinary_admin": [
                {"capabilityId": "admin.platform-overview.query", "mode": "ANSWER"},
                {"capabilityId": "admin.course.compliance-check", "mode": "DRAFT"},
                {"capabilityId": "platform.term-enrollment-window.upsert", "mode": "ACTION"},
            ],
        })

        self.assertEqual([], problems)

    def test_role_capability_boundaries_reject_super_admin_tools_for_ordinary_admin(self):
        problems = validate_capability_boundaries({
            "student": [{"capabilityId": "student.task.query", "mode": "ANSWER"}],
            "teacher": [
                {"capabilityId": "teacher.grading.query", "mode": "ANSWER"},
                {"capabilityId": "course.question-bank.create", "mode": "DRAFT"},
                {"capabilityId": "course.submission.grade", "mode": "ACTION"},
            ],
            "admin": [
                {"capabilityId": "admin.platform-overview.query", "mode": "ANSWER"},
                {"capabilityId": "admin.teacher-registration.batch-precheck", "mode": "DRAFT"},
                {"capabilityId": "admin.teacher-registration.review", "mode": "ACTION"},
            ],
            "ordinary_admin": [
                {"capabilityId": "admin.platform-overview.query", "mode": "ANSWER"},
                {"capabilityId": "admin.teacher-registration.review", "mode": "ACTION"},
            ],
        })

        self.assertIn("普通管理员不得获得教师注册预审或审核能力", problems)

    def test_strong_confirmation_plan_does_not_require_result_link_before_execution(self):
        self.assertTrue(valid_strong_confirmation_plan({
            "actionId": "42",
            "status": "WAITING_CONFIRMATION",
            "riskLevel": "HIGH",
            "confirmationPolicy": "STRONG_CONFIRM",
            "requiresConfirmation": True,
            "preview": {"审核决定": "通过教师注册"},
            "href": None,
        }))

    def test_generated_question_bank_requires_valid_single_choice_structure(self):
        self.assertTrue(valid_single_choice_questions([
            {"questionType": "SINGLE_CHOICE", "options": [
                {"label": "A", "correct": True}, {"label": "B", "correct": False}]},
            {"questionType": "SINGLE_CHOICE", "options": [
                {"label": "A", "correct": False}, {"label": "B", "correct": True}]},
            {"questionType": "SINGLE_CHOICE", "options": [
                {"label": "A", "correct": False}, {"label": "B", "correct": True}]},
        ]))
        self.assertFalse(valid_single_choice_questions([
            {"questionType": "SINGLE_CHOICE", "options": [
                {"label": "A", "correct": False}, {"label": "B", "correct": False}]},
        ], expected_count=1))


if __name__ == "__main__":
    unittest.main()
