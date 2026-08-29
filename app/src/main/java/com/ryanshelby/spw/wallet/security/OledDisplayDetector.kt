package com.ryanshelby.spw.wallet.security

import android.content.Context
import android.os.Build
import android.view.Display
import android.view.WindowManager
import java.io.File

/**
 * Hardware OLED / AMOLED Display Detection Utility.
 * Restricts OLED pure black mode exclusively to devices with physical OLED/AMOLED panels
 * to ensure true subpixel shutoff and zero power draw.
 */
object OledDisplayDetector {

    data class DetectionResult(
        val isOled: Boolean,
        val panelType: String,
        val details: String
    )

    fun detectDisplay(context: Context): DetectionResult {
        var isOled = false
        val reasons = mutableListOf<String>()

        // 1. Check Wide Color Gamut (Display P3 / DCI-P3 is standard on OLED/AMOLED)
        val isWideGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.resources.configuration.isScreenWideColorGamut
        } else false
        if (isWideGamut) {
            reasons.add("Wide Color Gamut (P3)")
        }

        // 2. Check Display HDR capabilities (AMOLED/OLED panels support HDR10, HLG, Dolby Vision)
        val display: Display? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.display
            } catch (e: Exception) {
                null
            }
        } else {
            (context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager)?.defaultDisplay
        }

        val hasHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            display?.isHdr == true
        } else false

        val hasHdrTypes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            (display?.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true)
        } else false

        if (hasHdr || hasHdrTypes) {
            reasons.add("High Dynamic Range (HDR)")
        }

        // 3. Check kernel sysfs and system panel descriptors for OLED/AMOLED indicators
        val panelKeywordFound = checkPanelDescriptors()
        if (panelKeywordFound != null) {
            reasons.add("Panel: $panelKeywordFound")
            isOled = true
        }

        // 4. Evaluate combined criteria:
        // Wide gamut AND HDR is a strong, reliable indicator of OLED/AMOLED on Android 8.0+
        if ((isWideGamut && (hasHdr || hasHdrTypes)) || panelKeywordFound != null) {
            isOled = true
        }

        // Fallback for popular known OLED device series (Pixel, Galaxy S/Note/Z, OnePlus, etc.)
        if (!isOled) {
            val model = Build.MODEL.lowercase()
            val device = Build.DEVICE.lowercase()
            val oledKeywords = listOf("pixel", "galaxy s", "galaxy note", "galaxy z", "oneplus", "xiaomi 1", "redmi note", "find x")
            if (oledKeywords.any { model.contains(it) || device.contains(it) } && isWideGamut) {
                isOled = true
                reasons.add("Verified Device Model ($model)")
            }
        }

        val panelType = if (isOled) "AMOLED / OLED" else "LCD / IPS"
        val details = if (isOled) {
            "Verified OLED Display: ${reasons.joinToString(", ")}"
        } else {
            "Standard LCD / IPS Display: True subpixel power shutdown requires an AMOLED/OLED panel."
        }

        return DetectionResult(isOled, panelType, details)
    }

    fun isOledDisplay(context: Context): Boolean {
        return detectDisplay(context).isOled
    }

    private fun checkPanelDescriptors(): String? {
        val candidatePaths = listOf(
            "/sys/class/graphics/fb0/msm_fb_panel_info",
            "/sys/class/graphics/fb0/name",
            "/sys/class/drm/card0-DSI-1/status",
            "/proc/cmdline"
        )

        for (path in candidatePaths) {
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val content = file.readText().lowercase()
                    when {
                        content.contains("amoled") -> return "AMOLED"
                        content.contains("super_amoled") -> return "Super AMOLED"
                        content.contains("oled") -> return "OLED"
                        content.contains("diamond") -> return "Diamond OLED"
                        content.contains("ea8061") || content.contains("sw43404") -> return "Samsung OLED IC"
                    }
                }
            } catch (_: Exception) {
                // Ignore sysfs read errors on restricted devices
            }
        }

        // Check system properties via reflection
        try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val getMethod = systemProperties.getMethod("get", String::class.java)
            val panelName = (getMethod.invoke(null, "ro.boot.panel_name") as? String)?.lowercase() ?: ""
            if (panelName.contains("oled") || panelName.contains("amoled")) {
                return "OLED ($panelName)"
            }
        } catch (_: Exception) {
        }

        return null
    }
}
