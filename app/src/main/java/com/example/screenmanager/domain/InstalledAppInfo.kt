package com.example.screenmanager.domain

import android.content.Context
import android.content.Intent
import com.example.screenmanager.model.AppOption

fun getInstalledApps(context: Context): List<AppOption> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }

    return pm.queryIntentActivities(intent, 0).mapNotNull { resolveInfo ->
        val packageName = resolveInfo.activityInfo.packageName

        // Preskačemo našu aplikaciju da korisnik ne bi blokirao sam Screen Manager
        if (packageName == context.packageName) return@mapNotNull null

        val appName = resolveInfo.loadLabel(pm).toString()
        val appIcon = resolveInfo.loadIcon(pm)

        // Vraćamo tvoj AppOption model direktno
        AppOption(
            id = packageName,
            name = appName,
            category = "Installed App",
            icon = appIcon.toString()// Možeš staviti bilo koju default vrednost ovde
        )
    }.sortedBy { it.name.lowercase() }
}