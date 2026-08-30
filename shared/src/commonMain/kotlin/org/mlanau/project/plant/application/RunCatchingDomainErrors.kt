package org.mlanau.project.plant.application

import kotlinx.coroutines.CancellationException
import org.mlanau.project.plant.domain.exception.DomainException

/**
 * Like `runCatching`, but only catches [DomainException] — the failures a use case can legitimately
 * report as part of normal operation (e.g. validation). Everything else propagates instead of being
 * silently absorbed into a [Result], which is what plain `runCatching` would do:
 * - [CancellationException] must never be swallowed, or coroutine cancellation stops propagating.
 * - An unexpected exception (a real bug) should crash loudly, not get flattened into a generic
 *   "something went wrong" [Result.failure] that a caller may not even be checking.
 */
inline fun <T> runCatchingDomainErrors(block: () -> T): Result<T> {
    return try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: DomainException) {
        Result.failure(e)
    }
}
