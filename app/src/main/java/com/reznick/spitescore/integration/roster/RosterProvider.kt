package com.reznick.spitescore.integration.roster

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.reznick.spitescore.data.repository.PlayerRosterRepository

/**
 * Read/write access to the SpiteScore player roster for sibling "Spite" apps (e.g. SpiteYatzy),
 * so the whole line shares one set of players instead of each app keeping its own. Guarded by a
 * signature-level permission in the manifest, so only apps signed with the same key can reach it.
 *
 * The roster is just names, so we expose a single [COLUMN_NAME] column. Delete takes the name as
 * selectionArgs[0]; insert reads it from the values.
 */
class RosterProvider : ContentProvider() {

    private lateinit var repo: PlayerRosterRepository

    override fun onCreate(): Boolean {
        repo = PlayerRosterRepository(context!!.applicationContext)
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor {
        val cursor = MatrixCursor(arrayOf(COLUMN_NAME))
        repo.getPlayers().forEach { cursor.addRow(arrayOf(it)) }
        return cursor
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val name = values?.getAsString(COLUMN_NAME)?.trim().orEmpty()
        if (name.isEmpty()) return null
        repo.addPlayer(name)
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
        return Uri.withAppendedPath(CONTENT_URI, Uri.encode(name))
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val name = selectionArgs?.firstOrNull()?.trim().orEmpty()
        if (name.isEmpty()) return 0
        repo.removePlayer(name)
        context?.contentResolver?.notifyChange(CONTENT_URI, null)
        return 1
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun getType(uri: Uri): String = "vnd.android.cursor.dir/vnd.$AUTHORITY.player"

    companion object {
        const val AUTHORITY = "com.reznick.spitescore.roster"
        const val COLUMN_NAME = "name"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/players")
    }
}
