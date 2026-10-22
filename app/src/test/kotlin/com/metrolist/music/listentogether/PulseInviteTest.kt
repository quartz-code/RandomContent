package com.metrolist.music.listentogether

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PulseInviteTest {
    @Test
    fun codeFromThePulseInvite() {
        val ru = "Заходи слушать вместе в Pulse 🎧\nОткрой Together → Войти, код комнаты: K7Q2M8XA\n\nСкопируй это сообщение — Pulse сам подставит код."
        val en = "Join me in Pulse and let's listen together 🎧\nOpen Together → Join, room code: K7Q2M8XA\n\nCopy this message."
        assertEquals("K7Q2M8XA", PulseInvite.extractCode(ru))
        assertEquals("K7Q2M8XA", PulseInvite.extractCode(en))
    }

    @Test
    fun codeFromMetrolistLinksAndBareCodes() {
        assertEquals("AB12CD34", PulseInvite.extractCode("https://metrolist.cc/listen?code=ab12cd34"))
        assertEquals("AB12CD34", PulseInvite.extractCode("  AB12CD34 "))
        assertEquals("AB12CD34", PulseInvite.extractCode("код AB12CD34"))
    }

    @Test
    fun ordinaryClipboardTextIsLeftAlone() {
        assertNull(PulseInvite.extractCode(null))
        assertNull(PulseInvite.extractCode("https://music.youtube.com/watch?v=dQw4w9WgXcQ"))
        assertNull(PulseInvite.extractCode("Кино — Группа крови"))
        assertNull(PulseInvite.extractCode("купи хлеб и молоко"))
        assertNull(PulseInvite.extractCode("код от домофона 1234"))
    }
}

class PulseServerWakeTest {
    @org.junit.Test
    fun healthPageIsNextToTheSocket() {
        org.junit.Assert.assertEquals("https://pulse-together.onrender.com/health", PulseServerWake.healthUrl("wss://pulse-together.onrender.com/ws"))
        org.junit.Assert.assertEquals("http://192.168.1.5:8080/health", PulseServerWake.healthUrl(" ws://192.168.1.5:8080/ws "))
        org.junit.Assert.assertNull(PulseServerWake.healthUrl("https://example.com"))
        org.junit.Assert.assertNull(PulseServerWake.healthUrl("wss://"))
    }
}
