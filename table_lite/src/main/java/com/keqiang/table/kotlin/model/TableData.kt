package com.keqiang.table.kotlin.model

import android.os.*
import android.view.View
import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.interfaces.CellFactory
import com.keqiang.table.kotlin.interfaces.ICellDraw
import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.util.Logger
import com.keqiang.table.kotlin.util.Utils
import java.util.*

/**
 * 表格数据。此数据只记录总行数，总列数，每行每列单元格数据。重新设置或增加数据时第一次通过[CellFactory]获取单元格数据，
 * 此时需要将绘制的数据通过[Cell.setData]绑定，最终绘制到界面的数据通过调用[ICellDraw.onCellDraw]实现。
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 08:55
 */
class TableData<T : Cell>(private val table: ITable<T>) {
    /**
     * 表格行数据，并不一定在改变后就立马同步显示到界面。在调用[setNewData]等改变行数据的方法时，
     * 此数据立即发生变更，但是界面数据还是使用[mRowsFinal],直至所有数据处理完毕再同步至[mRowsFinal]
     * 并且最终在界面显示
     */
    private val mRows: MutableList<Row<T>> = mutableListOf()

    /**
     * 表格列数据，并不一定在改变后就立马同步显示到界面。在调用[setNewData]等改变列数据的方法时，
     * 此数据立即发生变更，但是界面数据还是使用[mColumnsFinal],直至所有数据处理完毕再同步至[mColumnsFinal]
     * 并且最终在界面显示
     */
    private val mColumns: MutableList<Column<T>> = mutableListOf()

    /**
     * 最终绘制到界面的数据，不保证获取到的是最新数据，可能在获取的时刻，最新数据还在处理之中
     */
    private val mRowsFinal: MutableList<Row<T>> = mutableListOf()

    /**
     * 最终绘制到界面的数据，不保证获取到的是最新数据，可能在获取的时刻，最新数据还在处理之中
     */
    private val mColumnsFinal: MutableList<Column<T>> = mutableListOf()

    // 通过HandlerThread来实现数据在异步线程同步执行，以此实现同步锁机制
    private var mHandlerThread: HandlerThread? = null
    private var mHandler: Handler? = null

    private var dataProcessFinishListener: (() -> Unit)? = null

    init {
        createHandlerThread()
        table.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {
                Logger.d(TAG, "call onViewDetachedFromWindow")
                destroyHandlerThread()
            }

            override fun onViewAttachedToWindow(v: View?) {
                Logger.d(TAG, "call onViewAttachedToWindow")
                createHandlerThread()
            }
        })
    }

    private fun createHandlerThread() {
        if (mHandlerThread?.isAlive == true) {
            Logger.d(TAG, "handlerThread isAlive")
            return
        }

        Logger.d(TAG, "call createHandlerThread")
        mHandlerThread = HandlerThread("table data")
        mHandlerThread!!.start()
        mHandler = Handler(mHandlerThread!!.looper)
        try {
            val field = Looper::class.java.getDeclaredField("mQueue")
            field.isAccessible = true
            val queue = field.get(mHandlerThread!!.looper) as MessageQueue
            queue.addIdleHandler {
                Logger.d(TAG, "call queueIdle")
                // 用此方法来达到极短时间内数据发生变更，界面无需每次变更都去绘制，而是在最后一次数据变更结束后刷新一次的效果
                table.post(Runnable {
                    mRowsFinal.clear()
                    mColumnsFinal.clear()
                    mRowsFinal.addAll(mRows)
                    mColumnsFinal.addAll(mColumns)
                    table.syncReDraw()
                    dataProcessFinishListener?.invoke()
                })
                true
            }
        } catch (e: NoSuchFieldException) {
            e.printStackTrace()
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        }
    }

    private fun destroyHandlerThread() {
        Logger.d(TAG, "call destroyHandlerThread")
        if (mHandler != null) {
            mHandler!!.removeCallbacksAndMessages(null)
            mHandler = null
        }

        if (mHandlerThread != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                mHandlerThread!!.quitSafely()
            } else {
                mHandlerThread!!.quit()
            }
        }
    }

    /**
     * 总行数，不保证获取到的是最新数据，可能在获取的时刻，最新数据还在处理之中，如需获取最新数据，可通过设置
     * [setDataProcessFinishListener]监听在回调中调用该方法
     */
    val totalRow: Int
        get() = mRowsFinal.size

    /**
     * 总列数，不保证获取到的是最新数据，可能在获取的时刻，最新数据还在处理之中，如需获取最新数据，可通过设置
     * [setDataProcessFinishListener]监听在回调中调用该方法
     */
    val totalColumn: Int
        get() = mColumnsFinal.size

    /**
     * 行数据，不保证获取到的是最新数据，可能在获取的时刻，最新数据还在处理之中，如需获取最新数据，可通过设置
     * [setDataProcessFinishListener]监听在回调中调用该方法
     */
    val rows: List<Row<T>>
        get() = mRowsFinal

    /**
     * 列数据，不保证获取到的是最新数据，可能在获取的时刻，最新数据还在处理之中，如需获取最新数据，可通过设置
     * [setDataProcessFinishListener]监听在回调中调用该方法
     */
    val columns: List<Column<T>>
        get() = mColumnsFinal

    /**
     * 设置表格数据处理完成监听，由于数据的处理都是异步的，所以获取方法获取的数据不能保证在获取时刻是最新的，
     * 因此如果需要获取的数据和界面显示数据一致，可通过设置此监听，在回调中调用获取方法
     */
    fun setDataProcessFinishListener(listener: (() -> Unit)?) {
        dataProcessFinishListener = listener
    }

    /**
     * 设置新数据(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 调用此方法之前，请确保[ITable.cellFactory]不为null，否则将不做任何处理
     *
     * @param totalRow    表格行数
     * @param totalColumn 表格列数
     */
    fun setNewData(totalRow: Int, totalColumn: Int) {
        if (totalRow <= 0 || totalColumn <= 0) {
            mRows.clear()
            mColumns.clear()
            mRowsFinal.clear()
            mColumnsFinal.clear()
            dataProcessFinishListener?.invoke()
            table.asyncReDraw()
            return
        }

        if (table.cellFactory == null) {
            return
        }

        mHandler?.post {
            mRows.clear()
            mColumns.clear()
            mapCellDataByRow(0, 0, totalRow, totalColumn)

            for (i in 0 until totalRow) {
                val row = mRows[i]
                val actualRowHeight =
                    Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }

            for (i in 0 until totalColumn) {
                val column = mColumns[i]
                val actualColumnWidth =
                    Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }
        }
    }

    /**
     * 增加行数据(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 调用此方法之前，请确保[ITable.cellFactory]不为null，否则将不做任何处理。
     *
     * @param addRowCount    新增加的行数，数据会通过[CellFactory.get]获取
     * @param insertPosition 新数据插入位置，如果<=0则插入在头部，如果>=[totalRow]，则插入的尾部，
     * 否则插入到指定位置
     */
    @JvmOverloads
    fun addRowData(addRowCount: Int, insertPosition: Int = mRows.size) {
        if (addRowCount <= 0) {
            return
        }

        table.cellFactory ?: return

        mHandler?.post {
            val preTotalRow = mRows.size
            val position: Int = when {
                insertPosition < 0 -> 0
                insertPosition > preTotalRow -> preTotalRow
                else -> insertPosition
            }

            mapCellDataByRow(
                position,
                preTotalRow,
                preTotalRow + addRowCount,
                mColumns.size
            )

            for (i in position until position + addRowCount) {
                val row = mRows[i]
                val actualRowHeight =
                    Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }

            for (i in mColumns.indices) {
                val column = mColumns[i]
                if (column.width != TableConfig.INVALID_VALUE) {
                    val actualColumnWidth = Utils.getActualColumnWidth(
                        column,
                        position,
                        position + addRowCount,
                        table.tableConfig
                    )
                    if (actualColumnWidth > column.width) {
                        column.width = actualColumnWidth
                    }
                } else {
                    val actualColumnWidth = Utils.getActualColumnWidth(
                        column,
                        position,
                        position + addRowCount,
                        table.tableConfig
                    )
                    column.width = actualColumnWidth
                }
            }
        }
    }

    /**
     * 增加列数据(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 调用此方法之前，请确保[ITable.cellFactory]不为null，否则将不做任何处理。
     *
     * @param addColumnCount 新增加的行数，数据会通过[CellFactory.get]获取
     * @param insertPosition 新数据插入位置，如果<=0则插入在左边，如果>=[totalColumn]，则插入在右边
     * 否则插入到指定位置
     */
    @JvmOverloads
    fun addColumnData(addColumnCount: Int, insertPosition: Int = mColumns.size) {
        if (addColumnCount <= 0) {
            return
        }

        table.cellFactory ?: return

        mHandler?.post {
            val preTotalColumn = mColumns.size
            val position: Int = when {
                insertPosition < 0 -> 0
                insertPosition > preTotalColumn -> preTotalColumn
                else -> insertPosition
            }

            mapCellDataByColumn(
                position,
                preTotalColumn,
                preTotalColumn + addColumnCount,
                mRows.size
            )

            for (i in position until position + addColumnCount) {
                val column = mColumns[i]
                val actualColumnWidth =
                    Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }

            for (i in mRows.indices) {
                val row = mRows[i]
                if (row.height != TableConfig.INVALID_VALUE) {
                    val actualRowHeight = Utils.getActualRowHeight(
                        row,
                        position,
                        position + addColumnCount,
                        table.tableConfig
                    )
                    if (actualRowHeight > row.height) {
                        row.height = actualRowHeight
                    }
                } else {
                    val actualRowHeight = Utils.getActualRowHeight(
                        row,
                        position,
                        position + addColumnCount,
                        table.tableConfig
                    )
                    row.height = actualRowHeight
                }
            }
        }
    }

    /**
     * 删除行数据(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 建议之前为该行提供数据的数据源自行删除，不删除并不影响此次操作，但是可能会在[setNewData]时出现问题，
     * 因为数据源并没有发生变化，调用[setNewData]，表格数据还是之前的，本次操作并未生效
     *
     * @param positions 行所在位置
     */
    fun deleteRow(vararg positions: Int) {
        if (positions.isEmpty()) {
            return
        }

        mHandler?.post {
            val deleteRows = ArrayList<Row<*>>()
            for (position in positions) {
                if (position < 0 || position >= mRows.size) {
                    continue
                }
                val row = mRows[position]
                deleteRows.add(row)
                for (j in mColumns.indices) {
                    val cell = row.cells[j]
                    mColumns[j].cells.remove(cell)
                }
            }

            if (deleteRows.size == 0) {
                return@post
            }

            mRows.removeAll(deleteRows)

            // 重新统计所有列宽
            for (i in 0 until mColumns.size) {
                val column = mColumns[i]
                val actualColumnWidth =
                    Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }
        }
    }

    /**
     * 按区间删除行(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 建议之前为该区间行提供数据的数据源自行删除，不删除并不影响此次操作，但是可能会在[setNewData]时出现问题，
     * 因为数据源并没有发生变化，调用[setNewData]，表格数据还是之前的，本次操作并未生效。
     * 如果只想删除开始下标位置的数据，可调用[deleteRow]或end = start + 1
     *
     * @param start 开始下标，必须满足 0 <= start < [totalRow] && start < end下标
     * @param end   结束下标，必须满足 start < end <= [totalRow]
     */
    fun deleteRowRange(start: Int, end: Int) {
        mHandler?.post {
            if (start < 0 || start >= mRows.size || end < start || end > mRows.size) {
                return@post
            }

            val deleteRows = ArrayList<Row<*>>()
            for (i in start until end) {
                val row = mRows[i]
                deleteRows.add(row)
                for (j in mColumns.indices) {
                    val cell = row.cells[j]
                    mColumns[j].cells.remove(cell)
                }
            }

            if (deleteRows.size == 0) {
                return@post
            }

            mRows.removeAll(deleteRows)

            // 重新统计所有列宽
            for (i in 0 until mColumns.size) {
                val column = mColumns[i]
                val actualColumnWidth =
                    Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }
        }
    }

    /**
     * 删除列(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 建议之前为该列提供数据的数据源自行删除，不删除并不影响此次操作，但是可能会在[setNewData]时出现问题，
     * 因为数据源并没有发生变化，调用[setNewData]，表格数据还是之前的，本次操作并未生效
     *
     * @param positions 列所在位置
     */
    fun deleteColumn(vararg positions: Int) {
        if (positions.isEmpty()) {
            return
        }

        mHandler?.post {
            val deleteColumns = ArrayList<Column<*>>()
            val totalColumn = mColumns.size
            for (position in positions) {
                if (position < 0 || position >= totalColumn) {
                    continue
                }
                val column = mColumns[position]
                deleteColumns.add(column)
                for (j in mRows.indices) {
                    val cell = column.cells[j]
                    mRows[j].cells.remove(cell)
                }
            }

            if (deleteColumns.size == 0) {
                return@post
            }

            mColumns.removeAll(deleteColumns)

            // 重新统计所有行高
            for (i in 0 until mRows.size) {
                val row = mRows[i]
                val actualRowHeight =
                    Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }
        }
    }

    /**
     * 按区间删除列(异步操作，可在任何线程调用)，数据处理完成后会主动调用界面刷新操作。
     * 建议之前为该区间列提供数据的数据源自行删除，不删除并不影响此次操作，但是可能会在[setNewData]时出现问题，
     * 因为数据源并没有发生变化，调用[setNewData]，表格数据还是之前的，本次操作并未生效。
     * 如果只想删除开始下标位置的数据，可调用[deleteRow]或 end = start + 1
     *
     * @param start 开始下标，必须满足 0 <= start < [totalColumn] && start < end下标
     * @param end   结束下标，必须满足 start < end <= [totalColumn].
     *
     */
    fun deleteColumnRange(start: Int, end: Int) {
        mHandler?.post {
            if (start < 0 || start >= mColumns.size || end <= start || end > mColumns.size) {
                return@post
            }

            val deleteColumns = ArrayList<Column<*>>()
            for (i in start until end) {
                val column = mColumns[i]
                deleteColumns.add(column)
                for (j in mRows.indices) {
                    val cell = column.cells[j]
                    mRows[j].cells.remove(cell)
                }
            }

            if (deleteColumns.size == 0) {
                return@post
            }

            mColumns.removeAll(deleteColumns)

            // 重新统计所有行高
            for (i in 0 until mRows.size) {
                val row = mRows[i]
                val actualRowHeight =
                    Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }
        }
    }

    /**
     * 清除表格数据，异步操作，数据处理完成后会主动调用界面刷新操作。
     */
    fun clear() {
        mHandler?.post {
            mRows.clear()
            mColumns.clear()
        }
    }

    /**
     * 列位置交换，异步操作，数据处理完成后会主动调用界面刷新操作。
     *
     * @param from 需要交换的位置
     * @param to   目标位置
     */
    fun swapColumn(from: Int, to: Int) {
        mHandler?.post {
            if (from >= mColumns.size || from < 0 || to >= mColumns.size || to < 0 || from == to) {
                return@post
            }

            Collections.swap(mColumns, from, to)
            for (row in mRows) {
                Collections.swap(row.cells, from, to)
            }
        }
    }

    /**
     * 行位置交换，异步操作，数据处理完成后会主动调用界面刷新操作。
     *
     * @param from 需要交换的位置
     * @param to   目标位置
     */
    fun swapRow(from: Int, to: Int) {
        mHandler?.post {
            if (from >= mRows.size || from < 0 || to >= mRows.size || to < 0 || from == to) {
                return@post
            }

            Collections.swap(mRows, from, to)
            for (column in mColumns) {
                Collections.swap(column.cells, from, to)
            }
        }
    }

    /**
     * 获取单元格数据
     *
     * @param insertPosition 插入位置
     * @param rowStart       获取单元格行起始位置
     * @param totalRow       总行数
     * @param totalColumn    总列数
     */
    private fun mapCellDataByRow(
        insertPosition: Int,
        rowStart: Int,
        totalRow: Int,
        totalColumn: Int
    ) {
        val cellFactory = table.cellFactory ?: return
        var position = insertPosition
        for (i in rowStart until totalRow) {
            val row = Row<T>()
            mRows.add(position, row)

            for (j in 0 until totalColumn) {
                val cell = cellFactory[position, j]
                row.cells.add(cell)
                if (j >= mColumns.size) {
                    val column = Column<T>()
                    mColumns.add(column)

                    column.cells.add(cell)
                } else {
                    mColumns[j].cells.add(position, cell)
                }
            }

            position++
        }
    }

    /**
     * 获取单元格数据
     *
     * @param insertPosition 插入位置
     * @param columnStart    获取单元格行起始位置
     * @param totalRow       总行数
     * @param totalColumn    总列数
     */
    private fun mapCellDataByColumn(
        insertPosition: Int,
        columnStart: Int,
        totalColumn: Int,
        totalRow: Int
    ) {
        val cellFactory = table.cellFactory ?: return
        var position = insertPosition
        for (i in columnStart until totalColumn) {
            val column = Column<T>()
            mColumns.add(position, column)

            for (j in 0 until totalRow) {
                val cell = cellFactory[j, position]
                column.cells.add(cell)
                if (j >= mRows.size) {
                    val row = Row<T>()
                    mRows.add(row)

                    row.cells.add(cell)
                } else {
                    mRows[j].cells.add(position, cell)
                }
            }

            position++
        }
    }

    companion object {
        private val TAG = TableData::class.java.simpleName
    }
}