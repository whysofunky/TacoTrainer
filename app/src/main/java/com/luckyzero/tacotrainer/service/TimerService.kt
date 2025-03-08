package com.luckyzero.tacotrainer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.graphics.Color
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// https://robertohuertas.com/2019/06/29/android_foreground_services/

private const val ID_RUNNING_TIMER = 1

@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject
    lateinit var timerRunner: TimerRunner
    private var wakeLock: PowerManager.WakeLock? = null

    enum class Action {
        LOAD,
        START,
        PAUSE,
        RESUME,
        STOP,
        RESTART,
        CLEAR,
    }

    companion object {

        private fun makeIntent(context: Context, action: Action): Intent {
            return Intent(context, TimerService::class.java).also {
                it.action = action.name
            }
        }

        fun start(context: Context) {
            context.startService(makeIntent(context, Action.START))
        }

        fun pause(context: Context) {
            context.startService(makeIntent(context, Action.PAUSE))
        }

        fun resume(context: Context) {
            context.startService(makeIntent(context, Action.RESUME))
        }

        fun stop(context: Context) {
            context.startService(makeIntent(context, Action.STOP))
        }

        fun restart(context: Context) {
            context.startService(makeIntent(context, Action.RESTART))
        }

        fun clear(context: Context) {
            context.startService(makeIntent(context, Action.CLEAR))
        }

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            when (intent.action) {
                Action.START.name -> startWorkout()
                Action.PAUSE.name -> pauseWorkout()
                Action.RESUME.name -> resumeWorkout()
                Action.STOP.name -> stopWorkout()
                else -> error("Unexpected action ${intent.action}")
            }
        }
        // by returning this we make sure the service is restarted if the system kills the service.
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(ID_RUNNING_TIMER, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(ID_RUNNING_TIMER, notification, FOREGROUND_SERVICE_TYPE_NONE)
        } else {
            startForeground(ID_RUNNING_TIMER, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startWorkout() {
        // Starting service and starting workout are not the same thing
        timerRunner.serviceStart(lifecycleScope)
    }

    private fun pauseWorkout() {
        timerRunner.servicePause()
        stopSelf()
    }

    private fun resumeWorkout() {
        timerRunner.serviceResume(lifecycleScope)
    }

    private fun stopWorkout() {
        timerRunner.serviceStop()
        stopSelf()
    }

    private fun restartWorkout() {
        TODO("Need to implement")
    }

    private fun clearWorkout() {
        TODO("Need to implement")
    }

    private fun createNotification(): Notification {
        val notificationChannelId = "ENDLESS SERVICE CHANNEL"

        // depending on the Android API that we're dealing with we will have
        // to use a specific method to create the notification
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                "Endless Service notifications channel",
                NotificationManager.IMPORTANCE_HIGH
            ).let {
                it.description = "Endless Service channel"
                it.enableLights(true)
                it.lightColor = Color.RED
                it.enableVibration(true)
                it.vibrationPattern = longArrayOf(100, 100, 100)
                it
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
                )


        val builder: Notification.Builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(
                this,
                notificationChannelId
            )
        } else {
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Endless Service")
            .setContentText("This is your favorite endless service working")
            .setContentIntent(pendingIntent)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker("Ticker text")
            .setPriority(Notification.PRIORITY_HIGH) // for under android 26 compatibility
            .build()
    }
}