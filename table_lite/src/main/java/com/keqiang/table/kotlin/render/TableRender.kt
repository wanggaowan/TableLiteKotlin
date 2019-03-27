@file:Suppress("MemberVisibilityCanBePrivate")

package com.keqiang.table.kotlin.render

import android.graphics.*
import com.keqiang.table.R
import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.DragChangeSizeType
import com.keqiang.table.kotlin.model.ShowCell
import java.util.*

/**
 * 确定单元格位置，固定行列逻辑
 * <br></br>create by 汪高皖 on 2019/1/19 15:00
 */
open class TableRender<T : Cell>(protected val mTable: ITable<T>) {

    /**
     * 表格实际大小
     */
    val actualSizeRect: Rect
        get() {
            if (mTempActualSizeRect == null) {
                mTempActualSizeRect = Rect()
            }
            mTempActualSizeRect!!.set(mActualSizeRect)
            return mTempActualSizeRect as Rect
        }

    /**
     * 界面可见范围被绘制出来的单元格, 只读(但这只能限制集合, 对于集合元素ShowCell无效, 因此建议不要修改ShowCell内容, 否则可能导致绘制, 点击等出现非预期错误)。
     *
     * 单元格在集合中位置优先级：行列均固定 > 行固定 > 列固定 > 行列均不固定，这也是绘制的优先级
     */
    val showCells: List<ShowCell>
        get() = mShowCells

    /**
     * 用于裁剪画布
     */
    protected var mClipRect: Rect = Rect()

    /**
     * 表格实际大小
     */
    protected var mActualSizeRect: Rect = Rect()

    /**
     * 用于返回真实表格大小，防止外部修改mActualSizeRect
     */
    protected var mTempActualSizeRect: Rect? = null

    /**
     * 用于临时排序行列固定数据
     */
    protected var mTempFix: MutableList<Int> = ArrayList()

    /**
     * 固定行列数据升序排序
     */
    protected var mFixAscComparator: (Int, Int) -> Int = { o1: Int, o2: Int ->
        when {
            o1 < o2 -> -1
            o1 == o2 -> 0
            else -> 1
        }
    }

    /**
     * 固定行列数据降序排序
     */
    protected var mFixDescComparator: (Int, Int) -> Int = { o1: Int, o2: Int ->
        when {
            o1 < o2 -> 1
            o1 == o2 -> 0
            else -> -1
        }
    }

    /**
     * 界面可见的单元格数据
     */
    private val mShowCells: MutableList<ShowCell>

    /**
     * 绘制蒙层Paint
     */
    private var mMaskPaint: Paint? = null

    /**
     * 行改变行高时的指示器
     */
    private var mRowDragBitmap: Bitmap? = null

    /**
     * 列改变列宽时的指示器
     */
    private var mColumnDragBitmap: Bitmap? = null

    /**
     * 行列宽高同时改变的指示器
     */
    private var mRowColumnDragBitmap: Bitmap? = null

    init {
        mShowCells = ArrayList()
    }

    /**
     * 本次绘制是否生效
     *
     * @return `false`没有执行绘制操作，`true`以重新绘制
     */
    fun draw(canvas: Canvas): Boolean {
        val tableData = mTable.tableData
        val showRect = mTable.showRect

        val totalRow = tableData.totalRow
        val totalColumn = tableData.totalColumn
        if (totalRow == 0 || totalColumn == 0 || showRect.width() == 0 || showRect.height() == 0) {
            return false
        }

        ShowCell.recycleInstances(mShowCells)
        mShowCells.clear()
        statisticsTableActualSize()

        val fixTopRowHeight = drawRowFixTop(canvas)
        val fixBottomRowHeight = drawRowFixBottom(canvas, fixTopRowHeight)
        if (fixTopRowHeight + fixBottomRowHeight >= showRect.height()) {
            return true
        }

        val fixLeftColumnWidth = drawColumnFixLeft(canvas, fixTopRowHeight, fixBottomRowHeight)
        val fixRightColumnWidth =
            drawColumnFixRight(canvas, fixTopRowHeight, fixBottomRowHeight, fixLeftColumnWidth)
        if (fixLeftColumnWidth + fixRightColumnWidth >= showRect.width()) {
            return true
        }

        drawNoneFixCell(
            canvas,
            fixLeftColumnWidth,
            fixTopRowHeight,
            fixRightColumnWidth,
            fixBottomRowHeight
        )
        return true
    }

    /**
     * 统计表格实际的大小
     */
    protected fun statisticsTableActualSize() {
        val tableData = mTable.tableData
        val rows = tableData.rows
        val columns = tableData.columns

        var totalRowHeight = 0
        var totalColumnWidth = 0
        for (row in rows) {
            totalRowHeight += row.height
        }

        for (column in columns) {
            totalColumnWidth += column.width
        }


        mActualSizeRect.set(0, 0, totalColumnWidth, totalRowHeight)
    }

    /**
     * 绘制无需固定的单元格
     */
    protected fun drawNoneFixCell(
        canvas: Canvas,
        fixLeftColumnWidth: Int,
        fixTopRowHeight: Int,
        fixRightColumnWidth: Int,
        fixBottomRowHeight: Int
    ) {
        val showRect = mTable.showRect
        mClipRect.set(
            fixLeftColumnWidth,
            fixTopRowHeight,
            showRect.width() - fixRightColumnWidth,
            showRect.height() - fixBottomRowHeight
        )

        canvas.save()
        canvas.clipRect(mClipRect)

        val rows = mTable.tableData.rows
        val columns = mTable.tableData.columns
        var top = -mTable.touchHelper.scrollY
        val iCellDraw = mTable.cellDraw

        for (i in rows.indices) {
            if (top >= showRect.height() - fixBottomRowHeight) {
                break
            }

            val row = rows[i]

            if (top + row.height <= fixTopRowHeight) {
                top += row.height
                continue
            }

            var left = -mTable.touchHelper.scrollX
            for (j in columns.indices) {
                if (left >= showRect.width() - fixRightColumnWidth) {
                    break
                }

                val column = columns[j]
                if (left + column.width <= fixLeftColumnWidth) {
                    left += column.width
                    continue
                }

                canvas.save()

                mClipRect.set(left, top, column.width + left, row.height + top)
                canvas.clipRect(mClipRect)

                mShowCells.add(
                    ShowCell.getInstance(
                        i, j, mClipRect,
                        fixRow = false,
                        fixColumn = false
                    )
                )
                iCellDraw?.onCellDraw(mTable, canvas, row.cells[j], mClipRect, i, j)
                drawMask(canvas, mClipRect, i, j)

                canvas.restore()
                left += column.width
            }

            top += row.height
        }

        canvas.restore()
    }

    /**
     * 绘制固定在顶部的行
     *
     * @return 固定在顶部的行总高度
     */
    protected fun drawRowFixTop(canvas: Canvas): Int {
        val tableConfig = mTable.tableConfig
        if (tableConfig.rowTopFix.isEmpty()
            && !tableConfig.isHighLightSelectColumn
            && tableConfig.columnDragChangeWidthType == DragChangeSizeType.NONE
        ) {
            return 0
        }

        mTempFix.clear()
        if (tableConfig.rowTopFix.isNotEmpty()) {
            mTempFix.addAll(tableConfig.rowTopFix)
            Collections.sort(mTempFix, mFixAscComparator)
        }

        if (tableConfig.isHighLightSelectColumn || tableConfig.columnDragChangeWidthType != DragChangeSizeType.NONE) {
            if (!mTempFix.contains(0)) {
                mTempFix.add(0, 0)
            }
        }

        val showWidth = mTable.showRect.width()
        val rows = mTable.tableData.rows

        var fixTopRowHeight = 0
        // 离table顶部的距离
        var preTop = -mTable.touchHelper.scrollY
        var preStart = 0
        for (i in mTempFix.indices) {
            val tempRowTopFix = mTempFix[i]

            for (j in preStart until tempRowTopFix) {
                preTop += rows[j].height
            }

            if (preTop >= fixTopRowHeight) {
                // 如果固定行顶部离Table顶部高度比之前已经固定的行高度综总和大，则无需固定
                break
            }

            preStart = tempRowTopFix
            val row = rows[tempRowTopFix]
            val bottom = fixTopRowHeight + row.height

            val fixLeftColumnWidth =
                drawColumnFixLeftForFixRow(canvas, tempRowTopFix, fixTopRowHeight, bottom)
            val fixRightColumnWidth = drawColumnFixRightForFixRow(
                canvas,
                tempRowTopFix,
                fixTopRowHeight,
                bottom,
                fixLeftColumnWidth
            )
            if (fixLeftColumnWidth + fixRightColumnWidth >= showWidth) {
                fixTopRowHeight += row.height
                continue
            }

            drawNoneFixCellForFixRow(
                canvas,
                tempRowTopFix,
                fixTopRowHeight,
                fixTopRowHeight + row.height,
                fixLeftColumnWidth,
                fixRightColumnWidth
            )
            fixTopRowHeight += row.height
        }

        return fixTopRowHeight
    }

    /**
     * 绘制固定在底部的行
     *
     * @return 固定在底部的行总高度
     */
    protected fun drawRowFixBottom(canvas: Canvas, fixRowTopHeight: Int): Int {
        val showHeight = mTable.showRect.height()
        val actualSizeRect = mTable.actualSizeRect
        val tableConfig = mTable.tableConfig

        if (tableConfig.rowBottomFix.isEmpty() || actualSizeRect.height() <= showHeight) {
            return 0
        }

        mTempFix.clear()
        mTempFix.addAll(tableConfig.rowBottomFix)
        Collections.sort(mTempFix, mFixDescComparator)

        val rows = mTable.tableData.rows
        val showWidth = mTable.showRect.width()

        var fixBottomRowHeight = 0
        // 离table底部的距离
        var preBottom = mTable.touchHelper.scrollY
        var preStart = rows.size - 1
        for (i in mTempFix.indices) {
            val tempRowBottomFix = mTempFix[i]

            for (j in preStart downTo tempRowBottomFix + 1) {
                preBottom += rows[j].height
            }

            if (actualSizeRect.height() - preBottom <= showHeight || showHeight - fixBottomRowHeight <= fixRowTopHeight) {
                // 如果行底部没有超出屏幕或固定后底部高度比固定在顶部的行的总行高小则无需固定
                break
            }

            preStart = tempRowBottomFix
            val row = rows[tempRowBottomFix]
            val bottom = showHeight - fixBottomRowHeight
            val top = bottom - row.height

            var clipRow = false
            if (bottom - row.height < fixRowTopHeight) {
                clipRow = true
                canvas.save()
                mClipRect.set(0, fixRowTopHeight, showWidth, bottom)
                canvas.clipRect(mClipRect)
            }

            val fixLeftColumnWidth =
                drawColumnFixLeftForFixRow(canvas, tempRowBottomFix, top, bottom)
            val fixRightColumnWidth = drawColumnFixRightForFixRow(
                canvas,
                tempRowBottomFix,
                top,
                bottom,
                fixLeftColumnWidth
            )
            if (fixLeftColumnWidth + fixRightColumnWidth >= showWidth) {
                fixBottomRowHeight += row.height
                continue
            }

            drawNoneFixCellForFixRow(
                canvas,
                tempRowBottomFix,
                top,
                bottom,
                fixLeftColumnWidth,
                fixRightColumnWidth
            )
            fixBottomRowHeight += row.height

            if (clipRow) {
                canvas.restore()
            }
        }

        return fixBottomRowHeight
    }

    /**
     * 绘制固定在左边的列
     *
     * @return 固定在左边的列总宽度
     */
    protected fun drawColumnFixLeft(
        canvas: Canvas,
        fixTopRowHeight: Int,
        fixBottomRowHeight: Int
    ): Int {
        val showHeight = mTable.showRect.height()
        val tableConfig = mTable.tableConfig

        if (fixTopRowHeight + fixBottomRowHeight >= showHeight
            || tableConfig.columnLeftFix.isEmpty()
        ) {
            return 0
        }

        mTempFix.clear()
        if (tableConfig.columnLeftFix.isNotEmpty()) {
            mTempFix.addAll(tableConfig.columnLeftFix)
            Collections.sort(mTempFix, mFixAscComparator)
        }

        val iCellDraw = mTable.cellDraw
        val rows = mTable.tableData.rows
        val columns = mTable.tableData.columns

        var fixLeftColumnWidth = 0
        // 离table左边的距离
        var preWidth = -mTable.touchHelper.scrollX
        var preStart = 0
        for (i in mTempFix.indices) {
            val tempColumnLeftFix = mTempFix[i]

            for (j in preStart until tempColumnLeftFix) {
                preWidth += columns[j].width
            }

            if (preWidth >= fixLeftColumnWidth) {
                // 如果固定列左边离Table左边宽度比之前已经固定的列宽度综总和大，则无需固定
                break
            }

            preStart = tempColumnLeftFix
            val column = columns[tempColumnLeftFix]
            val right = fixLeftColumnWidth + column.width

            mClipRect.set(
                fixLeftColumnWidth,
                fixTopRowHeight,
                right,
                showHeight - fixBottomRowHeight
            )
            canvas.save()
            canvas.clipRect(mClipRect)

            var top = -mTable.touchHelper.scrollY
            for (j in rows.indices) {
                val row = rows[j]

                if (top >= showHeight - fixBottomRowHeight) {
                    break
                }

                if (top + row.height <= fixTopRowHeight) {
                    top += row.height
                    continue
                }

                mClipRect.set(fixLeftColumnWidth, top, right, top + row.height)
                canvas.save()
                canvas.clipRect(mClipRect)

                mShowCells.add(
                    ShowCell.getInstance(
                        j, tempColumnLeftFix, mClipRect,
                        fixRow = false,
                        fixColumn = true
                    )
                )
                iCellDraw?.onCellDraw(
                    mTable,
                    canvas,
                    row.cells[tempColumnLeftFix],
                    mClipRect,
                    j,
                    tempColumnLeftFix
                )
                drawMask(canvas, mClipRect, j, tempColumnLeftFix)

                canvas.restore()
                top += row.height
            }
            fixLeftColumnWidth += column.width

            canvas.restore()
        }

        return fixLeftColumnWidth
    }

    /**
     * 绘制固定在右边的列
     *
     * @return 固定在右边的列的总宽度
     */
    protected fun drawColumnFixRight(
        canvas: Canvas,
        fixTopRowHeight: Int,
        fixBottomRowHeight: Int,
        fixLeftColumnWidth: Int
    ): Int {
        val showWidth = mTable.showRect.width()
        val showHeight = mTable.showRect.height()
        val actualWidth = mTable.actualSizeRect.width()
        val tableConfig = mTable.tableConfig

        if (tableConfig.columnRightFix.isEmpty() || actualWidth <= showWidth || fixTopRowHeight + fixBottomRowHeight >= showHeight) {
            return 0
        }

        mTempFix.clear()
        mTempFix.addAll(tableConfig.columnRightFix)
        Collections.sort(mTempFix, mFixDescComparator)

        val iCellDraw = mTable.cellDraw
        val rows = mTable.tableData.rows
        val columns = mTable.tableData.columns

        var fixRightColumnWidth = 0
        // 离table右边的距离
        var preRight = mTable.touchHelper.scrollX
        var preStart = columns.size - 1
        for (i in mTempFix.indices) {
            val tempColumnRightFix = mTempFix[i]

            for (j in preStart downTo tempColumnRightFix + 1) {
                preRight += columns[j].width
            }

            if (actualWidth - preRight <= showWidth || showWidth - fixRightColumnWidth <= fixLeftColumnWidth) {
                // 如果列底左边没有超出屏幕或固定后右边宽度比固定在左边的列的总宽度小则无需固定
                break
            }

            preStart = tempColumnRightFix
            val column = columns[tempColumnRightFix]
            val right = showWidth - fixRightColumnWidth
            val left = right - column.width

            canvas.save()
            mClipRect.set(left, fixTopRowHeight, right, showHeight - fixBottomRowHeight)
            canvas.clipRect(mClipRect)

            var top = -mTable.touchHelper.scrollY
            for (j in rows.indices) {
                val row = rows[j]

                if (top >= showHeight - fixBottomRowHeight) {
                    break
                }

                if (top + row.height <= fixTopRowHeight) {
                    top += row.height
                    continue
                }

                mClipRect.set(left, top, right, row.height + top)
                canvas.save()
                canvas.clipRect(mClipRect)

                mShowCells.add(
                    ShowCell.getInstance(
                        j, tempColumnRightFix, mClipRect,
                        fixRow = false,
                        fixColumn = true
                    )
                )
                iCellDraw?.onCellDraw(
                    mTable,
                    canvas,
                    row.cells[tempColumnRightFix],
                    mClipRect,
                    j,
                    tempColumnRightFix
                )
                drawMask(canvas, mClipRect, j, tempColumnRightFix)

                canvas.restore()
                top += row.height
            }
            fixRightColumnWidth += column.width

            canvas.restore()
        }

        return fixRightColumnWidth
    }

    /**
     * 绘制固定行左右滑动时固定在左边的列
     *
     * @param fixRow 固定行的下标
     * @param top    固定行顶部位置
     * @param bottom 固定行底部位置
     * @return 固定在左边的列总宽度
     */
    protected fun drawColumnFixLeftForFixRow(
        canvas: Canvas,
        fixRow: Int,
        top: Int,
        bottom: Int
    ): Int {
        val tableConfig = mTable.tableConfig
        if (tableConfig.columnLeftFix.isEmpty()) {
            return 0
        }

        mTempFix.clear()
        if (tableConfig.columnLeftFix.isNotEmpty()) {
            mTempFix.addAll(tableConfig.columnLeftFix)
            Collections.sort(mTempFix, mFixAscComparator)
        }

        val iCellDraw = mTable.cellDraw
        val columns = mTable.tableData.columns

        var fixLeftColumnWidth = 0
        // 离table左边的距离
        var preWidth = -mTable.touchHelper.scrollX
        var preStart = 0
        for (i in mTempFix.indices) {
            val tempColumnLeftFix = mTempFix[i]

            for (j in preStart until tempColumnLeftFix) {
                preWidth += columns[j].width
            }

            if (preWidth >= fixLeftColumnWidth) {
                // 如果固定列左边离Table左边宽度比之前已经固定的列宽度综总和大，则无需固定
                break
            }

            preStart = tempColumnLeftFix
            val column = columns[tempColumnLeftFix]
            val right = fixLeftColumnWidth + column.width

            mClipRect.set(fixLeftColumnWidth, top, right, bottom)
            canvas.save()
            canvas.clipRect(mClipRect)

            mShowCells.add(
                ShowCell.getInstance(
                    fixRow, tempColumnLeftFix, mClipRect,
                    fixRow = true,
                    fixColumn = true
                )
            )
            iCellDraw?.onCellDraw(
                mTable,
                canvas,
                column.cells[fixRow],
                mClipRect,
                fixRow,
                tempColumnLeftFix
            )
            drawMask(canvas, mClipRect, fixRow, tempColumnLeftFix)

            canvas.restore()
            fixLeftColumnWidth += column.width
        }

        return fixLeftColumnWidth
    }

    /**
     * 绘制固定行左右滑动时固定在右边的列
     *
     * @param fixRow             固定行的下标
     * @param top                固定行顶部位置
     * @param bottom             固定行底部位置
     * @param fixLeftColumnWidth 固定在左边的列的宽度
     * @return 固定在右边的列总宽度
     */
    protected fun drawColumnFixRightForFixRow(
        canvas: Canvas,
        fixRow: Int,
        top: Int,
        bottom: Int,
        fixLeftColumnWidth: Int
    ): Int {
        val showWidth = mTable.showRect.width()
        val actualWidth = mTable.actualSizeRect.width()
        val tableConfig = mTable.tableConfig

        if (tableConfig.columnRightFix.isEmpty() || actualWidth <= showWidth) {
            return 0
        }

        mTempFix.clear()
        mTempFix.addAll(tableConfig.columnRightFix)
        Collections.sort(mTempFix, mFixDescComparator)

        val iCellDraw = mTable.cellDraw
        val columns = mTable.tableData.columns

        var fixRightColumnWidth = 0
        // 离table右边的距离
        var preRight = mTable.touchHelper.scrollX
        var preStart = columns.size - 1
        for (i in mTempFix.indices) {
            val tempColumnRightFix = mTempFix[i]

            for (j in preStart downTo tempColumnRightFix + 1) {
                preRight += columns[j].width
            }

            if (actualWidth - preRight <= showWidth || showWidth - fixRightColumnWidth <= fixLeftColumnWidth) {
                // 如果列底左边没有超出屏幕或固定后右边宽度比固定在左边的列的总宽度小则无需固定
                break
            }

            preStart = tempColumnRightFix
            val column = columns[tempColumnRightFix]
            val right = showWidth - fixRightColumnWidth
            val left = right - column.width

            mClipRect.set(left, top, right, bottom)
            canvas.save()
            canvas.clipRect(mClipRect)

            mShowCells.add(
                ShowCell.getInstance(
                    fixRow, tempColumnRightFix, mClipRect,
                    fixRow = true,
                    fixColumn = true
                )
            )
            iCellDraw?.onCellDraw(
                mTable,
                canvas,
                column.cells[fixRow],
                mClipRect,
                fixRow,
                tempColumnRightFix
            )
            drawMask(canvas, mClipRect, fixRow, tempColumnRightFix)

            canvas.restore()
            fixRightColumnWidth += column.width
        }

        return fixRightColumnWidth
    }

    /**
     * 绘制固定行左右滑动时无需固定的单元格
     *
     * @param fixRow              固定行的下标
     * @param top                 固定行顶部位置
     * @param bottom              固定行底部位置
     * @param fixLeftColumnWidth  固定在左边的列的宽度
     * @param fixRightColumnWidth 固定在右边的列的宽度
     */
    protected fun drawNoneFixCellForFixRow(
        canvas: Canvas,
        fixRow: Int,
        top: Int,
        bottom: Int,
        fixLeftColumnWidth: Int,
        fixRightColumnWidth: Int
    ) {
        val showWidth = mTable.showRect.width()
        mClipRect.set(fixLeftColumnWidth, top, showWidth - fixRightColumnWidth, bottom)

        canvas.save()
        canvas.clipRect(mClipRect)

        val columns = mTable.tableData.columns
        val iCellDraw = mTable.cellDraw

        var left = -mTable.touchHelper.scrollX
        for (j in columns.indices) {
            if (left >= showWidth - fixRightColumnWidth) {
                break
            }

            val column = columns[j]
            if (left + column.width <= fixLeftColumnWidth) {
                left += column.width
                continue
            }

            mClipRect.set(left, top, column.width + left, bottom)
            canvas.save()
            canvas.clipRect(mClipRect)

            mShowCells.add(
                ShowCell.getInstance(
                    fixRow, j, mClipRect,
                    fixRow = true,
                    fixColumn = false
                )
            )
            iCellDraw?.onCellDraw(mTable, canvas, column.cells[fixRow], mClipRect, fixRow, j)
            drawMask(canvas, mClipRect, fixRow, j)

            canvas.restore()
            left += column.width
        }

        canvas.restore()
    }

    /**
     * 绘制蒙层，高亮行列
     */
    protected fun drawMask(canvas: Canvas, drawRect: Rect, row: Int, column: Int) {
        val touchHelper = mTable.touchHelper
        if (row != touchHelper.getNeedMaskRowIndex() && column != touchHelper.getNeedMaskColumnIndex()) {
            return
        }

        if (mMaskPaint == null) {
            mMaskPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            mMaskPaint!!.strokeWidth = 1f
        }

        val tableConfig = mTable.tableConfig
        var highLightColor = tableConfig.highLightColor
        val alpha = Color.alpha(highLightColor)
        highLightColor -= alpha
        mMaskPaint!!.color = highLightColor
        if (touchHelper.getNeedMaskRowIndex() == touchHelper.getNeedMaskColumnIndex() && row == 0 && column == 0) {
            drawRect.inset(1, 1)
            mMaskPaint!!.alpha = 255
            mMaskPaint!!.style = Paint.Style.STROKE
            canvas.drawRect(drawRect, mMaskPaint!!)

            mMaskPaint!!.alpha = alpha
            mMaskPaint!!.style = Paint.Style.FILL
            canvas.drawRect(drawRect, mMaskPaint!!)

            // 绘制拖拽指示器
            if (!tableConfig.isEnableDragIndicator) {
                return
            }
            mMaskPaint!!.alpha = 255
            if (touchHelper.isDragChangeSize) {
                val resources = mTable.viewContext.resources
                if (mRowColumnDragBitmap == null) {
                    mRowColumnDragBitmap = BitmapFactory.decodeResource(
                        resources,
                        tableConfig.firstRowColumnDragIndicatorRes
                    )
                }
                val imageSize =
                    if (tableConfig.firstRowColumnDragIndicatorSize == TableConfig.INVALID_VALUE)
                        resources.getDimensionPixelSize(R.dimen.drag_image_size)
                    else
                        tableConfig.firstRowColumnDragIndicatorSize
                val left =
                    drawRect.left - if (tableConfig.firstRowColumnDragIndicatorHorizontalOffset == TableConfig.INVALID_VALUE)
                        resources.getDimensionPixelSize(R.dimen.first_row_column_drag_image_horizontal_offset)
                    else
                        tableConfig.firstRowColumnDragIndicatorHorizontalOffset
                val top =
                    drawRect.top - if (tableConfig.firstRowColumnDragIndicatorVerticalOffset == TableConfig.INVALID_VALUE)
                        resources.getDimensionPixelSize(R.dimen.first_row_column_drag_image_vertical_offset)
                    else
                        tableConfig.firstRowColumnDragIndicatorVerticalOffset
                drawRect.set(left, top, left + imageSize, top + imageSize)
                if (!drawRect.isEmpty) {
                    canvas.drawBitmap(mRowColumnDragBitmap!!, null, drawRect, mMaskPaint)
                }
            }
        } else if (column == touchHelper.getNeedMaskColumnIndex()) {
            mMaskPaint!!.style = Paint.Style.FILL
            mMaskPaint!!.alpha = 255
            canvas.drawLine(
                drawRect.left.toFloat(),
                drawRect.top.toFloat(),
                drawRect.left.toFloat(),
                drawRect.bottom.toFloat(),
                mMaskPaint!!
            )
            canvas.drawLine(
                drawRect.right.toFloat(),
                drawRect.top.toFloat(),
                drawRect.right.toFloat(),
                drawRect.bottom.toFloat(),
                mMaskPaint!!
            )

            mMaskPaint!!.alpha = alpha
            drawRect.inset(1, 0)
            canvas.drawRect(drawRect, mMaskPaint!!)

            // 绘制拖拽指示器
            if (!tableConfig.isEnableDragIndicator) {
                return
            }
            mMaskPaint!!.alpha = 255
            if (row == 0 && touchHelper.isDragChangeSize) {
                val resources = mTable.viewContext.resources
                if (mColumnDragBitmap == null) {
                    mColumnDragBitmap = BitmapFactory.decodeResource(
                        resources,
                        tableConfig.columnDragIndicatorRes
                    )
                }
                val imageSize =
                    if (tableConfig.columnDragIndicatorSize == TableConfig.INVALID_VALUE)
                        resources.getDimensionPixelSize(R.dimen.drag_image_size)
                    else
                        tableConfig.rowDragIndicatorSize
                val left = drawRect.left + drawRect.width() / 2 - imageSize / 2
                val top =
                    drawRect.top - if (tableConfig.columnDragIndicatorVerticalOffset == TableConfig.INVALID_VALUE)
                        resources.getDimensionPixelSize(R.dimen.column_drag_image_vertical_offset)
                    else
                        tableConfig.columnDragIndicatorVerticalOffset
                drawRect.set(left, top, left + imageSize, top + imageSize)
                if (!drawRect.isEmpty) {
                    canvas.drawBitmap(mColumnDragBitmap!!, null, drawRect, mMaskPaint)
                }
            }
        } else if (row == touchHelper.getNeedMaskRowIndex()) {
            mMaskPaint!!.style = Paint.Style.FILL
            mMaskPaint!!.alpha = 255
            canvas.drawLine(
                drawRect.left.toFloat(),
                drawRect.top.toFloat(),
                drawRect.right.toFloat(),
                drawRect.top.toFloat(),
                mMaskPaint!!
            )
            canvas.drawLine(
                drawRect.left.toFloat(),
                drawRect.bottom.toFloat(),
                drawRect.right.toFloat(),
                drawRect.bottom.toFloat(),
                mMaskPaint!!
            )

            mMaskPaint!!.alpha = alpha
            drawRect.inset(0, 1)
            canvas.drawRect(drawRect, mMaskPaint!!)

            // 绘制拖拽指示器
            if (!tableConfig.isEnableDragIndicator) {
                return
            }
            mMaskPaint!!.alpha = 255
            if (column == 0 && touchHelper.isDragChangeSize) {
                val resources = mTable.viewContext.resources
                if (mRowDragBitmap == null) {
                    mRowDragBitmap =
                        BitmapFactory.decodeResource(resources, tableConfig.rowDragIndicatorRes)
                }
                val imageSize = if (tableConfig.rowDragIndicatorSize == TableConfig.INVALID_VALUE)
                    resources.getDimensionPixelSize(R.dimen.drag_image_size)
                else
                    tableConfig.rowDragIndicatorSize
                val top = drawRect.top + drawRect.height() / 2 - imageSize / 2
                val left =
                    drawRect.left - if (tableConfig.rowDragIndicatorHorizontalOffset == TableConfig.INVALID_VALUE)
                        resources.getDimensionPixelSize(R.dimen.row_drag_image_horizontal_offset)
                    else
                        tableConfig.rowDragIndicatorHorizontalOffset
                drawRect.set(left, top, left + imageSize, top + imageSize)
                if (!drawRect.isEmpty) {
                    canvas.drawBitmap(mRowDragBitmap!!, null, drawRect, mMaskPaint)
                }
            }
        }
    }
}
