package io.biotrace.app.ui.nutrition

import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.data.local.entity.MealEntity
import io.biotrace.app.domain.llm.LlmClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class FoodScanViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val mealDao: MealDao = mock()
    private val llmClient: LlmClient = mock()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadTodayMeals accumulates calories and macronutrients accurately`() = runTest {
        val meal1 = MealEntity(
            id = "m1",
            name = "Avocado Toast with Poached Eggs",
            calories = 420,
            proteinGrams = 18.0,
            carbsGrams = 32.0,
            fatsGrams = 24.0,
            fiberGrams = 7.0,
            consumedAt = Instant.now(),
            photoPath = null
        )
        val meal2 = MealEntity(
            id = "m2",
            name = "Grilled Salmon & Quinoa Bowl",
            calories = 560,
            proteinGrams = 44.0,
            carbsGrams = 40.0,
            fatsGrams = 22.0,
            fiberGrams = 5.0,
            consumedAt = Instant.now(),
            photoPath = null
        )

        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(listOf(meal1, meal2))
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(listOf(meal1, meal2)))

        val viewModel = FoodScanViewModel(mealDao, llmClient)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(2, state.todayMeals.size)
        assertEquals(980, state.totalCalories)
        assertEquals(62.0, state.totalProtein, 0.01)
        assertEquals(72.0, state.totalCarbs, 0.01)
        assertEquals(46.0, state.totalFats, 0.01)
    }

    @Test
    fun `addManualMeal saves entity into mealDao`() = runTest {
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(listOf())
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val viewModel = FoodScanViewModel(mealDao, llmClient)
        advanceUntilIdle()

        viewModel.addManualMeal(
            name = "Greek Yogurt with Berries",
            calories = 210,
            protein = 20.0,
            carbs = 18.0,
            fats = 4.0,
            fiber = 3.0
        )
        advanceUntilIdle()

        verify(mealDao).insert(any())
        assertFalse(viewModel.uiState.value.isManualEntryOpen)
    }

    @Test
    fun `scanFoodPhoto calls LLM with multimodal image and parses response`() = runTest {
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(listOf())
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            {
              "name": "Chicken Tikka with Brown Rice",
              "calories": 520,
              "protein": 45.0,
              "carbs": 50.0,
              "fats": 14.0,
              "fiber": 4.0,
              "glycemic": "LOW",
              "confidence": 98
            }
        """.trimIndent()

        whenever(llmClient.generateContentWithImage(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = FoodScanViewModel(mealDao, llmClient)
        advanceUntilIdle()

        viewModel.scanFoodPhoto("dummy_base64_data", "image/jpeg")
        advanceUntilIdle()

        verify(llmClient).generateContentWithImage(any(), any(), any())
        verify(mealDao).insert(any())
        assertFalse(viewModel.uiState.value.isScanning)
        assertTrue(viewModel.uiState.value.scanMessage?.contains("Chicken Tikka with Brown Rice") == true)
    }

    @Test
    fun `scanCapturedBitmap calls LLM and populates pendingScanResult without auto-inserting`() = runTest {
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(listOf())
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            {
              "name": "Grilled Salmon Harvest Bowl",
              "calories": 640,
              "protein": 42.0,
              "carbs": 58.0,
              "fats": 22.0,
              "fiber": 6.0,
              "glycemic": "LOW",
              "confidence": 98
            }
        """.trimIndent()

        whenever(llmClient.generateContentWithImage(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = FoodScanViewModel(mealDao, llmClient)
        advanceUntilIdle()

        val mockBitmap: android.graphics.Bitmap = mock()
        whenever(mockBitmap.compress(any(), any(), any())).thenAnswer { invocation ->
            val stream = invocation.arguments[2] as java.io.OutputStream
            stream.write(byteArrayOf(1, 2, 3))
            true
        }

        viewModel.scanCapturedBitmap(mockBitmap)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isScanning)
        assertEquals(mockBitmap, state.capturedPhotoBitmap)
        val pending = state.pendingScanResult
        assertTrue(pending != null)
        assertEquals("Grilled Salmon Harvest Bowl", pending?.name)
        assertEquals(640, pending?.calories)
        assertEquals(42.0, pending?.proteinGrams ?: 0.0, 0.01)
        assertEquals(58.0, pending?.carbsGrams ?: 0.0, 0.01)
        assertEquals(22.0, pending?.fatsGrams ?: 0.0, 0.01)
        assertEquals(6.0, pending?.fiberGrams ?: 0.0, 0.01)
        assertEquals("LOW", pending?.glycemic)
        assertEquals(98, pending?.confidence)
        verify(mealDao, org.mockito.kotlin.never()).insert(any())
    }

    @Test
    fun `confirmAndLogScan inserts pending result into mealDao and clears pending state`() = runTest {
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(listOf())
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            {
              "name": "Steak and Sweet Potato",
              "calories": 720,
              "protein": 55.0,
              "carbs": 45.0,
              "fats": 28.0,
              "fiber": 5.0,
              "glycemic": "MEDIUM",
              "confidence": 94
            }
        """.trimIndent()

        whenever(llmClient.generateContentWithImage(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = FoodScanViewModel(mealDao, llmClient)
        advanceUntilIdle()

        val mockBitmap: android.graphics.Bitmap = mock()
        whenever(mockBitmap.compress(any(), any(), any())).thenAnswer { invocation ->
            val stream = invocation.arguments[2] as java.io.OutputStream
            stream.write(byteArrayOf(1, 2, 3))
            true
        }

        viewModel.scanCapturedBitmap(mockBitmap)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pendingScanResult != null)

        viewModel.confirmAndLogScan()
        advanceUntilIdle()

        verify(mealDao).insert(any())
        assertEquals(null, viewModel.uiState.value.pendingScanResult)
        assertTrue(viewModel.uiState.value.scanMessage?.contains("Steak and Sweet Potato") == true)
        assertTrue(viewModel.uiState.value.latestScannedTags.any { it.value == "720 kcal" })
    }

    @Test
    fun `dismissScanResult clears pendingScanResult and capturedPhotoBitmap`() = runTest {
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(listOf())
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            {
              "name": "Avocado Salad",
              "calories": 350,
              "protein": 8.0,
              "carbs": 20.0,
              "fats": 26.0,
              "fiber": 10.0,
              "glycemic": "LOW",
              "confidence": 95
            }
        """.trimIndent()

        whenever(llmClient.generateContentWithImage(any(), any(), any())).thenReturn(jsonResponse)

        val viewModel = FoodScanViewModel(mealDao, llmClient)
        advanceUntilIdle()

        val mockBitmap: android.graphics.Bitmap = mock()
        whenever(mockBitmap.compress(any(), any(), any())).thenAnswer { invocation ->
            val stream = invocation.arguments[2] as java.io.OutputStream
            stream.write(byteArrayOf(1, 2, 3))
            true
        }

        viewModel.scanCapturedBitmap(mockBitmap)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.pendingScanResult != null)
        assertEquals(mockBitmap, viewModel.uiState.value.capturedPhotoBitmap)

        viewModel.dismissScanResult()

        assertEquals(null, viewModel.uiState.value.pendingScanResult)
        assertEquals(null, viewModel.uiState.value.capturedPhotoBitmap)
    }
}
