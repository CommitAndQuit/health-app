---
description: Run agy task implementation workflow
---

Run the agy implementation workflow for a task spec.

Usage: /agy <task-spec>

Example: /agy 001a-remove-stress-and-gadgetbridge

This reads the task spec from `plan/tasks/<task-spec>` and runs agy with the
standard prompt and schema. The agy session will:

1. Read the task specification from `plan/tasks/`
2. Execute the implementation using the configured model
3. Verify with `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest`
4. Return a structured JSON report matching `plan/schemas/implement-report.json`

The command passes the task spec name as `$ARGUMENTS` to agy's `-p` flag.