package com.luckyzero.tacotrainer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.database.DbAccess
import com.luckyzero.tacotrainer.platform.DefaultClock
import com.luckyzero.tacotrainer.repositories.SegmentTreeLoader
import com.luckyzero.tacotrainer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch


// https://robertohuertas.com/2019/06/29/android_foreground_services/

private const val ID_RUNNING_TIMER = 1

@AndroidEntryPoint
class TimerService : LifecycleService() {

    enum class Action {
        START,
        PAUSE,
        RESUME,
        STOP
    }

    companion object {
        const val WORKOUT_ID_PARAM = "WorkoutId"

        private fun makeIntent(context: Context, action: Action, workoutId: Long? = null): Intent {
            return Intent(context, TimerService::class.java).also {
                it.action = action.name
                if (workoutId != null) it.putExtra(WORKOUT_ID_PARAM, workoutId)
            }
        }

        fun start(workoutId: Long, context: Context) {
            context.startService(makeIntent(context, Action.START, workoutId))
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

    }


    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false
    private var timer: PreloadedTimer? = null


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            when (intent.action) {
                Action.START.name -> startWorkout(intent.extras)
                Action.PAUSE.name -> pauseWorkout()
                Action.RESUME.name -> resumeWorkout()
                Action.STOP.name -> stopWorkout()
                else -> throw IllegalStateException("Unexpected action ${intent.action}")
            }
        }
        // by returning this we make sure the service is restarted if the system kills the service.
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        val notification = createNotification()
        startForeground(ID_RUNNING_TIMER, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startWorkout(extras: Bundle?) {
        // TODO: Stop any existing workout timer
        if (isServiceStarted) return
        isServiceStarted = true
    }

    private fun pauseWorkout() {
    }

    private fun resumeWorkout() {
    }

    private fun stopWorkout() {
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