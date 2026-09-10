package org.mlanau.project.plant.presentation.history

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.plant.presentation.UiError
import org.mlanau.project.plant.presentation.component.CareHistoryRow
import org.mlanau.project.plant.presentation.localizedMessage
import org.mlanau.project.shared.ui.component.AppSnackbarHost
import org.mlanau.project.shared.ui.component.ScreenHeader
import org.mlanau.project.shared.ui.theme.ContentMaxWidth
import org.mlanau.project.shared.ui.theme.ScreenGutter
import plantitas_app.shared.generated.resources.*

/** Every logged care of one plant, newest first — what the detail's short history leads to. */
@Composable
fun PlantHistoryScreen(
    viewModel: PlantHistoryViewModel,
    plantId: Int,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(plantId) {
        viewModel.loadHistory(plantId)
    }

    LaunchedEffect(uiState.error) {
        if (uiState.error == UiError.PlantNotFound) {
            viewModel.clearError()
            onBack()
        }
    }

    uiState.error?.takeIf { it != UiError.PlantNotFound }?.let { error ->
        val errorMessage = error.localizedMessage()
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearError()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { AppSnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(modifier = Modifier.fillMaxSize().widthIn(max = ContentMaxWidth)) {
                ScreenHeader(
                    title = stringResource(Res.string.care_history_title),
                    onNavigateBack = onBack,
                    backContentDescription = stringResource(Res.string.common_back)
                )

                if (uiState.isLoading) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = ScreenGutter, end = ScreenGutter, bottom = 32.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.plantName?.let { name ->
                            item {
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                )
                            }
                        }
                        if (uiState.history.isEmpty()) {
                            item {
                                Text(
                                    text = stringResource(Res.string.care_history_empty),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            items(uiState.history, key = { task -> task.id?.value ?: task.hashCode() }) { task ->
                                CareHistoryRow(task = task, onUndo = { viewModel.onUndoTask(task) })
                            }
                        }
                    }
                }
            }
        }
    }
}
