package com.keqiang.table.kotlin.draw

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.text.TextUtils
import android.view.Gravity
import com.keqiang.table.kotlin.draw.TextCellDraw.DrawConfig
import com.keqiang.table.kotlin.interfaces.ICellDraw
import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.model.Cell

/**
 * 基础文本类表格绘制。可配置内容参考[DrawConfig]，此类主要是一个教程类的实现，说明[ICellDraw]接口各方法该如何处理绘制逻辑
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 09:31
 */
@SuppressLint("RtlHardcoded")
abstract class TextCellDraw<T : Cell> : ICellDraw<T> {

    /**
     * 获取文本的高度
     */
    private val textHeight: Float
        get() {
            val metrics = PAINT.fontMetrics
            return metrics.descent - metrics.ascent
        }

    /**
     * 获取单元格绘制配置数据
     *
     * @param row    单元格所在行
     * @param column 单元格所在列
     */
    abstract fun getConfig(row: Int, column: Int): DrawConfig

    override fun onCellDraw(
        table: ITable<T>,
        canvas: Canvas,
        cell: T,
        drawRect: Rect,
        row: Int,
        column: Int
    ) {
        if (drawRect.width() <= 0 || drawRect.height() <= 0) {
            return
        }

        val drawConfig = getConfig(row, column)
        drawBackground(canvas, drawRect, drawConfig)
        drawText(canvas, cell, drawRect, drawConfig)
        drawBorder(table, canvas, drawRect, drawConfig, row, column)
    }

    /**
     * 绘制背景
     */
    private fun drawBackground(canvas: Canvas, drawRect: Rect, drawConfig: DrawConfig?) {
        if (drawConfig == null) {
            return
        }

        if (drawConfig.isDrawBackground) {
            fillBackgroundPaint(drawConfig)
            if (drawConfig.borderSize > 0) {
                TEMP_RECT.set(
                    drawRect.left + drawConfig.borderSize / 2,
                    drawRect.top + drawConfig.borderSize / 2,
                    drawRect.right - drawConfig.borderSize / 2,
                    drawRect.bottom - drawConfig.borderSize / 2
                )
                canvas.save()
                canvas.clipRect(TEMP_RECT)
                canvas.drawRect(drawRect, PAINT)
                canvas.restore()
            } else {
                canvas.drawRect(drawRect, PAINT)
            }
        }
    }

    /**
     * 绘制边框
     */
    private fun drawBorder(
        table: ITable<*>,
        canvas: Canvas,
        drawRect: Rect,
        drawConfig: DrawConfig?,
        row: Int,
        column: Int
    ) {
        if (drawConfig == null) {
            return
        }

        if (drawConfig.borderSize > 0) {
            fillBorderPaint(drawConfig)
            val tableData = table.tableData
            val left = if (column == 0) drawRect.left + drawConfig.borderSize / 2 else drawRect.left
            val top = if (row == 0) drawRect.top + drawConfig.borderSize / 2 else drawRect.top
            val right =
                if (column == tableData.totalColumn - 1) drawRect.right - drawConfig.borderSize / 2 else drawRect.right
            val bottom =
                if (row == tableData.totalRow - 1) drawRect.bottom - drawConfig.borderSize / 2 else drawRect.bottom
            TEMP_RECT.set(left, top, right, bottom)
            canvas.drawRect(TEMP_RECT, PAINT)
        }
    }

    /**
     * 绘制文本
     */
    private fun drawText(
        canvas: Canvas,
        cell: Cell,
        drawRect: Rect,
        drawConfig: DrawConfig?
    ) {
        val text = cell.getData<Any>() as? CharSequence ?: return

        if (TextUtils.isEmpty(text.toString())) {
            return
        }

        if (drawConfig == null) {
            fillTextPaint(null)
            PAINT.textAlign = Paint.Align.LEFT

            val staticLayout: StaticLayout =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    StaticLayout.Builder.obtain(
                        text, 0, text.length,
                        PAINT, drawRect.width()
                    )
                        .build()
                } else {
                    StaticLayout(
                        text, 0, text.length,
                        PAINT, drawRect.width(),
                        Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
                    )
                }

            canvas.save()
            canvas.translate(drawRect.left.toFloat(), drawRect.right.toFloat())
            staticLayout.draw(canvas)
            canvas.restore()
            return
        }

        fillTextPaint(drawConfig)

        var highVersion = false
        val staticLayout: StaticLayout
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            highVersion = true
            staticLayout = StaticLayout.Builder.obtain(
                text, 0, text.length,
                PAINT, drawRect.width()
            )
                .setMaxLines(if (drawConfig.isMultiLine) Integer.MAX_VALUE else 1)
                .build()
        } else {
            staticLayout = StaticLayout(
                text, 0, text.length,
                PAINT, drawRect.width(),
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
            )
        }

        val textHeight = staticLayout.height.toFloat()
        val gravity = drawConfig.gravity

        val x: Float
        val y: Float
        when (gravity and Gravity.HORIZONTAL_GRAVITY_MASK) {
            Gravity.LEFT -> {
                x = (drawRect.left + drawConfig.paddingLeft).toFloat()
                PAINT.textAlign = Paint.Align.LEFT
            }

            Gravity.RIGHT -> {
                x = (drawRect.right - drawConfig.paddingRight).toFloat()
                PAINT.textAlign = Paint.Align.RIGHT
            }

            Gravity.CENTER_HORIZONTAL -> {
                x = drawRect.left + drawRect.width() / 2f
                PAINT.textAlign = Paint.Align.CENTER
            }

            else -> {
                x = (drawRect.left + drawConfig.paddingLeft).toFloat()
                PAINT.textAlign = Paint.Align.LEFT
            }
        }

        y = when (gravity and Gravity.VERTICAL_GRAVITY_MASK) {
            Gravity.TOP -> (drawRect.top + drawConfig.paddingTop).toFloat()

            Gravity.BOTTOM -> drawRect.bottom.toFloat() - textHeight - drawConfig.paddingBottom.toFloat()

            Gravity.CENTER_VERTICAL -> drawRect.top + (drawRect.height() - textHeight) / 2

            else -> (drawRect.top + drawConfig.paddingTop).toFloat()
        }

        if (drawConfig.borderSize == 0
            && drawConfig.paddingLeft == 0
            && drawConfig.paddingRight == 0
            && drawConfig.paddingTop == 0
            && drawConfig.paddingBottom == 0
        ) {

            var cut = false
            if (!highVersion && !drawConfig.isMultiLine) {
                val singleTextHeight = textHeight
                // 保证单行
                TEMP_RECT.set(
                    drawRect.left,
                    y.toInt(),
                    drawRect.right,
                    (y + singleTextHeight).toInt()
                )
                if (TEMP_RECT.width() <= 0 || TEMP_RECT.height() <= 0) {
                    return
                }
                cut = true
            }

            canvas.save()
            if (cut) {
                canvas.clipRect(TEMP_RECT)
            }
            canvas.translate(x, y)
            staticLayout.draw(canvas)
            canvas.restore()
        } else {
            if (!highVersion && !drawConfig.isMultiLine) {
                // 低版本保证单行

                var top = drawRect.top + drawConfig.paddingTop + drawConfig.borderSize / 2
                if (top < y) {
                    top = y.toInt()
                }

                var bottom = drawRect.bottom - drawConfig.paddingBottom - drawConfig.borderSize / 2
                val singleTextHeight = textHeight
                if (bottom > y + singleTextHeight) {
                    bottom = (y + singleTextHeight).toInt()
                }

                TEMP_RECT.set(
                    drawRect.left + drawConfig.paddingLeft + drawConfig.borderSize / 2,
                    top,
                    drawRect.right - drawConfig.paddingRight - drawConfig.borderSize / 2,
                    bottom
                )
            } else {
                TEMP_RECT.set(
                    drawRect.left + drawConfig.paddingLeft + drawConfig.borderSize / 2,
                    drawRect.top + drawConfig.paddingTop + drawConfig.borderSize / 2,
                    drawRect.right - drawConfig.paddingRight - drawConfig.borderSize / 2,
                    drawRect.bottom - drawConfig.paddingBottom - drawConfig.borderSize / 2
                )
            }

            if (TEMP_RECT.width() > 0 && TEMP_RECT.height() > 0) {
                canvas.save()
                canvas.clipRect(TEMP_RECT)
                canvas.translate(x, y)
                staticLayout.draw(canvas)
                canvas.restore()
            }
        }
    }

    /**
     * 填充绘制文字画笔
     */
    private fun fillTextPaint(drawConfig: DrawConfig?) {
        PAINT.reset()
        PAINT.isAntiAlias = true
        PAINT.style = Paint.Style.FILL
        if (drawConfig == null) {
            PAINT.textSize = 16f
            PAINT.color = Color.BLACK
        } else {
            PAINT.textSize = drawConfig.textSize
            PAINT.color = drawConfig.textColor
        }
    }

    /**
     * 填充绘制背景画笔
     */
    private fun fillBackgroundPaint(drawConfig: DrawConfig?) {
        PAINT.reset()
        PAINT.isAntiAlias = true
        PAINT.style = Paint.Style.FILL
        if (drawConfig == null) {
            PAINT.color = Color.TRANSPARENT
        } else {
            PAINT.color = drawConfig.backgroundColor
        }
    }

    /**
     * 填充绘制边框画笔
     */
    private fun fillBorderPaint(drawConfig: DrawConfig?) {
        PAINT.reset()
        PAINT.isAntiAlias = true
        PAINT.style = Paint.Style.STROKE
        if (drawConfig == null) {
            PAINT.color = Color.GRAY
            PAINT.strokeWidth = 1f
        } else {
            PAINT.color = drawConfig.borderColor
            PAINT.strokeWidth = drawConfig.borderSize.toFloat()
        }
    }

    /**
     * 绘制内容配置
     */
    class DrawConfig {
        /**
         * 文字颜色
         */
        var textColor: Int = Color.BLACK

        /**
         * 文字大小
         */
        var textSize: Float = 0.toFloat()

        /**
         * 文字绘制位置
         */
        var gravity: Int = Gravity.LEFT

        /**
         * 左边距
         */
        var paddingLeft: Int = 0

        /**
         * 右边距
         */
        var paddingRight: Int = 0

        /**
         * 上边距
         */
        var paddingTop: Int = 0

        /**
         * 下边距
         */
        var paddingBottom: Int = 0

        /**
         * 是否绘制背景
         */
        var isDrawBackground: Boolean = false
        /**
         * 背景颜色
         */
        var backgroundColor: Int = Color.TRANSPARENT

        /**
         * 边框线条粗细,<=0则不绘制
         */
        var borderSize: Int = 0

        /**
         * 边框颜色
         */
        var borderColor: Int = Color.TRANSPARENT

        /**
         * 是否多行绘制
         */
        var isMultiLine: Boolean = false
    }

    companion object {
        private val PAINT = TextPaint(Paint.ANTI_ALIAS_FLAG)
        private val TEMP_RECT = Rect()
    }
}
