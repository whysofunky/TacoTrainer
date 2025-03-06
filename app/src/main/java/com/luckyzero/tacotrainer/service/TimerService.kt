package com.luckyzero.tacotrainer.service

import android.app.Notification
import android.content.Intent
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope

interface TimerServiceInterface {
    enum class Actions {
        START,
        STOP
    }
}

// https://robertohuertas.com/2019/06/29/android_foreground_services/

private const val ID_RUNNING_TIMER = 1

class TimerService : LifecycleService(), TimerServiceInterface {

    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent != null) {
            when (intent.action) {
                TimerServiceInterface.Actions.START.name -> startService(intent.extras)
                TimerServiceInterface.Actions.STOP.name -> stopService(intent.extras)
                else -> throw IllegalStateException("Unexpected action ${intent.action}")
            }
        }
        // by returning this we make sure the service is restarted if the system kills the service.
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        // val notification = createNotification()
        // startForeground(ID_RUNNING_TIMER, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun startService(extras: Bundle?) {

    }

    private fun stopService(extras: Bundle?) {

    }

//    private fun createNotification(): Notification {
//
//    }

}