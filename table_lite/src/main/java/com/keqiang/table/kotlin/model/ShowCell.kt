package com.keqiang.table.kotlin.model

import android.graphics.Canvas
import android.graphics.Rect
import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.interfaces.ICellDraw

/**
 * 记录显示在界面的单元格数据
 * <br></br>create by 汪高皖 on 2019/1/19 21:27
 */
class ShowCell private constructor() : ObjectPool.Poolable() {

    /**
     * 单元格所在行
     */
    var row: Int = TableConfig.INVALID_VALUE

    /**
     * 单元格所在列
     */
    var column: Int = TableConfig.INVALID_VALUE

    /**
     * 单元格绘制矩形，这是界面真实绘制的位置。
     * 此矩形大小可能比实际界面看到的大小要大，原因在于一些固定的行列遮挡了非固定行列的内容，
     * 但是非固定行列在绘制时还是需要按照实际大小进行绘制，否则内容就会显示错位，
     * 遮挡会通过[Canvas.clipRect]实现，用户在[ICellDraw.onCellDraw]
     * 时无需关心，只需按照传入的Rect范围内容进行绘制逻辑即可
     */
    val drawRect: Rect = Rect()

    /**
     * 是否是行固定
     */
    var isFixRow: Boolean = false

    /**
     * 是否是列固定
     */
    var isFixColumn: Boolean = false

    /**
     * 单利对象
     */
    private object Inner {
        internal var pool: ObjectPool<ShowCell> = ObjectPool.create(64, ShowCell())

        init {
            pool.replenishPercentage = 0.5f
        }
    }

    override fun instantiate(): ObjectPool.Poolable {
        return ShowCell()
    }

    override fun recycle() {
        drawRect.setEmpty()
        isFixRow = false
        isFixColumn = false
        row = TableConfig.INVALID_VALUE
        column = TableConfig.INVALID_VALUE
    }

    companion object {

        /**
         * 获取实例
         */
        val instance: ShowCell
            get() = Inner.pool.get()

        /**
         * 获取实例
         */
        fun getInstance(
            row: Int,
            column: Int,
            drawRect: Rect,
            fixRow: Boolean,
            fixColumn: Boolean
        ): ShowCell {
            val result = Inner.pool.get()
            result.row = row
            result.column = column
            result.drawRect.set(drawRect)
            result.isFixRow = fixRow
            result.isFixColumn = fixColumn
            return result
        }

        /**
         * 回收实例
         */
        fun recycleInstance(instance: ShowCell) {
            Inner.pool.recycle(instance)
        }

        /**
         * 回收实例集合
         */
        fun recycleInstances(instances: List<ShowCell>) {
            Inner.pool.recycle(instances)
        }
    }
}
