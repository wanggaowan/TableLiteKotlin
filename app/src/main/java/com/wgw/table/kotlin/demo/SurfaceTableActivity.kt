package com.wgw.table.kotlin.demo

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.keqiang.table.kotlin.SurfaceTable
import com.keqiang.table.kotlin.TableConfig
import com.keqiang.table.kotlin.draw.TextCellDraw
import com.keqiang.table.kotlin.interfaces.CellFactory
import com.keqiang.table.kotlin.model.Cell
import com.keqiang.table.kotlin.model.DragChangeSizeType
import com.keqiang.table.kotlin.model.FirstRowColumnCellActionType
import com.keqiang.table.kotlin.model.FixGravity
import com.wgw.table.demo.R
import java.util.*

class SurfaceTableActivity : AppCompatActivity() {
    private lateinit var mTable: SurfaceTable<AutoSizeCell>

    private lateinit var mRowList: MutableList<Row>
    private var mAlertDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_surface_table)
        mTable = findViewById(R.id.tableSurface)

        // 设置全局宽
        // mTable.getTableConfig().setColumnWidth(200);
        // 设置全局高
        // mTable.getTableConfig().setRowHeight(100);

        mTable.tableConfig.addRowFix(0, FixGravity.TOP_ROW)
        mTable.tableConfig.addColumnFix(0, FixGravity.LEFT_COLUMN)
        mTable.tableConfig.isHighLightSelectRow = true
        mTable.tableConfig.isHighLightSelectColumn = true
        mTable.tableConfig.firstRowColumnCellHighLightType = FirstRowColumnCellActionType.ROW

        mTable.tableConfig.rowDragChangeHeightType = DragChangeSizeType.LONG_PRESS
        mTable.tableConfig.columnDragChangeWidthType = DragChangeSizeType.LONG_PRESS
        mTable.tableConfig.firstRowColumnCellDragType = FirstRowColumnCellActionType.BOTH

        mTable.touchHelper.cellClickListener = { row, column ->
            Toast.makeText(
                this@SurfaceTableActivity,
                "row:$row,column:$column",
                Toast.LENGTH_SHORT
            ).show()
        }

        mRowList = ArrayList()
        for (i in 0..49) {
            val row = Row()
            mRowList.add(row)

            val columns = ArrayList<Column>()
            row.mColumns = columns
            for (j in 0..7) {
                val column = Column()
                column.text = "test$i$j\ntest$i$j"
                columns.add(column)
            }
        }

        mTable.cellDraw = TestTextCellDraw()
        // 设置固定宽高
        //        mTable.setCellFactory((row, column) -> new AutoSizeCell(200,100,mRowList.get(row).mColumns.get(column).text));
        mTable.cellFactory = object : CellFactory<AutoSizeCell> {
            override fun get(row: Int, column: Int): AutoSizeCell {
                return AutoSizeCell(mRowList[row].mColumns!![column].text!!)
            }
        }
        mTable.tableData.setNewData(mRowList.size, mRowList[0].mColumns!!.size)
    }

    fun addRow(view: View) {
        val rowList = ArrayList<Row>()
        val columnSize = mRowList[0].mColumns!!.size
        for (i in 0..0) {
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

        mTable.tableData.addRowData(1, 1)
    }

    fun addColumn(view: View) {
        for (i in mRowList.indices) {
            val row = mRowList[i]
            val columns = ArrayList<Column>()
            for (j in 0..0) {
                val column = Column()
                column.text = "addColumn$i$j"
                columns.add(column)
            }
            row.mColumns!!.addAll(1, columns)
        }
        mTable.tableData.addColumnData(1, 1)
    }

    fun deleteColumn(view: View) {
        if (mTable.tableData.totalColumn < 2) {
            return
        }

        for (row in mRowList) {
            row.mColumns!!.removeAt(1)
        }
        mTable.tableData.deleteColumnRange(1, 2)
    }

    fun deleteRow(view: View) {
        if (mTable.tableData.totalRow < 2) {
            return
        }
        mRowList.removeAt(1)
        mTable.tableData.deleteRowRange(1, 2)
    }

    fun areaupdate(view: View) {
        val newText = "updateupdate" + Random().nextInt(100)
        mRowList[1].mColumns!![1].text = newText
        mTable.syncReDrawCell(1, 1, newText)
    }

    fun swapRow(view: View) {
        mTable.tableData.swapRow(0, 1)
    }

    fun swapColumn(view: View) {
        mTable.tableData.swapColumn(0, 1)
    }

    fun testZero(view: View) {
        mAlertDialog = AlertDialog.Builder(this)
            .setPositiveButton("取消") { _, _ -> mAlertDialog!!.dismiss() }
            .setNegativeButton("确定") { _, _ -> mAlertDialog!!.dismiss() }
            .setCancelable(false)
            .setMessage("test")
            .create()
        mAlertDialog!!.show()
    }

    inner class TestTextCellDraw : TextCellDraw<AutoSizeCell>() {
        private var mDrawConfig: TextCellDraw.DrawConfig = TextCellDraw.DrawConfig()

        init {
            mDrawConfig.textColor = Color.BLACK
            mDrawConfig.textSize = 30f
            mDrawConfig.borderSize = 2
            mDrawConfig.borderColor = -0x131314
            mDrawConfig.gravity = Gravity.CENTER
            mDrawConfig.isMultiLine = true
        }

        override fun getConfig(row: Int, column: Int): TextCellDraw.DrawConfig {
            if (column == 0 || row == 0) {
                mDrawConfig.isDrawBackground = true
                mDrawConfig.backgroundColor = -0x2c2c2d
            } else {
                mDrawConfig.isDrawBackground = false
            }
            return mDrawConfig
        }
    }

    class Row {
        var mColumns: MutableList<Column>? = null
    }

    class Column {
        var text: String? = null
    }

    class AutoSizeCell @JvmOverloads constructor(
        data: Any?,
        width: Int = TableConfig.INVALID_VALUE,
        height: Int = TableConfig.INVALID_VALUE
    ) : Cell(data, width, height) {

        override fun measureWidth(): Int {
            // 当前单元格宽度和全局宽度都设置为TableConfig.INVALID_VALUE 自适应宽度,测量逻辑则根据IDraw中绘制逻辑选择不同的测量方案
            val data = getData<String>()
            val split = data.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            var maxWidth = 0f
            for (s in split) {
                val v = 60 + textPaint!!.measureText(s)
                if (v > maxWidth) {
                    maxWidth = v
                }
            }
            return maxWidth.toInt()
        }

        override fun measureHeight(): Int {
            // 当前单元格高度和全局高度都设置为TableConfig.INVALID_VALUE 自适应高度，测量逻辑则根据IDraw中绘制逻辑选择不同的测量方案
            val text = getData<String>()
            val staticLayout = StaticLayout(
                text, 0, text.length,
                textPaint, 200,
                Layout.Alignment.ALIGN_NORMAL, 1f, 0f, false
            )
            return staticLayout.height + 60
        }

        companion object {
            private var textPaint: TextPaint? = null

            init {
                textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG)
                textPaint!!.textSize = 30f
            }
        }
    }
}
