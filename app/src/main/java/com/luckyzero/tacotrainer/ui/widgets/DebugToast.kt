package com.luckyzero.tacotrainer.ui.widgets

import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable

fun debugToast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}