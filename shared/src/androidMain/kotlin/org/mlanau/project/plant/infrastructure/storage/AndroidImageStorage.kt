package org.mlanau.project.plant.infrastructure.storage

import android.content.Context
import java.io.File
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mlanau.project.plant.domain.service.ImageStorage

class AndroidImageStorage(private val context: Context) : ImageStorage {

    private val directory: File by lazy {
        File(context.filesDir, "plant_images").apply { mkdirs() }
    }

    override suspend fun save(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val file = File(directory, "${UUID.randomUUID()}.jpg")
        file.writeBytes(bytes)
        file.toURI().toString()
    }

    override suspend fun delete(uri: String) = withContext(Dispatchers.IO) {
        runCatching { File(URI(uri)).delete() }
        Unit
    }
}
