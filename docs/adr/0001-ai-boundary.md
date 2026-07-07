# AI service returns suggestions, Biz owns official facts

We decided that `edu-ai-service` must not connect to the Biz MySQL database and must not write official course, grade, comment, warning, exam, or forum records. AI features return answers, drafts, citations, explanations, task status, or suggestions; any accepted business result is saved by `edu-biz-service` through its own permission checks, state transitions, audit fields, and transactions.

This boundary keeps AI failures from breaking traditional teaching workflows, avoids duplicated authorization logic, and makes the system explainable in the defense: Biz owns facts, AI owns inference. The first AI implementation uses a Fake Adapter to freeze HTTP/SSE contracts and unblock integration; real Spring AI and Qdrant implementations can replace the adapter after the Biz context contracts and permission gates are stable.

