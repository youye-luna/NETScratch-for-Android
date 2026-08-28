package youyeluna.lanipscanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import youyeluna.lanipscanner.ui.AppNavHost
import youyeluna.lanipscanner.ui.LanguageManager
import youyeluna.lanipscanner.ui.LocalStrings
import youyeluna.lanipscanner.ui.ScanViewModel
import youyeluna.lanipscanner.ui.stringsFor
import youyeluna.lanipscanner.ui.theme.DhcpScannerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LanguageManager.init(this)
        enableEdgeToEdge()
        setContent {
            val strings = stringsFor(LanguageManager.currentLanguage)
            CompositionLocalProvider(LocalStrings provides strings) {
                DhcpScannerTheme {
                    val scanViewModel: ScanViewModel = viewModel()
                    AppNavHost(scanViewModel)
                }
            }
        }
    }
}
