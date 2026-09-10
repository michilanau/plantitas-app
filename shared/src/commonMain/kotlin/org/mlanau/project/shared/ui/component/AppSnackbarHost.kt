package org.mlanau.project.shared.ui.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.mlanau.project.shared.ui.theme.ScreenGutter

@Composable
fun AppSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            modifier = Modifier.padding(horizontal = ScreenGutter, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            containerColor = colors.inverseSurface,
            contentColor = colors.inverseOnSurface,
            actionColor = colors.inversePrimary,
            actionContentColor = colors.inversePrimary
        )
    }
}
