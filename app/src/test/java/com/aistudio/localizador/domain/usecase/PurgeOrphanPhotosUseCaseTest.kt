package com.aistudio.localizador.domain.usecase

import android.content.Context
import android.util.Log
import com.aistudio.localizador.data.repository.RouteRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

class PurgeOrphanPhotosUseCaseTest {

    private lateinit var tempDir: File
    private lateinit var imageDir: File
    private val context = mockk<Context>(relaxed = true)
    private val repository = mockk<RouteRepository>(relaxed = true)

    @Before
    fun setUp() {
        mockkStatic(Log::class)
        every { Log.d(any<String>(), any<String>()) } returns 0
        tempDir = createTempDir(prefix = "purge_test")
        imageDir = File(tempDir, "images")
        imageDir.mkdirs()
        every { context.filesDir } returns tempDir
    }

    @After
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    private fun createUseCase(): PurgeOrphanPhotosUseCase = PurgeOrphanPhotosUseCase(context, repository)

    @Test
    fun `purge_removes_orphan_photos_and_keeps_referenced`() = runTest {
        val referenced = File(imageDir, "IMG_1.jpg").apply { writeText("a") }
        File(imageDir, "IMG_2.png").apply { writeText("b") }
        File(imageDir, "IMG_3.jpg").apply { writeText("c") }
        coEvery { repository.getAllReferencedPhotoPaths() } returns listOf(
            "content://com.aistudio.localizador.fileprovider/files/images/IMG_1.jpg",
            "content://com.aistudio.localizador.fileprovider/files/images/IMG_4.jpg"
        )

        val result = createUseCase().execute()

        assertTrue(result.isSuccess)
        val purgeResult = result.getOrThrow()
        assertEquals(3, purgeResult.scanned)
        assertEquals(2, purgeResult.purged)
        assertTrue(referenced.exists())
        assertFalse(File(imageDir, "IMG_2.png").exists())
        assertFalse(File(imageDir, "IMG_3.jpg").exists())
    }

    @Test
    fun `purge_deletes_all_when_no_references`() = runTest {
        File(imageDir, "IMG_1.jpg").apply { writeText("a") }
        File(imageDir, "IMG_2.jpg").apply { writeText("b") }
        coEvery { repository.getAllReferencedPhotoPaths() } returns emptyList()

        val purgeResult = createUseCase().execute().getOrThrow()

        assertEquals(2, purgeResult.scanned)
        assertEquals(2, purgeResult.purged)
        assertTrue(tempDir.listFiles().orEmpty().none { it.name.startsWith("IMG_") })
    }

    @Test
    fun `purge_returns_zero_when_images_dir_missing`() = runTest {
        val isolatedDir = createTempDir(prefix = "purge_empty")
        every { context.filesDir } returns isolatedDir
        coEvery { repository.getAllReferencedPhotoPaths() } returns emptyList()

        val purgeResult = createUseCase().execute().getOrThrow()

        assertEquals(0, purgeResult.scanned)
        assertEquals(0, purgeResult.purged)
        isolatedDir.deleteRecursively()
    }

    @Test
    fun `purge_ignores_non_image_files`() = runTest {
        File(imageDir, "IMG_1.jpg").apply { writeText("a") }
        File(imageDir, "notas.txt").apply { writeText("hola") }
        coEvery { repository.getAllReferencedPhotoPaths() } returns emptyList()

        val purgeResult = createUseCase().execute().getOrThrow()

        assertEquals(1, purgeResult.scanned)
        assertEquals(1, purgeResult.purged)
        assertTrue(File(imageDir, "notas.txt").exists())
    }

    @Test
    fun `purge_matches_extension_case_insensitive`() = runTest {
        File(imageDir, "IMG_1.JPG").apply { writeText("a") }
        File(imageDir, "IMG_2.JPEG").apply { writeText("b") }
        File(imageDir, "IMG_3.PNG").apply { writeText("c") }
        coEvery { repository.getAllReferencedPhotoPaths() } returns listOf(
            "content://com.aistudio.localizador.fileprovider/files/images/IMG_1.JPG"
        )

        val purgeResult = createUseCase().execute().getOrThrow()

        assertEquals(3, purgeResult.scanned)
        assertEquals(2, purgeResult.purged)
        assertTrue(File(imageDir, "IMG_1.JPG").exists())
        assertFalse(File(imageDir, "IMG_2.JPEG").exists())
        assertFalse(File(imageDir, "IMG_3.PNG").exists())
    }

    @Test
    fun `purge_supports_absolute_path_references`() = runTest {
        File(imageDir, "IMG_1.jpg").apply { writeText("a") }
        File(imageDir, "IMG_2.jpg").apply { writeText("b") }
        coEvery { repository.getAllReferencedPhotoPaths() } returns listOf(
            "${imageDir.absolutePath}/IMG_1.jpg"
        )

        val purgeResult = createUseCase().execute().getOrThrow()

        assertEquals(1, purgeResult.purged)
        assertTrue(File(imageDir, "IMG_1.jpg").exists())
        assertFalse(File(imageDir, "IMG_2.jpg").exists())
    }

    @Test
    fun `purge_returns_failure_when_repository_throws`() = runTest {
        File(imageDir, "IMG_1.jpg").apply { writeText("a") }
        coEvery { repository.getAllReferencedPhotoPaths() } throws RuntimeException("DB error")

        val result = createUseCase().execute()

        assertTrue(result.isFailure)
        assertTrue(File(imageDir, "IMG_1.jpg").exists())
    }
}