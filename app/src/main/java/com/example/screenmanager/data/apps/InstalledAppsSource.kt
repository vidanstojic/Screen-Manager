package com.example.screenmanager.data.apps

import android.content.Context
import android.content.Intent
import com.example.screenmanager.model.AppOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Podaci o instaliranim aplikacijama iz PackageManager-a: imena i lista
 * aplikacija koje korisnik može da izabere za pravilo.
 *
 * PackageManager pozivi su spori, pa se imena keširaju, a lista se učitava
 * van main thread-a.
 */
class InstalledAppsSource(context: Context) {
    private val appContext = context.applicationContext
    private val labels = ConcurrentHashMap<String, String>()

    @Volatile
    private var launchable: List<AppOption>? = null

    /** Ime aplikacije za prikaz; za deinstaliranu aplikaciju vraća packageName. */
    fun label(packageName: String): String =
        labels.getOrPut(packageName) {
            runCatching {
                val pm = appContext.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
            }.getOrDefault(packageName)
        }

    /**
     * Aplikacije sa launcher ikonicom, sortirane po imenu. Naša aplikacija je
     * izostavljena da korisnik ne bi blokirao sam Screen Manager.
     */
    suspend fun launchableApps(): List<AppOption> {
        launchable?.let { return it }
        return withContext(Dispatchers.IO) {
            val pm = appContext.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER)
            pm.queryIntentActivities(intent, 0)
                .asSequence()
                .map { it.activityInfo.packageName to it.loadLabel(pm).toString() }
                .filter { (packageName, _) -> packageName != appContext.packageName }
                .distinctBy { (packageName, _) -> packageName }
                .onEach { (packageName, label) -> labels[packageName] = label }
                .map { (packageName, label) -> AppOption(id = packageName, name = label, category = "", icon = "") }
                .sortedBy { it.name.lowercase() }
                .toList()
                .also { launchable = it }
        }
    }
}
