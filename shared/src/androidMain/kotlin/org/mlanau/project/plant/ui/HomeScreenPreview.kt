package org.mlanau.project.plant.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview(showBackground = true)
@Composable
fun HomeContentPreview() {
    HomeContent(
        uiState = PlantMocks.homeUiStateSuccess,
        onClearError = {},
        onNavigateToPlantForm = {}
    )
}

@Preview(showBackground = true)
@Composable
fun HomeContentLoadingPreview() {
    HomeContent(
        uiState = PlantMocks.homeUiStateLoading,
        onClearError = {},
        onNavigateToPlantForm = {}
    )
}
