package org.mlanau.project.settings.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.mlanau.project.shared.ui.component.ScreenHeader
import plantitas_app.shared.generated.resources.Res
import plantitas_app.shared.generated.resources.about_description
import plantitas_app.shared.generated.resources.about_version
import plantitas_app.shared.generated.resources.app_name
import plantitas_app.shared.generated.resources.common_back
import plantitas_app.shared.generated.resources.ic_leaf

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ScreenHeader(
                title = stringResource(Res.string.app_name),
                onNavigateBack = onBack,
                backContentDescription = stringResource(Res.string.common_back)
            )
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(104.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_leaf),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Text(stringResource(Res.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(Res.string.about_version),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                Text(
                    stringResource(Res.string.about_description),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
