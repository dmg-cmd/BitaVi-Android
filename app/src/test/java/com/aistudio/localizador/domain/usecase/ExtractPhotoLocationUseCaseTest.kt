package com.aistudio.localizador.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ExtractPhotoLocationUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk()
    private val testUri: Uri = mockk()
    private lateinit var useCase: ExtractPhotoLocationUseCase

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        mockkStatic(MediaStore::class)
        every { Log.w(any<String>(), any<String>()) } returns 0
        every { Log.e(any<String>(), any<String>(), any<Throwable>()) } returns 0
        every { Log.e(any<String>(), any<String>()) } returns 0
        every { Log.d(any<String>(), any<String>()) } returns 0
        every { MediaStore.setRequireOriginal(any()) } returns testUri
        every { context.contentResolver } returns contentResolver
        useCase = ExtractPhotoLocationUseCase(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cuando el stream arroja IOException retorna null`() = runTest {
        every { contentResolver.openInputStream(any()) } throws IOException("Stream cerrado")

        val result = useCase.execute(testUri)

        assertNull("Debe capturar IOException y retornar null", result)
    }

    @Test
    fun `cuando el stream es nulo retorna null`() = runTest {
        every { contentResolver.openInputStream(any()) } returns null

        val result = useCase.execute(testUri)

        assertNull("Debe retornar null cuando openInputStream es null", result)
    }
}
