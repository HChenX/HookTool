/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2024 HookTool Contributions
 */
package com.hchen.hooktool.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 此类为线程池类，为辅助上下文类使用，项目不应该使用本类。
 */
public class ThreadPool {
    private static final int NUM_THREADS = 5; // 定义线程池中线程的数量
    private static volatile ExecutorService executor;

    // 获取线程池实例
    public static ExecutorService getInstance() {
        if (executor == null || executor.isShutdown()) {
            synchronized (ThreadPool.class) {
                if (executor == null || executor.isShutdown()) {
                    // 创建一个具有固定数量线程的线程池, 如果已经关机则重新创建
                    executor = Executors.newFixedThreadPool(NUM_THREADS);
                }
            }
        }
        return executor;
    }
}
