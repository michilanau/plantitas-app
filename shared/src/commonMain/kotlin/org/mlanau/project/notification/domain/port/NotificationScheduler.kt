package org.mlanau.project.notification.domain.port

interface NotificationScheduler {

    /**
     * How many of a reminder's future occurrences this platform needs registered up front.
     *
     * Where the OS wakes the app as a notification fires, one is enough: that notification
     * schedules the next and the chain builds itself. Where it does not, the whole run has to be
     * pre-registered, and the platform's own cap on pending notifications is what gets shared out
     * between the [activeRuleCount] reminders that want one.
     */
    fun seriesLengthFor(activeRuleCount: Int): Int

    /** Replaces the whole series hanging off the id of [series]' first element. */
    suspend fun schedule(series: List<ScheduledNotification>)

    /** Cancels the whole series hanging off [id], however much of it was registered. */
    fun cancel(id: NotificationId)

    companion object {
        /** The ceiling [seriesLengthFor] may return, and so the range [cancel] has to sweep. */
        const val MAX_SERIES_LENGTH = 8
    }
}
