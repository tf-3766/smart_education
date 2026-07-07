package com.zhongruan.edu.biz.course.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhongruan.edu.biz.course.domain.enums.ChapterStatus;
import com.zhongruan.edu.biz.course.domain.enums.CourseStatus;
import com.zhongruan.edu.biz.course.domain.enums.EnrollmentStatus;
import com.zhongruan.edu.biz.course.domain.enums.LearningStatus;
import org.junit.jupiter.api.Test;

class CourseStateMachineTest {
    @Test
    void courseCannotSkipReviewOrReturnFromOffline() {
        assertTrue(CourseStatus.DRAFT.canTransitionTo(CourseStatus.PENDING_REVIEW));
        assertFalse(CourseStatus.DRAFT.canTransitionTo(CourseStatus.PUBLISHED));
        assertTrue(CourseStatus.PENDING_REVIEW.canTransitionTo(CourseStatus.PUBLISHED));
        assertFalse(CourseStatus.OFFLINE.canTransitionTo(CourseStatus.PUBLISHED));
    }

    @Test
    void chapterPublicationAndOfflineAreOneWay() {
        assertTrue(ChapterStatus.DRAFT.canTransitionTo(ChapterStatus.PUBLISHED));
        assertTrue(ChapterStatus.PUBLISHED.canTransitionTo(ChapterStatus.OFFLINE));
        assertFalse(ChapterStatus.OFFLINE.canTransitionTo(ChapterStatus.PUBLISHED));
    }

    @Test
    void enrollmentAndLearningFactsOnlyMoveForward() {
        assertTrue(EnrollmentStatus.ENROLLED.canTransitionTo(EnrollmentStatus.WITHDRAWN));
        assertTrue(EnrollmentStatus.ENROLLED.canTransitionTo(EnrollmentStatus.COMPLETED));
        assertFalse(EnrollmentStatus.WITHDRAWN.canTransitionTo(EnrollmentStatus.ENROLLED));
        assertTrue(LearningStatus.NOT_STARTED.canTransitionTo(LearningStatus.COMPLETED));
        assertFalse(LearningStatus.COMPLETED.canTransitionTo(LearningStatus.IN_PROGRESS));
    }
}
