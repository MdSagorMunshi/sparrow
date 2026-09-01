package com.ryanshelby.spw.wallet.security

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionType
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

/**
 * Utility for generating professional, institutional bank-grade PDF account statements.
 */
object PdfStatementExportUtil {

    private const val TAG = "PdfStatementExport"
    private const val PAGE_WIDTH = 595 // A4 standard width in points (72 dpi)
    private const val PAGE_HEIGHT = 842 // A4 standard height in points (72 dpi)
    private const val MARGIN_LEFT = 36f
    private const val MARGIN_RIGHT = 36f
    private const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN_LEFT - MARGIN_RIGHT

    // Colors
    private val COLOR_PRIMARY_DARK = Color.rgb(15, 23, 42) // Slate 900
    private val COLOR_TEXT_SECONDARY = Color.rgb(71, 85, 105) // Slate 600
    private val COLOR_TEXT_MUTED = Color.rgb(148, 163, 184) // Slate 400
    private val COLOR_EMERALD = Color.rgb(5, 150, 105) // Emerald 600
    private val COLOR_RUBY = Color.rgb(220, 38, 38) // Red 600
    private val COLOR_CARD_BG = Color.rgb(248, 250, 252) // Slate 50
    private val COLOR_TABLE_HEADER_BG = Color.rgb(241, 245, 249) // Slate 100
    private val COLOR_ROW_ALT_BG = Color.rgb(250, 250, 250)
    private val COLOR_BORDER = Color.rgb(226, 232, 240) // Slate 200

    fun exportAndSharePdf(
        context: Context,
        transactions: List<TransactionItem>,
        walletAddress: String,
        networkName: String = "SPW Mainnet",
        periodLabel: String = "All Time"
    ): Boolean {
        return try {
            val pdfDocument = PdfDocument()

            val now = Date()
            val fileDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val headerDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss 'UTC'", Locale.US)
            val rowDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            val generatedDateStr = headerDateFormat.format(now)
            val statementId = "SPW-STMT-" + SimpleDateFormat("yyyyMMdd", Locale.US).format(now) + "-" + (1000..9999).random()

            // Calculate Financial Totals
            var totalInflow = 0.0
            var totalOutflow = 0.0
            var totalFees = 0.0
            for (tx in transactions) {
                if (tx.type == TransactionType.RECEIVE) {
                    totalInflow += tx.amountSpw
                } else {
                    totalOutflow += tx.amountSpw
                }
                totalFees += tx.feeSpw
            }
            val netVolume = totalInflow - totalOutflow

            // Pagination calculation
            val rowHeight = 28f
            val page1AvailableHeight = 490f // available for rows after header and summary box
            val page1MaxRows = (page1AvailableHeight / rowHeight).toInt().coerceAtLeast(1)
            val subsequentPageAvailableHeight = 670f
            val subsequentPageMaxRows = (subsequentPageAvailableHeight / rowHeight).toInt().coerceAtLeast(1)

            val totalRows = transactions.size
            val totalPages = if (totalRows <= page1MaxRows) {
                1
            } else {
                1 + ceil((totalRows - page1MaxRows).toDouble() / subsequentPageMaxRows).toInt()
            }

            var currentRowIndex = 0

            for (pageNumber in 1..totalPages) {
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Setup Paints
                val paint = Paint().apply { isAntiAlias = true }
                val boldTypeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                val regularTypeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val monoTypeface = Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL)

                var y = 40f

                if (pageNumber == 1) {
                    // ── Header (Page 1) ─────────────────────────
                    paint.typeface = boldTypeface
                    paint.textSize = 18f
                    paint.color = COLOR_PRIMARY_DARK
                    canvas.drawText("SPARROW NETWORK", MARGIN_LEFT, y + 16f, paint)

                    paint.typeface = regularTypeface
                    paint.textSize = 8.5f
                    paint.color = COLOR_TEXT_SECONDARY
                    canvas.drawText("OFFICIAL ACCOUNT STATEMENT & FINANCIAL AUDIT", MARGIN_LEFT, y + 28f, paint)

                    // Right-aligned statement badge
                    paint.color = COLOR_CARD_BG
                    val badgeRect = RectF(PAGE_WIDTH - MARGIN_RIGHT - 180f, y, PAGE_WIDTH - MARGIN_RIGHT, y + 36f)
                    canvas.drawRoundRect(badgeRect, 6f, 6f, paint)
                    paint.color = COLOR_BORDER
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 0.8f
                    canvas.drawRoundRect(badgeRect, 6f, 6f, paint)
                    paint.style = Paint.Style.FILL

                    paint.typeface = boldTypeface
                    paint.textSize = 8f
                    paint.color = COLOR_EMERALD
                    canvas.drawText("● VERIFIED ON-CHAIN LEDGER", PAGE_WIDTH - MARGIN_RIGHT - 170f, y + 14f, paint)

                    paint.typeface = monoTypeface
                    paint.textSize = 7.5f
                    paint.color = COLOR_TEXT_SECONDARY
                    canvas.drawText(statementId, PAGE_WIDTH - MARGIN_RIGHT - 170f, y + 26f, paint)

                    y += 50f

                    // ── Summary Box ─────────────────────────────
                    val summaryBoxHeight = 100f
                    val summaryRect = RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + summaryBoxHeight)
                    paint.color = COLOR_CARD_BG
                    canvas.drawRoundRect(summaryRect, 8f, 8f, paint)
                    paint.color = COLOR_BORDER
                    paint.style = Paint.Style.STROKE
                    paint.strokeWidth = 1f
                    canvas.drawRoundRect(summaryRect, 8f, 8f, paint)
                    paint.style = Paint.Style.FILL

                    // Account Details (Left Half)
                    paint.typeface = boldTypeface
                    paint.textSize = 7.5f
                    paint.color = COLOR_TEXT_MUTED
                    canvas.drawText("ACCOUNT / WALLET ADDRESS", MARGIN_LEFT + 14f, y + 18f, paint)

                    paint.typeface = monoTypeface
                    paint.textSize = 9f
                    paint.color = COLOR_PRIMARY_DARK
                    val displayAddress = if (walletAddress.length > 36) {
                        walletAddress.take(18) + "..." + walletAddress.takeLast(16)
                    } else {
                        walletAddress.ifBlank { "SPW Public Ledger" }
                    }
                    canvas.drawText(displayAddress, MARGIN_LEFT + 14f, y + 32f, paint)

                    paint.typeface = regularTypeface
                    paint.textSize = 8f
                    paint.color = COLOR_TEXT_SECONDARY
                    canvas.drawText("Network: $networkName  •  Period: $periodLabel  •  Records: ${transactions.size}", MARGIN_LEFT + 14f, y + 46f, paint)
                    canvas.drawText("Generated: $generatedDateStr", MARGIN_LEFT + 14f, y + 58f, paint)

                    // Financial Tiles (Right Half)
                    val rightColX = MARGIN_LEFT + CONTENT_WIDTH * 0.52f
                    val tileWidth = (CONTENT_WIDTH * 0.44f) / 2f

                    // Divider in summary box
                    paint.color = COLOR_BORDER
                    paint.strokeWidth = 0.8f
                    canvas.drawLine(rightColX - 10f, y + 10f, rightColX - 10f, y + summaryBoxHeight - 10f, paint)

                    // Total Received
                    paint.typeface = boldTypeface
                    paint.textSize = 7f
                    paint.color = COLOR_TEXT_MUTED
                    canvas.drawText("TOTAL INFLOW (RECEIVED)", rightColX, y + 18f, paint)
                    paint.typeface = boldTypeface
                    paint.textSize = 10f
                    paint.color = COLOR_EMERALD
                    canvas.drawText("+${String.format(Locale.US, "%.4f", totalInflow)} SPW", rightColX, y + 32f, paint)

                    // Total Sent
                    paint.typeface = boldTypeface
                    paint.textSize = 7f
                    paint.color = COLOR_TEXT_MUTED
                    canvas.drawText("TOTAL OUTFLOW (SENT)", rightColX + tileWidth, y + 18f, paint)
                    paint.typeface = boldTypeface
                    paint.textSize = 10f
                    paint.color = COLOR_PRIMARY_DARK
                    canvas.drawText("-${String.format(Locale.US, "%.4f", totalOutflow)} SPW", rightColX + tileWidth, y + 32f, paint)

                    // Net Flow
                    paint.typeface = boldTypeface
                    paint.textSize = 7f
                    paint.color = COLOR_TEXT_MUTED
                    canvas.drawText("NET VOLUME FLOW", rightColX, y + 56f, paint)
                    paint.typeface = boldTypeface
                    paint.textSize = 10f
                    paint.color = if (netVolume >= 0) COLOR_EMERALD else COLOR_RUBY
                    canvas.drawText((if (netVolume >= 0) "+" else "") + String.format(Locale.US, "%.4f", netVolume) + " SPW", rightColX, y + 70f, paint)

                    // Total Gas Fees
                    paint.typeface = boldTypeface
                    paint.textSize = 7f
                    paint.color = COLOR_TEXT_MUTED
                    canvas.drawText("NETWORK FEES PAID", rightColX + tileWidth, y + 56f, paint)
                    paint.typeface = boldTypeface
                    paint.textSize = 10f
                    paint.color = COLOR_TEXT_SECONDARY
                    canvas.drawText("${String.format(Locale.US, "%.4f", totalFees)} SPW", rightColX + tileWidth, y + 70f, paint)

                    y += summaryBoxHeight + 20f
                } else {
                    // Header for Subsequent Pages
                    paint.typeface = boldTypeface
                    paint.textSize = 10f
                    paint.color = COLOR_PRIMARY_DARK
                    canvas.drawText("SPARROW NETWORK ACCOUNT STATEMENT", MARGIN_LEFT, y + 12f, paint)

                    paint.typeface = monoTypeface
                    paint.textSize = 8f
                    paint.color = COLOR_TEXT_MUTED
                    canvas.drawText("Statement ID: $statementId  •  Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN_RIGHT - 240f, y + 12f, paint)

                    paint.color = COLOR_BORDER
                    paint.strokeWidth = 0.8f
                    canvas.drawLine(MARGIN_LEFT, y + 20f, PAGE_WIDTH - MARGIN_RIGHT, y + 20f, paint)

                    y += 30f
                }

                // ── Table Header ────────────────────────────────────
                val colDate = MARGIN_LEFT + 8f
                val colType = MARGIN_LEFT + 95f
                val colCounterparty = MARGIN_LEFT + 170f
                val colTxId = MARGIN_LEFT + 340f
                val colAmount = PAGE_WIDTH - MARGIN_RIGHT - 8f

                val tableHeaderHeight = 20f
                val tableHeaderRect = RectF(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + tableHeaderHeight)
                paint.color = COLOR_TABLE_HEADER_BG
                canvas.drawRoundRect(tableHeaderRect, 4f, 4f, paint)

                paint.typeface = boldTypeface
                paint.textSize = 7.5f
                paint.color = COLOR_PRIMARY_DARK

                canvas.drawText("DATE (UTC)", colDate, y + 13f, paint)
                canvas.drawText("TYPE", colType, y + 13f, paint)
                canvas.drawText("COUNTERPARTY / MEMO", colCounterparty, y + 13f, paint)
                canvas.drawText("TXID HASH", colTxId, y + 13f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("AMOUNT (SPW)", colAmount, y + 13f, paint)
                paint.textAlign = Paint.Align.LEFT

                y += tableHeaderHeight + 4f

                // ── Table Rows ──────────────────────────────────────
                if (totalRows == 0) {
                    paint.typeface = regularTypeface
                    paint.textSize = 9f
                    paint.color = COLOR_TEXT_MUTED
                    paint.textAlign = Paint.Align.CENTER
                    canvas.drawText("No transaction records found for this period.", PAGE_WIDTH / 2f, y + 30f, paint)
                    paint.textAlign = Paint.Align.LEFT
                } else {
                    val maxRowsThisPage = if (pageNumber == 1) page1MaxRows else subsequentPageMaxRows
                    var rowsDrawnThisPage = 0

                    while (currentRowIndex < totalRows && rowsDrawnThisPage < maxRowsThisPage) {
                        val tx = transactions[currentRowIndex]

                        // Row background alternating
                        if (rowsDrawnThisPage % 2 == 1) {
                            paint.color = COLOR_ROW_ALT_BG
                            canvas.drawRect(MARGIN_LEFT, y, PAGE_WIDTH - MARGIN_RIGHT, y + rowHeight, paint)
                        }

                        // Bottom divider per row
                        paint.color = COLOR_BORDER
                        paint.strokeWidth = 0.5f
                        canvas.drawLine(MARGIN_LEFT, y + rowHeight, PAGE_WIDTH - MARGIN_RIGHT, y + rowHeight, paint)

                        // Date
                        val txDate = Date(if (tx.timestamp > 1000000000000L) tx.timestamp else tx.timestamp * 1000L)
                        paint.typeface = regularTypeface
                        paint.textSize = 7.5f
                        paint.color = COLOR_TEXT_SECONDARY
                        canvas.drawText(rowDateFormat.format(txDate), colDate, y + 16f, paint)

                        // Type
                        val isIncoming = tx.type == TransactionType.RECEIVE
                        val isStealth = tx.type == TransactionType.STEALTH
                        paint.typeface = boldTypeface
                        paint.textSize = 7.5f
                        paint.color = if (isIncoming) COLOR_EMERALD else COLOR_PRIMARY_DARK
                        val typeStr = when {
                            isStealth -> "STEALTH"
                            isIncoming -> "RECEIVED"
                            else -> "SENT"
                        }
                        canvas.drawText(typeStr, colType, y + 16f, paint)

                        // Counterparty Address & Memo
                        val counterparty = if (isIncoming) tx.fromAddress else tx.toAddress
                        paint.typeface = monoTypeface
                        paint.textSize = 7.5f
                        paint.color = COLOR_PRIMARY_DARK
                        val displayCounterparty = if (counterparty.isNotBlank()) {
                            if (counterparty.length > 22) counterparty.take(9) + "..." + counterparty.takeLast(8) else counterparty
                        } else "Direct Protocol"
                        canvas.drawText(displayCounterparty, colCounterparty, y + 12f, paint)

                        if (tx.memo.isNotBlank()) {
                            paint.typeface = regularTypeface
                            paint.textSize = 6.5f
                            paint.color = COLOR_TEXT_MUTED
                            val memoTrunc = if (tx.memo.length > 30) tx.memo.take(28) + ".." else tx.memo
                            canvas.drawText("Memo: $memoTrunc", colCounterparty, y + 22f, paint)
                        }

                        // TXID
                        paint.typeface = monoTypeface
                        paint.textSize = 7f
                        paint.color = COLOR_TEXT_MUTED
                        val txidDisplay = if (tx.txHash.length > 16) tx.txHash.take(6) + "..." + tx.txHash.takeLast(6) else tx.txHash
                        canvas.drawText(txidDisplay, colTxId, y + 16f, paint)

                        // Amount & Fee
                        paint.textAlign = Paint.Align.RIGHT
                        paint.typeface = boldTypeface
                        paint.textSize = 8.5f
                        paint.color = if (isIncoming) COLOR_EMERALD else COLOR_PRIMARY_DARK
                        val amountStr = (if (isIncoming) "+" else "-") + String.format(Locale.US, "%.4f", tx.amountSpw)
                        canvas.drawText(amountStr, colAmount, y + 12f, paint)

                        if (tx.feeSpw > 0) {
                            paint.typeface = regularTypeface
                            paint.textSize = 6.5f
                            paint.color = COLOR_TEXT_MUTED
                            canvas.drawText("fee: ${String.format(Locale.US, "%.4f", tx.feeSpw)}", colAmount, y + 22f, paint)
                        }
                        paint.textAlign = Paint.Align.LEFT

                        y += rowHeight
                        currentRowIndex++
                        rowsDrawnThisPage++
                    }
                }

                // ── Footer (All Pages) ──────────────────────────────
                val footerY = PAGE_HEIGHT - 35f
                paint.color = COLOR_BORDER
                paint.strokeWidth = 0.8f
                canvas.drawLine(MARGIN_LEFT, footerY, PAGE_WIDTH - MARGIN_RIGHT, footerY, paint)

                paint.typeface = regularTypeface
                paint.textSize = 7f
                paint.color = COLOR_TEXT_MUTED
                canvas.drawText("Confidential Financial Record • Generated by SPARROW Protocol • sparrownetwork.io", MARGIN_LEFT, footerY + 14f, paint)

                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText("Page $pageNumber of $totalPages", PAGE_WIDTH - MARGIN_RIGHT, footerY + 14f, paint)
                paint.textAlign = Paint.Align.LEFT

                pdfDocument.finishPage(page)
            }

            // Save PDF to cache exports directory
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = fileDateFormat.format(now)
            val pdfFile = File(exportDir, "sparrow_statement_$timestamp.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            fos.flush()
            fos.close()
            pdfDocument.close()

            // Trigger System Share Sheet with read grant
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "SPARROW Account Statement ($statementId)")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Statement PDF", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Share Bank-Grade Statement (PDF)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to export PDF statement", e)
            Toast.makeText(context, "Export error: ${e.localizedMessage ?: "Failed to generate PDF"}", Toast.LENGTH_LONG).show()
            false
        }
    }
}
