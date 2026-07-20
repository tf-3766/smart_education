# AI stack uses Fake Adapter first, then Spring AI and Qdrant

> Current implementation (2026-07-18): Spring AI 1.1.8 and the fallback adapter share the `AiTextGenerator` boundary. Optional Qdrant RAG, per-conversation memory, and in-process `@Tool` course tools are implemented; external MCP remains a later integration boundary.

We decided to freeze AI contracts with a Fake Adapter first, while treating Spring AI and Qdrant as the default real implementation path. The current GitHub repository will not be migrated to GitLab during the MVP; member B will add a `.gitlab-ci.yml` delivery artifact so the project can demonstrate GitLab CI/CD compatibility without disrupting the existing remote and branch workflow.

This choice keeps the 18-day MVP from being blocked by model keys, embedding costs, vector-store setup, or Git hosting migration. If the team later switches to LangChain4j, Milvus, or a real GitLab mirror, that change needs a new ADR and must include the migration cost, owner, rollback plan, and impact on AI contracts.
