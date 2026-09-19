package org.mlanau.project.shared.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Resolves an `ImageStorage` reference (what `Plant.imageUrl` stores) to a URI Coil can load right
 * now — see `ImageStorage.resolve` for why this can't be done once and cached. Defaults to identity
 * so previews don't need to provide it.
 */
val LocalImageResolver = staticCompositionLocalOf<(String) -> String> { { it } }
