package com.tamed.music.sources

import android.util.Log

object TrackLog {
    fun d(tag: String, msg: String, about: String? = null) {
        val prefix = if (about != null) "[$about] " else ""
        Log.d(tag, "$prefix$msg")
    }

    fun i(tag: String, msg: String, about: String? = null) {
        val prefix = if (about != null) "[$about] " else ""
        Log.i(tag, "$prefix$msg")
    }

    fun w(tag: String, msg: String, tr: Throwable? = null, about: String? = null) {
        val prefix = if (about != null) "[$about] " else ""
        if (tr != null) Log.w(tag, "$prefix$msg", tr) else Log.w(tag, "$prefix$msg")
    }

    fun e(tag: String, msg: String, tr: Throwable? = null, about: String? = null) {
        val prefix = if (about != null) "[$about] " else ""
        if (tr != null) Log.e(tag, "$prefix$msg", tr) else Log.e(tag, "$prefix$msg")
    }
}
