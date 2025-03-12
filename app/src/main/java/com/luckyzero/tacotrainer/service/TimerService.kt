package com.luckyzero.tacotrainer.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.luckyzero.tacotrainer.R
import com.luckyzero.tacotrainer.models.SegmentInterface
import com.luckyzero.tacotrainer.repositories.TimerRepository
import com.luckyzero.tacotrainer.ui.MainActivity
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject


// https://robertohuertas.com/2019/06/29/android_foreground_services/

private const val TAG = "TimerService"
private const val NOTIFICATION_ID_FOREGROUND_SERVICE = 1
private const val WAKE_LOCK_TAG = "TacoTrainer:TimerJob"

@AndroidEntryPoint
class TimerService : LifecycleService() {

    @Inject
    lateinit var timerRunner: TimerRunner
    private var wakeLock: PowerManager.WakeLock? = null
    private var timerJob: Job? = null
    private var currentTimerId: Int? = null

    private val notificationManager: NotificationManagerCompat
        get() = NotificationManagerCompat.from(this)
    private val powerManager: PowerManager
        get() = getSystemService(Context.POWER_SERVICE) as PowerManager

    enum class Action {
        START,
        PAUSE,
        RESUME,
        STOP,
        RESTART,
    }

    companion object {
        private const val TIMER_CHANNEL_ID = "TacoTrainerTimer"

        private const val PARAM_TIMER_ID = "TimerId"

        fun start(timerId: Int, context: Context) {
            startService(Action.START, timerId, context)
        }

        fun pause(timerId: Int, context: Context) {
            startService(Action.PAUSE, timerId, context)
        }

        fun resume(timerId: Int, context: Context) {
            startService(Action.RESUME, timerId, context)
        }

        fun stop(timerId: Int, context: Context) {
            startService(Action.STOP, timerId, context)
        }

        fun restart(timerId: Int, context: Context) {
            startService(Action.RESTART, timerId, context)
        }

        private fun startService(action: Action, timerId: Int, context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(makeIntent(context, action, timerId))
            } else {
                context.startService(makeIntent(context, action, timerId))
            }
        }

        private fun makeIntent(context: Context, action: Action, timerId: Int): Intent {
            return Intent(context, TimerService::class.java).also {
                it.action = action.name
                it.putExtra(PARAM_TIMER_ID, timerId)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Ensure that the notification channel exists
        ensureNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val notification = createNotification(
            "timer service started",
            "onStartCommand ${intent?.action}"
        )
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.d(TAG, "no permission")
        }
        startForegroundCompat(NOTIFICATION_ID_FOREGROUND_SERVICE, notification)

        if (intent != null) {
            val timerId = intent.getIntExtra(PARAM_TIMER_ID, 0)
            check(timerId != 0) { "TimerId unset" }
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

    private fun startForegroundCompat(id: Int, notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(id, notification, FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(id, notification, FOREGROUND_SERVICE_TYPE_NONE)
        } else {
            startForeground(id, notification)
        }
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
        if (timerJob != null) {
            if (currentTimerId != timerId) {
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
            wakeLock?.release()
            wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                WAKE_LOCK_TAG,
            ).also {
                // TODO: Set the timeout to the duration of the workout.
                // TODO: Then, release the wakelock when we pause, and reacquire it if we resume.
                it.acquire(10*60*1000L /*10 minutes*/)
            }
        }
        lifecycleScope.launch {
            timerRunner.timerStateFlow.collect { timerState ->
                val notification = createTimerNotification(
                    timerRunner.workoutFlow.value,
                    timerState
                )

                requestNotificationPermission()
                notificationManager.notify(NOTIFICATION_ID_FOREGROUND_SERVICE, notification)
            }
        }
    }

    private fun requestNotificationPermission() {
        /*
        TODO: I'm not sure how I can do this from a service.
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            //return
        }
         */
    }

    private fun onTimerFinished(timerId: Int) {
        // TODO: The way this works, the service stops when the timer is paused, which means that
        // the notification goes away. It would be better if it remained running, but idle,
        // so that the notification could remain. For this to work properly, we'd also need to
        // persist the timer state so that it could survive app shutdown.
        Log.d(TAG, "onTimerFinished timerId $timerId")
        stopSelf()
        wakeLock?.release()
        wakeLock = null
        timerJob = null
    }

    private var prevNotificationPeriodId: Long? = null
    private fun createTimerNotification(workout: SegmentInterface.Workout?,
                                        timerState: TimerRepository.TimerState): Notification {
        val workoutTitle = workout?.name
        val title = "$workoutTitle in progress"
        val remainDurationStr = timerState.periodRemainMs?.let {
            UIUtils.formatDuration(UIUtils.millisToDurationSeconds(it))
        }
        val content = "${timerState.currentPeriod?.name} $remainDurationStr"
        // TODO: This doesn't work properly. The sound does not play when the period changes,
        // even though we request silent = false
        val silent = (timerState.currentPeriod?.segmentId == prevNotificationPeriodId)
        prevNotificationPeriodId = timerState.currentPeriod?.segmentId

        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder = NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setSilent(silent)
            .setAutoCancel(false)
        return builder.build()
    }

    private fun createNotification(
        title: String,
        content: String,
    ): Notification {
        val pendingIntent: PendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE,
            )
        val builder = NotificationCompat.Builder(this, TIMER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(null)
            .setContentText(null)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
        return builder.build()
    }

    private fun ensureNotificationChannel(): NotificationChannel? {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is not in the Support Library.
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationManager.getNotificationChannel(TIMER_CHANNEL_ID) ?: run {
                val name = getString(R.string.notification_channel_name)
                val description = getString(R.string.notification_channel_description)
                val importance = NotificationManager.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(TIMER_CHANNEL_ID, name, importance).apply {
                    this.description = description
                }
                // Register the channel with the system.
                notificationManager.createNotificationChannel(channel)
                channel
            }
        } else {
            null
        }
    }
}