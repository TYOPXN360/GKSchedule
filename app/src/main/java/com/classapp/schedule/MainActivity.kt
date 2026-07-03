package com.classapp.schedule

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.classapp.schedule.ui.theme.ClassAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREF_NAME = "locale_prefs"
        private const val KEY_APPLIED_LANG = "applied_lang"
    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lang = prefs.getString(KEY_APPLIED_LANG, "") ?: ""
        val context = if (lang.isNotEmpty() && lang != "system") {
            val locale = Locale.forLanguageTag(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocales(LocaleList(locale))
            newBase.createConfigurationContext(config)
        } else {
            newBase
        }
        super.attachBaseContext(context)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Save current language to pref if not set
        val settings = com.classapp.schedule.data.SettingsDataStore(this)
        val savedLang = runBlocking { settings.language.first() }
        val prefs = getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val lastApplied = prefs.getString(KEY_APPLIED_LANG, "") ?: ""

        if (savedLang != lastApplied) {
            prefs.edit().putString(KEY_APPLIED_LANG, savedLang).apply()
            // Recreate to apply new locale
            recreate()
            return
        }

        enableEdgeToEdge()
        requestNotificationPermissionIfNeeded()

        // Request high refresh rate without changing resolution
        window.attributes = window.attributes.apply {
            preferredRefreshRate = display?.supportedModes?.maxByOrNull { it.refreshRate }?.refreshRate ?: 120f
        }

        setContent {
            val vm: ScheduleViewModel = viewModel()
            val darkMode by vm.darkMode.collectAsState(initial = "system")
            val language by vm.language.collectAsState(initial = savedLang)

            // Watch for language changes from settings UI
            LaunchedEffect(language) {
                if (language != lastApplied) {
                    getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                        .edit().putString(KEY_APPLIED_LANG, language).apply()
                    recreate()
                }
            }

            ClassAppTheme(darkTheme = darkMode) {
                // Status bar: light mode = dark icons, dark mode = light icons
                val view = LocalView.current
                val isDark = when (darkMode) {
                    "dark" -> true
                    "light" -> false
                    else -> isSystemInDarkTheme()
                }
                LaunchedEffect(isDark) {
                    val window = (view.context as? android.app.Activity)?.window
                    if (window != null) {
                        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
                        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ScheduleApp(viewModel = vm)
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }
}
