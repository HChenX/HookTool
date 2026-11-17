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
package com.hchen.app.demo;

/**
 * 被 Hook 的示例目标类。
 * <p>
 * 覆盖普通方法、静态方法、重载、构造函数与静态初始化块。
 *
 * @author 焕晨HChen
 */
public class Target {
    private static int sLoaded;

    static {
        sLoaded = 1;
    }

    private String mSecret = "original";

    /**
     * 创建默认实例。
     */
    public Target() {
    }

    /**
     * 创建带初始密钥的实例。
     *
     * @param secret 初始密钥
     */
    public Target(String secret) {
        this.mSecret = secret;
    }

    /**
     * 返回问候语。
     *
     * @param name 名称
     * @return 问候语
     */
    public String greet(String name) {
        return "hello " + name;
    }

    /**
     * 两数相加。
     *
     * @param a 加数
     * @param b 被加数
     * @return 和
     */
    public static int add(int a, int b) {
        return a + b;
    }

    /**
     * 返回描述。
     *
     * @return 描述
     */
    public String describe() {
        return "target";
    }

    /**
     * 返回带前缀的描述。
     *
     * @param prefix 前缀
     * @return 描述
     */
    public String describe(String prefix) {
        return prefix + "target";
    }

    /**
     * 返回当前密钥。
     *
     * @return 密钥
     */
    public String getSecret() {
        return mSecret;
    }

    /**
     * 返回静态块是否已执行。
     *
     * @return 已执行时返回 1
     */
    public static int getLoaded() {
        return sLoaded;
    }

    /**
     * 空方法，供演示使用。
     */
    public void noise() {
    }
}