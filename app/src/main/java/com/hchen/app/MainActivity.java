/*
 * This file is part of HookTool.
 *
 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * HookTool is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with HookTool. If not, see <https://www.gnu.org/licenses/lgpl-2.1>.
 *
 * Copyright (C) 2024–2026 HChenX
 */
package com.hchen.app;

import android.os.Build;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

/**
 * HookTool 示例应用的主界面 Activity。
 *
 * <p>本类作为应用的入口界面，主要职责包括：</p>
 * <ul>
 *   <li>启用 Edge-to-Edge 全面屏沉浸式显示模式</li>
 *   <li>在 Android Q（API 29）及以上版本关闭导航栏对比度强制效果</li>
 *   <li>监听系统栏（状态栏与导航栏）的内边距变化，确保界面内容不被遮挡</li>
 * </ul>
 *
 * @see AppCompatActivity
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Activity 生命周期的创建阶段入口。
     *
     * <p>完成以下初始化操作：</p>
     * <ol>
     *   <li>通过 {@link EdgeToEdge#enable} 启用全面屏显示模式</li>
     *   <li>在 Android Q 及以上版本，调用
     *       {@code Window#setNavigationBarContrastEnforced(false)}
     *       关闭导航栏对比度强制，允许内容延伸至导航栏后方</li>
     *   <li>加载布局文件 {@code R.layout.activity_main}</li>
     *   <li>通过 {@link ViewCompat#setOnApplyWindowInsetsListener} 注册窗口内边距监听器，
     *       根据系统栏的 {@link Insets} 为根视图设置对应方向的内边距</li>
     * </ol>
     *
     * @param savedInstanceState 之前由系统保存的实例状态数据，
     *                           若为首次创建则为 {@code null}
     * @see EdgeToEdge
     * @see WindowInsetsCompat
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().setNavigationBarContrastEnforced(false);
        }

        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
