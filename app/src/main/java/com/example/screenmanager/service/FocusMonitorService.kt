package com.example.screenmanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import com.example.screenmanager.R
import com.example.screenmanager.domain.ServiceLocator
import com.example.screenmanager.domain.TimeBuckets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FocusMonitorService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val usageRepository by lazy { ServiceLocator.usageStatsRepository(this) }
    private val blockRepository by lazy { ServiceLocator.blockRepository(this) }
    private val powerManager by lazy { getSystemService(PowerManager::class.java) }
    private lateinit var overlayController: BlockOverlayController
    private var monitorJob: Job? = null
    private var wakeupEvaluationJob: Job? = null
    private var currentPackage: String? = null
    private var currentPackageStartedAt: Long = 0L

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> onScreenOff()
                Intent.ACTION_SCREEN_ON -> onScreenOn()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayController = BlockOverlayController(this) { sendHome() }
        registerReceiver(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            }
        )
        ServiceLocator.scheduleDailyMaintenance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        )
        startMonitoring()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        runCatching { unregisterReceiver(screenReceiver) }
        val now = System.currentTimeMillis()
        val packageName = currentPackage
        if (packageName != null && currentPackageStartedAt > 0L) {
            serviceScope.launch { usageRepository.recordSession(packageName, currentPackageStartedAt, now) }
        }
        overlayController.hide()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = serviceScope.launch {
            blockRepository.seedDefaultsIfNeeded()
            while (true) {
                runCatching { tick() }.onFailure { Log.w(TAG, "Monitor tick failed", it) }
                delay(1_000)
            }
        }
    }

    private suspend fun tick() {
        val now = System.currentTimeMillis()
        val packageName = usageRepository.findLastForegroundPackage(now - 15_000, now)
        if (packageName != null && packageName != currentPackage) {
            currentPackage?.let { usageRepository.recordSession(it, currentPackageStartedAt, now) }
            currentPackage = packageName
            currentPackageStartedAt = now
        }

        val decision = packageName?.let { blockRepository.getBlockDecision(it, now) }
        if (decision != null) {
            overlayController.show(decision)
        } else {
            overlayController.hide()
        }
    }

    private fun onScreenOff() {
        wakeupEvaluationJob?.cancel()
        overlayController.hide()
        serviceScope.launch {
            blockRepository.markScreenOff(System.currentTimeMillis())
        }
    }

    private fun onScreenOn() {
        val now = System.currentTimeMillis()
        serviceScope.launch {
            blockRepository.markScreenOn(now)
            val lastOff = blockRepository.getLastScreenOff()
            if (lastOff != null && now - lastOff >= TimeBuckets.sevenHoursMs) {
                blockRepository.markPotentialWakeup(now)
                startWakeupEvaluation(now)
            } else {
                blockRepository.clearPotentialWakeup()
            }
        }
    }

    private fun startWakeupEvaluation(startedAt: Long) {
        wakeupEvaluationJob?.cancel()
        wakeupEvaluationJob = serviceScope.launch {
            delay(TimeBuckets.wakeupEvaluationWindowMs)
            val lastInteraction = blockRepository.getLastInteraction() ?: 0L
            val userStayedActive = powerManager.isInteractive &&
                (lastInteraction >= startedAt || currentPackage != null)
            if (userStayedActive) {
                blockRepository.activateWakeupBlock(System.currentTimeMillis() + TimeBuckets.defaultWakeupBlockMs)
            } else {
                blockRepository.clearPotentialWakeup()
            }
        }
    }

    private fun sendHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus monitoring",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Praćenje fokusa i ekranskog vremena je aktivno")
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "focus_monitor"
        private const val NOTIFICATION_ID = 1001
        private const val TAG = "FocusMonitorService"
    }
}
