package com.luckyzero.tacotrainer.platform

import android.os.Looper

fun isMainThread() : Boolean {
    return Thread.currentThread() == Looper.getMainLooper().thread
}

fun assertMainThread() {
    if (!isMainThread()) {
        error("Should be in main thread")
    }
}

fun assertNotMainThread() {
    if (isMainThread()) {
        error("Should not be in main thread")
    }
}

