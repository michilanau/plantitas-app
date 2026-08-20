package org.mlanau.project.plant.infrastructure.storage

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import org.mlanau.project.plant.domain.service.ImageStorage
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.writeToURL

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
        val fileUrl = directory.URLByAppendingPathComponent("${NSUUID().UUIDString()}.jpg")!!
        bytes.usePinned { pinned ->
            val data = NSData.create(bytes = pinned.addressOf(0), length = bytes.size.toULong())
            data.writeToURL(fileUrl, atomically = true)
        }
        return fileUrl.absoluteString!!
    }

    override suspend fun delete(uri: String) {
        NSFileManager.defaultManager.removeItemAtURL(NSURL(string = uri), error = null)
    }
}
