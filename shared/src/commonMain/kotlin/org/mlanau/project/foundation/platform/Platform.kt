package org.mlanau.project.foundation.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
