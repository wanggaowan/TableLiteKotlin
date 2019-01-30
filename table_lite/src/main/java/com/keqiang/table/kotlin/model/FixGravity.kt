package com.keqiang.table.kotlin.model

import androidx.annotation.IntDef

/**
 * 行列固定位置
 *
 * @author Created by 汪高皖 on 2019/1/17 0017 17:13
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(FixGravity.TOP_ROW, FixGravity.BOTTOM_ROW, FixGravity.LEFT_COLUMN, FixGravity.RIGHT_COLUMN)
annotation class FixGravity {
    companion object {
        /**
         * 行固定在顶部
         */
        const val TOP_ROW = 0

        /**
         * 行固定在底部
         */
        const val BOTTOM_ROW = 1

        /**
         * 列固定在左边
         */
        const val LEFT_COLUMN = 2

        /**
         * 列固定在右边
         */
        const val RIGHT_COLUMN = 3
    }
}
