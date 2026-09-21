package org.mlanau.project.notification.domain.port

import kotlin.jvm.JvmInline

@JvmInline
value class NotificationId(val value: String) {

    /**
     * The id of the [index]th notification of the series based on this one. Index 0 keeps the bare
     * id, so a series silently takes over whatever a single-notification build of the app had
     * already scheduled under it instead of leaving it orphaned.
     */
    fun inSeries(index: Int): NotificationId =
        if (index == 0) this else NotificationId("$value#$index")
}
