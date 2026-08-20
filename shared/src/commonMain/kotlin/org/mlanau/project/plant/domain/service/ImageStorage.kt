package org.mlanau.project.plant.domain.service

/**
 * Persists plant photos as files, so the database only stores a small URI instead of the raw image
 * bytes inline. Plants used to store their photo as a `data:image/...;base64,...` URL directly in
 * the `imageUrl` column, which bloated every row and meant every plant's full photo had to be
 * decoded just to render the list.
 */
interface ImageStorage {
    /** Writes [bytes] to a new file and returns a URI usable both as an image-loader model (e.g.
     * Coil's `AsyncImage`) and as the argument to a later [delete] call. */
    suspend fun save(bytes: ByteArray): String

    /** Removes the file behind [uri]. Safe to call on a URI this storage didn't create (e.g. one
     * already removed) — it's a no-op rather than an error, since callers use it for best-effort
     * cleanup (an old photo being replaced, or a plant being deleted). */
    suspend fun delete(uri: String)
}
