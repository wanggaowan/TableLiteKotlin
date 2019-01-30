package com.keqiang.table.kotlin.util

import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.model.Column
import com.keqiang.table.kotlin.model.Row

/**
 * 工具类
 * <br></br>create by 汪高皖 on 2019/1/20 17:19
 */
object Utils {

    /**
     * 获取整行实际行高
     *
     * @param row         需要处理的行
     * @param start       行中单元格开始位置
     * @param end         行中单元格结束位置
     * @param tableConfig 表格配置
     * @return 从开始单元格到结束单元格(不包含结束单元格)中所有单元格最大高度
     */
    fun getActualRowHeight(row: Row<*>, start: Int, end: Int, tableConfig: TableConfig): Int {
        if (row.isDragChangeSize) {
            return row.height
        }

        var actualRowHeight = 0

        val cells = row.cells
        for (i in start until end) {
            val cell = cells!![i]
            val height = cell.height
            val rowHeight = tableConfig.rowHeight
            if (height == TableConfig.INVALID_VALUE && rowHeight == TableConfig.INVALID_VALUE) {
                // 自适应单元格行高
                val measureHeight = cell.measureHeight()
                if (actualRowHeight < measureHeight) {
                    actualRowHeight = measureHeight
                }
            } else if (height != TableConfig.INVALID_VALUE) {
                if (actualRowHeight < height) {
                    actualRowHeight = height
                }
            } else {
                // 单元格自适应但配置了全局行高，所以使用全局行高
                if (actualRowHeight < rowHeight) {
                    actualRowHeight = rowHeight
                }
            }
        }

        if (actualRowHeight < tableConfig.minRowHeight) {
            actualRowHeight = tableConfig.minRowHeight
        } else if (tableConfig.maxRowHeight != TableConfig.INVALID_VALUE && actualRowHeight > tableConfig.maxRowHeight) {
            actualRowHeight = tableConfig.maxRowHeight
        }

        return actualRowHeight
    }

    /**
     * 获取整行实际行高
     *
     * @param column      需要处理的列
     * @param start       列中单元格开始位置
     * @param end         列中单元格结束位置
     * @param tableConfig 表格配置
     * @return 从开始单元格到结束单元格(不包含结束单元格)中所有单元格最大宽度
     */
    fun getActualColumnWidth(column: Column<*>, start: Int, end: Int, tableConfig: TableConfig): Int {
        if (column.isDragChangeSize) {
            return column.width
        }

        var actualColumnWidth = 0

        val cells = column.cells
        for (i in start until end) {
            val cell = cells!![i]
            val width = cell.width
            val columnWidth = tableConfig.columnWidth
            if (width == TableConfig.INVALID_VALUE && columnWidth == TableConfig.INVALID_VALUE) {
                // 自适应单元格列宽
                val measureWidth = cell.measureWidth()
                if (actualColumnWidth < measureWidth) {
                    actualColumnWidth = measureWidth
                }
            } else if (width != TableConfig.INVALID_VALUE) {
                if (actualColumnWidth < width) {
                    actualColumnWidth = width
                }
            } else {
                // 单元格自适应但配置了全局列宽，所以使用全局列宽
                if (actualColumnWidth < columnWidth) {
                    actualColumnWidth = columnWidth
                }
            }
        }

        if (actualColumnWidth < tableConfig.minColumnWidth) {
            actualColumnWidth = tableConfig.minColumnWidth
        } else if (tableConfig.maxColumnWidth != TableConfig.INVALID_VALUE && actualColumnWidth > tableConfig.maxColumnWidth) {
            actualColumnWidth = tableConfig.maxColumnWidth
        }

        return actualColumnWidth
    }
}
