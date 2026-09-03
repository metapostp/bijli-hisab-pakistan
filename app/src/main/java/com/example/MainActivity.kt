package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.BijliRepository
import com.example.data.TariffRepository
import com.example.localization.AppLanguage
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ThemeMode
import androidx.compose.ui.platform.LocalContext

enum class AppScreen {
    HOME,
    SPLIT_BILL,
    CALCULATE_BILL,
    METER_CONVERTER,
    PROPERTIES,
    HISTORY,
    TARIFF_SETTINGS
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: BijliRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getInstance(applicationContext)
        repository = BijliRepository(database.bijliDao())

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
            val systemInDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.SYSTEM -> systemInDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            MyApplicationTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BijliApp(
                        repository = repository,
                        isDarkTheme = isDark,
                        onToggleTheme = {
                            themeMode = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun BijliApp(
    repository: BijliRepository,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    onToggleTheme: () -> Unit = {}
) {
    val context = LocalContext.current
    val tariffRepository = remember { TariffRepository(context) }
    var currentScreen by remember { mutableStateOf(AppScreen.HOME) }
    var currentLang by remember { mutableStateOf(AppLanguage.ENGLISH) }
    var transferredPortions by remember { mutableStateOf<List<com.example.billing.PortionInput>?>(null) }

    Crossfade(targetState = currentScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.HOME -> HomeScreen(
                repository = repository,
                tariffRepository = tariffRepository,
                currentLang = currentLang,
                isDarkTheme = isDarkTheme,
                onToggleTheme = onToggleTheme,
                onToggleLanguage = {
                    currentLang = if (currentLang == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
                },
                onNavigateToSplitBill = { currentScreen = AppScreen.SPLIT_BILL },
                onNavigateToCalculateBill = { currentScreen = AppScreen.CALCULATE_BILL },
                onNavigateToProperties = { currentScreen = AppScreen.PROPERTIES },
                onNavigateToHistory = { currentScreen = AppScreen.HISTORY },
                onNavigateToMeterConverter = { currentScreen = AppScreen.METER_CONVERTER },
                onNavigateToTariffSettings = { currentScreen = AppScreen.TARIFF_SETTINGS }
            )
            AppScreen.SPLIT_BILL -> SplitBillScreen(
                repository = repository,
                currentLang = currentLang,
                onBack = { currentScreen = AppScreen.HOME },
                onBillSaved = { currentScreen = AppScreen.HISTORY },
                initialPortions = transferredPortions,
                onNavigateToConverter = { currentScreen = AppScreen.METER_CONVERTER },
                tariffRepository = tariffRepository,
                onNavigateToTariffSettings = { currentScreen = AppScreen.TARIFF_SETTINGS }
            )
            AppScreen.CALCULATE_BILL -> CalculateBillScreen(
                currentLang = currentLang,
                tariffRepository = tariffRepository,
                onNavigateToTariffSettings = { currentScreen = AppScreen.TARIFF_SETTINGS },
                onBack = { currentScreen = AppScreen.HOME }
            )
            AppScreen.METER_CONVERTER -> MeterConverterScreen(
                currentLang = currentLang,
                onBack = { currentScreen = AppScreen.HOME },
                onTransferToSplitBill = { portions ->
                    transferredPortions = portions
                    currentScreen = AppScreen.SPLIT_BILL
                }
            )
            AppScreen.PROPERTIES -> PropertyManagerScreen(
                repository = repository,
                currentLang = currentLang,
                onBack = { currentScreen = AppScreen.HOME },
                onSelectPropertyForSplit = { prop, units ->
                    currentScreen = AppScreen.SPLIT_BILL
                }
            )
            AppScreen.HISTORY -> HistoryScreen(
                repository = repository,
                currentLang = currentLang,
                onBack = { currentScreen = AppScreen.HOME }
            )
            AppScreen.TARIFF_SETTINGS -> TariffSettingsScreen(
                tariffRepository = tariffRepository,
                currentLang = currentLang,
                onBack = { currentScreen = AppScreen.HOME }
            )
        }
    }
}
