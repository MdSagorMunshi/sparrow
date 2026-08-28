package com.ryanshelby.spw.wallet.security

import java.net.URLDecoder

data class ParsedQrResult(
    val address: String,
    val amount: String? = null,
    val memo: String? = null
)

object QrUriParser {
    fun parse(raw: String): ParsedQrResult {
        val trimmed = raw.trim()

        // Strip known blockchain URL/URI schemes if present
        val withoutScheme = when {
            trimmed.startsWith("spw:", ignoreCase = true) -> trimmed.substring(4)
            trimmed.startsWith("spwwallet:", ignoreCase = true) -> trimmed.substring(10)
            trimmed.startsWith("ethereum:", ignoreCase = true) -> trimmed.substring(9)
            trimmed.startsWith("bitcoin:", ignoreCase = true) -> trimmed.substring(8)
            else -> trimmed
        }

        val parts = withoutScheme.split("?", limit = 2)
        val address = parts[0].trim()

        var amount: String? = null
        var memo: String? = null

        if (parts.size > 1) {
            val queryParams = parts[1].split("&")
            for (param in queryParams) {
                val kv = param.split("=", limit = 2)
                if (kv.size == 2) {
                    val key = kv[0].trim().lowercase()
                    val value = try {
                        URLDecoder.decode(kv[1].trim(), "UTF-8")
                    } catch (_: Exception) {
                        kv[1].trim()
                    }
                    when (key) {
                        "amount", "value", "val" -> amount = value
                        "memo", "message", "msg" -> memo = value
                    }
                }
            }
        }

        return ParsedQrResult(address = address, amount = amount, memo = memo)
    }
}
