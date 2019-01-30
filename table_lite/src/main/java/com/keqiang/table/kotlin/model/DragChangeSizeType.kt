package com.keqiang.table.kotlin.model

import androidx.annotation.IntDef

/**
 * 拖拽改变行高或列宽类型
 *
 * @author Created by 汪高皖 on 2019/1/17 0017 17:13
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@kotlin.annotation.Retention(AnnotationRetention.SOURCE)
@IntDef(DragChangeSizeType.NONE, DragChangeSizeType.CLICK, DragChangeSizeType.LONG_PRESS)
annotation class DragChangeSizeType {
    companion object {
        /**
         * 不能改变行高或列宽
         */
        const val NONE = 0

        /**
         * 点击后拖拽即可实现行高列宽的改变
         */
        const val CLICK = 1

        /**
         * 长按后拖拽即可实现行高列宽的改变
         */
        const val LONG_PRESS = 2
    }
}
