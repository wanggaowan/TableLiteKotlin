package com.keqiang.table.kotlin.model

import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.interfaces.CellFactory
import com.keqiang.table.kotlin.interfaces.ICellDraw
import com.keqiang.table.kotlin.interfaces.ITable
import com.keqiang.table.kotlin.util.AsyncExecutor
import com.keqiang.table.kotlin.util.Utils
import java.util.*

/**
 * 表格数据。此数据只记录总行数，总列数，每行每列单元格数据。重新设置或增加数据时第一次通过[CellFactory]获取单元格数据，
 * 此时需要将绘制的数据通过[Cell.setData]绑定。最终绘制到界面的数据通过调用[ICellDraw.onCellDraw]实现
 *
 * @author Created by 汪高皖 on 2019/1/15 0015 08:55
 */
class TableData<T : Cell>(private val table: ITable<T>) {
    private val mRows: MutableList<Row<T>>
    private val mColumns: MutableList<Column<T>>

    /**
     * @return 总行数
     */
    val totalRow: Int
        get() = mRows.size

    /**
     * @return 总列数
     */
    val totalColumn: Int
        get() = mColumns.size

    init {
        mRows = ArrayList()
        mColumns = ArrayList()
    }

    /**
     * 行数据
     */
    val rows: List<Row<T>>
        get() = mRows

    /**
     * 列数据
     */
    val columns: List<Column<T>>
        get() = mColumns

    /**
     * 设置新数据，单元格数据会通过[CellFactory.get]获取
     *
     * @param totalRow    表格行数
     * @param totalColumn 表格列数
     */
    fun setNewData(totalRow: Int, totalColumn: Int) {
        if (totalRow <= 0 || totalColumn <= 0) {
            return
        }

        table.cellFactory ?: return

        mRows.clear()
        mColumns.clear()
        AsyncExecutor.execute(Runnable {
            mapCellDataByRow(0, 0, totalRow, totalColumn)

            for (i in 0 until totalRow) {
                val row = mRows[i]
                val actualRowHeight = Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }

            for (i in 0 until totalColumn) {
                val column = mColumns[i]
                val actualColumnWidth = Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }

            table.asyncReDraw()
        })
    }

    /**
     * 增加行数据
     *
     * @param addRowCount    新增加的行数，数据会通过[CellFactory.get]获取
     * @param insertPosition 新数据插入位置，如果<=0则插入在头部，如果>=[.getTotalRow]，则插入的尾部，
     * 否则插入到指定位置
     */
    @JvmOverloads
    fun addRowData(addRowCount: Int, insertPosition: Int = totalRow) {
        if (addRowCount <= 0) {
            return
        }

        table.cellFactory ?: return

        AsyncExecutor.execute(Runnable {
            val preTotalRow = totalRow
            val position: Int = when {
                insertPosition < 0 -> 0
                insertPosition > totalRow -> totalRow
                else -> insertPosition
            }
            mapCellDataByRow(position, preTotalRow, preTotalRow + addRowCount, totalColumn)

            for (i in position until position + addRowCount) {
                val row = mRows[i]
                val actualRowHeight = Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }

            for (i in mColumns.indices) {
                val column = mColumns[i]
                if (column.width != TableConfig.INVALID_VALUE) {
                    val actualColumnWidth = Utils.getActualColumnWidth(column, position, position + addRowCount, table.tableConfig)
                    if (actualColumnWidth > column.width) {
                        column.width = actualColumnWidth
                    }
                } else {
                    val actualColumnWidth = Utils.getActualColumnWidth(column, position, position + addRowCount, table.tableConfig)
                    column.width = actualColumnWidth
                }
            }

            table.asyncReDraw()
        })
    }

    /**
     * 增加列数据
     *
     * @param addColumnCount 新增加的行数，数据会通过[CellFactory.get]获取
     * @param insertPosition 新数据插入位置，如果<=0则插入在左边，如果>=[.getTotalColumn]，则插入在右边，
     * 否则插入到指定位置
     */
    @JvmOverloads
    fun addColumnData(addColumnCount: Int, insertPosition: Int = totalColumn) {
        if (addColumnCount <= 0) {
            return
        }

        table.cellFactory ?: return

        AsyncExecutor.execute(Runnable {
            val preTotalColumn = totalColumn
            val position: Int = when {
                insertPosition < 0 -> 0
                insertPosition > totalColumn -> totalColumn
                else -> insertPosition
            }

            mapCellDataByColumn(position, preTotalColumn, preTotalColumn + addColumnCount, totalRow)

            for (i in position until position + addColumnCount) {
                val column = mColumns[i]
                val actualColumnWidth = Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }

            for (i in mRows.indices) {
                val row = mRows[i]
                if (row.height != TableConfig.INVALID_VALUE) {
                    val actualRowHeight = Utils.getActualRowHeight(row, position, position + addColumnCount, table.tableConfig)
                    if (actualRowHeight > row.height) {
                        row.height = actualRowHeight
                    }
                } else {
                    val actualRowHeight = Utils.getActualRowHeight(row, position, position + addColumnCount, table.tableConfig)
                    row.height = actualRowHeight
                }
            }

            table.asyncReDraw()
        })
    }

    /**
     * 删除行数据，之前为该行提供数据的数据源需要自己删除，否则只会看到行减少，但是指定删除位置数据还是之前的
     *
     * @param positions 行所在位置
     */
    fun deleteRow(vararg positions: Int) {
        if (positions.isEmpty()) {
            return
        }

        AsyncExecutor.execute(Runnable {
            val deleteRows = ArrayList<Row<*>>()
            for (position in positions) {
                if (position < 0 || position >= totalRow) {
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
                return@Runnable
            }

            mRows.removeAll(deleteRows)

            // 重新统计所有列宽
            for (i in 0 until totalColumn) {
                val column = mColumns[i]
                val actualColumnWidth = Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }
            table.asyncReDraw()
        })
    }

    /**
     * 按区间删除行，之前为该行提供数据的数据源需要自己删除，否则只会看到行减少，但是指定删除位置数据还是之前的
     *
     * @param start 开始下标，必须满足 0 <= start < [.getTotalRow] && start < end下标
     * @param end   结束下标，必须满足 start < end <= [.getTotalRow].
     * 如果只想删除开始下标位置的数据，可调用[.deleteRow]或end = start + 1
     */
    fun deleteRowRange(start: Int, end: Int) {
        if (start < 0 || start >= totalRow || end < start || end > totalRow) {
            return
        }

        AsyncExecutor.execute(Runnable {
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
                return@Runnable
            }

            mRows.removeAll(deleteRows)

            // 重新统计所有列宽
            for (i in 0 until totalColumn) {
                val column = mColumns[i]
                val actualColumnWidth = Utils.getActualColumnWidth(column, 0, column.cells.size, table.tableConfig)
                column.width = actualColumnWidth
            }
            table.asyncReDraw()
        })
    }

    /**
     * 删除列，之前为该行提供数据的数据源需要自己删除，否则只会看到行减少，但是指定删除位置数据还是之前的
     *
     * @param positions 列所在位置
     */
    fun deleteColumn(vararg positions: Int) {
        if (positions.isEmpty()) {
            return
        }

        AsyncExecutor.execute(Runnable {
            val deleteColumns = ArrayList<Column<*>>()
            val totalColumn = totalColumn
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
                return@Runnable
            }

            mColumns.removeAll(deleteColumns)

            // 重新统计所有行高
            for (i in 0 until totalRow) {
                val row = mRows[i]
                val actualRowHeight = Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }
            table.asyncReDraw()
        })
    }

    /**
     * 按区间删除列，之前为该行提供数据的数据源需要自己删除，否则只会看到行减少，但是指定删除位置数据还是之前的
     *
     * @param start 开始下标，必须满足 0 <= start < [.getTotalColumn] && start < end下标
     * @param end   结束下标，必须满足 start < end <= [.getTotalColumn].
     * 如果只想删除开始下标位置的数据，可调用[.deleteRow]或 end = start + 1
     */
    fun deleteColumnRange(start: Int, end: Int) {
        if (start < 0 || start >= totalColumn || end <= start || end > totalColumn) {
            return
        }

        AsyncExecutor.execute(Runnable {
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
                return@Runnable
            }

            mColumns.removeAll(deleteColumns)

            // 重新统计所有行高
            for (i in 0 until totalRow) {
                val row = mRows[i]
                val actualRowHeight = Utils.getActualRowHeight(row, 0, row.cells.size, table.tableConfig)
                row.height = actualRowHeight
            }
            table.asyncReDraw()
        })
    }

    fun clear() {
        mRows.clear()
        mColumns.clear()
        table.syncReDraw()
    }

    /**
     * 列位置交换
     *
     * @param from 需要交换的位置
     * @param to   目标位置
     */
    fun swapColumn(from: Int, to: Int) {
        if (from >= totalColumn || from < 0 || to >= totalColumn || to < 0 || from == to) {
            return
        }

        AsyncExecutor.execute(Runnable {
            Collections.swap(mColumns, from, to)
            for (row in mRows) {
                Collections.swap(row.cells, from, to)
            }
            table.asyncReDraw()
        })
    }

    /**
     * 行位置交换
     *
     * @param from 需要交换的位置
     * @param to   目标位置
     */
    fun swapRow(from: Int, to: Int) {
        if (from >= totalColumn || from < 0 || to >= totalColumn || to < 0 || from == to) {
            return
        }

        AsyncExecutor.execute(Runnable {
            Collections.swap(mRows, from, to)
            for (column in mColumns) {
                Collections.swap(column.cells, from, to)
            }
            table.asyncReDraw()
        })
    }

    /**
     * 获取单元格数据
     *
     * @param insertPosition 插入位置
     * @param rowStart       获取单元格行起始位置
     * @param totalRow       总行数
     * @param totalColumn    总列数
     */
    private fun mapCellDataByRow(insertPosition: Int, rowStart: Int, totalRow: Int, totalColumn: Int) {
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
    private fun mapCellDataByColumn(insertPosition: Int, columnStart: Int, totalColumn: Int, totalRow: Int) {
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
}