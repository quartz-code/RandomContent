package com.metrolist.music.pulse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AccountLibraryTest {
    @Test
    fun signedOutOrTheSameAccountKeepsTheLibrary() {
        assertEquals(AccountLibrary.Plan.Keep, AccountLibrary.plan(owner = "a", current = null))
        assertEquals(AccountLibrary.Plan.Keep, AccountLibrary.plan(owner = null, current = null))
        assertEquals(AccountLibrary.Plan.Keep, AccountLibrary.plan(owner = "a", current = "a"))
    }

    @Test
    fun anotherAccountGetsItsOwnLibrary() {
        assertEquals(AccountLibrary.Plan.Switch("a", "b"), AccountLibrary.plan(owner = "a", current = "b"))
        assertEquals(AccountLibrary.Plan.Adopt("b"), AccountLibrary.plan(owner = null, current = "b"))
    }

    @Test
    fun keysTellAccountsAndChannelsApart() {
        val key = AccountLibrary.keyOf("me@gmail.com|channel1")
        assertEquals(key, AccountLibrary.keyOf("ME@gmail.com|channel1"))
        assertNotEquals(key, AccountLibrary.keyOf("me@gmail.com|channel2"))
        assertNotEquals(key, AccountLibrary.keyOf("other@gmail.com|channel1"))
        assertEquals(24, key.length)
    }
}
