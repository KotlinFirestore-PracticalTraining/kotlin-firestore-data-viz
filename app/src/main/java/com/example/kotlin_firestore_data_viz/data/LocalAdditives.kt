package com.example.kotlin_firestore_data_viz.data

import android.content.Context
import com.google.gson.Gson
import java.text.Normalizer
import java.util.Locale

/** One additive row from assets/additives_local.json */
data class AdditiveLocal(
    val code: String,              // e.g., "E330"
    val aliases: List<String>,     // ["sitruunahappo","citric acid"]
    val nameFi: String? = null,    // optional display names
    val nameEn: String? = null,
    val category: String? = null   // optional category (we can fallback by E-range)
)

object LocalAdditives {
    @Volatile private var cached: List<AdditiveLocal>? = null

    /** Load once from assets/additives_local.json */
    fun all(context: Context): List<AdditiveLocal> {
        cached?.let { return it }
        val json = context.assets.open("additives_local.json")
            .bufferedReader().use { it.readText() }
        val list = Gson().fromJson(json, Array<AdditiveLocal>::class.java).toList()
        cached = list
        return list
    }

    /** Detect E-codes from freeform ingredients text (FI/EN with light inflection tolerance). */
    fun detectEcodesFromText(context: Context, text: String): List<String> {
        if (text.isBlank()) return emptyList()
        val found = linkedSetOf<String>()

        // Raw E-number like "E330", "E 330", "e-150d"
        val eNum = Regex("""\b[eE]\s*[-]?\s*(\d{3,4}[a-dA-D]?)\b""")
        for (m in eNum.findAll(text)) {
            found += normalizeE("E" + m.groupValues[1])
        }

        // Alias matching
        val normText = normalize(text)
        val all = all(context)
        for (a in all) {
            for (alias in a.aliases) {
                val normAlias = normalize(alias)
                if (normAlias.isBlank()) continue
                val pattern =
                    if (normAlias.length <= 3)
                        Regex("""(?<![a-z0-9])${Regex.escape(normAlias)}(?![a-z0-9])""")
                    else
                        Regex("""(?<![a-z0-9])${Regex.escape(normAlias)}[a-z]{0,4}(?![a-z0-9])""")
                if (pattern.containsMatchIn(normText)) {
                    found += normalizeE(a.code)
                    break
                }
            }
        }
        return found.toList().sorted()
    }

    private fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("\\p{Mn}+".toRegex(), "")           // ä→a, ö→o
            .replace("[\\u00AD‐-–—]".toRegex(), "-")
            .replace("\\s+".toRegex(), " ")
            .trim()

    private fun normalizeE(code: String): String =
        code.trim().uppercase(Locale.ROOT)
            .replace(Regex("""^E(\d{3,4})([A-D])?$"""), "E$1")
}
