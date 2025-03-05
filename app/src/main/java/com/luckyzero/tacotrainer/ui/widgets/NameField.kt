package com.luckyzero.tacotrainer.ui.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.sp

private const val TAG = "NameField"

@Composable
fun NameField(
    name: MutableState<String>,
    textStyle: TextStyle = LocalTextStyle.current,
    modifier: Modifier = Modifier,
) {
    val initialText = name.value
    val textState = remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length)
            )
        )
    }

    BasicTextField(
        value = textState.value,
        onValueChange = { newValue: TextFieldValue ->
            textState.value = newValue
            name.value = newValue.text
        },
        singleLine = true,
        textStyle = textStyle.copy(fontSize = 20.sp),
        keyboardOptions = KeyboardOptions.Default.copy(
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {}
        ),
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
