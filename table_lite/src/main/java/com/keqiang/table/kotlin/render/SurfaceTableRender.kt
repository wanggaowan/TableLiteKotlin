package com.keqiang.table.kotlin.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Rect
import android.view.SurfaceHolder

import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.util.Utils

/**
 * 确定单元格位置，固定行列逻辑
 *
 * create by 汪高皖 on 2019/1/19 17:13
 */
class SurfaceTableRender<T : Cell>(table: ITable<T>, private var mHolder: SurfaceHolder?) :
    TableRender<T>(table), SurfaceHolder.Callback {
    /**
     * 判断Surface是否准备好，当为`true`时才可绘制内容
     */
    private var mDrawEnable: Boolean = false

    /**
     * 记录当前绘制了几帧
     */
    private var mDrawCount: Int = 0

    init {
        if (mHolder != null) {
            mHolder!!.addCallback(this)
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mDrawEnable = true
        draw()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mDrawEnable = false
        val surface = mHolder!!.surface
        surface?.release()
    }

    fun setHolder(holder: SurfaceHolder) {
        if (mHolder === holder) {
            return
        }

        if (mHolder != null) {
            mHolder!!.removeCallback(this)
        }

        mDrawEnable = false
        mDrawCount = 0
        mHolder = holder
        if (mHolder != null) {
            mHolder!!.addCallback(this)
        }
    }

    /**
     * 本次绘制是否生效
     *
     * @return `false`没有执行绘制操作，`true`已重新绘制
     */
    fun draw(): Boolean {
        if (!mDrawEnable) {
            return false
        }

        val canvas = mHolder!!.lockCanvas() ?: return false

        // 清屏
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val draw = draw(canvas)
        if (draw) {
            if (mDrawCount < 2) {
                mDrawCount++
            }
        } else {
            // 执行了清屏操作，但是又没有绘制内容，这样下次就不能直接局部刷新
            // 需要让前后两帧都有内容，这样下次局部刷新才能获取到差异范围
            mDrawCount = 0
        }
        mHolder!!.unlockCanvasAndPost(canvas)
        return draw
    }

    /**
     * 局部刷新单元格
     *
     * @param rowIndex    需要刷新的单元格所在行
     * @param columnIndex 需要刷新的单元格所在列
     * @param data        新数据
     */
    fun reDrawCell(rowIndex: Int, columnIndex: Int, data: Any) {
        val rows = mTable.tableData.rows
        val columns = mTable.tableData.columns
        if (!mDrawEnable
            || rowIndex < 0
            || rowIndex >= rows.size
            || columnIndex < 0
            || columnIndex >= columns.size
        ) {
            return
        }

        val row = rows[rowIndex]
        val cell = row.cells[columnIndex]
        cell.setData(data)

        val actualRowHeight = Utils.getActualRowHeight(row, 0, row.cells.size, mTable.tableConfig)
        val column = columns[columnIndex]
        val actualColumnWidth =
            Utils.getActualColumnWidth(column, 0, column.cells.size, mTable.tableConfig)
        if (actualRowHeight != row.height || actualColumnWidth != column.width) {
            row.height = actualRowHeight
            column.width = actualColumnWidth
            draw()
            return
        }

        val showCells = showCells
        var drawRect: Rect? = null
        for (showCell in showCells) {
            if (showCell.row == rowIndex && showCell.column == columnIndex) {
                drawRect = showCell.drawRect
                break
            }
        }

        if (drawRect == null) {
            return
        }

        val screenWidth = mTable.showRect.width()
        val screenHeight = mTable.showRect.height()
        if (drawRect.left >= screenWidth
            || drawRect.right <= 0
            || drawRect.top >= screenHeight
            || drawRect.bottom <= 0
        ) {
            return
        }

        if (mDrawCount <= 2) {
            // 前两帧需要完整绘制，SurfaceView是双缓冲机制，是两个Canvas交替显示
            // 第一帧显示第一个Canvas，第二帧显示第二个Canvas，这时第一个Canvas退到后台。
            // 从第三帧开始第一个和第二个Canvas交替前后台显示，此时可以对比两个Canvas的差异进行局部刷新
            draw()
            return
        }

        mClipRect.set(drawRect)
        var canvas: Canvas? = mHolder!!.lockCanvas(mClipRect)
        var tryCount = 5
        while (canvas == null) {
            canvas = mHolder!!.lockCanvas(mClipRect)
            if (canvas != null) {
                break
            } else if (tryCount <= 0) {
                break
            } else {
                tryCount--
            }
        }
        if (canvas == null) {
            return
        }
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        // 重新赋值一次是防止需要更新的Cell四边超出Table边界时，
        // 调用mHolder.lockCanvas(mClipRect)会更新mClipRect区间，让其保证在可见区域范围内
        // 这样就会改变单元格原本大小
        mClipRect.set(drawRect)
        val iCellDraw = mTable.cellDraw
        iCellDraw?.onCellDraw(mTable, canvas, cell, mClipRect, rowIndex, columnIndex)
        mHolder!!.unlockCanvasAndPost(canvas)
    }
}
