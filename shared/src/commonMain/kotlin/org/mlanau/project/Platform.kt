package org.mlanau.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform