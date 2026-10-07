package com.example.screenmanager.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.screenmanager.ui.common.OnResume
import com.example.screenmanager.ui.navigation.AppNavHost
import com.example.screenmanager.ui.theme.ScreenManagerTheme

/**
 * Ulazna tačka Compose UI-a (poziva je MainActivity).
 *
 * Mapa UI koda:
 * - `ui/theme`       → boje, tipografija, oblici, razmaci
 * - `ui/components`  → gradivni elementi koje dele svi ekrani (kartice, dugmad, grafik...)
 * - `ui/common`      → pomoćne stvari bez izgleda (formatiranje, ikonice aplikacija, lifecycle)
 * - `ui/model`       → modeli koje ekrani prikazuju
 * - `ui/navigation`  → lista ekrana, back-stack, donja navigacija
 * - `ui/feature/<x>` → jedan folder po ekranu: Screen (izgled) + ViewModel (stanje)
 */
@Composable
fun ScreenManagerApp(appViewModel: AppViewModel = viewModel()) {
    // Po povratku u aplikaciju: osveži dozvole i povuci nove podatke o potrošnji.
    OnResume(appViewModel::onResume)

    ScreenManagerTheme {
        AppNavHost()
    }
}
