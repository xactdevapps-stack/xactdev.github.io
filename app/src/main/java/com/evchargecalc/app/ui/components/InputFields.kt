package com.evchargecalc.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw -> onValueChange(raw.filter { it.isDigit() || it == '.' }) },
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )
}

@Composable
fun ProfileDropdown(
    title: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String?) -> Unit
) {
    SelectionDropdown(
        title = title,
        selectedText = selectedText,
        options = options,
        onSelect = onSelect,
        includeNoneOption = true,
        noneLabel = "None (ad hoc)"
    )
}

@Composable
fun SelectionDropdown(
    title: String,
    selectedText: String,
    options: List<Pair<String, String>>,
    onSelect: (String?) -> Unit,
    includeNoneOption: Boolean,
    noneLabel: String = "None"
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodySmall)
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
                    .clickable { expanded = true }
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(selectedText, modifier = Modifier.weight(1f))
                Text("v", color = MaterialTheme.colorScheme.primary)
            }

            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (includeNoneOption) {
                    DropdownMenuItem(
                        text = { Text(noneLabel) },
                        onClick = {
                            onSelect(null)
                            expanded = false
                        }
                    )
                }
                options.forEach { (id, label) ->
                    DropdownMenuItem(
                        text = { Text(label) },
                        onClick = {
                            onSelect(id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
