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
        whenever(mealDao.getMealsBetweenFlow(any(), any())).thenReturn(flowOf(emptyList()))

        val jsonResponse = """
            {
              "name": "Chicken Tikka with Brown Rice",
              "calories": 520,
              "protein": 45.0,
              "carbs": 50.0,
              "fats": 14.0,
              "fiber": 4.0
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
}
