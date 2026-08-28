package youyeluna.lanipscanner.ui

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 语言偏好管理器（单例）
 * 使用 SharedPreferences 持久化语言选择
 */
object LanguageManager {
    private const val PREFS_NAME = "language_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    private var prefs: SharedPreferences? = null

    /** 当前语言（Compose 可观察） */
    var currentLanguage by mutableStateOf(Language.ZH_CN)
        private set

    /** 初始化，从 SharedPreferences 读取已保存的语言 */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedCode = prefs?.getString(KEY_LANGUAGE, null)
        currentLanguage = Language.entries.firstOrNull { it.code == savedCode }
            ?: detectSystemLanguage()
    }

    /** 切换语言 */
    fun setLanguage(language: Language) {
        currentLanguage = language
        prefs?.edit()?.putString(KEY_LANGUAGE, language.code)?.apply()
    }

    /** 根据系统语言匹配，匹配不上默认简中 */
    private fun detectSystemLanguage(): Language {
        val locale = java.util.Locale.getDefault()
        val lang = locale.language
        val country = locale.country
        return when {
            lang == "zh" && country == "TW" -> Language.ZH_TW
            lang == "zh" && country == "HK" -> Language.ZH_TW
            lang == "zh" -> Language.ZH_CN
            lang == "en" -> Language.EN
            else -> Language.ZH_CN
        }
    }
}
