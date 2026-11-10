package com.metrolist.music.pulse

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.constants.AccountChannelHandleKey
import com.metrolist.music.constants.AccountEmailKey
import com.metrolist.music.constants.AccountNameKey
import com.metrolist.music.constants.DataSyncIdKey
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.LastFullSyncKey
import com.metrolist.music.db.InternalDatabase
import com.metrolist.music.utils.SYNC_SNAPSHOT_FILES
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.reconcileMarker
import com.metrolist.music.utils.safeDataStoreEdit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.security.MessageDigest

/**
 * Pulse: each YouTube Music account (and each channel of one) gets its own library on the phone —
 * likes, playlists, downloads list, history and play counts. The database of the account in use
 * lives at the usual place; the others wait in `files/accounts/<key>/` and come back on sign-in.
 * Downloaded audio stays shared in the download cache, so going back to an account is instant.
 *
 * Runs before Room opens the database: a sign-in or channel switch restarts the process, so the
 * swap always happens on a closed database.
 */
object AccountLibrary {
    private const val DIR = "accounts"
    private const val OWNER = "owner"

    internal sealed interface Plan {
        /** Signed out, or the library already belongs to this account. */
        data object Keep : Plan

        /** The library has no owner yet (the first start of this version, or after "clear data"): it becomes this account's, cleaned of what the account does not have. */
        data class Adopt(val key: String) : Plan

        data class Switch(val from: String, val to: String) : Plan
    }

    internal fun plan(owner: String?, current: String?): Plan = when {
        current == null || owner == current -> Plan.Keep
        owner == null -> Plan.Adopt(current)
        else -> Plan.Switch(owner, current)
    }

    /** Email and channel name the account; signed out, null. */
    internal fun accountKey(prefs: Preferences): String? {
        val cookie = prefs[InnerTubeCookieKey].orEmpty()
        if ("SAPISID" !in runCatching { parseCookieString(cookie) }.getOrDefault(emptyMap())) return null
        val dataSyncId = prefs[DataSyncIdKey].orEmpty().substringBefore("||")
        val who = prefs[AccountEmailKey].orEmpty().ifBlank { prefs[AccountChannelHandleKey].orEmpty() }.ifBlank { prefs[AccountNameKey].orEmpty() }
        if (who.isBlank() && dataSyncId.isBlank()) return null
        return keyOf("$who|$dataSyncId")
    }

    internal fun keyOf(identity: String): String =
        MessageDigest.getInstance("SHA-256").digest(identity.lowercase().toByteArray())
            .take(12).joinToString("") { "%02x".format(it) }

    fun prepare(context: Context) {
        runCatching {
            val prefs = runBlocking { context.dataStore.data.first() }
            val root = File(context.filesDir, DIR).apply { mkdirs() }
            val ownerFile = File(root, OWNER)
            val owner = ownerFile.takeIf { it.exists() }?.readText()?.trim()?.ifEmpty { null }
            when (val plan = plan(owner, accountKey(prefs))) {
                Plan.Keep -> Unit
                is Plan.Adopt -> {
                    // Whatever is liked or saved here and not in the account came from another one.
                    SYNC_SNAPSHOT_FILES.forEach { File(context.filesDir, reconcileMarker(it)).createNewFile() }
                    ownerFile.writeText(plan.key)
                    Timber.i("AccountLibrary: library adopted by %s", plan.key)
                }
                is Plan.Switch -> {
                    val parked = File(root, plan.from).apply { mkdirs() }
                    val incoming = File(root, plan.to)
                    move(accountFiles(context), parked)
                    if (incoming.isDirectory) {
                        incoming.listFiles()?.forEach { file -> move(listOf(file), File(destinationFor(context, file.name))) }
                        incoming.delete()
                    }
                    ownerFile.writeText(plan.to)
                    // Sync the new account now, not after the cooldown left by the old one.
                    runBlocking { context.safeDataStoreEdit { it[LastFullSyncKey] = 0L } }
                    Timber.i("AccountLibrary: switched library %s -> %s", plan.from, plan.to)
                }
            }
        }.onFailure { Timber.e(it, "AccountLibrary: could not prepare the account's library") }
    }

    /** "Sign out and clear data": what is left (downloaded songs) goes to whoever signs in next. */
    fun release(context: Context) {
        File(File(context.filesDir, DIR), OWNER).delete()
    }

    private fun accountFiles(context: Context): List<File> {
        val db = context.getDatabasePath(InternalDatabase.DB_NAME)
        return listOf(db, File(db.path + "-wal"), File(db.path + "-shm"), File(db.path + "-journal")) +
            SYNC_SNAPSHOT_FILES.flatMap { listOf(File(context.filesDir, it), File(context.filesDir, reconcileMarker(it))) }
    }

    private fun destinationFor(context: Context, name: String): String =
        if (name.startsWith(InternalDatabase.DB_NAME)) context.getDatabasePath(name).path else File(context.filesDir, name).path

    /** Moves [files] that exist into [target] (a directory, or the exact path for a single file). */
    private fun move(files: List<File>, target: File) {
        for (file in files) {
            if (!file.exists()) continue
            val to = if (target.isDirectory) File(target, file.name) else target
            to.parentFile?.mkdirs()
            to.delete()
            if (!file.renameTo(to)) {
                file.copyTo(to, overwrite = true)
                file.delete()
            }
        }
    }
}
