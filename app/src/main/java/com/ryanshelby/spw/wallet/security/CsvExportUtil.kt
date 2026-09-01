package com.ryanshelby.spw.wallet.security

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import com.ryanshelby.spw.wallet.data.model.TransactionType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting SPW transaction history to a standardized accounting CSV report.
 */
object CsvExportUtil {

    private const val TAG = "CsvExportUtil"

    fun generateCsv(transactions: List<TransactionItem>, includeSummary: Boolean = true): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()

        // CSV Header
        sb.append("Date (UTC),Timestamp,Transaction ID,Type,Token,Amount SPW,Fee SPW,From Address,To Address,Status,Block Number,Memo\n")

        var totalInflow = 0.0
        var totalOutflow = 0.0
        var totalFees = 0.0

        for (tx in transactions) {
            val dateStr = try {
                dateFormat.format(Date(if (tx.timestamp > 1000000000000L) tx.timestamp else tx.timestamp * 1000L))
            } catch (e: Exception) {
                tx.timestamp.toString()
            }

            val txid = escapeCsv(tx.txHash)
            val typeStr = escapeCsv(tx.type.name)
            val token = escapeCsv(tx.tokenSymbol)
            val amount = String.format(Locale.US, "%.8f", tx.amountSpw)
            val fee = String.format(Locale.US, "%.8f", tx.feeSpw)
            val from = escapeCsv(tx.fromAddress)
            val to = escapeCsv(tx.toAddress)
            val status = escapeCsv(tx.status.name)
            val block = tx.blockNumber.toString()
            val memo = escapeCsv(tx.memo)

            if (tx.type == TransactionType.RECEIVE) {
                totalInflow += tx.amountSpw
            } else if (tx.type == TransactionType.SEND || tx.type == TransactionType.STEALTH) {
                totalOutflow += tx.amountSpw
            }
            totalFees += tx.feeSpw

            sb.append("$dateStr,${tx.timestamp},$txid,$typeStr,$token,$amount,$fee,$from,$to,$status,$block,$memo\n")
        }

        if (includeSummary && transactions.isNotEmpty()) {
            val netVolume = totalInflow - totalOutflow
            sb.append("\n")
            sb.append("--- FINANCIAL AUDIT SUMMARY ---\n")
            sb.append("Total Inflows (SPW),${String.format(Locale.US, "%.8f", totalInflow)}\n")
            sb.append("Total Outflows (SPW),${String.format(Locale.US, "%.8f", totalOutflow)}\n")
            sb.append("Net Volume (SPW),${String.format(Locale.US, "%.8f", netVolume)}\n")
            sb.append("Total Gas Fees (SPW),${String.format(Locale.US, "%.8f", totalFees)}\n")
            sb.append("Total Transaction Count,${transactions.size}\n")
        }

        return sb.toString()
    }

    fun generateCsvFile(context: Context, transactions: List<TransactionItem>, label: String = "Ledger"): File? {
        return try {
            val csvContent = generateCsv(transactions, includeSummary = true)
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val safeLabel = label.lowercase().replace(" ", "_")
            val file = File(exportDir, "sparrow_${safeLabel}_$timestamp.csv")
            file.writeText(csvContent)
            file
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate CSV file", e)
            null
        }
    }

    fun exportAndShareCsv(
        context: Context,
        transactions: List<TransactionItem>,
        label: String = "Ledger"
    ): Boolean {
        return try {
            val file = generateCsvFile(context, transactions, label)
            if (file == null) {
                Toast.makeText(context, "Failed to generate CSV file", Toast.LENGTH_SHORT).show()
                return false
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "SPW Wallet Financial $label ($timestamp)")
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("Ledger CSV", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export $label (CSV)").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to share CSV", e)
            Toast.makeText(context, "Share error: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_LONG).show()
            false
        }
    }

    fun exportAndSaveCsvToDevice(
        context: Context,
        transactions: List<TransactionItem>,
        label: String = "Ledger"
    ): Boolean {
        val file = generateCsvFile(context, transactions, label)
        if (file == null) {
            Toast.makeText(context, "Failed to generate CSV file", Toast.LENGTH_SHORT).show()
            return false
        }
        return StorageExportHelper.saveFileToDownloads(context, file, "text/csv")
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
