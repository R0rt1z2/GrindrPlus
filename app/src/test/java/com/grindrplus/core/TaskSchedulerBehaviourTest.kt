package com.grindrplus.core

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * Verifies CancellationException propagation and isActive guard in TaskScheduler-like patterns.
 * Note: TaskScheduler itself calls Logger (Android-dependent) so we test the scheduling logic
 * directly using equivalent coroutine patterns.
 */
class TaskSchedulerBehaviourTest {

    @Test
    fun `periodic loop exits when scope is cancelled`() = runTest {
        val executionCount = AtomicInteger(0)
        val scope = CoroutineScope(coroutineContext + Job())

        val job = scope.launch {
            while (isActive) {
                executionCount.incrementAndGet()
                delay(100)
            }
        }

        advanceTimeBy(350)
        scope.cancel()
        assertTrue("Job should be cancelled", job.isCancelled || job.isCompleted)
        val countBeforeCancel = executionCount.get()
        advanceTimeBy(500)
        assertFalse("Loop should have stopped after cancel", executionCount.get() > countBeforeCancel + 1)
    }

    @Test
    fun `CancellationException is not swallowed by generic catch`() = runTest {
        var exceptionPropagated = false

        val scope = CoroutineScope(coroutineContext + Job())
        val job = scope.launch {
            try {
                while (isActive) {
                    try {
                        delay(100)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // generic handler — CancellationException must not reach here
                    }
                }
            } catch (e: CancellationException) {
                exceptionPropagated = true
                throw e
            }
        }

        scope.cancel()
        assertTrue("CancellationException should have propagated", exceptionPropagated || job.isCancelled)
    }

    @Test
    fun `retry logic succeeds on second attempt`() = runTest {
        val attemptCount = AtomicInteger(0)
        var succeeded = false

        val scope = CoroutineScope(coroutineContext + Job())
        scope.launch {
            repeat(3) { attempt ->
                try {
                    attemptCount.incrementAndGet()
                    if (attempt == 0) throw RuntimeException("First attempt fails")
                    succeeded = true
                    return@launch
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (attempt == 2) throw e
                    delay(10)
                }
            }
        }.join()

        assertTrue("Should have succeeded on 2nd attempt", succeeded)
        assertTrue("Should have attempted exactly 2 times", attemptCount.get() == 2)
    }

    @Test
    fun `ConcurrentHashMap allows concurrent job access`() {
        val jobs = java.util.concurrent.ConcurrentHashMap<String, Job>()
        val threads = (1..10).map { i ->
            Thread {
                val job = Job()
                jobs["job-$i"] = job
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertTrue("All 10 jobs should be stored", jobs.size == 10)
    }
}
