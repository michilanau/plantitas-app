package org.mlanau.project.plant.application

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.first
import org.mlanau.project.plant.domain.repository.PlantRepository
import org.mlanau.project.plant.domain.service.ImageStorage

/**
 * One-time data migration: plants used to store their photo as a `data:image/...;base64,...` URL
 * directly in the database. This converts any that are still in that shape to a file written
 * through [ImageStorage], rewriting the row to point at it instead. Idempotent — plants already
 * pointing at a file are left untouched — so it's safe to run on every app start rather than
 * needing a one-shot flag.
 */
class MigrateBase64PlantImages(
    private val plantRepository: PlantRepository,
    private val imageStorage: ImageStorage
) {
    @OptIn(ExperimentalEncodingApi::class)
    suspend operator fun invoke() {
        val plants = plantRepository.findAll().first()
        for (plant in plants) {
            val imageUrl = plant.imageUrl ?: continue
            if (!imageUrl.startsWith(DATA_URL_PREFIX)) continue

            val base64Payload = imageUrl.substringAfter(BASE64_MARKER, missingDelimiterValue = "")
            if (base64Payload.isEmpty()) continue

            val bytes = runCatching { Base64.decode(base64Payload) }.getOrNull() ?: continue
            val newImageUrl = imageStorage.save(bytes)
            plantRepository.save(plant.copy(imageUrl = newImageUrl))
        }
    }

    private companion object {
        const val DATA_URL_PREFIX = "data:"
        const val BASE64_MARKER = "base64,"
    }
}
