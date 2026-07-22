package org.mlanau.project

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel
import org.mlanau.project.application.HomeViewModel
import org.mlanau.project.ui.home.HomeScreen

@Composable
fun App() {
    MaterialTheme {
        val viewModel = koinViewModel<HomeViewModel>()
        HomeScreen(viewModel)
    }
}
