package com.wgw.table.kotlin.demo

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.TextPaint
import android.view.Gravity
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.keqiang.table.kotlin.Table
import com.keqiang.table.kotlin.draw.TextCellDraw
import com.keqiang.table.kotlin.interfaces.CellFactory
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.DragChangeSizeType
import com.keqiang.table.kotlin.model.FirstRowColumnCellActionType
import com.keqiang.table.kotlin.model.FixGravity
import com.wgw.table.demo.R
import java.util.*

class TableActivity : AppCompatActivity() {
    private lateinit var mTable: Table<Cell>
    private lateinit var mTextPaint: Paint
    private lateinit var mRowList: MutableList<Row>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_table)
        mTable = findViewById(R.id.table)

        // mTable.getTableConfig().setColumnWidth(200);
        // mTable.tableConfig.rowHeight = 100
        mTable.tableConfig.addRowFix(0, FixGravity.TOP_ROW)
        // mTable.tableConfig.addRowFix(3, FixGravity.TOP_ROW)
        // mTable.tableConfig.addRowFix(5, FixGravity.TOP_ROW)
        //
        mTable.tableConfig.addColumnFix(0, FixGravity.LEFT_COLUMN)
        // mTable.tableConfig.addColumnFix(3, FixGravity.LEFT_COLUMN)
        // mTable.tableConfig.addColumnFix(5, FixGravity.LEFT_COLUMN)

        mTable.tableConfig.isHighLightSelectRow = true
        mTable.tableConfig.isHighLightSelectColumn = true
        mTable.tableConfig.isBothHighLightRowAndColumn = true
        mTable.tableConfig.firstRowColumnCellHighLightType = FirstRowColumnCellActionType.BOTH

        mTable.tableConfig.rowDragChangeHeightType = DragChangeSizeType.LONG_PRESS
        mTable.tableConfig.columnDragChangeWidthType = DragChangeSizeType.LONG_PRESS
        mTable.tableConfig.firstRowColumnCellDragType = FirstRowColumnCellActionType.BOTH

        // mTable.tableConfig.needRecoveryHighLightOnDragChangeSizeEnded = false

        mTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
        mTextPaint.textSize = 30f
        mTextPaint.color = Color.BLACK

        mRowList = ArrayList()
        for (i in 0..29) {
            val row = Row()
            mRowList.add(row)

            val columns = ArrayList<Column>()
            row.mColumns = columns
            for (j in 0..9) {
                val column = Column()
                column.text = "test$i$j"
                columns.add(column)
            }
        }

        mTable.cellDraw = TestTextCellDraw()
        mTable.cellFactory = object : CellFactory<Cell> {
            override fun get(row: Int, column: Int): Cell {
                return object : Cell(mRowList[row].mColumns!![column].text) {
                    override fun measureWidth(): Int {
                        return (60 + mTextPaint.measureText(getData())).toInt()
                    }
                }
            }
        }

        mTable.tableData.setNewData(30, 10)
    }

    fun addRow(view: View) {
        val rowList = ArrayList<Row>()
        val columnSize = mRowList[0].mColumns!!.size
        for (i in 0..1) {
            val row = Row()
            rowList.add(row)

            val columns = ArrayList<Column>()
            row.mColumns = columns
            for (j in 0 until columnSize) {
                val column = Column()
                column.text = "addRow$i$j"
                columns.add(column)
            }
        }

        mRowList.addAll(1, rowList)

        mTable.tableData.addRowData(2, 1)
    }

    fun addColumn(view: View) {
        for (i in mRowList.indices) {
            val row = mRowList[i]
            val columns = ArrayList<Column>()
            for (j in 0..1) {
                val column = Column()
                column.text = "addColumn$i$j"
                columns.add(column)
            }
            row.mColumns!!.addAll(1, columns)
        }
        mTable.tableData.addColumnData(2, 1)
    }

    fun deleteColumn(view: View) {
        for (row in mRowList) {
            row.mColumns!!.removeAt(1)
            row.mColumns!!.removeAt(1)
        }
        mTable.tableData.deleteColumnRange(1, 3)
    }

    fun deleteRow(view: View) {
        mRowList.removeAt(1)
        mRowList.removeAt(1)
        mTable.tableData.deleteRowRange(1, 3)
    }

    inner class TestTextCellDraw : TextCellDraw<Cell>() {
        private var mDrawConfig: DrawConfig = DrawConfig()

        init {
            mDrawConfig.textColor = Color.BLACK
            mDrawConfig.textSize = 30f
            mDrawConfig.borderSize = 2
            mDrawConfig.borderColor = Color.GRAY
            mDrawConfig.gravity = Gravity.CENTER
            mDrawConfig.backgroundColor = Color.LTGRAY
        }

        override fun getConfig(row: Int, column: Int): DrawConfig {
            mDrawConfig.isDrawBackground = row == 0 || column == 0
            return mDrawConfig
        }
    }

    class Row {
        var mColumns: MutableList<Column>? = null
    }

    class Column {
        var text: String? = null
    }
}
