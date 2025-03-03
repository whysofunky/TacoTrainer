package com.luckyzero.tacotrainer.ui.widgets

import android.util.Log
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.luckyzero.tacotrainer.ui.utils.UIUtils
import com.luckyzero.tacotrainer.ui.utils.removeAll
import java.lang.reflect.Modifier

private const val TAG = "DurationField"

@Composable
fun DurationField(duration: MutableIntState,
                  textStyle: TextStyle = LocalTextStyle.current,
                  label: @Composable (() -> Unit)? = null,
                  placeholder: @Composable (() -> Unit)? = null,
                  leadingIcon: @Composable (() -> Unit)? = null,
                  trailingIcon: @Composable (() -> Unit)? = null,
                  modifier: Modifier = Modifier(),
) {
    val initialText = UIUtils.formatDuration(duration.intValue, UIUtils.DurationElement.NONE)
    val textDuration = remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    TextField(
        value = textDuration.value,
        onValueChange = { newValue ->
            Log.d(TAG, "input value '${newValue.text}', " +
                    "selection ${newValue.selection.start}-${newValue.selection.end}")
            val numbersOnly = newValue.text
                .removeAll(":. ")
                .padStart(6, '0')
            val selectionEnd = newValue.selection.end
            val length = numbersOnly.length
            val seconds = numbersOnly.takeLast(2).toInt()
            val minutes = numbersOnly.take(length-2).takeLast(2).toInt()
            val hours = numbersOnly.take(length-4).takeLast(2).toInt()
            // TODO: It would be nice to retain zeros to the right of the selection.
            val newText = if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else if (minutes > 0) {
                "%d:%02d".format(minutes, seconds)
            } else if (seconds > 0) {
                "%d".format(seconds)
            } else {
                ""
            }
            val outputValue = newValue.copy(text = newText)
            Log.d(TAG, "output value '${outputValue.text}', " +
                    "selection ${outputValue.selection.start}-${outputValue.selection.end}")
            textDuration.value = outputValue
            val durationSeconds = seconds + minutes * 60 + hours * 3600
            duration.intValue = durationSeconds
        },
        singleLine = true,
        keyboardOptions =  KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                textDuration.value = textDuration.value.copy(
                    text = UIUtils.formatDuration(duration.intValue, UIUtils.DurationElement.NONE)
                )
            }
        ),
        textStyle = textStyle.copy(textAlign = TextAlign.End),
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        )
}

@Composable
fun BasicDurationField(
    duration: MutableIntState,
    textStyle: TextStyle = LocalTextStyle.current
) {
    val initialText = UIUtils.formatDuration(duration.intValue, UIUtils.DurationElement.NONE)
    val testState = remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    BasicTextField(
        value = testState.value,
        onValueChange = { newValue ->
            Log.d(TAG, "input value '${newValue.text}', " +
                    "selection ${newValue.selection.start}-${newValue.selection.end}")
            val numbersOnly = newValue.text
                .removeAll(":. ")
                .padStart(6, '0')
            val selectionEnd = newValue.selection.end
            val length = numbersOnly.length
            val seconds = numbersOnly.takeLast(2).toInt()
            val minutes = numbersOnly.take(length-2).takeLast(2).toInt()
            val hours = numbersOnly.take(length-4).takeLast(2).toInt()
            // TODO: It would be nice to retain zeros to the right of the selection.
            val newText = if (hours > 0) {
                "%d:%02d:%02d".format(hours, minutes, seconds)
            } else if (minutes > 0) {
                "%d:%02d".format(minutes, seconds)
            } else if (seconds > 0) {
                "%d".format(seconds)
            } else {
                ""
            }
            val outputValue = newValue.copy(text = newText)
            Log.d(TAG, "output value '${outputValue.text}', " +
                    "selection ${outputValue.selection.start}-${outputValue.selection.end}")
            testState.value = outputValue
            val durationSeconds = seconds + minutes * 60 + hours * 3600
            duration.intValue = durationSeconds
        },
        singleLine = true,
        keyboardOptions =  KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                testState.value = testState.value.copy(
                    text = UIUtils.formatDuration(duration.intValue, UIUtils.DurationElement.NONE)
                )
            }
        ),
        textStyle = textStyle.copy(textAlign = TextAlign.End),
    )
}


// TODO: Add a custom background and/or decorations
@Composable
fun BasicCountField(count: MutableIntState,
                    textStyle: TextStyle = LocalTextStyle.current,
                  ) {
    val textFieldState = remember {
        mutableStateOf(TextFieldValue(count.intValue.toString()))
    }
    BasicTextField(
        value = textFieldState.value,
        onValueChange = { newValue: TextFieldValue ->
            Log.d(TAG, "input value '${newValue.text}', " +
                    "selection ${newValue.selection.start}-${newValue.selection.end}")
            val numbersOnly = newValue.text
                .removeAll(":. ")
                .takeLast(6) // No more than six digits. Prevents integer overflow.
            val selectionEnd = newValue.selection.end
            val newValueInt = if (numbersOnly.isBlank()) 0 else numbersOnly.toInt()
            // TODO: It would be nice to retain zeros to the right of the selection.
            val newText = if (newValueInt > 0) {
                newValueInt.toString()
            } else {
                ""
            }
            val outputValue = newValue.copy(text = newText)
            Log.d(TAG, "output value '${outputValue.text}', " +
                    "selection ${outputValue.selection.start}-${outputValue.selection.end}")
            textFieldState.value = outputValue
            count.intValue = newValueInt
        },
        singleLine = true,
        keyboardOptions =  KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                textFieldState.value = textFieldState.value.copy(
                    text = count.intValue.toString()
                )
            }
        ),
        textStyle = textStyle.copy(textAlign = TextAlign.End),
    )
}

@Composable
fun CountField(count: MutableIntState,
               textStyle: TextStyle = LocalTextStyle.current,
               label: @Composable (() -> Unit)? = null,
               placeholder: @Composable (() -> Unit)? = null,
               leadingIcon: @Composable (() -> Unit)? = null,
               trailingIcon: @Composable (() -> Unit)? = null,
               modifier: Modifier = Modifier()
) {
    val initialText = count.intValue.toString()
    val textFieldState = remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }
    TextField(
        value = textFieldState.value,
        onValueChange = { newValue: TextFieldValue ->
            Log.d(TAG, "input value '${newValue.text}', " +
                    "selection ${newValue.selection.start}-${newValue.selection.end}")
            val numbersOnly = newValue.text
                .removeAll(":. ")
                .takeLast(6) // No more than six digits. Prevents integer overflow.
            val selectionEnd = newValue.selection.end
            val newValueInt = if (numbersOnly.isBlank()) 0 else numbersOnly.toInt()
            // TODO: It would be nice to retain zeros to the right of the selection.
            val newText = if (newValueInt > 0) {
                newValueInt.toString()
            } else {
                ""
            }
            val outputValue = newValue.copy(text = newText)
            Log.d(TAG, "output value '${outputValue.text}', " +
                    "selection ${outputValue.selection.start}-${outputValue.selection.end}")
            textFieldState.value = outputValue
            count.intValue = newValueInt
        },
        singleLine = true,
        keyboardOptions =  KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                textFieldState.value = textFieldState.value.copy(
                    text = count.intValue.toString()
                )
            }
        ),
        textStyle = textStyle.copy(textAlign = TextAlign.End),
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
    )
}
