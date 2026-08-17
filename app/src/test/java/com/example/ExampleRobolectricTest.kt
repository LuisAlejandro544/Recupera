package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.CategoryFilter
import com.example.model.FileHealth
import com.example.model.HealthLevel
import com.example.model.MediaType
import com.example.model.RecoverablePhoto
import com.example.model.RecoverySource
import com.example.model.SortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Recuperar Fotos", appName)
    }

    @Test
    fun `test photo model size formatting`() {
        val photoKb = RecoverablePhoto(
            id = "1",
            name = "test.jpg",
            filePath = "/sdcard/test.jpg",
            fileSizeBytes = 500 * 1024L,
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.TRASH_MEDIASTORE,
            fileExtension = "jpg"
        )
        assertEquals("500 KB", photoKb.formattedSize)

        val photoMb = RecoverablePhoto(
            id = "2",
            name = "test_large.jpg",
            filePath = "/sdcard/test_large.jpg",
            fileSizeBytes = 3_500_000L,
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.THUMBNAILS_CACHE,
            fileExtension = "jpg"
        )
        assertTrue(photoMb.formattedSize.contains("MB"))
    }

    @Test
    fun `test video model and duration formatting`() {
        val video = RecoverablePhoto(
            id = "vid_1",
            name = "vacation_video.mp4",
            filePath = "/sdcard/vacation_video.mp4",
            fileSizeBytes = 15_000_000L,
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.DEEP_STORAGE,
            fileExtension = "mp4",
            mediaType = MediaType.VIDEO,
            durationMs = 65_000L
        )
        assertTrue(video.isVideo)
        assertFalse(video.isAudio)
        assertEquals("01:05", video.durationFormatted)
    }

    @Test
    fun `test audio model and health level calculation`() {
        val audio = RecoverablePhoto(
            id = "aud_1",
            name = "voice_note.opus",
            filePath = "/sdcard/WhatsApp/Media/WhatsApp Voice Notes/voice_note.opus",
            fileSizeBytes = 120_000L,
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.APP_TEMP_CACHE,
            fileExtension = "opus",
            mediaType = MediaType.AUDIO,
            durationMs = 12_000L,
            health = FileHealth(
                percentage = 85,
                level = HealthLevel.GOOD,
                description = "Audio de mensajería con metadatos y duración legible"
            )
        )
        assertTrue(audio.isAudio)
        assertFalse(audio.isVideo)
        assertEquals("00:12", audio.durationFormatted)
        assertEquals(85, audio.health.percentage)
        assertEquals(HealthLevel.GOOD, audio.health.level)
    }

    @Test
    fun `test document model and category validation`() {
        val doc = RecoverablePhoto(
            id = "doc_1",
            name = "Project_Report.pdf",
            filePath = "/sdcard/Documents/Project_Report.pdf",
            fileSizeBytes = 2_400_000L,
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.DEEP_STORAGE,
            fileExtension = "pdf",
            mediaType = MediaType.DOCUMENT,
            dimensions = "Documento PDF (PDF)"
        )
        assertTrue(doc.isDocument)
        assertFalse(doc.isImage)
        assertFalse(doc.isVideo)
        assertFalse(doc.isAudio)
        assertEquals("Project_Report.pdf", doc.name)
        assertEquals("Documento PDF (PDF)", doc.dimensions)
    }

    @Test
    fun `test orphan clean result calculation`() {
        val result = com.example.model.OrphanCleanResult(
            scannedCount = 15,
            deletedCount = 15,
            freedBytes = 10_485_760L,
            isDryRun = false
        )
        assertEquals(15, result.deletedCount)
        assertEquals(10_485_760L, result.freedBytes)
        assertEquals("10.0 MB", result.formattedFreedSize)
        assertFalse(result.isDryRun)
    }

    @Test
    fun `test shizuku state and privilege calculation`() {
        val stateAdb = com.example.shizuku.ShizukuState(
            status = com.example.shizuku.ShizukuStatus.AUTHORIZED_ACTIVE,
            isBinderAlive = true,
            isPermissionGranted = true,
            version = 13,
            uid = 2000,
            isEnhancedScanEnabled = true
        )
        assertTrue(stateAdb.isReadyForEnhancedScan)
        assertEquals("ADB (Shell de Sistema)", stateAdb.uidLabel)

        val stateRoot = com.example.shizuku.ShizukuState(
            status = com.example.shizuku.ShizukuStatus.AUTHORIZED_ACTIVE,
            isBinderAlive = true,
            isPermissionGranted = true,
            version = 13,
            uid = 0,
            isEnhancedScanEnabled = false
        )
        assertFalse(stateRoot.isReadyForEnhancedScan)
        assertEquals("Root (Superusuario)", stateRoot.uidLabel)
    }

    @Test
    fun `test header repair on damaged jpeg file`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempFile = java.io.File(context.cacheDir, "damaged_photo.jpg")
        // Write damaged payload without JPEG SOI (FF D8)
        tempFile.writeBytes(byteArrayOf(0x00, 0x00, 0xFF.toByte(), 0xDB.toByte(), 0x00, 0x43, 0x00, 0x01))

        val damagedPhoto = RecoverablePhoto(
            id = "repair_1",
            name = "damaged_photo.jpg",
            filePath = tempFile.absolutePath,
            fileSizeBytes = tempFile.length(),
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.THUMBNAILS_CACHE,
            fileExtension = "jpg",
            health = FileHealth(percentage = 20, level = HealthLevel.DAMAGED, description = "Cabecera corrupta")
        )

        assertTrue(com.example.engine.repair.HeaderRepairEngine.isRepairRecommended(damagedPhoto))

        val result = com.example.engine.repair.HeaderRepairEngine.repairMediaHeader(context, damagedPhoto)
        assertTrue(result.isSuccess)
        assertTrue(result.repairedFilePath != null)
        val repairedFile = java.io.File(result.repairedFilePath!!)
        assertTrue(repairedFile.exists())
        val headerBytes = repairedFile.readBytes().take(2).toByteArray()
        assertEquals(0xFF.toByte(), headerBytes[0])
        assertEquals(0xD8.toByte(), headerBytes[1])
    }

    @Test
    fun `test duplicate detector groups identical files`() = kotlinx.coroutines.test.runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file1 = java.io.File(context.cacheDir, "dup1.jpg")
        val file2 = java.io.File(context.cacheDir, "dup2.jpg")
        val content = "IDENTICAL_IMAGE_CONTENT_SAMPLE_FOR_TESTING_123456789".toByteArray()
        file1.writeBytes(content)
        file2.writeBytes(content)

        val photo1 = RecoverablePhoto(
            id = "dup_1",
            name = "IMG_001.jpg",
            filePath = file1.absolutePath,
            fileSizeBytes = file1.length(),
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.TRASH_MEDIASTORE,
            fileExtension = "jpg",
            health = FileHealth(percentage = 100, level = HealthLevel.EXCELLENT, description = "Intacto")
        )

        val photo2 = RecoverablePhoto(
            id = "dup_2",
            name = "thumb_001.jpg",
            filePath = file2.absolutePath,
            fileSizeBytes = file2.length(),
            lastModifiedTimestamp = System.currentTimeMillis(),
            sourceCategory = RecoverySource.THUMBNAILS_CACHE,
            fileExtension = "jpg",
            health = FileHealth(percentage = 70, level = HealthLevel.GOOD, description = "Miniatura")
        )

        val scanResult = com.example.engine.duplicate.DuplicateMediaDetector.findDuplicates(listOf(photo1, photo2))
        assertEquals(1, scanResult.groups.size)
        assertEquals(1, scanResult.totalDuplicatesCount)
        assertEquals(photo1.id, scanResult.groups[0].primaryPhoto.id)
        assertEquals(photo2.id, scanResult.groups[0].duplicates[0].id)
    }
}
