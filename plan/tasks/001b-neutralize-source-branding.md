# Task 001b — Neutralise source branding and replace the wearable dialog

## Goal

Task 001a removed the Gadgetbridge pipeline and stress metric, but user-facing
code still names a specific watch and vendor. Remove every remaining
vendor/device reference and replace the "Connected Wearable" dialog with a
neutral data-source panel. After this task, searching the app and README for
vendor names returns nothing.

Read `AGENTS.md` at the repository root first. Obey it.

## Files in scope

- `app/src/main/java/io/biotrace/app/data/healthconnect/LastSyncStore.kt` (new)
- `app/src/main/java/io/biotrace/app/data/healthconnect/HealthConnectSyncService.kt`
- `app/src/main/java/io/biotrace/app/ui/common/BioTraceTopBar.kt`
- `app/src/main/java/io/biotrace/app/data/repository/VitalsRepositoryImpl.kt`
- `app/src/main/java/io/biotrace/app/domain/clinical/VitalsCalculations.kt`
- `readme.md`
- Any other file under `app/src` that still contains a vendor/device name.

## 1. Add a last-sync store

Create `app/src/main/java/io/biotrace/app/data/healthconnect/LastSyncStore.kt`:

```kotlin
package io.biotrace.app.data.healthconnect

import android.content.Context

/**
 * Records when BioTrace last completed a Health Connect sync.
 * Deliberately source-agnostic: only a timestamp is stored.
 */
object LastSyncStore {
    private const val PREFS = "sync_status"
    private const val KEY_LAST_SYNC = "last_sync_epoch_ms"

    fun record(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putLong(KEY_LAST_SYNC, System.currentTimeMillis())
            .apply()
    }

    fun lastSyncMillis(context: Context): Long? {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_SYNC, 0L)
        return value.takeIf { it > 0L }
    }
}
```

Update `HealthConnectSyncService.kt`:
- Add `@ApplicationContext private val context: Context` to the constructor
  (import `dagger.hilt.android.qualifiers.ApplicationContext`).
- At the end of `syncAll(...)`, call `LastSyncStore.record(context)` after the
  individual sync calls complete, then return the combined `SyncResult`.
  Keep the existing return value semantics unchanged.

## 2. Replace the wearable dialog

`app/src/main/java/io/biotrace/app/ui/common/BioTraceTopBar.kt`
- Re-add a dialog that opens when the avatar is clicked and `onAvatarClick` is
  `null`. Name it `DataSourceDialog`.
- Content:
  - Title: `"Data Source"`
  - Row with `Icons.Default.CheckCircle` (ClinicalEmerald) and `"Health Connect"`.
  - Body: `"BioTrace reads health data directly from Health Connect. Data appears here from any app you allow to write to Health Connect."`
  - `"Last sync: <label>"` using `LastSyncStore.lastSyncMillis(context)` and
    `DateTimeFormatter.ofPattern("MMM d, HH:mm", Locale.getDefault())` in the
    system zone; if null, show `"Last sync: Not synced yet"`.
  - A `TextButton` "Close".
- Change the avatar `contentDescription` to `"Data source"`.
- Do not add any import/file-picker button, device name, model, serial, or
  vendor mention.
- Keep the `onAvatarClick` override behaviour unchanged for screens that pass it.
- Keep the existing visual language (colors, shapes, spacing). Remove unused
  imports.

## 3. Neutralise labels in the repository

`app/src/main/java/io/biotrace/app/data/repository/VitalsRepositoryImpl.kt`
- Every `deviceSource` and `dataSourceLabel` must describe only Health Connect:
  - `"HONOR MagicWatch 2 • Health Connect"` → `"Health Connect"`
  - `"HONOR MagicWatch 2 • Fallback"` → `"Health Connect • Derived"`
  - `"HONOR MagicWatch 2 • SpO2"` → `"Health Connect"`
  - `"HONOR MagicWatch 2 • Sleep"` → `"Health Connect"`
  - `"HONOR MagicWatch 2 • Workout"` → `"Health Connect"`
  - `"HONOR MagicWatch 2-38D • Health Connect"` → `"Health Connect"`
  - `"HONOR MagicWatch 2-38D • Health Connect Fallback"` → `"Health Connect • Derived"`
  - `StatusBadge("TRUSLEEP", ...)` → `StatusBadge("SLEEP STAGES", ...)`
- Replace `"HUAWEI TruSleep architecture"` with
  `"Sleep stages recorded in Health Connect"`.
- Leave the remaining labels (for example `"Health Connect & Profile Attributes"`)
  as they are.

## 4. Comments and docs

`app/src/main/java/io/biotrace/app/domain/clinical/VitalsCalculations.kt`
- Rewrite the comment mentioning a specific Honor watch to a generic
  `"(for example, a wearable that does not emit RestingHeartRateRecord)"`.

`readme.md`
- Update the description so it no longer says data is "fed by Gadgetbridge".
  Say data comes from apps that write to Health Connect.
- Replace the `Gadgetbridge` architecture-diagram label with `Health Connect`.
- Do not invent new features or change unrelated sections.
- If the README advertises stress as a feature, update that claim to match the
  app after task 001a (stress is no longer a BioTrace metric).

## 5. Final sweep

Search the whole app for leftover vendor/device names and fix every hit:

```bash
rg -n -i "honor|huawei|magicwatch|hebe|gadgetbridge|trurelax|trusleep|huawei_stress" app/src readme.md
```

Expected: no matches.

Note: the word "Stressed" in the mood enum and any generic use of the word
"stress" in unrelated copy may remain. Vendor names may not.

## Constraints

- Do not change Health Connect record ingestion, Room schema, or unrelated UI.
- Do not touch `plan/todo.md`; it is the user's own notes.
- Do not touch `build.gradle.kts`, dependency versions, `.env`, or `.git/`.
- Do not commit, push, checkout, or reset.
- Stay inside `app/src` and `readme.md`.

## Verify

1. `./gradlew :app:compileDebugKotlin` must pass.
2. `./gradlew :app:testDebugUnitTest` must pass.
3. The final-sweep command above must return no matches.
4. Report with the required JSON schema. Set `testsPassed` to true only if both
   Gradle commands actually passed.
