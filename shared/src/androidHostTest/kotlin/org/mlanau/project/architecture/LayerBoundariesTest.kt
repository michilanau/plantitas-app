package org.mlanau.project.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withPackage
import com.lemonappdev.konsist.api.verify.assertFalse
import kotlin.test.Test

/**
 * Enforces the hexagonal layering the codebase is meant to have: domain depends on nothing,
 * application depends only on domain, and infrastructure/presentation are the only layers allowed
 * to depend on platform frameworks. A commit that violates one of these fails a JVM unit test
 * instead of only being caught in review.
 */
class LayerBoundariesTest {

    private val allFiles = Konsist.scopeFromProject()

    @Test
    fun `domain does not depend on infrastructure`() {
        allFiles
            .files
            .withPackage("..domain..")
            .assertFalse { file ->
                file.imports.any { it.name.contains(".infrastructure.") }
            }
    }

    @Test
    fun `domain does not depend on presentation`() {
        allFiles
            .files
            .withPackage("..domain..")
            .assertFalse { file ->
                file.imports.any { it.name.contains(".presentation.") }
            }
    }

    @Test
    fun `domain does not depend on Android, Compose or SQLDelight`() {
        allFiles
            .files
            .withPackage("..domain..")
            .assertFalse { file ->
                file.imports.any {
                    it.name.startsWith("androidx.") ||
                        it.name.startsWith("android.") ||
                        it.name.startsWith("org.jetbrains.compose.") ||
                        it.name.startsWith("app.cash.sqldelight.")
                }
            }
    }

    @Test
    fun `application does not depend on infrastructure`() {
        allFiles
            .files
            .withPackage("..application..")
            .assertFalse { file ->
                file.imports.any { it.name.contains(".infrastructure.") }
            }
    }

    @Test
    fun `application does not depend on presentation`() {
        allFiles
            .files
            .withPackage("..application..")
            .assertFalse { file ->
                file.imports.any { it.name.contains(".presentation.") }
            }
    }

    @Test
    fun `presentation does not depend on infrastructure`() {
        allFiles
            .files
            .withPackage("..presentation..")
            .assertFalse { file ->
                file.imports.any { it.name.contains(".infrastructure.") }
            }
    }
}
