package com.example.screenmanager.alarm.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.ui.feature.alarms.ring.WakeCheckScreen
import com.example.screenmanager.ui.theme.AppSection
import com.example.screenmanager.ui.theme.ScreenManagerTheme
import com.example.screenmanager.ui.theme.SectionTheme
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * "Are you awake?" — prikazuje se preko zaključanog ekrana. Zatvara se kad
 * korisnik potvrdi ili kad provera nestane (istekao rok → alarm zvoni).
 */
class WakeCheckActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val store = AlarmModule.runtimeStore(this)
        val deadline = store.wakeChecks.value[alarmId]?.deadline
        if (deadline == null) {
            finish()
            return
        }
        lifecycleScope.launch {
            store.wakeChecks.map { it[alarmId]?.deadline }.collectLatest { if (it == null) finishAndRemoveTask() }
        }

        setContent {
            ScreenManagerTheme {
                SectionTheme(AppSection.Alarms) {
                    WakeCheckScreen(
                        deadline = deadline,
                        onConfirm = {
                            WakeCheckController(this).confirm(alarmId)
                            finishAndRemoveTask()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_ROUND = "round"

        fun intent(context: Context, alarmId: Long, round: Int): Intent =
            Intent(context, WakeCheckActivity::class.java)
                .putExtra(EXTRA_ALARM_ID, alarmId)
                .putExtra(EXTRA_ROUND, round)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
    }
}
