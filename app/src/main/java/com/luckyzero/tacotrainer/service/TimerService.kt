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
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

// https://robertohuertas.com/2019/06/29/android_foreground_services/

private const val TAG = "TimerService"
private const val ID_RUNNING_TIMER = 1

@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject
    lateinit var timerRunner: TimerRunner
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private var currentTimerId: Int? = null

    enum class Action {
        START,
        PAUSE,
        RESUME,
        STOP,
        RESTART,
    }

    companion object {
        private const val PARAM_TIMER_ID = "TimerId"
        private fun makeIntent(context: Context, action: Action, timerId: Int): Intent {
            return Intent(context, TimerService::class.java).also {
                it.action = action.name
                it.putExtra(PARAM_TIMER_ID, timerId)
            }
        }

        fun start(timerId: Int, context: Context) {
            context.startService(makeIntent(context, Action.START, timerId))
        }

        fun pause(timerId: Int, context: Context) {
            context.startService(makeIntent(context, Action.PAUSE, timerId))
        }

        fun resume(timerId: Int, context: Context) {
            context.startService(makeIntent(context, Action.RESUME, timerId))
        }

        fun stop(timerId: Int, context: Context) {
            context.startService(makeIntent(context, Action.STOP, timerId))
        }

        fun restart(timerId: Int, context: Context) {
            context.startService(makeIntent(context, Action.RESTART, timerId))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            val timerId = intent.getIntExtra(PARAM_TIMER_ID, 0)
            check(timerId != 0, { "TimerId unset" } )
            when (intent.action) {
                Action.START.name -> startWorkout(timerId)
                Action.PAUSE.name -> pauseWorkout(timerId)
                Action.RESUME.name -> resumeWorkout(timerId)
                Action.STOP.name -> stopWorkout(timerId)
                Action.RESTART.name -> restartWorkout(timerId)
                else -> error("Unexpected action ${intent.action}")
            }
        }
        // by returning this we make sure the service is restarted if the system kills the service.
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        // TODO: No notification gets displayed here, I don't know why not.
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

    private fun startWorkout(timerId: Int) {
        timerRunner.serviceStart(timerId)
        startLoop(timerId)
    }

    private fun pauseWorkout(timerId: Int) {
        timerRunner.servicePause(timerId)
    }

    private fun resumeWorkout(timerId: Int) {
        timerRunner.serviceResume(timerId)
        startLoop(timerId)
    }

    private fun stopWorkout(timerId: Int) {
        timerRunner.serviceStop(timerId)
    }

    private fun restartWorkout(timerId: Int) {
        timerRunner.serviceRestart(timerId)
        startLoop(timerId)
    }

    private fun startLoop(timerId: Int) {
        Log.d(TAG, "start loop")
        if (timerJob != null) {
            if (currentTimerId != timerId) {
                Log.d(TAG, "replacing job for timer $currentTimerId with $timerId")
                timerJob?.cancel()
                timerJob = null
            } else {
                Log.w(TAG, "requested another job for timer $timerId")
            }
        }
        if (timerJob == null) {
            timerJob = lifecycleScope.launch {
                timerRunner.runTicks(timerId)
                onTimerFinished(timerId)
            }
        }
    }

    private fun onTimerFinished(timerId: Int) {
        Log.d(TAG, "onTimerFinished timerId $timerId")
        stopSelf()
        timerJob = null
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