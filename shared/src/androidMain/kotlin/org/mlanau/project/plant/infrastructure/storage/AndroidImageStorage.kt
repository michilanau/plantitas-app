package org.mlanau.project.plant.infrastructure.storage

import android.content.Context
import java.io.File
import java.net.URI
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mlanau.project.plant.domain.port.ImageStorage

class AndroidImageStorage(private val context: Context) : ImageStorage {

    private val directory: File by lazy {
        File(context.filesDir, "plant_images").apply { mkdirs() }
    }

    override suspend fun save(bytes: ByteArray): String = withContext(Dispatchers.IO) {
        val fileName = "${UUID.randomUUID()}.jpg"
        File(directory, fileName).writeBytes(bytes)
        fileName
    }

    // A `file:` URI here means this was saved before save() returned a bare file name — keep
    // resolving those as-is so images from installs already in the wild don't go missing.
    override fun resolve(reference: String): String =
        if (reference.startsWith("file:")) reference else File(directory, reference).toURI().toString()

    override suspend fun delete(reference: String) = withContext(Dispatchers.IO) {
        val file = if (reference.startsWith("file:")) File(URI(reference)) else File(directory, reference)
        runCatching { file.delete() }
        Unit
    }
}
