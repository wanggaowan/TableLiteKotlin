package com.keqiang.table.kotlin

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.keqiang.table.R
import com.keqiang.table.kotlin.model.DragChangeSizeType
import com.keqiang.table.kotlin.model.FirstRowColumnCellActionType
import com.keqiang.table.kotlin.model.FixGravity
import java.util.*

/**
 * 表格配置类
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 10:16
 */
class TableConfig {

    /**
     * 最小行高
     */
    var minRowHeight: Int = 100

    /**
     * 最大行高，如果值为[INVALID_VALUE]则无限制
     */
    var maxRowHeight: Int = INVALID_VALUE

    /**
     * 最小列宽
     */
    var minColumnWidth: Int = 200

    /**
     * 最大列宽，如果值为[INVALID_VALUE]则无限制
     */
    var maxColumnWidth: Int = INVALID_VALUE

    /**
     * 全局行高，限制在[minRowHeight]和[maxRowHeight]之间,
     * 如果值为[INVALID_VALUE],则高度根据该行内容自适应,
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
     * ```
     */
    var rowHeight: Int = INVALID_VALUE

    /**
     * 全局列宽，限制在[minColumnWidth]和[maxColumnWidth]之间。
     * 如果值为[INVALID_VALUE],则宽度根据该列内容自适应
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
    var columnWidth: Int = INVALID_VALUE

    /**
     * 记录固定在顶部的行
     */
    private val mRowTopFix: MutableSet<Int>

    /**
     * 记录固定在底部的行
     */
    private val mRowBottomFix: MutableSet<Int>

    /**
     * 记录固定在左边的列
     */
    private val mColumnLeftFix: MutableSet<Int>

    /**
     * 记录固定在右边的列
     */
    private val mColumnRightFix: MutableSet<Int>

    /**
     * 是否高亮显示选中行，当且仅当点击第一列内容才会高亮显示整行
     */
    var isHighLightSelectRow: Boolean = false

    /**
     * 是否高亮显示选中列，当且仅当点击第一行内容才会高亮显示整列
     */
    var isHighLightSelectColumn: Boolean = false

    /**
     * 第一行第一列单元格点击时高亮处理方式,取值参考[FirstRowColumnCellActionType]
     */
    @FirstRowColumnCellActionType
    var firstRowColumnCellHighLightType: Int = FirstRowColumnCellActionType.BOTH

    /**
     * 高亮时，覆盖在行或列上的颜色，如果不设置透明度，则内容将会被高亮颜色遮挡
     */
    @ColorInt
    var highLightColor: Int = 0x20438CFF // 蓝色，透明度20

    /**
     * 拖拽行改变行高类型,当且仅当拖拽第一列单元格才会改变行高
     */
    @DragChangeSizeType
    var rowDragChangeHeightType: Int = DragChangeSizeType.LONG_PRESS

    /**
     * 拖拽列改变列宽类型,当且仅当拖拽第一行单元格才会改变列宽
     */
    @DragChangeSizeType
    var columnDragChangeWidthType: Int = DragChangeSizeType.LONG_PRESS

    /**
     * 第一行第一列单元格拖拽时列宽行高处理方式，取值参考[FirstRowColumnCellActionType]
     */
    @FirstRowColumnCellActionType
    var firstRowColumnCellDragType: Int = FirstRowColumnCellActionType.BOTH

    /**
     * 拖拽改变行高或列宽时，覆盖在行或列上的颜色，如果不设置透明度，则内容将会被高亮颜色遮挡
     */
    @ColorInt
    var dragHighLightColor: Int = 0x206BD98D // 绿色，透明度20

    /**
     * 拖拽改变列宽或行高后是否需要恢复之前高亮行或列，如果为`false`，则拖拽结束后取消高亮内容,默认值为`true`
     */
    var needRecoveryHighLightOnDragChangeSizeEnded: Boolean = true

    /**
     * 拖拽改变行高列宽时是否绘制指示器
     */
    var isEnableDragIndicator: Boolean = true

    /**
     * 行改变行高时的指示器图片资源Id，默认为[R.drawable.top_bottom],绘制位置垂直居中
     */
    @DrawableRes
    var rowDragIndicatorRes: Int = R.drawable.top_bottom

    /**
     * 行改变行高时的指示器绘制大小，如果为[INVALID_VALUE]，则取[R.dimen.drag_image_size]
     */
    var rowDragIndicatorSize: Int = INVALID_VALUE

    /**
     * 行高指示器离单元格左侧的偏移值，如果为[INVALID_VALUE]，则取[R.dimen.row_drag_image_horizontal_offset]
     */
    var rowDragIndicatorHorizontalOffset: Int = INVALID_VALUE

    /**
     * 列改变列宽时的指示器图片资源Id，默认为[R.drawable.left_right]，绘制位置水平居中
     */
    @DrawableRes
    var columnDragIndicatorRes: Int = R.drawable.left_right

    /**
     * 列改变列宽时的指示器绘制大小，如果为[INVALID_VALUE]，则取[R.dimen.drag_image_size]
     */
    var columnDragIndicatorSize: Int = INVALID_VALUE

    /**
     * 列宽指示器离单元格顶部的偏移值,如果为[INVALID_VALUE]，则取[R.dimen.column_drag_image_vertical_offset]
     */
    var columnDragIndicatorVerticalOffset: Int = INVALID_VALUE

    /**
     * 第一行第一列同时改变列宽行高指示器图片资源Id，默认为[R.drawable.left_right]，绘制位置左上角
     */
    @DrawableRes
    var firstRowColumnDragIndicatorRes: Int = R.drawable.diagonal_angle

    /**
     * 第一行第一列同时改变列宽行高指示器大小，如果为[INVALID_VALUE]，则取[R.dimen.drag_image_size]
     */
    var firstRowColumnDragIndicatorSize: Int = INVALID_VALUE

    /**
     * 第一行第一列同时改变列宽行高指示器离单元格顶部的偏移值,如果为[INVALID_VALUE]，则取[R.dimen.first_row_column_drag_image_vertical_offset]
     */
    var firstRowColumnDragIndicatorVerticalOffset: Int = INVALID_VALUE

    /**
     * 第一行第一列同时改变列宽行高指示器离单元格左边的偏移值,如果为[INVALID_VALUE]，则取[R.dimen.first_row_column_drag_image_horizontal_offset]
     */
    var firstRowColumnDragIndicatorHorizontalOffset: Int = INVALID_VALUE

    /**
     * @return 固定在顶部的行下标，只读
     */
    val rowTopFix: Set<Int>
        get() = mRowTopFix

    /**
     * @return 固定在底部的行下标，只读
     */
    val rowBottomFix: Set<Int>
        get() = mRowBottomFix

    /**
     * @return 固定在左边的列下标，只读
     */
    val columnLeftFix: Set<Int>
        get() = mColumnLeftFix

    /**
     * @return 固定在右边的列，只读
     */
    val columnRightFix: Set<Int>
        get() = mColumnRightFix

    init {
        mRowTopFix = HashSet()
        mRowBottomFix = HashSet()
        mColumnLeftFix = HashSet()
        mColumnRightFix = HashSet()
    }

    /**
     * 设置固定行,同一行以第一次设置固定位置为主，也就是更新行的固定位置时需要先删除之前设置的内容再设置新内容
     *
     * @param row        行位置
     * @param fixGravity 固定位置，值：[FixGravity.TOP_ROW]、[FixGravity.BOTTOM_ROW]，
     * 默认[FixGravity.TOP_ROW]
     */
    fun addRowFix(row: Int, @FixGravity fixGravity: Int) {
        if (fixGravity == FixGravity.BOTTOM_ROW) {
            if (!mRowTopFix.contains(row)) {
                mRowBottomFix.add(row)
            }
        } else if (!mRowBottomFix.contains(row)) {
            mRowTopFix.add(row)
        }
    }

    /**
     * 移除行固定
     *
     * @param row 行位置
     */
    fun removeRowFix(row: Int, @FixGravity fixGravity: Int) {
        if (fixGravity == FixGravity.BOTTOM_ROW) {
            mRowBottomFix.remove(row)
        } else {
            mRowTopFix.remove(row)
        }
    }

    /**
     * 清除所有顶部或底部固定行
     */
    fun clearRowFix(@FixGravity fixGravity: Int) {
        if (fixGravity == FixGravity.BOTTOM_ROW) {
            mRowBottomFix.clear()
        } else {
            mRowTopFix.clear()
        }
    }

    /**
     * 清除所有行固定
     */
    fun clearRowFix() {
        mRowBottomFix.clear()
        mRowTopFix.clear()
    }

    /**
     * 设置固定列,同一列以第一次设置固定位置为主，也就是更新列的固定位置时需要先删除之前设置的内容再设置新内容
     *
     * @param column     列位置
     * @param fixGravity 固定位置，值：[FixGravity.LEFT_COLUMN]、[FixGravity.RIGHT_COLUMN],
     * 默认[FixGravity.LEFT_COLUMN]
     */
    fun addColumnFix(column: Int, @FixGravity fixGravity: Int) {
        if (fixGravity == FixGravity.RIGHT_COLUMN) {
            if (!mColumnLeftFix.contains(column)) {
                mColumnRightFix.add(column)
            }
        } else if (!mColumnRightFix.contains(column)) {
            mColumnLeftFix.add(column)
        }
    }

    /**
     * 移除列固定
     *
     * @param column 列位置
     */
    fun removeColumnFix(column: Int, @FixGravity fixGravity: Int) {
        if (fixGravity == FixGravity.RIGHT_COLUMN) {
            mColumnRightFix.remove(column)
        } else {
            mColumnLeftFix.remove(column)
        }
    }

    /**
     * 清除所有列固定
     */
    fun clearColumnFix(@FixGravity fixGravity: Int) {
        if (fixGravity == FixGravity.RIGHT_COLUMN) {
            mColumnRightFix.clear()
        } else {
            mColumnLeftFix.clear()
        }
    }

    /**
     * 清除所有列固定
     */
    fun clearColumnFix() {
        mColumnRightFix.clear()
        mColumnLeftFix.clear()
    }

    companion object {
        /**
         * 表示无效或没有设置过值
         */
        const val INVALID_VALUE: Int = -1
    }
}
