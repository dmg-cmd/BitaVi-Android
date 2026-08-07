package com.aistudio.localizador.ui

import android.content.Context
import android.database.Cursor
import android.net.Uri
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val cursor: Cursor = mockk(relaxed = true)
    private val mockUri: Uri = mockk()
    private lateinit var useCase: ContactUseCase

    @Before
    fun setUp() {
        useCase = ContactUseCase(context)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `cuando el cursor devuelve nulo debe retornar Result failure`() = runTest {
        every { context.contentResolver.query(any(), null, null, null, null) } returns null
        val result = useCase.getContactDetails(mockUri)
        assertTrue(result.isFailure)
    }

    @Test
    fun `cuando el cursor es valido debe mapear el contacto correctamente`() = runTest {
        every { context.contentResolver.query(any(), null, null, null, null) } returns cursor
        every { cursor.moveToFirst() } returns true
        every { cursor.getColumnIndex("display_name") } returns 0
        every { cursor.getString(0) } returns "Delm Bitaru"
        val result = useCase.getContactDetails(mockUri)
        assertTrue(result.isSuccess)
        assertEquals("Delm Bitaru", result.getOrNull()?.name)
    }
}
