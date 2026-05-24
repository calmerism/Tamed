/*
 * Tamed Project (2026)
 * Original project contributors
 * Licensed Under GPL-3.0 | see git history for contributors
 */



package com.tamed.music.extensions

import androidx.sqlite.db.SimpleSQLiteQuery
import java.net.InetSocketAddress
import java.net.InetSocketAddress.createUnresolved

inline fun <reified T : Enum<T>> String?.toEnum(defaultValue: T): T =
    if (this == null) {
        defaultValue
    } else {
        try {
            enumValueOf(this)
        } catch (e: IllegalArgumentException) {
            defaultValue
        }
    }

fun String.toSQLiteQuery(): SimpleSQLiteQuery = SimpleSQLiteQuery(this)

fun String.toInetSocketAddress(): InetSocketAddress {
    val (host, port) = split(":")
    return createUnresolved(host, port.toInt())
}

fun String.resize(width: Int, height: Int): String {
    return if (this.contains("googleusercontent.com") || this.contains("ggpht.com")) {
        this.replace(Regex("=w\\d+-h\\d+.*"), "=w$width-h$height-c-k-c0x00ffffff-no-rj")
    } else {
        this
    }
}
