package org.mlanau.project.plant.domain.port

interface ImageStorage {
    /** Persists [bytes] and returns a stable reference — not a directly loadable path, see [resolve]. */
    suspend fun save(bytes: ByteArray): String

    /**
     * Turns a reference returned by [save] into a URI that can be loaded right now. Cheap and
     * synchronous: call it on every read, don't cache the result across app launches — on some
     * platforms the real path changes between launches even though the reference doesn't.
     */
    fun resolve(reference: String): String

    suspend fun delete(reference: String)
}
