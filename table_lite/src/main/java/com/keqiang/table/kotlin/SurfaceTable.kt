package com.keqiang.table.kotlin

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Looper
import android.util.AttributeSet
import android.util.SparseArray
import android.view.MotionEvent
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.keqiang.table.kotlin.interfaces.CellFactory
import com.keqiang.table.kotlin.interfaces.ICellDraw
import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.ShowCell
import com.keqiang.table.kotlin.model.TableData
import com.keqiang.table.kotlin.render.SurfaceTableRender
import com.keqiang.table.kotlin.util.AsyncExecutor
import com.keqiang.table.kotlin.util.Logger

/**
 * 暂不提供使用，目前还未解决层级问题，如果xml中使用该表格，将始终浮在最上层
 * 实现表格的绘制,可实现局部单元格刷新，这对于表格有网络照片需要显示时在性能上有很大提升。<br></br>
 * 主要类说明：
 *
 *  * [TableConfig]用于配置一些基础的表格数据
 *  * [TableData]用于指定表格行列数，增删行列，清除数据，记录单元格数据，用于绘制时提供每个单元格位置和大小
 *  * [TouchHelper]用于处理点击，移动，快速滑动逻辑以及设置相关参数
 *  * [CellFactory]用于提供单元格数据，指定固定宽高或自适应时测量宽高
 *  * [ICellDraw]用于绘制整个表格背景和单元格内容
 *
 *
 * @author Created by 汪高皖 on 2019/1/18 0018 10:27
 */
internal class SurfaceTable<T : Cell>
@JvmOverloads constructor(
    context: Context, attrs:
    AttributeSet? = null,
    defStyle: Int = 0
) : FrameLayout(context, attrs, defStyle), ITable<T> {

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
     * SurfaceTable绘制逻辑
     */
    private var mTableRender: SurfaceTableRender<T>
    /**
     * 用于显示表格内容，这是真实的表格位置内容所处位置
     */
    @Suppress("JoinDeclarationAndAssignment")
    private var mSurfaceView: SurfaceView

    /**
     * 是否第一次初始化SurfaceView
     */
    private var mFirstInitSurfaceView: Boolean = false

    /**
     * 记录各方向SurfaceView对象
     */
    private var mSurfaceViewMap: SparseArray<SurfaceView>? = null

    /**
     * 当前屏幕方向
     */
    private var mOrientation = Configuration.ORIENTATION_UNDEFINED

    init {
        mSurfaceView = SurfaceView(context)
        val layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        mSurfaceView.layoutParams = layoutParams
        mSurfaceView.setZOrderOnTop(true)
        mSurfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
        addView(mSurfaceView)
        mTableRender = SurfaceTableRender(this, mSurfaceView.holder)
        mFirstInitSurfaceView = true
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
        Logger.e("onSizeChanged")
        if (mShowRect.width() == w && mShowRect.height() == h) {
            mFirstInitSurfaceView = false
            return
        }
        mShowRect.set(0, 0, w, h)
        touchHelper.onScreenSizeChange()

        if (mFirstInitSurfaceView) {
            mFirstInitSurfaceView = false
            return
        }

        if (mOrientation == Configuration.ORIENTATION_UNDEFINED) {
            return
        }

        Looper.myQueue().addIdleHandler {
            var surfaceView: SurfaceView?
            surfaceView = if (mOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                mSurfaceViewMap!!.get(Configuration.ORIENTATION_PORTRAIT)
            } else {
                mSurfaceViewMap!!.get(Configuration.ORIENTATION_LANDSCAPE)

            }

            if (surfaceView != null) {
                removeView(surfaceView)
            }

            surfaceView = mSurfaceViewMap!!.get(mOrientation)
            if (surfaceView == null) {
                surfaceView = SurfaceView(context)
                val layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                surfaceView.layoutParams = layoutParams
                surfaceView.setZOrderOnTop(true)
                surfaceView.holder.setFormat(PixelFormat.TRANSLUCENT)
                addView(surfaceView)
                mTableRender.setHolder(surfaceView.holder)

                mSurfaceViewMap!!.put(mOrientation, surfaceView)
            } else {
                addView(surfaceView)
                mTableRender.setHolder(surfaceView.holder)
            }
            false
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Logger.e("onConfigurationChanged")
        when {
            newConfig.orientation == Configuration.ORIENTATION_PORTRAIT -> {
                if (mSurfaceViewMap == null) {
                    // 界面初始方向时横屏
                    mSurfaceViewMap = SparseArray(2)
                    mSurfaceViewMap!!.put(Configuration.ORIENTATION_LANDSCAPE, mSurfaceView)
                }
                mOrientation = Configuration.ORIENTATION_PORTRAIT
            }

            newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE -> {
                if (mSurfaceViewMap == null) {
                    // 界面初始方向时竖屏
                    mSurfaceViewMap = SparseArray(2)
                    mSurfaceViewMap!!.put(Configuration.ORIENTATION_PORTRAIT, mSurfaceView)
                }
                mOrientation = Configuration.ORIENTATION_LANDSCAPE
            }

            else -> mOrientation = Configuration.ORIENTATION_UNDEFINED
        }
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
        AsyncExecutor.shutdownNow()
    }

    override fun asyncReDraw() {
        // surfaceView可以直接在非主线程中调用绘制
        mTableRender.draw()
    }

    override fun syncReDraw() {
        mTableRender.draw()
    }

    /**
     * 以异步方式局部刷新单元格
     *
     * @param row    需要刷新的单元格所在行
     * @param column 需要刷新的单元格所在列
     * @param data   新数据
     */
    fun asyncReDrawCell(row: Int, column: Int, data: Any) {
        // surfaceView可以直接在非主线程中调用绘制
        mTableRender.reDrawCell(row, column, data)
    }

    /**
     * 以同步方式局部刷新单元格
     *
     * @param row    需要刷新的单元格所在行
     * @param column 需要刷新的单元格所在列
     * @param data   新数据
     */
    fun syncReDrawCell(row: Int, column: Int, data: Any) {
        mTableRender.reDrawCell(row, column, data)
    }
}
