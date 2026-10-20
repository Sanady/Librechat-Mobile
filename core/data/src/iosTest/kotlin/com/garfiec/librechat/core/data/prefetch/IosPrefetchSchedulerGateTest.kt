package com.garfiec.librechat.core.data.prefetch

import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * A test process installs no BGTask launch handlers, which is exactly the state the app was in
 * when 2026.10.6 submitted at launch: before the gate, this call threw
 * `NSInternalInconsistencyException` out of `BGTaskScheduler` and took the process with it.
 */
class IosPrefetchSchedulerGateTest {

    private val identifiers = listOf(IosPrefetchTasks.REFRESH_ID, IosPrefetchTasks.PROCESSING_ID)

    private fun dueKey(identifier: String) = "prefetch_task_due_$identifier"

    @Test
    fun `ensureScheduled submits nothing while the launch handlers are unregistered`() {
        val defaults = NSUserDefaults.standardUserDefaults
        identifiers.forEach { defaults.removeObjectForKey(dueKey(it)) }
        assertFalse(IosPrefetchTasks.launchHandlersRegistered)

        IosPrefetchScheduler().ensureScheduled(allowMetered = true)

        // A skipped submission records no due date, so the next emission starts from scratch.
        identifiers.forEach { assertNull(defaults.objectForKey(dueKey(it))) }
    }
}
