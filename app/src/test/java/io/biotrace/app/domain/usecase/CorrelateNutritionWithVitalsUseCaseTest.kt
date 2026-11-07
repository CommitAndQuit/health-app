package io.biotrace.app.domain.usecase

import io.biotrace.app.data.local.dao.MealDao
import io.biotrace.app.domain.repository.VitalsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CorrelateNutritionWithVitalsUseCaseTest {

    private val mealDao: MealDao = mock()
    private val vitalsRepository: VitalsRepository = mock()
    private val useCase = CorrelateNutritionWithVitalsUseCase(mealDao, vitalsRepository)

    @Test
    fun `returns empty correlation when no meals or vitals exist`() = runTest {
        whenever(vitalsRepository.getVitalsBetween(any(), any(), any())).thenReturn(emptyList())
        whenever(mealDao.getMealsBetween(any(), any())).thenReturn(emptyList())

        val result = useCase.execute()

        assertEquals(NutritionWearableCorrelationResult.EMPTY, result)
    }
}
