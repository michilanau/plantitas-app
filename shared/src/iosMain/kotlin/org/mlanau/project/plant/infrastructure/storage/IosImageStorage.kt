package org.mlanau.project.plant.infrastructure.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.mlanau.project.plant.domain.port.ImageStorage
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

/**
 * Saves under a bare file name rather than the full `file://.../Documents/...` URL: the app
 * container's UUID in that path changes on every reinstall/update, so a URL saved today would be
 * dangling by the next sideload refresh. [resolve] rebuilds the current URL from the file name on
 * every read instead of baking a URL into storage.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosImageStorage : ImageStorage {

    private val directory: NSURL by lazy {
        val documents = NSFileManager.defaultManager
            .URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            .first() as NSURL
        val dir = documents.URLByAppendingPathComponent("plant_images")!!
        NSFileManager.defaultManager.createDirectoryAtURL(
            url = dir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        dir
    }

    override suspend fun save(bytes: ByteArray): String {
        val fileName = "${NSUUID().UUIDString()}.jpg"
        val fileUrl = directory.URLByAppendingPathComponent(fileName)!!
        val data = if (bytes.isEmpty()) {
            NSData()
        } else {
            bytes.usePinned { pinned -> NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong()) }
        }
        data.writeToURL(fileUrl, atomically = true)
        return fileName
    }

    // A `file:` URI here means this was saved before save() returned a bare file name — keep
    // resolving those as-is so images from installs already in the wild don't go missing.
    override fun resolve(reference: String): String =
        if (reference.startsWith("file:")) reference
        else directory.URLByAppendingPathComponent(reference)!!.absoluteString!!

    override suspend fun delete(reference: String) {
        val url = if (reference.startsWith("file:")) {
            NSURL(string = reference)
        } else {
            directory.URLByAppendingPathComponent(reference)
        }
        NSFileManager.defaultManager.removeItemAtURL(url!!, error = null)
    }
}
