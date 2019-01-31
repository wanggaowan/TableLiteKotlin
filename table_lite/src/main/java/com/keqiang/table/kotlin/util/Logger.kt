package com.keqiang.table.kotlin.util

import android.util.Log

import com.keqiang.table.BuildConfig


/**
 * 日志工具
 */
@Suppress("unused", "MemberVisibilityCanBePrivate")
object Logger {
    private var isLogEnable = BuildConfig.DEBUG

    private var tag = Logger::class.java.simpleName

    fun debug(isEnable: Boolean) {
        debug(tag, isEnable)
    }

    fun debug(logTag: String, isEnable: Boolean) {
        tag = logTag
        isLogEnable = isEnable
    }

    fun v(msg: String) {
        v(tag, msg)
    }

    fun v(tag: String, msg: String) {
        v(isLogEnable, tag, msg)
    }

    fun v(isLogEnable: Boolean, tag: String, msg: String) {
        if (isLogEnable) {
            Log.v(tag, msg)
        }
    }

    fun d(msg: String) {
        d(tag, msg)
    }

    fun d(tag: String, msg: String) {
        d(isLogEnable, tag, msg)
    }

    fun d(isLogEnable: Boolean, tag: String, msg: String) {
        if (isLogEnable) {
            Log.d(tag, msg)
        }
    }

    fun i(msg: String) {
        i(tag, msg)
    }

    fun i(tag: String, msg: String) {
        i(isLogEnable, tag, msg)
    }

    fun i(isLogEnable: Boolean, tag: String, msg: String) {
        if (isLogEnable) {
            Log.i(tag, msg)
        }
    }

    fun w(msg: String) {
        w(tag, msg)
    }

    fun w(tag: String, msg: String) {
        w(isLogEnable, tag, msg)
    }

    fun w(isLogEnable: Boolean, tag: String, msg: String) {
        if (isLogEnable) {
            Log.w(tag, msg)
        }
    }

    fun e(msg: String) {
        e(tag, msg)
    }

    fun e(tag: String, msg: String) {
        e(isLogEnable, tag, msg)
    }

    fun e(isLogEnable: Boolean, tag: String, msg: String) {
        if (isLogEnable) {
            Log.e(tag, msg)
        }
    }

    fun printStackTrace(t: Throwable) {
        printStackTrace(tag, t)
    }

    fun printStackTrace(tag: String, t: Throwable) {
        printStackTrace(isLogEnable, tag, t)
    }

    fun printStackTrace(isLogEnable: Boolean, tag: String, t: Throwable) {
        e(isLogEnable, tag, Log.getStackTraceString(t))
    }
}
