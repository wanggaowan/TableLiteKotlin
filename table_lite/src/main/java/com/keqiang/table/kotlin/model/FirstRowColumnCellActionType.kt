package com.keqiang.table.kotlin.model

import androidx.annotation.IntDef

/**
 * 第一行第一列的单元格处理高亮，拖拽改变列宽行高的响应逻辑
 *
 * @author Created by 汪高皖 on 2019/1/17 0017 17:13
 */
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(
    FirstRowColumnCellActionType.BOTH,
    FirstRowColumnCellActionType.ROW,
    FirstRowColumnCellActionType.COLUMN,
    FirstRowColumnCellActionType.NONE
)
annotation class FirstRowColumnCellActionType {
    companion object {
        /**
         * 同时处理行列逻辑
         */
        const val BOTH: Int = 0

        /**
         * 仅处理行逻辑
         */
        const val ROW: Int = 1

        /**
         * 仅处理列逻辑
         */
        const val COLUMN: Int = 2

        /**
         * 都不处理
         */
        const val NONE: Int = 3
    }
}
