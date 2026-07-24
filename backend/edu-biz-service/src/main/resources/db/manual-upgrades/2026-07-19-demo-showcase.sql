SET NAMES utf8mb4;
-- Comprehensive idempotent demo data for the online education assistant platform.
-- Bootstrap already contains the required schema. Passwords reuse documented local demo accounts.

INSERT INTO sys_user (id, username, password_hash, display_name, user_status, created_at, created_by, updated_at, updated_by, deleted, version) VALUES
(1010,'student_a','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','林晓雨','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),
(1011,'student_b','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','周子航','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),
(1012,'student_c','$2b$10$PdES/6jxHSkOMhYepC0Q2.9UCOkPGfR0XNt9T1.WBf9twstpDZ11u','陈嘉怡','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),
(1013,'teacher_pending','$2b$10$4/jxzR1iDdnQVYlELBd2zuN3wCdDNlcAfjX4bX.4e08ggPOiLcieS','待审核教师','PENDING',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),
(1014,'admin_ops','$2b$10$qklC5Vnw0Ov6Q3AVg1onj.DUeTJwQ0Zxh.o0fD0qkexIAF6y05yRG','测试普通管理员','ENABLED',CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0)
ON DUPLICATE KEY UPDATE display_name=VALUES(display_name), user_status=VALUES(user_status), deleted=0, updated_at=CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (id,user_id,role_id,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(4010,1010,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4011,1011,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),
(4012,1012,2001,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),(4013,1013,2002,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0),
(4014,1014,2003,CURRENT_TIMESTAMP,0,CURRENT_TIMESTAMP,0,0,0)
ON DUPLICATE KEY UPDATE deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_course_enrollment (id,course_id,student_id,status,enrolled_at,withdrawn_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(21210,21001,1010,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1010,0,0),
(21211,21001,1011,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),
(21212,21001,1012,'ENROLLED',CURRENT_TIMESTAMP,NULL,CURRENT_TIMESTAMP,1012,CURRENT_TIMESTAMP,1012,0,0)
ON DUPLICATE KEY UPDATE status='ENROLLED',withdrawn_at=NULL,deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_course_chapter (id,course_id,title,description,sort_order,status,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(22010,21001,'第二章 算法与实践','从概念理解进入算法设计、测试与复盘。',20,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(22011,21001,'第三章 项目协作','围绕在线教育项目开展小组讨论与阶段验收。',30,'PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),description=VALUES(description),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_course_lesson (id,course_id,chapter_id,title,content_type,content,video_url,estimated_minutes,sort_order,status,unlock_type,unlock_at,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(23010,21001,22010,'快速排序：分区思想','RICH_TEXT','理解快速排序的分区过程、递归边界、平均与最坏时间复杂度，并能使用测试样例验证实现。',NULL,25,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(23011,21001,22010,'算法复杂度与测试','RICH_TEXT','使用大 O 表示法分析算法，并通过空数组、重复元素和逆序输入覆盖边界条件。',NULL,20,20,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(23012,21001,22011,'需求分析与接口联调','RICH_TEXT','围绕课程、作业、考试、讨论和 AI 助手梳理前后端业务流程，形成可验收的测试记录。',NULL,30,10,'PUBLISHED','IMMEDIATE',NULL,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),content=VALUES(content),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_lesson_learning_record (id,course_id,chapter_id,lesson_id,student_id,status,started_at,completed_at,last_studied_at,study_seconds,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(26010,21001,22010,23010,1010,'COMPLETED',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 2 DAY),DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 2 DAY),DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 2 DAY),1800,CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1010,0,0),
(26011,21001,22010,23010,1011,'LEARNING',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 DAY),NULL,CURRENT_TIMESTAMP,420,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),
(26012,21001,22010,23010,1012,'NOT_STARTED',NULL,NULL,NULL,0,CURRENT_TIMESTAMP,1012,CURRENT_TIMESTAMP,1012,0,0)
ON DUPLICATE KEY UPDATE status=VALUES(status),study_seconds=VALUES(study_seconds),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_assignment (id,course_id,lesson_id,title,description,response_mode,questions_json,max_score,status,open_at,due_at,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(31010,21001,23010,'快速排序编程实践','实现快速排序并说明分区策略。可直接粘贴代码，也可上传本地代码或测试报告。','MIXED',NULL,100,'PUBLISHED','2026-07-01 08:00:00','2026-09-10 23:59:59',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(31011,21001,23011,'算法基础综合练习','完成选择、判断、填空和简答题。','QUIZ','[{"questionId":"q1","questionType":"SINGLE_CHOICE","stem":"快速排序平均时间复杂度是？","options":["O(n)","O(n log n)","O(n²)","O(log n)"],"score":20,"correctAnswers":["1"]},{"questionId":"q2","questionType":"TRUE_FALSE","stem":"快速排序一定是稳定排序。","options":["正确","错误"],"score":20,"correctAnswers":["1"]},{"questionId":"q3","questionType":"FILL_BLANK","stem":"分区后基准元素位于其最终____。","options":[],"score":20,"correctAnswers":["位置"]},{"questionId":"q4","questionType":"SHORT_ANSWER","stem":"说明如何测试快速排序的边界条件。","options":[],"score":40,"correctAnswers":[]}]',100,'PUBLISHED','2026-07-01 08:00:00','2026-09-15 23:59:59',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(31012,21001,23012,'项目阶段复盘','结合本周联调情况写一份问题、原因、修复和验证记录。','TEXT',NULL,100,'PUBLISHED','2026-07-01 08:00:00','2026-09-20 23:59:59',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),description=VALUES(description),response_mode=VALUES(response_mode),questions_json=VALUES(questions_json),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_assignment_submission (id,assignment_id,course_id,student_id,attempt_no,content,answers_json,file_id,file_key,file_url,status,submitted_at,score,teacher_comment,ai_comment_draft_id,graded_by,graded_at,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(32010,31010,21001,1010,1,'def quick_sort(items):\n    if len(items) <= 1: return items\n    pivot = items[len(items)//2]\n    return quick_sort([x for x in items if x < pivot]) + [x for x in items if x == pivot] + quick_sort([x for x in items if x > pivot])',NULL,NULL,NULL,NULL,'GRADED',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 DAY),92,'实现正确，测试样例完整；可进一步说明空间复杂度。',NULL,1002,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),
(32011,31010,21001,1011,1,'已提交 Java 版本思路和三组测试样例。',NULL,NULL,NULL,NULL,'SUBMITTED',CURRENT_TIMESTAMP,NULL,NULL,NULL,NULL,NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),
(32012,31011,21001,1010,1,'已完成在线题目。','{"q1":["1"],"q2":["1"],"q3":["位置"],"q4":["覆盖空数组、单元素、重复元素、正序和逆序输入，并比较输出是否有序。"]}',NULL,NULL,NULL,'GRADED',CURRENT_TIMESTAMP,95,'客观题正确，简答题覆盖全面。',NULL,1002,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),
(32013,31011,21001,1012,1,'已完成部分题目。','{"q1":["2"],"q2":["1"],"q3":["位置"],"q4":["测试普通输入。"]}',NULL,NULL,NULL,'SUBMITTED',CURRENT_TIMESTAMP,NULL,NULL,NULL,NULL,NULL,NULL,CURRENT_TIMESTAMP,1012,CURRENT_TIMESTAMP,1012,0,0),
(32014,31012,21001,1011,1,'本周完成课程资料预览和作业提交联调，发现受保护文件地址不能直接用于图片标签，改为携带令牌读取 Blob 后验证通过。',NULL,NULL,NULL,NULL,'SUBMITTED',CURRENT_TIMESTAMP,NULL,NULL,NULL,NULL,NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0)
ON DUPLICATE KEY UPDATE content=VALUES(content),answers_json=VALUES(answers_json),status=VALUES(status),score=VALUES(score),teacher_comment=VALUES(teacher_comment),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_grade_record (id,course_id,student_id,source_type,source_id,score,max_score,weight,grade_status,comment,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(33010,21001,1010,'ASSIGNMENT',31010,92,100,20,'PUBLISHED','快速排序编程实践成绩',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(33011,21001,1010,'ASSIGNMENT',31011,95,100,20,'PUBLISHED','算法基础综合练习成绩',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE score=VALUES(score),grade_status='PUBLISHED',comment=VALUES(comment),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_forum_topic (id,course_id,author_id,title,content,status,pinned,reply_count,last_replied_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(34010,21001,1010,'快速排序基准元素怎么选？','我分别尝试首元素、中间元素和随机元素作为基准，想讨论它们对接近有序数组的影响。','VISIBLE',1,3,CURRENT_TIMESTAMP,DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 2 DAY),1010,CURRENT_TIMESTAMP,1010,0,0),
(34011,21001,1011,'分享一组边界测试','空数组、单元素、全重复、已经有序和完全逆序是我认为最值得保留的五组测试。','VISIBLE',0,2,CURRENT_TIMESTAMP,DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 DAY),1011,CURRENT_TIMESTAMP,1011,0,0),
(34012,21001,1012,'项目联调心得','接口字段、按钮状态和数据库状态必须一起验证，只看页面成功提示并不够。','VISIBLE',0,1,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1012,CURRENT_TIMESTAMP,1012,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),content=VALUES(content),pinned=VALUES(pinned),reply_count=VALUES(reply_count),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_forum_reply (id,topic_id,course_id,author_id,parent_reply_id,content,status,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(35010,34010,21001,1002,NULL,'这个对比很好。建议再加入随机数据，并记录递归深度。','VISIBLE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(35011,34010,21001,1011,NULL,'随机基准能降低持续遇到极端划分的概率。','VISIBLE',CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),
(35012,34010,21001,1010,35011,'我会补一组固定随机种子的实验，方便复现。','VISIBLE',CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1010,0,0),
(35013,34011,21001,1002,NULL,'还可以增加包含负数和大量重复元素的输入。','VISIBLE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(35014,34011,21001,1012,35013,'同意，重复元素也能验证三路分区的效果。','VISIBLE',CURRENT_TIMESTAMP,1012,CURRENT_TIMESTAMP,1012,0,0),
(35015,34012,21001,1002,NULL,'很好，验收时请把请求、响应和页面结果放在同一条测试记录中。','VISIBLE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE content=VALUES(content),status='VISIBLE',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_question_bank (id,course_id,name,description,status,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(37010,21001,'算法综合演示题库','覆盖单选、多选、判断、填空和简答，可用于手动组卷与 AI 组卷建议。','ACTIVE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE name=VALUES(name),description=VALUES(description),status='ACTIVE',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_question (id,bank_id,course_id,question_type,stem,analysis,difficulty,score,status,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(37110,37010,21001,'SINGLE_CHOICE','快速排序平均时间复杂度是？','平均情况下为 O(n log n)。','EASY',15,'ACTIVE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(37111,37010,21001,'MULTIPLE_CHOICE','以下哪些输入适合作为边界测试？','空数组、重复元素和逆序输入都应覆盖。','MEDIUM',20,'ACTIVE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(37112,37010,21001,'TRUE_FALSE','快速排序在所有实现中都是稳定排序。','通常不稳定。','EASY',15,'ACTIVE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(37113,37010,21001,'FILL_BLANK','快速排序通过____操作把序列划分为两部分。','参考答案：分区。','EASY',20,'ACTIVE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(37114,37010,21001,'SHORT_ANSWER','说明如何验证一个快速排序实现是正确的。','应包含多类输入、结果有序性和元素集合不变性。','HARD',30,'ACTIVE',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE stem=VALUES(stem),analysis=VALUES(analysis),difficulty=VALUES(difficulty),score=VALUES(score),status='ACTIVE',deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_question_option (id,question_id,option_label,option_content,is_correct,sort_order,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(37210,37110,'A','O(n)',0,10,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(37211,37110,'B','O(n log n)',1,20,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(37212,37110,'C','O(n²)',0,30,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(37213,37111,'A','空数组',1,10,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(37214,37111,'B','全重复元素',1,20,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(37215,37111,'C','完全逆序',1,30,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(37216,37111,'D','只测一个普通数组',0,40,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(37217,37112,'A','正确',0,10,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(37218,37112,'B','错误',1,20,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE option_content=VALUES(option_content),is_correct=VALUES(is_correct),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_exam (id,course_id,title,description,status,start_at,end_at,duration_minutes,total_score,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(38010,21001,'算法综合能力测试','覆盖五种题型，展示题库组卷、学生答题和教师阅卷流程。','PUBLISHED','2026-07-01 09:00:00','2026-12-31 10:30:00',90,100,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),description=VALUES(description),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_exam_paper (id,exam_id,course_id,title,total_score,status,ai_generation_record_id,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(38110,38010,21001,'算法综合能力测试 A 卷',100,'PUBLISHED',NULL,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),total_score=100,status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_exam_paper_question (id,paper_id,question_id,question_order,score,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(38210,38110,37110,1,15,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(38211,38110,37111,2,20,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(38212,38110,37112,3,15,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(38213,38110,37113,4,20,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),(38214,38110,37114,5,30,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE score=VALUES(score),deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_exam_attempt (id,exam_id,paper_id,student_id,status,started_at,submitted_at,score,graded_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(38310,38010,38110,1010,'GRADED',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 1 HOUR),CURRENT_TIMESTAMP,91,CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),
(38311,38010,38110,1011,'SUBMITTED',DATE_SUB(CURRENT_TIMESTAMP,INTERVAL 45 MINUTE),CURRENT_TIMESTAMP,NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0)
ON DUPLICATE KEY UPDATE status=VALUES(status),score=VALUES(score),graded_at=VALUES(graded_at),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_learning_warning (id,course_id,student_id,warning_type,warning_level,warning_status,summary,suggestion,ai_explanation_draft_id,generated_at,handled_by,handle_remark,handled_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(36010,21001,1012,'PROGRESS_LAG','HIGH','OPEN','课程学习进度明显落后','建议教师联系学生确认学习阻碍，并制定两周补学计划。',NULL,CURRENT_TIMESTAMP,NULL,NULL,NULL,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(36011,21001,1011,'MISSING_ASSIGNMENT','MEDIUM','OPEN','存在未完成作业','建议在截止日前完成算法综合练习。',NULL,CURRENT_TIMESTAMP,NULL,NULL,NULL,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE warning_status=VALUES(warning_status),summary=VALUES(summary),suggestion=VALUES(suggestion),deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_warning_evidence (id,warning_id,evidence_type,source_id,metric_code,metric_value,description,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(36110,36010,'LESSON_PROGRESS',23010,'completedLessonRate','0.00','算法章节尚未开始学习。',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(36111,36011,'ASSIGNMENT',31011,'missingAssignmentCount','1','算法综合练习尚未提交。',CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE metric_value=VALUES(metric_value),description=VALUES(description),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_announcement (id,scope_type,course_id,title,content,audience,status,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(35010,'COURSE',21001,'算法章节学习安排','请先学习快速排序课时，再完成编程实践和综合练习；有疑问可在课程讨论区发帖。','STUDENT','PUBLISHED',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),content=VALUES(content),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;
INSERT INTO edu_exam_answer (id,attempt_id,question_id,answer_content,score,teacher_comment,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(38410,38310,37110,'B',15,'回答正确。',CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),(38411,38310,37111,'A,B,C',20,'覆盖完整。',CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),(38412,38310,37112,'B',15,'回答正确。',CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),(38413,38310,37113,'分区',18,'概念正确。',CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),(38414,38310,37114,'使用空数组、单元素、重复、正序、逆序和随机输入，检查输出有序且元素集合不变。',23,'方法完整，可补充性能测试。',CURRENT_TIMESTAMP,1010,CURRENT_TIMESTAMP,1002,0,0),
(38415,38311,37110,'B',NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),(38416,38311,37111,'A,C',NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),(38417,38311,37112,'B',NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),(38418,38311,37113,'分区',NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0),(38419,38311,37114,'使用多组输入比较预期结果。',NULL,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0)
ON DUPLICATE KEY UPDATE answer_content=VALUES(answer_content),score=VALUES(score),teacher_comment=VALUES(teacher_comment),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_grade_record (id,course_id,student_id,source_type,source_id,score,max_score,weight,grade_status,comment,published_at,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(33012,21001,1010,'EXAM',38010,91,100,40,'PUBLISHED','算法综合能力测试成绩',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0)
ON DUPLICATE KEY UPDATE score=VALUES(score),grade_status='PUBLISHED',comment=VALUES(comment),deleted=0,updated_at=CURRENT_TIMESTAMP;

INSERT INTO edu_notification (id,recipient_user_id,title,content,category,status,source_type,idempotency_key,announcement_id,course_id,assignment_id,exam_id,warning_id,created_at,created_by,updated_at,updated_by,deleted,version) VALUES
(50110,1010,'作业已批改：快速排序编程实践','成绩 92 分，请查看教师评语。','ASSIGNMENT','PUBLISHED','GRADE_PUBLISHED','demo:grade:33010:user:1010',NULL,21001,31010,NULL,NULL,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(50111,1011,'待完成：算法基础综合练习','请在截止时间前完成在线题目。','ASSIGNMENT','PUBLISHED','ASSIGNMENT_PUBLISHED','demo:assignment:31011:user:1011',NULL,21001,31011,NULL,NULL,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(50112,1012,'学习预警：进度落后','请进入课程查看补学建议。','WARNING','PUBLISHED','WARNING_CREATED','demo:warning:36010:user:1012',NULL,21001,NULL,NULL,36010,CURRENT_TIMESTAMP,1002,CURRENT_TIMESTAMP,1002,0,0),
(50113,1002,'有新的考试答卷待阅卷','周子航已提交算法综合能力测试。','EXAM','PUBLISHED','EXAM_SUBMITTED','demo:exam:38010:teacher:1002',NULL,21001,NULL,38010,NULL,CURRENT_TIMESTAMP,1011,CURRENT_TIMESTAMP,1011,0,0)
ON DUPLICATE KEY UPDATE title=VALUES(title),content=VALUES(content),status='PUBLISHED',deleted=0,updated_at=CURRENT_TIMESTAMP;
