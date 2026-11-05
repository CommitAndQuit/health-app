# AGENTS.md — BioTrace working contract

BioTrace is a local-first Android health app. This file is read by every agent
(OpenCode and Antigravity CLI). Keep it short and binding.

## Data source rule

- Android **Health Connect** is the only source of health data.
- Never reference or depend on a specific upstream publisher (Gadgetbridge,
  Huawei Health, Garmin, Fitbit, Samsung Health, etc.) in code, UI copy,
  resources, tests, or docs.
- Do not add direct database imports, file imports, or vendor SDKs. If a metric
  is not published by Health Connect, it does not belong in the product.
- Health Connect record metadata may be used internally, but user-facing copy
  says "Health Connect", not a vendor or device name.

## Architecture

- `domain/` — models, repository interfaces, use cases. No Android UI deps.
- `data/healthconnect/` — Health Connect client, sync service, worker.
- `data/local/` — Room entities, DAOs, database.
- `data/repository/` — repository implementations.
- `ui/<feature>/` — Compose screens and ViewModels (MVVM + Hilt).
- DI modules live in `di/`.

## Build and verify

```bash
./gradlew :app:compileDebugKotlin
./gradlew :app:testDebugUnitTest
```

Both must pass before a change is considered done. Do not commit or push unless
explicitly instructed.

## Change discipline

- Stay inside the file scope given by the task.
- Do not modify build files, dependency versions, or Room schema unrelated to
  the task.
- Do not read or modify `.env`, keystores, or anything under `.git/`.
- Prefer deleting dead code over leaving it commented out.
