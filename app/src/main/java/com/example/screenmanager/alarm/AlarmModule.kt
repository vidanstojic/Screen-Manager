package com.example.screenmanager.alarm

import android.content.Context
import com.example.screenmanager.alarm.data.AlarmDetoxStore
import com.example.screenmanager.alarm.data.AlarmRuntimeStore

/**
 * Fabrika zavisnosti Smart Alarm dela. Odvojena od
 * [com.example.screenmanager.domain.ServiceLocator] da alarm deo ne bi
 * menjao zajedničke fajlove Screen Manager-a; bazu i repozitorijum alarma
 * i dalje daje ServiceLocator.
 */
object AlarmModule {
    @Volatile
    private var runtimeStore: AlarmRuntimeStore? = null

    @Volatile
    private var detoxStore: AlarmDetoxStore? = null

    /** Keširano: servisi (blokada aplikacija i sajtova) posmatraju isti StateFlow. */
    fun detoxStore(context: Context): AlarmDetoxStore =
        detoxStore ?: synchronized(this) {
            detoxStore ?: AlarmDetoxStore(context).also { detoxStore = it }
        }

    fun runtimeStore(context: Context): AlarmRuntimeStore =
        runtimeStore ?: synchronized(this) {
            runtimeStore ?: AlarmRuntimeStore(context).also { runtimeStore = it }
        }
}
