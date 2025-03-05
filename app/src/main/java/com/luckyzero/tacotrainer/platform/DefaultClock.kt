package com.luckyzero.tacotrainer.platform

import android.os.SystemClock

interface ClockInterface {
    fun elapsedRealtime(): Long
}

object DefaultClock : ClockInterface {
    override fun elapsedRealtime(): Long {
        return SystemClock.elapsedRealtime()
    }
}