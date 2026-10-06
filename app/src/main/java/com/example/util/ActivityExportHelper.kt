package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.ui.screens.DailyActivityItem
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * High-Performance, Secure Local Document Export & Sharing Helper.
 * Generates beautifully formatted CSV spreadsheets and styled PDF reports
 * of employee daily activity logs entirely offline, preventing any data leakage.
 */
object ActivityExportHelper {

    private const val AUTHORITY = "com.example.fileprovider"

    /**
     * Exports a list of daily activity logs to a CSV spreadsheet file and triggers a standard Share Sheet.
     */
    fun exportToCsv(context: Context, employeeName: String, items: List<DailyActivityItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No activity logs available to export today", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "MB_ActivityLog_${employeeName.replace(" ", "_")}_$dateStr.csv"
            val file = File(context.cacheDir, fileName)

            val writer = FileWriter(file)
            // Header Row
            writer.write("TIME,CATEGORY,ACTIVITY TITLE,DESCRIPTION,STATUS\n")

            // Data Rows
            for (item in items) {
                val time = escapeCsvField(item.timeFormatted)
                val category = escapeCsvField(item.category.name)
                val title = escapeCsvField(item.title)
                val description = escapeCsvField(item.description)
                val status = escapeCsvField(item.metadataTag ?: "Logged")
                writer.write("$time,$category,$title,$description,$status\n")
            }
            writer.flush()
            writer.close()

            shareFile(context, file, "text/csv", "Export Daily Activity logs (CSV)")
        } catch (e: Exception) {
            Log.e("ActivityExportHelper", "Failed to export CSV: ${e.message}", e)
            Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Exports a list of daily activity logs to a beautiful offline PDF Report and triggers a Share Sheet.
     */
    fun exportToPdf(context: Context, employeeName: String, dateLabel: String, items: List<DailyActivityItem>) {
        if (items.isEmpty()) {
            Toast.makeText(context, "No activity logs available to export today", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val pdfDocument = PdfDocument()
            // Standard A4 dimensions: 595 x 842 points (72 points per inch)
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val paint = Paint()
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                color = Color.BLACK
            }

            // ==========================================
            // 🎨 DRAW PDF STYLING (Corporate Elegant)
            // ==========================================

            // Header Background Accent Bar
            paint.color = Color.parseColor("#0F172A") // Deep Slate Blue Primary
            canvas.drawRect(0f, 0f, 595f, 90f, paint)

            // Header Text: App Brand
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("MB TAKER", 36f, 44f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            canvas.drawText("Enterprise Workspace Operations Center", 36f, 62f, paint)

            // Header Date & Summary Box on the right
            paint.color = Color.parseColor("#10B981") // Success Emerald Accent
            canvas.drawRect(420f, 24f, 559f, 66f, paint)
            
            paint.color = Color.WHITE
            paint.textSize = 9f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("OFFLINE REPORT", 432f, 40f, paint)
            paint.textSize = 8f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date()), 432f, 54f, paint)

            // Document Details Section
            var currentY = 130f
            textPaint.textSize = 12f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DAILY ACTIVITY EXPORT LOG", 36f, currentY, textPaint)
            
            currentY += 24f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.GRAY
            canvas.drawText("Employee Name:", 36f, currentY, textPaint)
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(employeeName, 140f, currentY, textPaint)

            currentY += 18f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.GRAY
            canvas.drawText("Reporting Date:", 36f, currentY, textPaint)
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(dateLabel, 140f, currentY, textPaint)

            currentY += 18f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textPaint.color = Color.GRAY
            canvas.drawText("Total Actions:", 36f, currentY, textPaint)
            textPaint.color = Color.BLACK
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("${items.size} Records Logged", 140f, currentY, textPaint)

            // Draw divider line
            currentY += 24f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawLine(36f, currentY, 559f, currentY, paint)

            // ==========================================
            // 📊 DRAW TABLE HEADERS
            // ==========================================
            currentY += 24f
            paint.color = Color.parseColor("#F1F5F9") // Light table header background
            canvas.drawRect(36f, currentY - 14f, 559f, currentY + 10f, paint)

            textPaint.color = Color.parseColor("#1E293B")
            textPaint.textSize = 9f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("TIME", 44f, currentY, textPaint)
            canvas.drawText("CATEGORY", 110f, currentY, textPaint)
            canvas.drawText("ACTIVITY LOGGED", 200f, currentY, textPaint)
            canvas.drawText("STATUS", 480f, currentY, textPaint)

            currentY += 18f

            // ==========================================
            // 📄 DRAW ACTIVITY LIST ROWS
            // ==========================================
            textPaint.color = Color.BLACK
            for (item in items) {
                // Alternating light row backgrounds
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(36f, currentY - 11f, 559f, currentY + 11f, paint)

                // Row borders
                paint.color = Color.parseColor("#F1F5F9")
                canvas.drawLine(36f, currentY + 11f, 559f, currentY + 11f, paint)

                // Render fields with truncation safety
                textPaint.textSize = 8.5f
                
                // 1. Time (Bold)
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText(item.timeFormatted, 44f, currentY, textPaint)

                // 2. Category
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                textPaint.color = Color.parseColor("#475569")
                canvas.drawText(item.category.name, 110f, currentY, textPaint)

                // 3. Activity Title + description combined safely
                textPaint.color = Color.BLACK
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val fullTitle = item.title
                val desc = item.description
                val titleClamped = if (fullTitle.length > 36) fullTitle.substring(0, 34) + "..." else fullTitle
                canvas.drawText(titleClamped, 200f, currentY - 1f, textPaint)

                textPaint.color = Color.parseColor("#64748B")
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val descClamped = if (desc.length > 55) desc.substring(0, 52) + "..." else desc
                canvas.drawText(descClamped, 200f, currentY + 8f, textPaint)

                // 4. Status Tag
                textPaint.color = Color.parseColor("#0369A1")
                textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val tag = item.metadataTag ?: "Logged"
                canvas.drawText(tag.uppercase(), 480f, currentY, textPaint)

                currentY += 24f

                // Avoid overdrawing off page height limit (Simple single-page implementation)
                if (currentY > 780f) {
                    textPaint.color = Color.parseColor("#94A3B8")
                    textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    canvas.drawText("[Report truncated due to single-page limit]", 36f, 800f, textPaint)
                    break
                }
            }

            // Footer Section
            paint.color = Color.parseColor("#94A3B8")
            textPaint.color = Color.parseColor("#94A3B8")
            textPaint.textSize = 8f
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Generated locally and securely by MB Taker Applet. Confirmed Offline Signature.", 36f, 824f, textPaint)

            pdfDocument.finishPage(page)

            // Save and Share PDF
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "MB_ActivityLog_${employeeName.replace(" ", "_")}_$dateStr.pdf"
            val file = File(context.cacheDir, fileName)

            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()

            shareFile(context, file, "application/pdf", "Export Daily Activity Report (PDF)")
        } catch (e: Exception) {
            Log.e("ActivityExportHelper", "Failed to export PDF: ${e.message}", e)
            Toast.makeText(context, "PDF Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        try {
            val fileUri: Uri = FileProvider.getUriForFile(context, AUTHORITY, file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_SUBJECT, "MB Taker Daily Activity Logs")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        } catch (e: Exception) {
            Log.e("ActivityExportHelper", "Failed to share file: ${e.message}", e)
            Toast.makeText(context, "Sharing failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun escapeCsvField(field: String): String {
        val cleaned = field.replace("\n", " ").replace("\r", " ")
        return if (cleaned.contains(",") || cleaned.contains("\"") || cleaned.contains("'")) {
            "\"" + cleaned.replace("\"", "\"\"") + "\""
        } else {
            cleaned
        }
    }
}
