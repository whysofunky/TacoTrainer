package com.luckyzero.tacotrainer.ui.widgets

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign

private const val TAG = "CountField"

@Composable
fun CountField(count: MutableIntState,
               textStyle: TextStyle = LocalTextStyle.current,
               modifier: Modifier = Modifier,
) {
    val textState = remember {
        mutableStateOf(TextFieldValue(count.intValue.toString()))
    }

    val onValueChange = { newValue: TextFieldValue ->
        Log.d(TAG, "input value '${newValue.text}', " +
                "selection ${newValue.selection.start}-${newValue.selection.end}")
        val numbersOnly = newValue.text
            .filter { it.isDigit() }
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
        textState.value = outputValue
        count.intValue = newValueInt
    }

    BasicTextField(
        value = textState.value,
        onValueChange = onValueChange,
        singleLine = true,
        keyboardOptions =  KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done,
            keyboardType = KeyboardType.Number
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                textState.value = textState.value.copy(
                    text = count.intValue.toString()
                )
            }
        ),
        textStyle = textStyle.copy(textAlign = TextAlign.End),
        decorationBox = { innerTextField ->
            Row(modifier = Modifier.background(color = WidgetCommon.EDIT_BACKGROUND_COLOR)) {
                innerTextField()
            }
        },
        modifier = modifier.onFocusChanged {
            if (it.isFocused) {
                val text = textState.value.text
                textState.value = textState.value.copy(selection = TextRange(0, text.length))
            }
        },
    )
}
