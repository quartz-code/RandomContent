package com.metrolist.music.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginTransferTest {
    private val session = LoginTransfer.Session(cookie = "SAPISID=abc; SID=def", visitorData = "Cgt", dataSyncId = "123", authUser = "1", accountName = "Quartz")

    @Test
    fun sealedSessionOpensWithTheSameCodeInsideAForwardedMessage() {
        val code = LoginTransfer.newPairingCode()
        val sealed = LoginTransfer.seal(session, code, nowMs = 1_000)
        assertTrue(sealed.startsWith(LoginTransfer.PREFIX))
        assertTrue("the cookie must not be readable", "SAPISID" !in sealed)
        val forwarded = "Pulse: вход на компьютере\n$sealed\n"
        val opened = LoginTransfer.open(forwarded, code.lowercase().replace("-", " "), nowMs = 60_000)
        assertEquals(LoginTransfer.Opened.Ok(session.copy(createdAtMs = 1_000)), opened)
    }

    @Test
    fun wrongCodeOldMessageAndOtherTextAreRefused() {
        val sealed = LoginTransfer.seal(session, "K7QP-9XMB", nowMs = 0)
        assertEquals(LoginTransfer.Opened.WrongCode, LoginTransfer.open(sealed, "K7QP-9XMC", nowMs = 0))
        assertEquals(LoginTransfer.Opened.Expired, LoginTransfer.open(sealed, "K7QP-9XMB", nowMs = LoginTransfer.MAX_AGE_MS + 1))
        assertEquals(LoginTransfer.Opened.NotFound, LoginTransfer.open("просто текст", "K7QP-9XMB"))
        assertEquals(LoginTransfer.Opened.WrongCode, LoginTransfer.open(sealed.dropLast(4), "K7QP-9XMB", nowMs = 0))
    }

    @Test
    fun theVkSignInTravelsAlongAndOldMessagesStillOpen() {
        val withVk = session.copy(vkUserId = 42, vkCookies = "remixsid=xyz; remixlang=0")
        val sealed = LoginTransfer.seal(withVk, "K7QP-9XMB", nowMs = 0)
        assertTrue("remixsid" !in sealed)
        assertEquals(LoginTransfer.Opened.Ok(withVk), LoginTransfer.open(sealed, "K7QP-9XMB", nowMs = 0))
        // A message without VK, as older versions make it, opens with no VK session in it.
        val plain = LoginTransfer.open(LoginTransfer.seal(session, "K7QP-9XMB", nowMs = 0), "K7QP-9XMB", nowMs = 0) as LoginTransfer.Opened.Ok
        assertEquals(0L, plain.session.vkUserId)
        assertEquals("", plain.session.vkCookies)
    }

    @Test
    fun codesAreEightUnambiguousCharacters() {
        repeat(50) {
            val code = LoginTransfer.newPairingCode()
            assertEquals(9, code.length)
            assertNotNull(LoginTransfer.normalizeCode(code))
            assertTrue(code.none { it in "01OIL" })
        }
        assertNull(LoginTransfer.normalizeCode("K7QP-9XM"))
        assertNull(LoginTransfer.normalizeCode("K7QP-9XM0"))
        assertEquals("K7QP9XMB", LoginTransfer.normalizeCode(" k7qp 9xmb "))
    }
}
