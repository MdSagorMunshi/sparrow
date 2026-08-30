package com.ryanshelby.spw.wallet.nfc

data class NfcPaymentInvoice(
    val address: String,
    val name: String,
    val amount: Double? = null,
    val token: String? = null,
    val timestampMs: Long,
    val nonce: String
) {
    fun toJson(): String {
        return """
            {
                "address": "${escapeJson(address)}",
                "name": "${escapeJson(name)}",
                "amount": ${amount?.toString() ?: "null"},
                "token": ${if (token != null) "\"${escapeJson(token)}\"" else "null"},
                "timestampMs": $timestampMs,
                "nonce": "${escapeJson(nonce)}"
            }
        """.trimIndent()
    }

    companion object {
        fun fromJson(jsonString: String): NfcPaymentInvoice {
            // A simple regex-based parser for our known format
            fun extractString(key: String): String? {
                val regex = "\"$key\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1)
            }
            fun extractDouble(key: String): Double? {
                val regex = "\"$key\"\\s*:\\s*([0-9.]+)".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1)?.toDoubleOrNull()
            }
            fun extractLong(key: String): Long? {
                val regex = "\"$key\"\\s*:\\s*([0-9]+)".toRegex()
                return regex.find(jsonString)?.groupValues?.get(1)?.toLongOrNull()
            }

            return NfcPaymentInvoice(
                address = extractString("address") ?: "",
                name = extractString("name") ?: "",
                amount = extractDouble("amount"),
                token = extractString("token"),
                timestampMs = extractLong("timestampMs") ?: 0L,
                nonce = extractString("nonce") ?: ""
            )
        }
        
        private fun escapeJson(str: String): String {
            return str.replace("\\", "\\\\").replace("\"", "\\\"")
        }
    }
}
