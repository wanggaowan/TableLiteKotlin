package com.keqiang.table.kotlin

import android.animation.TimeInterpolator
import android.animation.TypeEvaluator
import android.animation.ValueAnimator
import android.graphics.Point
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.Scroller

import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.DragChangeSizeType
import com.keqiang.table.kotlin.model.FirstRowColumnCellActionType

/**
 * 处理点击，移动，快速滑动逻辑
 *
 * @author Created by 汪高皖 on 2019/1/17 0017 09:53
 */
class TouchHelper<T : Cell>(private val mTable: ITable<T>) {
    /**
     * 表格实际大小是可显示区域大小的几倍时才开启快速滑动,范围[1,∞)
     */
    var enableFlingRate: Float = 1.2f

    /**
     * 快速滑动速率,数值越大，滑动越快
     */
    var flingRate: Float = 1f

    /**
     * 快速滑动时是否X轴和Y轴都进行滑动
     */
    var flingXY: Boolean = false

    /**
     * 需要高亮显示的行
     */
    var highLightRowIndex: Int = TableConfig.INVALID_VALUE
        private set

    /**
     * 需要高亮显示的列
     */
    var highLightColumnIndex: Int = TableConfig.INVALID_VALUE
        private set

    /**
     * 拖拽需要高亮显示的行
     */
    var dragRowIndex: Int = TableConfig.INVALID_VALUE
        private set

    /**
     * 拖拽需要高亮显示的列
     */
    var dragColumnIndex: Int = TableConfig.INVALID_VALUE
        private set

    /**
     * 水平滑动时滑出部分离控件左边的距离
     */
    var scrollX: Int = 0
        set(value) {
            scrollTo(value, scrollY)
        }

    /**
     * 垂直滑动时滑出部分离控件顶部的距离
     */
    var scrollY: Int = 0
        set(value) {
            scrollTo(scrollX, value)
        }

    /**
     * @return 需要绘制蒙层的行Index
     */
    internal fun getNeedMaskRowIndex(): Int {
        return if (isDragChangeSize) {
            dragRowIndex
        } else {
            highLightRowIndex
        }
    }

    /**
     * @return 需要绘制蒙层的列Index
     */
    internal fun getNeedMaskColumnIndex(): Int {
        return if (isDragChangeSize) {
            dragColumnIndex
        } else {
            highLightColumnIndex
        }
    }

    /**
     * `true`触发拖拽改变列宽或行高的动作
     */
    internal var isDragChangeSize: Boolean = false
        private set

    /**
     * 单元格点击监听
     */
    private var mCellClickListener: CellClickListener? = null

    /**
     * 快速滑动时记录滑动之前的偏移值
     */
    private var mTempScrollX: Int = 0

    /**
     * 快速滑动时记录滑动之前的偏移值
     */
    private var mTempScrollY: Int = 0

    /**
     * 处理手势滑动
     */
    private val mGestureDetector: GestureDetector

    /**
     * 最小滑动速度
     */
    private var mMinimumFlingVelocity: Int = 50

    /**
     * 用来处理fling
     */
    private lateinit var mScroller: Scroller

    /**
     * 当前是否快速滚动中
     */
    private var mFling: Boolean = false

    /**
     * 点击处单元格所在行
     */
    private var mClickRowIndex = TableConfig.INVALID_VALUE

    /**
     * 点击处单元格所在列
     */
    private var mClickColumnIndex = TableConfig.INVALID_VALUE

    /**
     * 是否触发了长按事件
     */
    private var mLongPressDone: Boolean = false

    /**
     * 长按时X轴坐标
     */
    private var longPressX = 0f

    /**
     * 长按时Y轴坐标
     */
    private var longPressY = 0f

    // 用于处理快速滑动
    private val mStartPoint = Point(0, 0)
    private val mEndPoint = Point()
    private val mInterpolator: TimeInterpolator
    private val mEvaluator: PointEvaluator

    /**
     * 处理点击和移动
     */
    private val mClickAndMoveGestureListener = object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            mFling = false
            mClickRowIndex = TableConfig.INVALID_VALUE
            mClickColumnIndex = TableConfig.INVALID_VALUE
            val showCells = mTable.showCells
            for (showCell in showCells) {
                if (showCell.drawRect.contains(e.x.toInt(), e.y.toInt())) {
                    mClickRowIndex = showCell.row
                    mClickColumnIndex = showCell.column
                    break
                }
            }
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (mClickRowIndex == TableConfig.INVALID_VALUE || mClickColumnIndex == TableConfig.INVALID_VALUE) {
                return false
            } else {
                val tableConfig = mTable.tableConfig
                var highLightRowIndex: Int
                var highLightColumnIndex: Int

                highLightRowIndex =
                    if (!tableConfig.isHighLightSelectRow || mClickColumnIndex != 0
                        || mClickRowIndex == 0
                        && (tableConfig.firstRowColumnCellHighLightType == FirstRowColumnCellActionType.NONE
                                || tableConfig.firstRowColumnCellHighLightType == FirstRowColumnCellActionType.COLUMN)
                    ) {
                        // 不需要高亮选中行或点击的不是第一列或点击的是第一行第一列，但是第一行第一列单元格不高亮选中行
                        TableConfig.INVALID_VALUE
                    } else {
                        // 点击第一列内容表示行需要高亮，记录高亮行位置
                        mClickRowIndex
                    }

                highLightColumnIndex =
                    if (!tableConfig.isHighLightSelectColumn || mClickRowIndex != 0
                        || mClickColumnIndex == 0
                        && (tableConfig.firstRowColumnCellHighLightType == FirstRowColumnCellActionType.NONE
                                || tableConfig.firstRowColumnCellHighLightType == FirstRowColumnCellActionType.ROW)
                    ) {
                        // 不需要高亮选中列或点击的不是第一行或点击的是第一行第一列，但是第一行第一列单元格不高亮选中列
                        TableConfig.INVALID_VALUE
                    } else {
                        // 点击第一行内容表示列需要高亮，记录高亮列位置
                        mClickColumnIndex
                    }

                if (tableConfig.isBothHighLightRowAndColumn && (mClickRowIndex == 0 || mClickColumnIndex == 0)) {
                    if (highLightRowIndex == TableConfig.INVALID_VALUE
                        && this@TouchHelper.highLightRowIndex != TableConfig.INVALID_VALUE
                    ) {
                        highLightRowIndex = this@TouchHelper.highLightRowIndex
                    }

                    if (highLightColumnIndex == TableConfig.INVALID_VALUE
                        && this@TouchHelper.highLightColumnIndex != TableConfig.INVALID_VALUE
                    ) {
                        highLightColumnIndex = this@TouchHelper.highLightColumnIndex
                    }
                }

                if (highLightRowIndex != this@TouchHelper.highLightRowIndex || highLightColumnIndex != this@TouchHelper.highLightColumnIndex) {
                    this@TouchHelper.highLightRowIndex = highLightRowIndex
                    this@TouchHelper.highLightColumnIndex = highLightColumnIndex
                    notifyViewChanged()
                }

                mCellClickListener?.invoke(mClickRowIndex, mClickColumnIndex)
                return true
            }
        }

        override fun onLongPress(e: MotionEvent) {
            longPressX = e.x
            longPressY = e.y
            mLongPressDone = true
            dragChangeSize(0f, 0f, true)
        }

        override fun onScroll(
            e1: MotionEvent,
            e2: MotionEvent,
            distanceX: Float,
            distanceY: Float
        ): Boolean {
            val dispose = dragChangeSize(distanceX, distanceY, false)
            if (dispose) {
                return true
            }

            val showRect = mTable.showRect
            val actualSizeRect = mTable.actualSizeRect
            if (showRect.width() >= actualSizeRect.width() && showRect.height() >= actualSizeRect.height()) {
                return false
            }

            val originalX = scrollX
            val originalY = scrollY
            scrollX += distanceX.toInt()
            scrollY += distanceY.toInt()
            return if (judgeNeedUpdateTable(originalX, originalY)) {
                notifyViewChanged()
                true
            } else {
                false
            }
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            val showRect = mTable.showRect
            val actualSizeRect = mTable.actualSizeRect
            if (showRect.width() * enableFlingRate >= actualSizeRect.width()
                && showRect.height() * enableFlingRate >= actualSizeRect.height()
            ) {
                // 只有表格宽且表格高度有显示区域两倍大小时才可快速滑动
                return false
            }

            //根据滑动速率 设置Scroller final值,然后使用属性动画计算
            if (Math.abs(velocityX) > mMinimumFlingVelocity
                || Math.abs(velocityY) > mMinimumFlingVelocity
            ) {
                mScroller.finalX = 0
                mScroller.finalY = 0
                mTempScrollX = scrollX
                mTempScrollY = scrollY
                mScroller.fling(
                    0,
                    0,
                    velocityX.toInt(),
                    velocityY.toInt(),
                    -50000,
                    50000,
                    -50000,
                    50000
                )
                mFling = true
                startFilingAnim(flingXY)
            }

            return true
        }
    }

    init {
        val context = mTable.viewContext
        mScroller = Scroller(context)
        mInterpolator = DecelerateInterpolator()
        mEvaluator = PointEvaluator()
        mGestureDetector = GestureDetector(context, mClickAndMoveGestureListener)
        mMinimumFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    /**
     * 设置单元格点击监听
     */
    fun setCellClickListener(listener: CellClickListener?) {
        mCellClickListener = listener
    }

    /**
     * 设置X轴、Y轴滑动距离
     */
    fun scrollTo(x: Int, y: Int) {
        val oX = scrollX
        val oY = scrollY
        scrollX = x
        scrollY = y
        if (judgeNeedUpdateTable(oX, oY)) {
            notifyViewChanged()
        }
    }

    /**
     * 处理事件分发
     */
    internal fun dispatchTouchEvent(view: View, event: MotionEvent): Boolean {
        val parent = view.parent
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                //ACTION_DOWN的时候，赶紧把事件hold住
                if (mTable.showRect.contains(event.x.toInt(), event.y.toInt())) {
                    //判断是否落在图表内容区中
                    parent.requestDisallowInterceptTouchEvent(true)
                } else {
                    parent.requestDisallowInterceptTouchEvent(false)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                var isDisallowIntercept = true
                if (!isDragChangeSize && (scrollY == 0 || scrollY >= mTable.actualSizeRect.height() - mTable.showRect.height())) {
                    isDisallowIntercept = false
                }
                parent.requestDisallowInterceptTouchEvent(isDisallowIntercept)
                if (isDisallowIntercept) {
                    return true
                }
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP ->
                parent.requestDisallowInterceptTouchEvent(false)
        }
        return false
    }

    /**
     * 处理触摸时间
     */
    internal fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_MOVE -> if (mLongPressDone) {
                val dispose = dragChangeSize(
                    longPressX - event.x,
                    longPressY - event.y, false
                )
                longPressX = event.x
                longPressY = event.y
                return dispose
            }

            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (isDragChangeSize) {
                    isDragChangeSize = false
                    if (!mTable.tableConfig.needRecoveryHighLightOnDragChangeSizeEnded) {
                        highLightRowIndex = TableConfig.INVALID_VALUE
                        highLightColumnIndex = TableConfig.INVALID_VALUE
                    }
                    notifyViewChanged()
                }
                mLongPressDone = false
                dragRowIndex = TableConfig.INVALID_VALUE
                dragColumnIndex = TableConfig.INVALID_VALUE
            }
        }
        return mGestureDetector.onTouchEvent(event)
    }

    /**
     * 屏幕宽高发送变化
     */
    internal fun onScreenSizeChange() {
        judgeNeedUpdateTable(scrollX, scrollY)
    }

    /**
     * 通知表格刷新
     */
    private fun notifyViewChanged() {
        mTable.syncReDraw()
    }

    /**
     * 判断滑动后是否需要更新表格
     *
     * @param originalX X轴原始滑动距离
     * @param originalY Y轴原始滑动距离
     * @return `true` 需要更新
     */
    private fun judgeNeedUpdateTable(originalX: Int, originalY: Int): Boolean {
        val showRect = mTable.showRect
        val actualSizeRect = mTable.actualSizeRect
        if (scrollX < 0) {
            scrollX = 0
        } else {
            val diff = actualSizeRect.width() - showRect.width()
            if (diff <= 0) {
                scrollX = 0
            } else if (scrollX > diff) {
                scrollX = diff
            }
        }

        if (scrollY < 0) {
            scrollY = 0
        } else {
            val diff = actualSizeRect.height() - showRect.height()
            if (diff <= 0) {
                scrollY = 0
            } else if (scrollY > diff) {
                scrollY = diff
            }
        }

        return scrollX != originalX || scrollY != originalY
    }

    /**
     * 开始飞滚
     */
    private fun startFilingAnim(doubleWay: Boolean) {
        val scrollX = Math.abs(mScroller.finalX)
        val scrollY = Math.abs(mScroller.finalY)

        when {
            doubleWay -> mEndPoint.set(
                (mScroller.finalX * flingRate).toInt(),
                (mScroller.finalY * flingRate).toInt()
            )

            scrollX > scrollY -> {
                val showRect = mTable.showRect
                val actualSizeRect = mTable.actualSizeRect
                if (actualSizeRect.width() <= showRect.width() * enableFlingRate) {
                    // 只有表格实际宽比显示区域宽度大mEnableFlingRate倍才可快速滑动
                    return
                }
                mEndPoint.set((mScroller.finalX * flingRate).toInt(), 0)
            }

            else -> {
                val showRect = mTable.showRect
                val actualSizeRect = mTable.actualSizeRect
                if (actualSizeRect.height() <= showRect.height() * enableFlingRate) {
                    // 只有表格实际高比显示区域高度大mEnableFlingRate倍才可快速滑动
                    return
                }
                mEndPoint.set(0, (mScroller.finalY * flingRate).toInt())
            }
        }

        val valueAnimator = ValueAnimator.ofObject(mEvaluator, mStartPoint, mEndPoint)
        valueAnimator.interpolator = mInterpolator
        valueAnimator.addUpdateListener { animation ->
            if (mFling) {
                val point = animation.animatedValue as Point
                val originalX = this.scrollX
                val originalY = this.scrollY
                this.scrollX = mTempScrollX - point.x
                this.scrollY = mTempScrollY - point.y
                if (judgeNeedUpdateTable(originalX, originalY)) {
                    notifyViewChanged()
                }

                // 以下判断依据了judgeNeedUpdateTable的结果，
                // judgeNeedUpdateTable会更改mScrollX和mScrollY的值
                if (this.scrollX == 0 && this.scrollY == 0) {
                    animation.cancel()
                } else {
                    val actualSizeRect = mTable.actualSizeRect
                    val showRect = mTable.showRect
                    val xDiff = actualSizeRect.width() - showRect.width()
                    val yDiff = actualSizeRect.height() - showRect.height()
                    if (this.scrollX == xDiff && this.scrollY == yDiff) {
                        animation.cancel()
                    }
                }
            } else {
                animation.cancel()
            }
        }
        val duration = (Math.max(scrollX, scrollY) * flingRate).toInt() / 2
        valueAnimator.duration = (if (duration > 300) 300 else duration).toLong()
        valueAnimator.start()
    }

    /**
     * 处理拖拽改变行高列宽事件
     *
     * @param mustNotifyViewChange 当点击了第一行或第一列单元格但宽高未发生改变时是否强制刷新界面
     */
    private fun dragChangeSize(
        distanceX: Float,
        distanceY: Float,
        mustNotifyViewChange: Boolean
    ): Boolean {
        val tableConfig = mTable.tableConfig
        val dragChangeSizeRowIndex: Int
        val dragChangeSizeColumnIndex: Int

        dragChangeSizeRowIndex =
            if (tableConfig.rowDragChangeHeightType == DragChangeSizeType.NONE
                || mClickColumnIndex != 0
                || mClickRowIndex == 0
                && (tableConfig.firstRowColumnCellDragType == FirstRowColumnCellActionType.NONE
                        || tableConfig.firstRowColumnCellDragType == FirstRowColumnCellActionType.COLUMN)
            ) {
                // 不需要拖拽改变行高，或点击的不是第一列或点击的是第一行第一列，但是第一行第一列单元格不需要拖拽改变行高
                TableConfig.INVALID_VALUE
            } else {
                // 点击第一列内容表示行需要高亮，记录高亮行位置
                mClickRowIndex
            }

        dragChangeSizeColumnIndex =
            if (tableConfig.columnDragChangeWidthType == DragChangeSizeType.NONE
                || mClickRowIndex != 0
                || mClickColumnIndex == 0
                && (tableConfig.firstRowColumnCellDragType == FirstRowColumnCellActionType.NONE
                        || tableConfig.firstRowColumnCellDragType == FirstRowColumnCellActionType.ROW)
            ) {
                // 不需要拖拽改变列宽，或点击的不是第一行或点击的是第一行第一列，但是第一行第一列单元格不需要拖拽改变列宽
                TableConfig.INVALID_VALUE
            } else {
                // 点击第一行内容表示列需要高亮，记录高亮列位置
                mClickColumnIndex
            }

        if (dragChangeSizeRowIndex != TableConfig.INVALID_VALUE || dragChangeSizeColumnIndex != TableConfig.INVALID_VALUE) {
            if (dragChangeSizeRowIndex == 0 && dragChangeSizeColumnIndex == 0) {
                if (!mLongPressDone && tableConfig.rowDragChangeHeightType == DragChangeSizeType.LONG_PRESS
                    && tableConfig.columnDragChangeWidthType == DragChangeSizeType.LONG_PRESS
                ) {
                    return false
                }

                isDragChangeSize = true
                dragRowIndex = dragChangeSizeRowIndex
                dragColumnIndex = dragChangeSizeColumnIndex
                val tableData = mTable.tableData
                val row = tableData.rows[0]
                val column = tableData.columns[0]
                var height = (row.height - distanceY).toInt()
                var width = (column.width - distanceX).toInt()
                if (height < tableConfig.minRowHeight) {
                    height = tableConfig.minRowHeight
                } else if (tableConfig.maxRowHeight != TableConfig.INVALID_VALUE && height > tableConfig.maxRowHeight) {
                    height = tableConfig.maxRowHeight
                }

                if (width < tableConfig.minColumnWidth) {
                    width = tableConfig.minColumnWidth
                } else if (tableConfig.maxColumnWidth != TableConfig.INVALID_VALUE && width > tableConfig.maxColumnWidth) {
                    width = tableConfig.maxColumnWidth
                }

                row.isDragChangeSize = true
                column.isDragChangeSize = true
                if (row.height != height || column.width != width) {
                    row.height = height
                    column.width = width
                    notifyViewChanged()
                } else if (mustNotifyViewChange) {
                    notifyViewChanged()
                }
            } else if (dragChangeSizeRowIndex != TableConfig.INVALID_VALUE) {
                if (!mLongPressDone && tableConfig.rowDragChangeHeightType == DragChangeSizeType.LONG_PRESS) {
                    return false
                }

                isDragChangeSize = true
                dragRowIndex = dragChangeSizeRowIndex
                dragColumnIndex = TableConfig.INVALID_VALUE
                val tableData = mTable.tableData
                val row = tableData.rows[dragChangeSizeRowIndex]
                var height = (row.height - distanceY).toInt()
                if (height < tableConfig.minRowHeight) {
                    height = tableConfig.minRowHeight
                } else if (tableConfig.maxRowHeight != TableConfig.INVALID_VALUE && height > tableConfig.maxRowHeight) {
                    height = tableConfig.maxRowHeight
                }

                row.isDragChangeSize = true
                if (row.height != height) {
                    row.height = height
                    notifyViewChanged()
                } else if (mustNotifyViewChange) {
                    notifyViewChanged()
                }
            } else {
                if (!mLongPressDone && tableConfig.columnDragChangeWidthType == DragChangeSizeType.LONG_PRESS) {
                    return false
                }

                isDragChangeSize = true
                dragRowIndex = TableConfig.INVALID_VALUE
                dragColumnIndex = dragChangeSizeColumnIndex
                val tableData = mTable.tableData
                val column = tableData.columns[dragChangeSizeColumnIndex]
                var width = (column.width - distanceX).toInt()
                if (width < tableConfig.minColumnWidth) {
                    width = tableConfig.minColumnWidth
                } else if (tableConfig.maxColumnWidth != TableConfig.INVALID_VALUE && width > tableConfig.maxColumnWidth) {
                    width = tableConfig.maxColumnWidth
                }

                column.isDragChangeSize = true
                if (column.width != width) {
                    column.width = width
                    notifyViewChanged()
                } else if (mustNotifyViewChange) {
                    notifyViewChanged()
                }
            }
            return true
        }
        return false
    }

    /**
     * 移动点估值器
     */
    class PointEvaluator : TypeEvaluator<Point> {
        private val point = Point()

        override fun evaluate(fraction: Float, startValue: Point, endValue: Point): Point {
            val x = (startValue.x + fraction * (endValue.x - startValue.x)).toInt()
            val y = (startValue.y + fraction * (endValue.y - startValue.y)).toInt()
            point.set(x, y)
            return point
        }
    }
}

/**
 * 单元格点击监听
 */
typealias CellClickListener = (row: Int, column: Int) -> Unit