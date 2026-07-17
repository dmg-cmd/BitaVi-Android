package com.aistudio.localizador.domain.usecase

import android.net.Uri
import com.aistudio.localizador.ui.PhotoManager
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class ExtractPhotoLocationUseCaseTest {

    private val photoManager: PhotoManager = mockk()
    private lateinit var useCase: ExtractPhotoLocationUseCase
    private val testUri: Uri = mockk()

    @Before
    fun setUp() {
        mockkStatic(Uri::class)
        every { Uri.parse(any()) } returns testUri
        useCase = ExtractPhotoLocationUseCase(photoManager)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun testExecuteWithCoordinates() = runTest {
        every { photoManager.getPhotoLocation(testUri) } returns Pair(19.4326, -99.1332)

        val result = useCase.execute(testUri)

        assertNotNull("Debe retornar las coordenadas", result)
        assertEquals(19.4326, result!!.first, 0.0001)
        assertEquals(-99.1332, result.second, 0.0001)
    }

    @Test
    fun testExecuteWithNullResult() = runTest {
        every { photoManager.getPhotoLocation(testUri) } returns null

        val result = useCase.execute(testUri)

        assertNull("Debe retornar null si no hay metadatos GPS", result)
    }
}
