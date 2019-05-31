package com.keqiang.table.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.keqiang.table.kotlin.interfaces.CellFactory
import com.keqiang.table.kotlin.interfaces.ICellDraw
import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.ShowCell
import com.keqiang.table.kotlin.model.TableData
import com.keqiang.table.kotlin.render.TableRender
import com.keqiang.table.kotlin.util.AsyncExecutor

/**
 * 实现表格的绘制。如果有单元格经常变更，但整体几乎不变的需求，
 * 请使用[SurfaceTable],该表格有当前表格的所有功能且可以实现局部单元格刷新<br></br>
 * 主要类说明：
 *
 *  * [TableConfig]用于配置一些基础的表格数据
 *  * [TableData]用于指定表格行列数，增删行列，清除数据，记录单元格数据，用于绘制时提供每个单元格位置和大小
 *  * [TouchHelper]用于处理点击，移动，快速滑动逻辑以及设置相关参数
 *  * [CellFactory]用于提供单元格数据，指定固定宽高或自适应时测量宽高
 *  * [ICellDraw]用于绘制整个表格背景和单元格内容
 *
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 08:29
 */
class Table<T : Cell>
@JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : View(context, attrs, defStyle), ITable<T> {

    override val viewContext: Context
        get() = context

    override var cellFactory: CellFactory<T>? = null
        set(value) {
            if (value == null) {
                return
            }
            field = value
        }

    override var cellDraw: ICellDraw<T>? = null
        set(value) {
            if (value == null) {
                return
            }
            field = value
        }

    override val tableConfig: TableConfig
        get() = mTableConfig

    override val tableData: TableData<T>
        get() = mTableData

    override val touchHelper: TouchHelper<T>
        get() = mTouchHelper

    override val showRect: Rect
        get() {
            if (mOnlyReadShowRect == null) {
                mOnlyReadShowRect = Rect()
            }
            mOnlyReadShowRect!!.set(mShowRect)
            return mOnlyReadShowRect as Rect
        }

    override val actualSizeRect: Rect
        get() = mTableRender.actualSizeRect

    override val showCells: List<ShowCell>
        get() = mTableRender.showCells

    /**
     * 屏幕上可展示的区域
     */
    private var mShowRect: Rect = Rect()

    /**
     * 屏幕上可展示的区域，防止外部实际改变mShowRect
     */
    private var mOnlyReadShowRect: Rect? = null

    /**
     * 表格数据
     */
    private var mTableData: TableData<T> = TableData(this)

    /**
     * 表格配置
     */
    private var mTableConfig: TableConfig = TableConfig()

    /**
     * 处理触摸逻辑
     */
    private var mTouchHelper: TouchHelper<T> = TouchHelper(this)

    /**
     * 确定单元格位置，固定行列逻辑
     */
    private var mTableRender: TableRender<T> = TableRender(this)

    /**
     * 界面绘制
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        mTableRender.draw(canvas)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        val dispose = touchHelper.dispatchTouchEvent(this, event)
        val superDispose = super.dispatchTouchEvent(event)
        return dispose || superDispose
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHelper.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mShowRect.set(0, 0, w, h)
        touchHelper.onScreenSizeChange()
    }

    /*
     滑动模型图解:https://blog.csdn.net/luoang/article/details/70912058
     */

    /**
     * 水平方向可滑动范围
     */
    public override fun computeHorizontalScrollRange(): Int {
        return actualSizeRect.right
    }

    /**
     * 垂直方向可滑动范围
     */
    public override fun computeVerticalScrollRange(): Int {
        return actualSizeRect.height()
    }

    /**
     * 水平方向滑动偏移值
     *
     * @return 滑出View左边界的距离，>0的值
     */
    public override fun computeHorizontalScrollOffset(): Int {
        return Math.max(0, touchHelper.scrollX)
    }

    /**
     * 垂直方向滑动偏移值
     *
     * @return 滑出View顶部边界的距离，>0的值
     */
    public override fun computeVerticalScrollOffset(): Int {
        return Math.max(0, touchHelper.scrollY)
    }

    /**
     * 判断垂直方向是否可以滑动
     *
     * @param direction <0：手指滑动方向从上到下(显示内容逐渐移动到顶部)，>0：手指滑动方向从下到上(显示内容逐渐移动到底部)
     */
    override fun canScrollVertically(direction: Int): Boolean {
        return touchHelper.isDragChangeSize || if (direction < 0) {
            // 向顶部滑动
            touchHelper.scrollY > 0
        } else {
            // 向底部滑动
            actualSizeRect.height() > touchHelper.scrollY + mShowRect.height()
        }
    }

    /**
     * 判断水平方向是否可以滑动
     *
     * @param direction <0：手指滑动方向从左到右(显示内容逐渐移动到左边界)，>0：手指滑动方向从右到左(显示内容逐渐移动到右边界)
     */
    override fun canScrollHorizontally(direction: Int): Boolean {
        return touchHelper.isDragChangeSize || if (direction < 0) {
            // 向顶部滑动
            touchHelper.scrollX > 0
        } else {
            // 向底部滑动
            actualSizeRect.width() > touchHelper.scrollX + mShowRect.width()
        }
    }

    /**
     * 水平方向展示内容大小
     */
    public override fun computeHorizontalScrollExtent(): Int {
        return mShowRect.width()
    }

    /**
     * 垂直方向展示内容大小
     */
    public override fun computeVerticalScrollExtent(): Int {
        return mShowRect.height()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        AsyncExecutor.shutdown()
    }

    override fun asyncReDraw() {
        postInvalidate()
    }

    override fun syncReDraw() {
        invalidate()
    }
}
