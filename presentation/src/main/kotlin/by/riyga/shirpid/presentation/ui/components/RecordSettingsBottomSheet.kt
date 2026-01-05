package by.riyga.shirpid.presentation.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import by.riyga.shirpid.presentation.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSettingsBottomSheet(
    sheetState: SheetState = rememberModalBottomSheetState(),
    onChooseDefaultLocation: () -> Unit = {},
    onRemoveRecord: () -> Unit = {},
    onChangeDate: () -> Unit = {},
    onShowMap: () -> Unit = {},
    onDismiss: () -> Unit = {}
) {
    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismiss
    ) {
        Column(
            Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp)
        ) {
            Item(
                icon = painterResource(R.drawable.ic_my_location),
                title = "Текущее местоположение",
                onClick = onChooseDefaultLocation
            )
            Spacer(Modifier.size(8.dp))
            Item(
                icon = painterResource(R.drawable.ic_map_search),
                title = "Выбрать на карте",
                onClick = onShowMap
            )
            Spacer(Modifier.size(8.dp))
            Item(
                icon = painterResource(R.drawable.ic_edit_calendar),
                title = "Редактировать дату",
                onClick = onChangeDate
            )
            Spacer(Modifier.size(8.dp))
            Item(
                icon = painterResource(R.drawable.ic_delete),
                title = "Удалить запись",
                onClick = onRemoveRecord
            )
        }
    }
}

@Composable
private fun Item(
    title: String,
    icon: Painter,
    onClick: () -> Unit = {}
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = icon,
                contentDescription = null
            )
            Text(
                text = title,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    RecordSettingsBottomSheet()
}