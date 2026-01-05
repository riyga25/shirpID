package by.riyga.shirpid.presentation.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirdDatePickerDialog(
    state: DatePickerState,
    onDismiss: () -> Unit,
    onDatePick: (Long?) -> Unit
) {
    DatePickerDialog(
        onDismissRequest = {
            onDismiss.invoke()
        },
        confirmButton = {
            TextButton(
                onClick = { onDatePick.invoke(state.selectedDateMillis) },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onDismiss.invoke() }
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        DatePicker(
            state = state,
            showModeToggle = false
        )
    }
}