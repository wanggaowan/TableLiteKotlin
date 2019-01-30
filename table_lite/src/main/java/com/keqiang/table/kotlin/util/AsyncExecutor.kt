package com.keqiang.table.kotlin.util

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * 异步线程控制类
 *
 * @author 汪高皖
 */
object AsyncExecutor {
    val isShutdown: Boolean
        get() = mExecutor!!.isShutdown

    private val CPU_COUNT = Runtime.getRuntime().availableProcessors()
    private val MAX_CACHE_POOL_SIZE = 2 * CPU_COUNT + 1
    private var mExecutor: ExecutorService? = null

    init {
        mExecutor = ThreadPoolExecutor(
                0,
                MAX_CACHE_POOL_SIZE,
                60L, TimeUnit.SECONDS,
                SynchronousQueue(),
                DefaultThreadFactory("table-lite"),
                ThreadPoolExecutor.CallerRunsPolicy())
    }

    fun execute(runnable: Runnable) {
        if (isShutdown) {
            mExecutor = ThreadPoolExecutor(
                    0,
                    MAX_CACHE_POOL_SIZE,
                    60L, TimeUnit.SECONDS,
                    SynchronousQueue(),
                    DefaultThreadFactory("table-lite"),
                    ThreadPoolExecutor.CallerRunsPolicy())
        }

        mExecutor!!.execute(runnable)
    }

    fun shutdown() {
        mExecutor!!.shutdown()
    }

    fun shutdownNow(): List<Runnable> {
        return mExecutor!!.shutdownNow()
    }

    private class DefaultThreadFactory internal constructor(poolName: String) : ThreadFactory {
        private val group: ThreadGroup
        private val threadNumber = AtomicInteger(1)
        private val namePrefix: String

        init {
            val s = System.getSecurityManager()
            group = if (s != null)
                s.threadGroup
            else
                Thread.currentThread().threadGroup
            namePrefix = poolName + " pool-" +
                    poolNumber.getAndIncrement() +
                    "-thread-"
        }

        override fun newThread(r: Runnable): Thread {
            val t = Thread(group, r,
                    namePrefix + threadNumber.getAndIncrement(),
                    0)
            if (t.isDaemon)
                t.isDaemon = false
            if (t.priority != Thread.NORM_PRIORITY)
                t.priority = Thread.NORM_PRIORITY
            return t
        }

        companion object {
            private val poolNumber = AtomicInteger(1)
        }
    }
}


