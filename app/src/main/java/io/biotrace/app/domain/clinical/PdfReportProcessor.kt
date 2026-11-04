package io.biotrace.app.domain.clinical

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

data class ProcessedDocument(
    val mimeType: String,
    val base64Data: String,
    val fileName: String,
    val pageCount: Int,
    val previewBitmap: Bitmap?,
    val byteSize: Long
)

@Singleton
class PdfReportProcessor @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        const val MAX_PDF_BYTES = 20 * 1024 * 1024 // 20 MB inline limit for Gemini API
    }

    suspend fun processUri(uri: Uri): ProcessedDocument = withContext(Dispatchers.IO) {
        val fileName = queryFileName(uri) ?: "Lab_Report_${System.currentTimeMillis()}"
        val detectedMime = context.contentResolver.getType(uri) ?: when {
            fileName.endsWith(".pdf", ignoreCase = true) -> "application/pdf"
            fileName.endsWith(".png", ignoreCase = true) -> "image/png"
            fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
            else -> "image/jpeg"
        }

        if (detectedMime == "application/pdf") {
            processPdf(uri, fileName)
        } else {
            processImage(uri, fileName, detectedMime)
        }
    }

    suspend fun processFile(file: File): ProcessedDocument = withContext(Dispatchers.IO) {
        if (!file.exists()) {
            throw IllegalArgumentException("File does not exist: ${file.absolutePath}")
        }
        val bytes = file.readBytes()
        if (bytes.size > MAX_PDF_BYTES) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", bytes.size.toDouble() / (1024 * 1024))
            throw IllegalArgumentException("PDF report is $sizeMb MB, exceeding the 20MB limit. Please upload a smaller document.")
        }

        var pageCount = 1
        var previewBitmap: Bitmap? = null

        try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    val scale = (1080f / page.width.toFloat()).coerceAtMost(2.0f)
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    previewBitmap = bitmap
                }
                renderer.close()
            }
        } catch (_: Exception) {
            // Preview rendering fallback
        }

        val payloadBytes = if (pageCount > 10) {
            try {
                extractClinicalPages(file, maxPages = 10)
            } catch (_: Exception) {
                bytes
            }
        } else {
            bytes
        }

        val base64 = Base64.encodeToString(payloadBytes, Base64.NO_WRAP)
        ProcessedDocument(
            mimeType = "application/pdf",
            base64Data = base64,
            fileName = file.name,
            pageCount = pageCount,
            previewBitmap = previewBitmap,
            byteSize = file.length()
        )
    }

    private fun extractClinicalPages(sourceFile: File, maxPages: Int = 10): ByteArray {
        val tempPdf = android.graphics.pdf.PdfDocument()
        ParcelFileDescriptor.open(sourceFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
            val renderer = PdfRenderer(pfd)
            val pagesToProcess = renderer.pageCount.coerceAtMost(maxPages)
            for (i in 0 until pagesToProcess) {
                val page = renderer.openPage(i)
                val scale = (1200f / page.width.toFloat()).coerceAtMost(1.5f)
                val w = (page.width * scale).toInt()
                val h = (page.height * scale).toInt()
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bmp)
                canvas.drawColor(Color.WHITE)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                val docPage = tempPdf.startPage(pageInfo)
                docPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                tempPdf.finishPage(docPage)
                bmp.recycle()
            }
            renderer.close()
        }
        val baos = ByteArrayOutputStream()
        tempPdf.writeTo(baos)
        tempPdf.close()
        return baos.toByteArray()
    }

    private fun processPdf(uri: Uri, fileName: String): ProcessedDocument {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open stream for selected PDF")

        val bytes = inputStream.use { it.readBytes() }
        if (bytes.size > MAX_PDF_BYTES) {
            val sizeMb = String.format(java.util.Locale.US, "%.1f", bytes.size.toDouble() / (1024 * 1024))
            throw IllegalArgumentException("PDF report is $sizeMb MB, exceeding the 20MB limit. Please upload a smaller document.")
        }

        var pageCount = 1
        var previewBitmap: Bitmap? = null

        // Render page 1 thumbnail using native android.graphics.pdf.PdfRenderer
        val tempFile = File.createTempFile("biotrace_scan_", ".pdf", context.cacheDir)
        try {
            FileOutputStream(tempFile).use { it.write(bytes) }
            ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                val renderer = PdfRenderer(pfd)
                pageCount = renderer.pageCount
                if (pageCount > 0) {
                    val page = renderer.openPage(0)
                    val scale = (1080f / page.width.toFloat()).coerceAtMost(2.0f)
                    val width = (page.width * scale).toInt()
                    val height = (page.height * scale).toInt()

                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bitmap)
                    canvas.drawColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    previewBitmap = bitmap
                }
                renderer.close()
            }
        } catch (_: Exception) {
            // If rendering preview fails (e.g. specialized font stream), PDF bytes are still intact for Vision LLM
        } finally {
            tempFile.delete()
        }

        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return ProcessedDocument(
            mimeType = "application/pdf",
            base64Data = base64,
            fileName = fileName,
            pageCount = pageCount,
            previewBitmap = previewBitmap,
            byteSize = bytes.size.toLong()
        )
    }

    private fun processImage(uri: Uri, fileName: String, mimeType: String): ProcessedDocument {
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            @Suppress("DEPRECATION")
            android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                decoder.isMutableRequired = true
            }
        }

        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, outputStream)
        val bytes = outputStream.toByteArray()
        val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)

        return ProcessedDocument(
            mimeType = mimeType,
            base64Data = base64,
            fileName = fileName,
            pageCount = 1,
            previewBitmap = bitmap,
            byteSize = bytes.size.toLong()
        )
    }

    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        name = cursor.getString(nameIndex)
                    }
                }
            } catch (_: Exception) {}
        }
        if (name == null) {
            name = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else path
            }
        }
        return name
    }
}
