# TableLite Kotlin语言实现
Android 轻量版Table组件，提供Table和SurfaceTable两种组件，
SurfaceTable采用SurfaceView实现界面绘制，因此相比Table，可实现局部
单元格刷新，不需要每次全局刷新，这对于表格中需要绘制网络照片来说可以极大的
提升性能和效率。
* [项目github地址](https://github.com/wanggaowan/TableLite)
* [版本更新日志](/update.md/)

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://jitpack.io/v/wanggaowan/TableLite.svg)](https://jitpack.io/#wanggaowan/TableLite)

#### 如何使用：
1. 添加 JitPack repository 到你的build文件
   ```groovy
    allprojects {
    	repositories {
    		...
    		maven { url 'https://www.jitpack.io' }
    	}
    }
   ```

2. 增加依赖
   ```groovy
   dependencies {
   	implementation 'com.github.wanggaowan:TableLiteKotlin:1.0'
   }
   ```

3. 使用表格View
   ```xml
   <com.keqiang.table.Table
           android:id="@+id/table"
           android:layout_width="match_parent"
           android:layout_height="match_parent"
           android:layout_margin="10dp"
           android:background="@android:color/white"/>
           
   <com.keqiang.table.SurfaceTable
           android:id="@+id/tableSurface"
           android:layout_width="match_parent"
           android:layout_height="match_parent"
           android:layout_margin="10dp"
           android:background="@android:color/white"/>        
   ```
#### 下面对Table和SurfaceTable中主要的类进行说明：
**1.TableConfig(表格全局数据配置)，可配置项如下：**
 - 全局行高，最大最小行高。
 - 全局列宽，最大最小列宽。
 - 是否需要固定行(顶部或底部)，是否需要固定列(左边或右边)
 - 是否需要行选中高亮，列选中高亮以及高亮颜色(设置为true时自动固定第一列或第一行)
 - 是否需要拖拽改变行高或列宽(三种配置：NONE,不需要,CLICK，点击即可修改,LONG_PRESS，长按后修改)，
  拖拽时方向指示器图标，指示器大小，是否绘制指示器(当需要拖拽改变行高或列宽设置为不是DragChangeSizeType.NONE
  时自动固定第一列或第一行)

**2.TableData(表格数据处理)，功能如下：**
 - 设置新数据
 - 增加行，可指定添加位置
 - 增加列，可指定添加位置
 - 删除行列，按位置和按区间
 - 清除数据
 - 行数据位置交换，列数据位置交换

**3.TouchHelper(表格触摸处理)，可配置项如下：**
 - 设置单元格点击监听
 - 设置Fling手势滑动速率，滑动方向（横向，纵向或双向）以及表格实际大小相较于
 显示区域大小的多少倍时才处理Fling手势

**4.CellFactory(接口)，用于动态获取单元格(Cell)数据，Cell用于设置单元格宽高
以及如果要自适应宽高时实现测量宽高值逻辑，另外将数据源绑定到Cell，这样就无需接口
模型转换成特定对象才可以使用。**

**5.IDraw(接口)，用于处理单元格绘制逻辑，目前只提供了一种实现：TextCellDraw，
该实现主要目的是作为一种绘制Demo，建议根据自己的需求实现自定义IDraw**

#### SurfaceTable中独有的方法
```java
 /**
  * 以同步方式局部刷新单元格
  *
  * @param row    需要刷新的单元格所在行
  * @param column 需要刷新的单元格所在列
  * @param data   新数据
  */
  public void syncReDrawCell(int row, int column, Object data) {
      // 代码省略
  }

 /**
  * 以异步方式局部刷新单元格
  *
  * @param row    需要刷新的单元格所在行
  * @param column 需要刷新的单元格所在列
  * @param data   新数据
  */
  public void asyncReDrawCell(int row, int column, Object data) {
      // 代码省略
  }
```

#### Proguard
无需添加任何混淆规则，可直接混淆

#### *License*
TableLite is released under the Apache 2.0 license.
```
Copyright 2019 wanggaowan.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at following link.

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitat
```