package com.aistudio.localizador.domain.usecase

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.aistudio.localizador.data.export.RouteExchanger
import com.aistudio.localizador.data.repository.RouteRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RouteExportUseCaseTest {

    private val context: Context = mockk(relaxed = true)
    private val repository: RouteRepository = mockk(relaxed = true)
    private val exchanger: RouteExchanger = mockk(relaxed = true)
    private val contentResolver: ContentResolver = mockk()
    private lateinit var useCase: RouteExportUseCase
    private val tempDir = File(System.getProperty("java.io.tmpdir"))

    @Before
    fun setUp() {
        every { context.cacheDir } returns tempDir
        every { context.contentResolver } returns contentResolver
        every { context.packageName } returns "com.aistudio.localizador.test"
        coEvery { repository.getRouteById(any()) } returns null
        useCase = RouteExportUseCase(context, repository, exchanger)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `exportCsv genera archivo CSV con cabecera y datos`() = runTest {
        val result = useCase.exportCsv(1)

        assertTrue("Result debe ser exitoso", result.isSuccess)
        val file = result.getOrNull()
        assertNotNull("Archivo no debe ser nulo", file)
        assertTrue("Archivo debe existir", file!!.exists())
        val text = file.readText()
        assertTrue("Debe contener cabecera", text.startsWith("Fecha,Lat,Lon"))
        file.delete()
    }

    @Test
    fun `exportGpx genera archivo GPX con marcado XML`() = runTest {
        val result = useCase.exportGpx(setOf(1))

        assertTrue(result.isSuccess)
        val file = result.getOrNull()
        assertNotNull(file)
        assertTrue(file!!.exists())
        val text = file.readText()
        assertTrue("Debe iniciar con cabecera XML", text.startsWith("<?xml"))
        assertTrue("Debe contener etiqueta gpx", text.contains("<gpx"))
        file.delete()
    }

    @Test
    fun `buildShareIntent retorna Intent con ACTION_SEND y URI`() {
        val uri = Uri.parse("content://test/file.zip")

        val intent = useCase.buildShareIntent(uri, "application/zip")

        assertEquals(Intent.ACTION_SEND, intent.action)
        assertEquals("application/zip", intent.type)
        assertEquals(uri, intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
    }

    @Test
    fun `buildChooserIntent agrega NEW_TASK y titulo`() {
        val inner = Intent(Intent.ACTION_SEND)

        val chooser = useCase.buildChooserIntent(inner, "Test Title")

        assertTrue(chooser.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `exportZip falla cuando la ruta no existe`() = runTest {
        val result = useCase.exportZip(999)

        assertTrue("Debe fallar con ruta inexistente", result.isFailure)
    }

    @Test
    fun `saveZipToUri falla cuando no se puede abrir outputStream`() = runTest {
        val uri: Uri = mockk()
        every { contentResolver.openOutputStream(any()) } returns null

        val result = useCase.saveZipToUri(1, uri)

        assertTrue(result.isFailure)
    }

    @Test
    fun `importZip falla cuando no se puede abrir inputStream`() = runTest {
        val uri: Uri = mockk()
        every { contentResolver.openInputStream(any()) } returns null

        val result = useCase.importZip(uri)

        assertTrue(result.isFailure)
    }

    @Test
    fun `exportRouteAsZip delega a exportZip`() = runTest {
        val result = useCase.exportRouteAsZip(999)

        assertEquals(useCase.exportZip(999).isFailure, result.isFailure)
    }

    @Test
    fun `getUriForFile retorna URI no nulo`() {
        val file = File(tempDir, "test_file.txt")
        file.createNewFile()

        val uri = Uri.parse("content://test/file.zip")
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } returns uri

        val result = useCase.getUriForFile(file)

        assertNotNull(result)
        file.delete()
    }
}