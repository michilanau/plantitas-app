package org.mlanau.project.shared.platform

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
