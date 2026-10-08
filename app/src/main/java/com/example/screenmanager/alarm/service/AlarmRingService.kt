package com.example.screenmanager.alarm.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import com.example.screenmanager.alarm.AlarmModule
import com.example.screenmanager.alarm.data.PendingSnooze
import com.example.screenmanager.alarm.domain.RingPolicy
import com.example.screenmanager.alarm.domain.RingReason
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.model.AlarmRule
import com.example.screenmanager.model.MissionType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Foreground servis koji drži alarm "živim" dok zvoni: zvuk, vibracija,
 * notifikacija preko celog ekrana i automatsko odlaganje posle
 * [RingPolicy.AUTO_SILENCE_MS].
 *
 * Komande (Intent akcije):
 * - [ACTION_RING] — počni da zvoniš (iz AlarmReceiver-a, wake-up check-a ili probe);
 * - [ACTION_SNOOZE] — odloži (ekran alarma ili dugme na notifikaciji);
 * - [ACTION_DISMISS] — ugasi; ekran alarma ga šalje tek kad je misija rešena.
 *
 * Tip `mediaPlayback`: servis pušta zvuk. Pokretanje iz pozadine je dozvoljeno
 * jer ga pokreće tačan alarm (setAlarmClock / setExactAndAllowWhileIdle).
 */
class AlarmRingService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val player by lazy { AlarmPlayer(this) }
    private val notifications by lazy { AlarmNotifications(this) }
    private var ringJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> {
                // startForeground mora odmah (rok 5 s), pre čitanja baze.
                startForeground(
                    AlarmNotifications.RINGING_ID,
                    notifications.ringing(AlarmRingSession.active.value),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
                handleRing(intent)
            }
            ACTION_SNOOZE -> snooze()
            ACTION_DISMISS -> dismiss()
            else -> stopIfIdle()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    private fun handleRing(intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        val reason = runCatching { RingReason.valueOf(intent.getStringExtra(EXTRA_REASON).orEmpty()) }
            .getOrDefault(RingReason.SCHEDULED)
        @Suppress("DEPRECATION")
        val draft = intent.getSerializableExtra(EXTRA_DRAFT) as? AlarmRule
        val snoozeCount = intent.getIntExtra(EXTRA_SNOOZE_COUNT, 0)
        val wakeCheckRound = intent.getIntExtra(EXTRA_WAKE_CHECK_ROUND, 0)

        scope.launch {
            val alarm = draft ?: withContext(Dispatchers.IO) {
                runCatching { ServiceLocator.alarmRepository(this@AlarmRingService).findById(alarmId) }.getOrNull()
            }
            if (alarm == null) {
                Log.w(TAG, "Alarm $alarmId not found, nothing to ring")
                stopIfIdle()
                return@launch
            }
            startRinging(ActiveRing(alarm, reason, System.currentTimeMillis(), snoozeCount, wakeCheckRound))
        }
    }

    private fun startRinging(ring: ActiveRing) {
        // Novi alarm prekida onaj koji već zvoni (dva alarma u istom minutu).
        stopRinging()
        AlarmRingSession.start(ring)
        player.start(ring.alarm.sound)
        notifications.ringing(ring).let { startForeground(AlarmNotifications.RINGING_ID, it, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) }
        openRingScreen()

        ringJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val elapsed = now - ring.startedAt
                val fade = RingPolicy.volumeFraction(elapsed, ring.alarm.sound.fadeInSeconds)
                val duck = if (RingPolicy.isDucked(AlarmRingSession.lastMissionInteractionAt, now)) RingPolicy.DUCKED_FRACTION else 1f
                player.setVolumeFraction(fade * duck)
                if (elapsed >= RingPolicy.AUTO_SILENCE_MS) {
                    autoSilence()
                    break
                }
                delay(VOLUME_TICK_MS)
            }
        }
    }

    /**
     * Ekran alarma: na zaključanom telefonu ga otvara full-screen notifikacija;
     * kad je telefon u upotrebi, sistem bi prikazao samo heads-up notifikaciju,
     * pa se ekran otvara direktno (dozvoljeno uz dozvolu za prikaz preko drugih aplikacija).
     */
    private fun openRingScreen() {
        if (!Settings.canDrawOverlays(this)) return
        runCatching {
            startActivity(
                Intent(this, AlarmRingActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_USER_ACTION)
            )
        }.onFailure { Log.w(TAG, "Cannot open ring screen directly", it) }
    }

    private fun snooze() {
        val ring = AlarmRingSession.active.value ?: return stopIfIdle()
        if (!ring.canSnooze) return
        stopRinging()
        val until = RingPolicy.snoozeUntil(System.currentTimeMillis(), ring.alarm.snooze)
        val count = ring.snoozeCount + 1
        ServiceLocator.alarmScheduler(this).scheduleSnooze(ring.alarm.id, until, count)
        AlarmModule.runtimeStore(this).putSnooze(PendingSnooze(ring.alarm.id, until, count))
        notifications.showSnoozed(
            ring.alarm.id,
            ring.alarm.label,
            until,
            canDismiss = ring.alarm.mission.type == MissionType.NONE
        )
        finish()
    }

    private fun dismiss() {
        val ring = AlarmRingSession.active.value ?: return stopIfIdle()
        stopRinging()
        scope.launch {
            withContext(Dispatchers.IO + NonCancellable) {
                AlarmDismissFlow.onDismissed(applicationContext, ring.alarm, ring.reason, ring.wakeCheckRound)
            }
            finish()
        }
    }

    private fun autoSilence() {
        val ring = AlarmRingSession.active.value ?: return
        if (ring.canSnooze) {
            snooze()
        } else {
            stopRinging()
            notifications.showMissed(ring.alarm.label, ring.startedAt)
            finish()
        }
    }

    private fun stopRinging() {
        ringJob?.cancel()
        ringJob = null
        player.stop()
        AlarmRingSession.clear()
    }

    private fun stopIfIdle() {
        if (AlarmRingSession.active.value == null) finish()
    }

    private fun finish() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val ACTION_RING = "com.example.screenmanager.alarm.RING"
        const val ACTION_SNOOZE = "com.example.screenmanager.alarm.SNOOZE"
        const val ACTION_DISMISS = "com.example.screenmanager.alarm.DISMISS"
        private const val EXTRA_ALARM_ID = "alarm_id"
        private const val EXTRA_REASON = "reason"
        private const val EXTRA_DRAFT = "draft"
        private const val EXTRA_SNOOZE_COUNT = "snooze_count"
        private const val EXTRA_WAKE_CHECK_ROUND = "wake_check_round"
        private const val VOLUME_TICK_MS = 250L
        private const val TAG = "AlarmRingService"

        /** Pokreće zvonjenje sačuvanog alarma [alarmId]. */
        fun ring(
            context: Context,
            alarmId: Long,
            reason: RingReason,
            snoozeCount: Int = 0,
            wakeCheckRound: Int = 0
        ) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_RING
                putExtra(EXTRA_ALARM_ID, alarmId)
                putExtra(EXTRA_REASON, reason.name)
                putExtra(EXTRA_SNOOZE_COUNT, snoozeCount)
                putExtra(EXTRA_WAKE_CHECK_ROUND, wakeCheckRound)
            }
            context.startForegroundService(intent)
        }

        /** Probno zvonjenje iz forme — sa trenutnim (možda nesačuvanim) podešavanjima. */
        fun test(context: Context, draft: AlarmRule) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_RING
                putExtra(EXTRA_ALARM_ID, draft.id)
                putExtra(EXTRA_REASON, RingReason.TEST.name)
                putExtra(EXTRA_DRAFT, draft)
            }
            context.startForegroundService(intent)
        }

        fun command(context: Context, action: String) {
            context.startService(Intent(context, AlarmRingService::class.java).setAction(action))
        }
    }
}
