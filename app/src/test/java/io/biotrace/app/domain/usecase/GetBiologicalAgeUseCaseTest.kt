package io.biotrace.app.domain.usecase

import io.biotrace.app.data.local.dao.LabBiomarkerDao
import io.biotrace.app.data.local.entity.LabBiomarkerEntity
import io.biotrace.app.data.local.store.UserProfileStore
import io.biotrace.app.domain.longevity.BiologicalAgeCalculator
import io.biotrace.app.domain.longevity.LongevityTier
import io.biotrace.app.domain.model.BentoCardTemplate
import io.biotrace.app.domain.model.ProfileTriptychSummary
import io.biotrace.app.domain.model.VitalMetricType
import io.biotrace.app.domain.model.VitalSummaryCardModel
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class GetBiologicalAgeUseCaseTest {

    private val vitalsRepository: VitalsRepository = mock()
    private val labBiomarkerDao: LabBiomarkerDao = mock()
    private val userProfileStore: UserProfileStore = mock()
    private val calculator = BiologicalAgeCalculator()

    private lateinit var useCase: GetBiologicalAgeUseCase

    @Before
    fun setUp() {
        useCase = GetBiologicalAgeUseCase(
            vitalsRepository = vitalsRepository,
            labBiomarkerDao = labBiomarkerDao,
            userProfileStore = userProfileStore,
            calculator = calculator
        )
    }

    private fun createLabBiomarker(
        name: String,
        value: Double,
        recordedAt: Instant = Instant.now()
    ) = LabBiomarkerEntity(
        id = "bio_${name}_${recordedAt.toEpochMilli()}",
        biomarkerName = name,
        rawName = name,
        value = value,
        unit = "mg/dL",
        referenceMin = null,
        referenceMax = null,
        flag = null,
        recordedAt = recordedAt,
        source = "MANUAL"
    )

    private fun createCard(
        metricType: VitalMetricType,
        primaryValue: String,
        primaryUnit: String = "",
        badgeText: String? = null,
        progress: Float? = null
    ) = VitalSummaryCardModel(
        metricType = metricType,
        primaryValue = primaryValue,
        primaryUnit = primaryUnit,
        secondaryText = "Detail",
        badgeText = badgeText,
        progress = progress,
        template = BentoCardTemplate.TEMPLATE_A
    )

    @Test
    fun `invoke gathers vitals from snapshot and labs from DAO to compute optimal biological age`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(40)

        val cards = listOf(
            createCard(
                metricType = VitalMetricType.RESTING_HEART_RATE,
                primaryValue = "56 bpm",
                primaryUnit = "bpm"
            ),
            createCard(
                metricType = VitalMetricType.SLEEP,
                primaryValue = "85",
                primaryUnit = "score",
                badgeText = "7h 48m"
            )
        )
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(cards)

        val labs = listOf(
            createLabBiomarker("FASTING_GLUCOSE", 85.0),
            createLabBiomarker("HS_CRP", 0.5)
        )
        whenever(labBiomarkerDao.getAll()).thenReturn(labs)

        val result = useCase()

        assertEquals(40, result.chronologicalAge)
        assertTrue("Biological age should be younger than chronological age", result.biologicalAge < 40.0)
        assertEquals(-4.2, result.ageDelta, 0.01)
        assertEquals(LongevityTier.OPTIMAL, result.longevityTier)
        assertEquals(70, result.confidencePercentage)
        assertEquals(4, result.drivers.size)
    }

    @Test
    fun `invoke falls back to ProfileTriptychSummary when dashboard snapshot vitals are missing`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(45)
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())

        val triptych = ProfileTriptychSummary(
            userName = "Test User",
            deviceLabel = "Health Connect",
            cardioStatus = "OPTIMAL",
            restingHr = "58 bpm",
            restingHrDelta = "-2 bpm",
            sleepScore = "90%",
            sleepStatus = "Deep / REM OK"
        )
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(triptych)
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())

        val result = useCase()

        assertEquals(45, result.chronologicalAge)
        // RHR 58 (<60 -> -1.4), SleepScore 90% -> sleepDuration 7.2 hrs (in 7.0..8.5 -> -0.8)
        // Total delta = -2.2
        assertEquals(-2.2, result.ageDelta, 0.01)
        assertEquals(50, result.confidencePercentage)
        assertEquals(LongevityTier.BALANCED, result.longevityTier)
    }

    @Test
    fun `invoke deduplicates lab biomarkers retaining latest recorded entry`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(40)
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(
            ProfileTriptychSummary(restingHr = "-- bpm", sleepScore = "--%")
        )

        val now = Instant.now()
        val latestOptimal = createLabBiomarker("FASTING_GLUCOSE", 85.0, recordedAt = now)
        val olderStrained = createLabBiomarker("FASTING_GLUCOSE", 130.0, recordedAt = now.minusSeconds(86400))

        // DAO returns ordered by recorded_at DESC, so latest comes first
        whenever(labBiomarkerDao.getAll()).thenReturn(listOf(latestOptimal, olderStrained))

        val result = useCase()

        // 85.0 should be used (delta = -0.8), not 130.0
        assertEquals(-0.8, result.ageDelta, 0.01)
        assertTrue(result.drivers.any { it.title == "Optimal Fasting Glucose" })
    }

    @Test
    fun `invoke canonicalizes biomarker names with different aliases and cases`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(40)
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(
            ProfileTriptychSummary(restingHr = "-- bpm", sleepScore = "--%")
        )

        // Raw alias names: "glucose" -> FASTING_GLUCOSE, "hs-crp" -> HS_CRP
        val labs = listOf(
            createLabBiomarker("glucose", 88.0),
            createLabBiomarker("hs-crp", 0.6)
        )
        whenever(labBiomarkerDao.getAll()).thenReturn(labs)

        val result = useCase()

        // Both should be recognized: glucose 88.0 (<=90 -> -0.8), hs-crp 0.6 (<0.8 -> -1.2) -> delta -2.0
        assertEquals(-2.0, result.ageDelta, 0.01)
        assertTrue(result.drivers.any { it.title == "Optimal Fasting Glucose" })
        assertTrue(result.drivers.any { it.title == "Low Systemic Inflammation" })
    }

    @Test
    fun `invoke parses sleep duration from card progress when badgeText and numeric primary are absent`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(35)

        // Progress 0.975 represents 7.8 hours (0.975 * 8.0 = 7.8)
        val cards = listOf(
            createCard(
                metricType = VitalMetricType.SLEEP,
                primaryValue = "--",
                progress = 0.975f
            )
        )
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(cards)
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())

        val result = useCase()

        // 7.8 hours sleep duration -> -0.8 delta
        assertEquals(-0.8, result.ageDelta, 0.01)
        assertTrue(result.drivers.any { it.title == "Optimal Sleep Duration" })
    }

    @Test
    fun `invoke handles complete absence of data gracefully`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(38)
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(emptyList())
        whenever(vitalsRepository.getProfileTriptychSummary()).thenReturn(
            ProfileTriptychSummary(restingHr = "-- bpm", sleepScore = "--%")
        )
        whenever(labBiomarkerDao.getAll()).thenReturn(emptyList())

        val result = useCase()

        assertEquals(38, result.chronologicalAge)
        assertEquals(38.0, result.biologicalAge, 0.01)
        assertEquals(0.0, result.ageDelta, 0.01)
        assertEquals(1.0, result.agingPace, 0.01)
        assertEquals(LongevityTier.BALANCED, result.longevityTier)
        assertEquals(30, result.confidencePercentage)
        assertTrue(result.drivers.isEmpty())
        assertNotNull(result.primaryRecommendation)
    }

    @Test
    fun `invoke handles elevated risk scenario returning elevated strain tier`() = runTest {
        whenever(userProfileStore.getChronologicalAge()).thenReturn(40)

        val cards = listOf(
            createCard(
                metricType = VitalMetricType.RESTING_HEART_RATE,
                primaryValue = "82 bpm",
                primaryUnit = "bpm"
            ),
            createCard(
                metricType = VitalMetricType.SLEEP,
                primaryValue = "5.2",
                primaryUnit = "hrs"
            )
        )
        whenever(vitalsRepository.getVitalsDashboardSnapshot()).thenReturn(cards)

        val labs = listOf(
            createLabBiomarker("FASTING_GLUCOSE", 120.0),
            createLabBiomarker("HS_CRP", 3.5)
        )
        whenever(labBiomarkerDao.getAll()).thenReturn(labs)

        val result = useCase()

        assertEquals(40, result.chronologicalAge)
        assertEquals(4.1, result.ageDelta, 0.01)
        assertEquals(LongevityTier.ELEVATED_STRAIN, result.longevityTier)
        assertTrue(result.agingPace > 1.0)
    }
}
