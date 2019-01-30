package com.keqiang.table.kotlin.model

import com.keqiang.table.kotlin.TableConfig

/**
 * 配置单元格数据
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 08:32
 */
open class Cell
@JvmOverloads constructor(
        data: Any?,
        width: Int = TableConfig.INVALID_VALUE,
        height: Int = TableConfig.INVALID_VALUE
) {

    /**
     * 如果当前单元格所在列其它单元格宽度大于此值，则以最大宽度为主，如果希望当前列宽度一致，则需设置整列单元格宽度大小一致.
     * 如果值为[TableConfig.INVALID_VALUE]则表示宽度自适应,自适应宽度需要覆写[measureWidth]，
     * 宽度大小受 [TableConfig.minColumnWidth]和[TableConfig.maxColumnWidth]限制
     * ```
     *     // 绘制时宽度获取逻辑
     *     int actualColumnWidth = 0;
     *     // 省略部分代码
     *     ...
     *
     *     int width = cell.getWidth();
     *     int columnWidth = tableConfig.getColumnWidth();
     *     if (width == TableConfig.INVALID_VALUE && columnWidth == TableConfig.INVALID_VALUE) {
     *         // 自适应单元格列宽
     *         int measureWidth = cell.measureWidth();
     *         if (actualColumnWidth < measureWidth) {
     *             actualColumnWidth = measureWidth;
     *         }
     *     } else if (width != TableConfig.INVALID_VALUE) {
     *         if (actualColumnWidth < width) {
     *             actualColumnWidth = width;
     *         }
     *     } else {
     *         // 单元格自适应但配置了全局列宽，所以使用全局列宽
     *         if (actualColumnWidth < columnWidth) {
     *             actualColumnWidth = columnWidth;
     *         }
     *     }
     *
     *     // 省略部分代码
     *     ...
     * ```
     */
    var width = TableConfig.INVALID_VALUE

    /**
     * 如果当前单元格所在行其它单元格高度大于此值，则以最大高度为主，如果希望当前行高度一致，则需设置整行单元格高度大小一致
     * 当值为[TableConfig.INVALID_VALUE],则表示高度自适应,自适应高度需要覆写[measureHeight]。
     * 高度大小受[TableConfig.minRowHeight]和[TableConfig.maxRowHeight]限制
     * ```
     *     // 绘制时高度获取逻辑
     *     int actualRowHeight = 0;
     *     // 省略部分代码
     *     ...
     *
     *     int cellRowHeight = cell.getHeight();
     *     int rowHeight = tableConfig.getRowHeight();
     *     if (cellRowHeight == TableConfig.INVALID_VALUE && rowHeight == TableConfig.INVALID_VALUE) {
     *         // 自适应单元格行高
     *         int measureHeight = cell.measureHeight();
     *         if (actualRowHeight < measureHeight) {
     *              actualRowHeight = measureHeight;
     *         }
     *     } else if (height != TableConfig.INVALID_VALUE) {
     *         if (actualRowHeight < height) {
     *             actualRowHeight = height;
     *         }
     *     } else {
     *         // 单元格自适应但配置了全局行高，所以使用全局行高
     *         if (actualRowHeight < rowHeight) {
     *             actualRowHeight = rowHeight;
     *         }
     *     }
     *
     *     // 省略部分代码
     *     ...
     *
     * ```
     */
    var height = TableConfig.INVALID_VALUE
    private var data: Any? = null

    init {
        this@Cell.width = width
        this@Cell.height = height
        this@Cell.data = data
    }

    fun <T> getData(): T {
        @Suppress("UNCHECKED_CAST")
        return data as T
    }

    fun setData(data: Any) {
        this.data = data
    }

    /**
     * 测量绘制内容的宽度，大小限制在[TableConfig.minColumnWidth]和[TableConfig.maxColumnWidth]之间
     */
    open fun measureWidth(): Int {
        return 0
    }

    /**
     * 测量绘制内容的高度，大小限制在[TableConfig.minRowHeight]和[TableConfig.maxRowHeight]之间
     */
    open fun measureHeight(): Int {
        return 0
    }
}
