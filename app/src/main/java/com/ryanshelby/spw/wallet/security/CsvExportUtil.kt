package com.ryanshelby.spw.wallet.security

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.ryanshelby.spw.wallet.data.model.TransactionItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for exporting SPW transaction history to a standardized accounting CSV report.
 */
object CsvExportUtil {

    fun generateCsv(transactions: List<TransactionItem>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val sb = StringBuilder()

        // CSV Header
        sb.append("Date,Timestamp,Transaction ID,Type,Token,Amount,Fee SPW,From Address,To Address,Status,Block Number,Memo\n")

        for (tx in transactions) {
            val dateStr = try {
                dateFormat.format(Date(tx.timestamp * 1000L))
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

            sb.append("$dateStr,${tx.timestamp},$txid,$typeStr,$token,$amount,$fee,$from,$to,$status,$block,$memo\n")
        }

        return sb.toString()
    }

    fun exportAndShareCsv(context: Context, transactions: List<TransactionItem>): Boolean {
        return try {
            val csvContent = generateCsv(transactions)
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(exportDir, "sparrow_ledger_$timestamp.csv")
            file.writeText(csvContent)

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, "SPARROW Wallet Transaction Ledger ($timestamp)")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Financial Ledger (CSV)")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
