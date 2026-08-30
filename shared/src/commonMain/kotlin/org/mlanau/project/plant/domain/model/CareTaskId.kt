package org.mlanau.project.plant.domain.model

import kotlin.jvm.JvmInline
import org.mlanau.project.plant.domain.exception.InvalidIdException

@JvmInline
value class CareTaskId(val value: Int) {
    init {
        if (value <= 0) throw InvalidIdException()
    }
}
