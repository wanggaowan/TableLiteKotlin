package com.keqiang.table.kotlin.model

import com.keqiang.table.kotlin.TableConfig
import java.util.*

/**
 * 处理表格行相关数据
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 08:30
 */
class Row<T : Cell> {
    /**
     * 行高
     */
    var height: Int = TableConfig.INVALID_VALUE

    /**
     * 是否拖拽改变了行高
     *
     * `true`则列宽自适应取消，始终根据拖拽改变后的列宽显示
     */
    var isDragChangeSize: Boolean = false

    var cells: MutableList<T> = ArrayList()
        private set
}
