package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.aistudio.localizador.data.model.Route
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ShareExperienceUseCaseTest {

    private val useCase = ShareExperienceUseCase()
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `invoke con fotos vacias debe retornar chooser intent valido`() {
        val route = Route(id = 1, name = "Ruta Perimetral", distanceMeters = 3200.0, startTime = 0L, endTime = 1000L)
        val resultIntent = useCase(context, route, emptyList())

        assertNotNull(resultIntent)
        assertEquals(Intent.ACTION_CHOOSER, resultIntent.action)
    }
}
