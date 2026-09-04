package com.shary.app.viewmodels.authentication

import java.util.concurrent.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AuthenticationSessionTransitionTest {

    @Test
    fun successfulTransitionClearsPreviousSessionDataOnce() = runBlocking {
        var clearCount = 0

        val result = runSessionReplacingAuthTransition(
            clearSessionData = { clearCount++ },
            transition = { Result.success("signed-in") }
        )

        assertEquals("signed-in", result.getOrThrow())
        assertEquals(1, clearCount)
    }

    @Test
    fun failedTransitionClearsBeforeAndAfterAttempt() = runBlocking {
        var clearCount = 0
        val failure = IllegalStateException("invalid credentials")

        val result = runSessionReplacingAuthTransition<String>(
            clearSessionData = { clearCount++ },
            transition = { Result.failure(failure) }
        )

        assertSame(failure, result.exceptionOrNull())
        assertEquals(2, clearCount)
    }

    @Test
    fun thrownTransitionFailureBecomesResultAndClearsAgain() = runBlocking {
        var clearCount = 0
        val failure = IllegalStateException("transport failed")

        val result = runSessionReplacingAuthTransition<String>(
            clearSessionData = { clearCount++ },
            transition = { throw failure }
        )

        assertSame(failure, result.exceptionOrNull())
        assertEquals(2, clearCount)
    }

    @Test
    fun cancellationClearsAgainAndPropagates() {
        var clearCount = 0

        try {
            runBlocking {
                runSessionReplacingAuthTransition<String>(
                    clearSessionData = { clearCount++ },
                    transition = { throw CancellationException("cancelled") }
                )
            }
            throw AssertionError("CancellationException was not propagated")
        } catch (_: CancellationException) {
            // Expected.
        }

        assertEquals(2, clearCount)
    }
}
