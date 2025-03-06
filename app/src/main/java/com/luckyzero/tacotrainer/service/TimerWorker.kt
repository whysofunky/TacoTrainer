package com.luckyzero.tacotrainer.service

import android.app.NotificationManager
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters

// https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running

// Intermediate progress
// https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/observe
/*
class TimerWorker(
    context: Context,
    workerParams: WorkerParameters,
    ) : CoroutineWorker(context, workerParams) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as
                NotificationManager

    override suspend fun doWork(): Result {

        val inputUrl = inputData.getString(KEY_INPUT_URL)
            ?: return Result.failure()
        val outputFile = inputData.getString(KEY_OUTPUT_FILE_NAME)
            ?: return Result.failure()
        // Mark the Worker as important
        val progress = "Starting Download"
        setForeground(createForegroundInfo(progress))
        download(inputUrl, outputFile)
        return Result.success()
    }
} */
