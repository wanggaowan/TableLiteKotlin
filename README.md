# TableLite Kotlin语言实现
Android 轻量版Table组件。[Java语言版本](https://github.com/wanggaowan/TableLite)

**说明**：SurfaceTable现不提供使用，如果需要使用，可自行下载源码并对外开放，目前SurfaceTable无法解决始终绘制在最顶层问题，这样对于多层（z轴）布局（比如刷新：SwipeRefreshLayout）下嵌套SurfaceTable，会导致刷新界面被SurfaceTable遮挡问题。如果需求没有多层（z轴）嵌套，则使用没有问题

* [项目github地址](https://github.com/wanggaowan/TableLiteKotlin)
* [版本更新日志](/update.md/)
* [demo APK](/app-debug.apk)

[![License](https://img.shields.io/badge/license-Apache%202-4EB1BA.svg)](https://www.apache.org/licenses/LICENSE-2.0.html)
[![](https://jitpack.io/v/wanggaowan/TableLiteKotlin.svg)](https://jitpack.io/#wanggaowan/TableLiteKotlin)

#### 如何使用：
1. 添加 JitPack repository 到你的build文件
   ```groovy
    allprojects {
        repositories {
            maven { url 'https://www.jitpack.io' }
        }
    }
   ```

2. 增加依赖
   ```groovy
   dependencies {
        implementation 'com.github.wanggaowan:TableLiteKotlin:1.8'
   }
   ```

3. 使用表格View
   ```xml
    <com.keqiang.table.kotlin.Table
           android:id="@+id/table"
           android:layout_width="match_parent"
           android:layout_height="match_parent"
           android:background="@android:color/white"/>
   ```
#### 下面对Table中主要的类进行说明：
**1.TableConfig(表格全局数据配置)，可配置项如下：**
  - 全局行高，最大最小行高。
  - 全局列宽，最大最小列宽。
  - 是否需要固定行(顶部或底部)，是否需要固定列(左边或右边)
  - 是否需要行选中高亮，列选中高亮、行高都选中高亮以及高亮颜色
  - 是否需要拖拽改变行高或列宽(三种配置：NONE：不需要,CLICK：点击即可修改,LONG_PRESS：长按后修改)以及拖拽时高亮颜色，
   拖拽时方向指示器图标，指示器大小，是否绘制指示器
  - 拖拽改变行高或列宽结束后是否需要恢复之前高亮内容

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
 - 滑动表格到指定位置

**4.CellFactory(接口)，用于动态获取单元格(Cell)数据，Cell用于设置单元格宽高
以及如果要自适应宽高时实现测量宽高值逻辑，另外将数据源绑定到Cell，这样就无需接口
模型转换成特定对象才可以使用。**

**5.ICellDraw(接口)，用于处理单元格绘制逻辑，目前只提供了一种实现：TextCellDraw，
该实现主要目的是作为一种绘制Demo，建议根据自己的需求实现自定义ICellDraw**


#### Proguard
无需添加任何混淆规则，可直接混淆

#### *License*
TableLiteKotlin is released under the Apache 2.0 license.
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