package com.example.i18n

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.InputStream
import java.util.Locale

/**
 * Global reactive Localization / i18n Manager.
 * Loads translations dynamically from assets/locales/{code}.json.
 */
object LocaleManager {

    private const val TAG = "LocaleManager"
    private const val PREFS_NAME = "quran_app_locale_prefs"
    private const val KEY_LANGUAGE_CODE = "selected_language_code"

    private val _currentLanguage = MutableStateFlow(Language.ARABIC)
    val currentLanguage: StateFlow<Language> = _currentLanguage.asStateFlow()

    private val _strings = MutableStateFlow<Map<String, String>>(emptyMap())
    val strings: StateFlow<Map<String, String>> = _strings.asStateFlow()

    // Cache fallback (Arabic) strings in memory
    private var fallbackStrings: Map<String, String> = emptyMap()

    private var initialized = false

    fun init(context: Context) {
        if (initialized) return
        val prefs = getPrefs(context)

        // Preload fallback Arabic strings
        fallbackStrings = loadLocaleJson(context, "ar")

        val savedCode = prefs.getString(KEY_LANGUAGE_CODE, null)
        val initialLanguage = if (savedCode != null) {
            Language.findByCode(savedCode)
        } else {
            // Auto-detect device language on first open
            val deviceLang = Locale.getDefault().language
            val matched = Language.ALL.find { it.code.equals(deviceLang, ignoreCase = true) }
            matched ?: Language.ARABIC
        }

        _currentLanguage.value = initialLanguage
        _strings.value = if (initialLanguage.code == "ar") {
            fallbackStrings
        } else {
            val loaded = loadLocaleJson(context, initialLanguage.code)
            fallbackStrings + loaded // merge with fallback for any missing key
        }

        initialized = true
    }

    fun setLanguage(context: Context, language: Language) {
        val prefs = getPrefs(context)
        prefs.edit().putString(KEY_LANGUAGE_CODE, language.code).apply()

        val loaded = if (language.code == "ar") {
            fallbackStrings
        } else {
            val langMap = loadLocaleJson(context, language.code)
            fallbackStrings + langMap
        }

        _currentLanguage.value = language
        _strings.value = loaded
    }

    fun setLanguageByCode(context: Context, code: String) {
        setLanguage(context, Language.findByCode(code))
    }

    fun t(key: String, defaultVal: String = ""): String {
        return _strings.value[key] ?: fallbackStrings[key] ?: defaultVal.ifEmpty { key }
    }

    private fun loadLocaleJson(context: Context, code: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val assetPath = "locales/$code.json"
            val inputStream: InputStream = context.assets.open(assetPath)
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val keys = jsonObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = jsonObject.optString(key, "")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading locale JSON for $code: ${e.message}")
        }
        return map
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}

val LocalAppLanguage = staticCompositionLocalOf { Language.ARABIC }
val LocalAppStrings = compositionLocalOf { emptyMap<String, String>() }

/**
 * Idiomatic Jetpack Compose string translation helper.
 */
@Composable
@ReadOnlyComposable
fun stringTranslate(key: String, defaultVal: String = ""): String {
    val strings = LocalAppStrings.current
    return strings[key] ?: LocaleManager.t(key, defaultVal)
}
