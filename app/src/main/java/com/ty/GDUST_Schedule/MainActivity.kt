package com.ty.GDUST_Schedule

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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ty.GDUST_Schedule.ui.theme.ClassAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Locale

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PREF_NAME = "locale_prefs"
        private const val KEY_APPLIED_LANG = "applied_lang"
        private const val DISCLAIMER_PREF = "disclaimer_prefs"
        private const val KEY_DISCLAIMER_AGREED = "disclaimer_agreed"
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
        val settings = com.ty.GDUST_Schedule.data.SettingsDataStore(this)
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

            // Disclaimer state
            val disclaimerPrefs = getSharedPreferences(DISCLAIMER_PREF, Context.MODE_PRIVATE)
            var disclaimerAgreed by remember { mutableStateOf(disclaimerPrefs.getBoolean(KEY_DISCLAIMER_AGREED, false)) }

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
                    // Disclaimer dialog
                    if (!disclaimerAgreed) {
                        AlertDialog(
                            onDismissRequest = {},
                            title = {
                                Text(
                                    text = stringResource(R.string.disclaimer_title),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            },
                            text = {
                                Text(
                                    text = stringResource(R.string.disclaimer_text),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.verticalScroll(rememberScrollState())
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        disclaimerPrefs.edit().putBoolean(KEY_DISCLAIMER_AGREED, true).apply()
                                        disclaimerAgreed = true
                                    }
                                ) {
                                    Text(stringResource(R.string.disclaimer_agree))
                                }
                            },
                            dismissButton = {
                                OutlinedButton(
                                    onClick = { finish() }
                                ) {
                                    Text(stringResource(R.string.disclaimer_exit))
                                }
                            }
                        )
                    } else {
                        ScheduleApp(viewModel = vm)
                    }
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
