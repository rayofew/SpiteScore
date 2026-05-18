package com.reznick.spitescore.integration.spitecards

object SchemaVersion {
    const val CURRENT = 1

    fun isCompatible(version: Int): Boolean = version == CURRENT

    fun incompatibleMessage(version: Int): String =
        "This game came from a newer version of SpiteCards (schema v$version). Update SpiteScore to open it."
}
