package com.wgw.table.kotlin.demo

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.wgw.table.demo.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun goTableActivity(view: View) {
        val intent = Intent(this, TableActivity::class.java)
        startActivity(intent)
    }

    fun goSurfaceTableActivity(view: View) {
        val intent = Intent(this, SurfaceTableActivity::class.java)
        startActivity(intent)
    }
}
