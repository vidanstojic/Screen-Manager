package com.example.screenmanager.ui.navigation

import android.os.Parcelable
import com.example.screenmanager.ui.feature.limits.RuleTarget
import kotlinx.parcelize.Parcelize

/**
 * Svi ekrani aplikacije. Novi ekran se dodaje ovde, a zatim mapira na svoj
 * composable u [AppNavHost] — to su jedina dva mesta koja znaju za navigaciju.
 *
 * Ekrani su Parcelable da bi back-stack preživeo rotaciju i gašenje procesa.
 */
sealed interface Screen : Parcelable {

    /** Stabilan ključ ekrana: po njemu se čuva njegovo stanje (skrol, izbor) dok je na stack-u. */
    val route: String

    /** Ekrani iz donje navigacije. */
    sealed interface Tab : Screen

    @Parcelize
    data object Home : Tab {
        override val route: String get() = "tab/home"
    }

    @Parcelize
    data object Stats : Tab {
        override val route: String get() = "tab/stats"
    }

    @Parcelize
    data object Limits : Tab {
        override val route: String get() = "tab/limits"
    }

    @Parcelize
    data object Alarms : Tab {
        override val route: String get() = "tab/alarms"
    }

    /** Detalji potrošnje jedne aplikacije. */
    @Parcelize
    data class AppDetails(val packageName: String, val appName: String) : Screen {
        override val route: String get() = "app/$packageName"
    }

    /** Forma za novo ili postojeće pravilo; [target] kaže koje. */
    @Parcelize
    data class RuleEditor(val target: RuleTarget) : Screen {
        override val route: String get() = "rule/$target"
    }

    /** Forma za alarm; `alarmId == null` znači novi alarm. */
    @Parcelize
    data class AlarmEditor(val alarmId: Long?) : Screen {
        override val route: String get() = "alarm/${alarmId ?: "new"}"
    }

    /** Dozvole i informacije o aplikaciji. */
    @Parcelize
    data object Settings : Screen {
        override val route: String get() = "settings"
    }
}
