package org.mlanau.project.plant.domain.port

interface ImageStorage {
    suspend fun save(bytes: ByteArray): String

    suspend fun delete(uri: String)
}
