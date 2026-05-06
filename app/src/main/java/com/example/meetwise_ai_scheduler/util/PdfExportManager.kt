package com.example.meetwise_ai_scheduler.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.util.Log
import com.example.meetwise_ai_scheduler.domain.model.Minutes
import java.io.File
import java.io.FileOutputStream

class PdfExportManager(private val context: Context) {

    fun exportMinutesToPdf(minutes: Minutes, fileName: String): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        var yPosition = 40f

        // Title
        paint.textSize = 20f
        paint.isFakeBoldText = true
        canvas.drawText("Meeting Minutes", 40f, yPosition, paint)
        yPosition += 40f

        // Summary Header
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("Summary:", 40f, yPosition, paint)
        yPosition += 20f

        // Summary Text (Basic wrapping)
        paint.isFakeBoldText = false
        paint.textSize = 12f
        val summaryLines = wrapText(minutes.summary, 80)
        for (line in summaryLines) {
            canvas.drawText(line, 40f, yPosition, paint)
            yPosition += 15f
        }
        yPosition += 30f

        // Action Items Header
        paint.isFakeBoldText = true
        paint.textSize = 14f
        canvas.drawText("Action Items:", 40f, yPosition, paint)
        yPosition += 25f

        // Action Items
        paint.isFakeBoldText = false
        paint.textSize = 12f
        for (item in minutes.actionItems) {
            val status = if (item.done) "[x]" else "[ ]"
            canvas.drawText("$status ${item.task} (${item.owner})", 40f, yPosition, paint)
            yPosition += 20f
        }

        pdfDocument.finishPage(page)

        // Save file
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "$fileName.pdf")
        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Log.d("PdfExport", "PDF saved to: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("PdfExport", "Error writing PDF", e)
            return null
        } finally {
            pdfDocument.close()
        }

        return file
    }

    private fun wrapText(text: String, maxLength: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.length + word.length > maxLength) {
                lines.add(currentLine.toString())
                currentLine = StringBuilder()
            }
            currentLine.append("$word ")
        }
        lines.add(currentLine.toString())
        return lines
    }
}
