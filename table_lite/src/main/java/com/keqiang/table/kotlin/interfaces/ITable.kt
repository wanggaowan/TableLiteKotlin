package com.keqiang.table.kotlin.interfaces

import android.content.Context
import android.graphics.Rect
import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.TouchHelper
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.ShowCell
import com.keqiang.table.kotlin.model.TableData

/**
 * @author Created by 汪高皖 on 2019/1/18 0018 10:49
 */
interface ITable<T : Cell> {
    val viewContext: Context

    /**
     * 获取单元格数据
     */
    var cellFactory: CellFactory<T>?

    /**
     * 用于绘制表格背景以及单元格内容
     */
    var cellDraw: ICellDraw<T>?

    /**
     * @return [TableConfig]，该类主要配置
     */
    val tableConfig: TableConfig

    /**
     * @return [TableData]，处理表格单元数据的增删操作
     */
    val tableData: TableData<T>

    /**
     * @return [TouchHelper],处理点击，移动逻辑，配置点击监听等相关操作
     */
    val touchHelper: TouchHelper<T>

    /**
     * @return 表格在屏幕显示大小，只读，修改该值并不会实际生效
     */
    val showRect: Rect

    /**
     * @return 表格实际大小，只读，修改该值并不会实际生效
     */
    val actualSizeRect: Rect

    /**
     * @return 界面可见范围被绘制出来的单元格, 只读(但这只能限制集合, 对于集合元素ShowCell无效, 因此建议不要修改ShowCell内容, 否则可能导致绘制, 点击等出现非预期错误)。
     * 单元格在集合中位置优先级：行列均固定 > 行固定 > 列固定 > 行列均不固定，这也是绘制的优先级
     */
    val showCells: List<ShowCell>

    /**
     * 通知异步重绘，可在非UI线程调用
     */
    fun asyncReDraw()

    /**
     * 通知同步重绘，必须在UI线程调用
     */
    fun syncReDraw()
}
