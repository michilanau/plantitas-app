package org.mlanau.project.plant.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    HomeContent(
        uiState = PlantMocks.homeUiStateSuccess,
        onSavePlant = { _, _, _ -> },
        onClearError = {}
    )
}

@Preview(showBackground = true)
@Composable
fun HomeContentLoadingPreview() {
    HomeContent(
        uiState = PlantMocks.homeUiStateLoading,
        onSavePlant = { _, _, _ -> },
        onClearError = {}
    )
}
