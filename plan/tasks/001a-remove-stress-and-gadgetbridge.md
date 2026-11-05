# Task 001a — Remove the stress metric and the Gadgetbridge pipeline

## Goal

BioTrace must consume health data only from Android Health Connect. The current
stress feature exists only because the app imports a Gadgetbridge SQLite file
and reads its `HUAWEI_STRESS_SAMPLE` table. Remove that whole path and the
stress metric that depends on it. After this task the project must compile with
no remaining references to Gadgetbridge, HUAWEI, stress samples, stress DAOs, or
the STRESS metric type.

Read `AGENTS.md` at the repository root first. Obey it.

## Files to delete

- `app/src/main/java/io/biotrace/app/data/gadgetbridge/GadgetbridgeStressDataSource.kt`
- `app/src/main/java/io/biotrace/app/data/gadgetbridge/GadgetbridgeStressParser.kt`
- `app/src/main/java/io/biotrace/app/data/local/entity/StressSampleEntity.kt`
- `app/src/main/java/io/biotrace/app/data/local/dao/StressDao.kt`
- `app/src/main/java/io/biotrace/app/domain/model/StressLevel.kt`
- `app/src/test/java/io/biotrace/app/data/gadgetbridge/GadgetbridgeStressDataSourceTest.kt`
- `app/src/test/java/io/biotrace/app/domain/model/StressLevelTest.kt`
- If present, also delete the ignored fixtures
  `app/src/test/java/io/biotrace/app/data/local/Gadgetbridge` and
  `app/src/test/java/io/biotrace/app/data/local/nodomain.freeyourgadget.gadgetbridge_255.apk`.

## Required changes

### 1. Database

`app/src/main/java/io/biotrace/app/data/local/database/BioTraceDatabase.kt`
- Remove `StressSampleEntity` from `entities` and `stressDao()`.
- Bump `version` from 2 to 3. `fallbackToDestructiveMigration()` is already
  configured in `DatabaseModule`, so no migration class is needed.

`app/src/main/java/io/biotrace/app/di/DatabaseModule.kt`
- Remove the `StressDao` import and `provideStressDao` provider.

### 2. Repository contract and implementation

`app/src/main/java/io/biotrace/app/domain/repository/VitalsRepository.kt`
- Remove `importGadgetbridgeUri`, `refreshGadgetbridgeStress`,
  `hasSavedGadgetbridgeUri`, and the `android.net.Uri` import.

`app/src/main/java/io/biotrace/app/data/repository/VitalsRepositoryImpl.kt`
- Remove the `GadgetbridgeStressDataSource` and `StressDao` constructor
  parameters and fields.
- Remove `stressDao.deleteAll()` from `clearAll()`.
- Remove the three Gadgetbridge repository overrides.
- Remove all stress card and stress detail code, including `fetchStressCard`,
  `getStressDetailData`, the `STRESS` branch in `getMetricDetailData`, and the
  stress entry in `getVitalsDashboardSnapshot` (7 cards remain).
- Remove all now-unused imports (`GadgetbridgeStressDataSource`, `StressDao`,
  `StressSampleEntity`, `StressLevel`, `StressZoneDistribution`, `Uri`).

Add a neutral profile source:
- Create `app/src/main/java/io/biotrace/app/domain/model/UserProfile.kt`:

```kotlin
package io.biotrace.app.domain.model

data class UserProfile(
    val userName: String = "BioTrace User",
    val stepsGoal: Int = 8_000,
    val sleepGoalMinutes: Int = 420
)
```

- In `VitalsRepositoryImpl`, add `private val userProfile = UserProfile()` and
  replace every `meta.stepsGoal` with `userProfile.stepsGoal`,
  `meta.sleepGoalMinutes` with `userProfile.sleepGoalMinutes`, and
  `meta.userName` with `userProfile.userName`.
- In `fetchBodyMetricsCard()` remove the `meta.weightKg` and `meta.heightCm`
  fallbacks; use only Health Connect `WeightRecord` / `HeightRecord`.
- In `getProfileTriptychSummary()` set `deviceLabel = "Health Connect"`.

### 3. Domain models

`app/src/main/java/io/biotrace/app/domain/model/VitalModels.kt`
- Remove the `STRESS` enum constant from `VitalMetricType` and its branch in
  the `icon` property.
- Remove `StressZoneDistribution`.
- Remove the `stressLevel` field from `VitalSummaryCardModel`.
- Change `VitalHistoryItem.deviceSource` default to `"Health Connect"`.
- Change `ProfileTriptychSummary` defaults: `userName = "BioTrace User"`,
  `deviceLabel = "Health Connect"`.
- Change `VitalDetailUiState.dataSourceLabel` default to `"Health Connect"`.

### 4. UI

`app/src/main/java/io/biotrace/app/ui/vitals/VitalsScreen.kt`
- Remove the SAF file-picker launcher and the `importGadgetbridgeDatabase` calls.
- Remove the import notice banner, the stress card branch, the stress accent
  mapping in the `when` (line ~853), and the "Link Gadgetbridge" action (line
  ~924).
- Remove the SQLite import button from the bottom action row and its
  `onPickGadgetbridge` parameter.
- Remove the hardcoded subtitle `"HONOR MagicWatch 2-38D • Hebe-B19S"`; pass a
  neutral subtitle such as `"Health Connect"`.

`app/src/main/java/io/biotrace/app/ui/vitals/VitalsViewModel.kt`
- Remove all Gadgetbridge import/refresh state and functions
  (`hasSavedGadgetbridgeDb`, `importMessage`, `importGadgetbridgeDatabase`,
  `refreshGadgetbridgeStress` calls).
- Keep `lastSyncTime` as-is.

`app/src/main/java/io/biotrace/app/ui/vitals/VitalDetailViewModel.kt`
- Remove `importGadgetbridgeDatabase` and the Gadgetbridge URI state.
- Replace `getOrDefault(VitalMetricType.STRESS)` with
  `getOrDefault(VitalMetricType.HEART_RATE)`.

`app/src/main/java/io/biotrace/app/ui/vitals/VitalDetailScreen.kt`
- Remove the stress description branch, the import/update-file action, and all
  stress-specific branches.
- Replace the hardcoded sample `deviceSource` values
  `"HONOR MagicWatch 2-38D • Gadgetbridge"` with `"Health Connect"`.

`app/src/main/java/io/biotrace/app/ui/insights/BiometricInsightsViewModel.kt`
- Remove the `StressDao` dependency and all stress sample loading.
- Remove `stressDistribution`, `totalStressSamples`, and `averageStress` from
  the UI state.
- Remove the "Stress vs Resting Heart Rate" coupling insight that depends on
  stress samples. Keep the other insights.
- Keep the remaining insights compiling with unchanged behaviour.

`app/src/main/java/io/biotrace/app/ui/insights/BiometricInsightsScreen.kt`
- Remove the stress zone distribution card, its parameters, and the
  `"$totalSamples Huawei samples from Gadgetbridge"` text.

`app/src/main/java/io/biotrace/app/ui/common/BioTraceTopBar.kt`
- Remove the `onImportGadgetbridgeUri` parameter, the `OpenDocument` launcher,
  and `WearableConnectionDialog`. For now, leave the avatar click invoking
  `onAvatarClick` only (the replacement dialog is task 001b).
- Remove now-unused imports.

`app/src/main/java/io/biotrace/app/testing/LabHubTestReceiver.kt`
- Remove the `GadgetbridgeStressDataSource` injection, the
  `ACTION_IMPORT_GADGETBRIDGE` constant, and the branch that imports the
  Gadgetbridge DB.

`app/src/main/AndroidManifest.xml`
- Remove the `io.biotrace.app.TEST_IMPORT_GADGETBRIDGE` intent-filter action.

### 5. Tests

- Delete the two test files listed above.
- Repair any remaining test that references removed types or stress state.
- Do not weaken or delete unrelated tests.

## Constraints

- Do not touch `build.gradle.kts`, version catalogs, or dependency versions.
- Do not change Health Connect ingestion logic or unrelated Room entities.
- Do not redesign the app or change the visual language.
- Do not read or modify `.env`, keystores, or `.git/`.
- Do not commit, push, checkout, or reset.
- Stay inside the files listed here unless a compile error forces a mechanical
  follow-up; report any extra file in `filesChanged`.

## Verify

1. `./gradlew :app:compileDebugKotlin` must pass. Fix compile errors caused by
   this task and re-run, up to 3 attempts.
2. Then run:

```bash
rg -n -i "gadgetbridge|huawei|stresssample|stressdao|stresslevel|vitalmetrictype\.stress" app/src/main app/src/test
```

Expected: no matches. (Comments and unrelated words such as the "Stressed" mood
enum are fine, but brand names are not.)

3. Report with the required JSON schema. Set `testsPassed` to true only if the
   compile command actually passed. Put anything you could not complete in
   `blockers`.
