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
package com.hchen.hooktool.callback;

import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hchen.hooktool.data.AppData;

import java.util.List;

/**
 * 应用数据获取回调接口。
 * <p>
 * 该接口定义了从 {@link PackageManager} 查询应用包信息的标准契约，
 * 并提供了异步查询完成后的回调机制。泛型 {@code T} 用于指定
 * 包信息元素的具体类型，以支持不同精度的查询需求。
 *
 * @param <T> 包信息列表中元素的类型
 * @author 焕晨HChen
 */
public interface IAppDataGetter<T> {
    /**
     * 从 PackageManager 中查询目标应用的包信息列表。
     * <p>
     * 实现者应在此方法内部调用 {@link PackageManager} 的相关 API
     * （例如 {@code getInstalledPackages} 或 {@code getInstalledApplications}），
     * 以获取满足业务需求的包信息集合。
     *
     * @param pm 用于执行包信息查询的 {@link PackageManager} 实例，不为 {@code null}
     * @return 包含目标应用包信息的列表，不为 {@code null}
     * @throws PackageManager.NameNotFoundException 当指定的包名不存在时抛出
     */
    @NonNull
    List<T> getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException;

    /**
     * 异步获取应用数据完成时的回调方法。
     * <p>
     * 当异步查询操作执行完毕后（不论成功或失败），框架将自动调用此方法。
     * 查询成功时，{@code e} 参数为 {@code null}，{@code appData} 包含有效数据；
     * 查询失败时，{@code e} 携带异常信息。
     *
     * @param appData 查询结果对应的应用数据数组，失败时可能为空或数据不完整
     * @param e       查询过程中捕获的异常；查询成功时为 {@code null}
     */
    default void getAsyncAppData(@NonNull AppData[] appData, @Nullable PackageManager.NameNotFoundException e) {
    }
}
