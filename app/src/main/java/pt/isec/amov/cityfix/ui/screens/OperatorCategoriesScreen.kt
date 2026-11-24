package pt.isec.amov.cityfix.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.isec.amov.cityfix.R
import pt.isec.amov.cityfix.model.Category
import pt.isec.amov.cityfix.viewmodel.OperatorCategoriesUiState

@Composable
fun OperatorCategoriesScreen(
    state: OperatorCategoriesUiState,
    onRefresh: () -> Unit,
    onSaveCategory: (String?, String) -> Unit,
    onSetCategoryActive: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditorOpen by rememberSaveable { mutableStateOf(false) }
    var editedCategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryTitle by rememberSaveable { mutableStateOf("") }
    var versionAtEditorOpen by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(state.mutationVersion) {
        if (isEditorOpen && state.mutationVersion > versionAtEditorOpen) {
            isEditorOpen = false
            editedCategoryId = null
            categoryTitle = ""
        }
    }

    if (isEditorOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!state.isSaving) {
                    isEditorOpen = false
                    editedCategoryId = null
                }
            },
            title = { Text(stringResource(if (editedCategoryId == null) R.string.categories_add else R.string.categories_edit)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = categoryTitle,
                        onValueChange = { categoryTitle = it },
                        label = { Text(stringResource(R.string.categories_name_label)) },
                        singleLine = true,
                        enabled = !state.isSaving,
                        isError = state.errorMessage != null,
                        supportingText = state.errorMessage?.let { message ->
                            { Text(message, color = MaterialTheme.colorScheme.error) }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    enabled = categoryTitle.isNotBlank() && !state.isSaving,
                    onClick = { onSaveCategory(editedCategoryId, categoryTitle) }
                ) {
                    if (state.isSaving) CircularProgressIndicator()
                    else Text(stringResource(R.string.btn_save))
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !state.isSaving,
                    onClick = {
                        isEditorOpen = false
                        editedCategoryId = null
                    }
                ) { Text(stringResource(R.string.btn_cancel)) }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.categories_problem_title),
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onRefresh, enabled = !state.isLoading && !state.isSaving) {
                Text(stringResource(R.string.btn_refresh), maxLines = 1, softWrap = false)
            }
        }

        state.errorMessage?.takeIf { !isEditorOpen }?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Button(
            enabled = !state.isSaving,
            onClick = {
                editedCategoryId = null
                categoryTitle = ""
                versionAtEditorOpen = state.mutationVersion
                isEditorOpen = true
            }
        ) { Text(stringResource(R.string.categories_add)) }

        when {
            state.isLoading -> CircularProgressIndicator()
            state.categories.isEmpty() -> Text(stringResource(R.string.categories_empty))
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 320.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.categories, key = Category::id) { category ->
                    CategoryRow(
                        category = category,
                        isSaving = state.isSaving,
                        onEdit = {
                            editedCategoryId = category.id
                            categoryTitle = category.title
                            versionAtEditorOpen = state.mutationVersion
                            isEditorOpen = true
                        },
                        onActiveChange = { active -> onSetCategoryActive(category.id, active) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    isSaving: Boolean,
    onEdit: () -> Unit,
    onActiveChange: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(category.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(if (category.active) R.string.categories_active else R.string.categories_inactive),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onEdit, enabled = !isSaving) {
                Text(stringResource(R.string.btn_edit))
            }
            Switch(
                checked = category.active,
                onCheckedChange = onActiveChange,
                enabled = !isSaving
            )
        }
    }
}
