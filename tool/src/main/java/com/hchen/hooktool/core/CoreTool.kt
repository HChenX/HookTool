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
package com.hchen.hooktool.core

import android.content.Context
import android.content.res.Resources
import androidx.annotation.IdRes
import com.hchen.hooktool.ModuleData
import com.hchen.hooktool.callback.IAsyncPrefs
import com.hchen.hooktool.callback.IPrefsApply
import com.hchen.hooktool.core.CoreTool.Companion.asyncPrefs
import com.hchen.hooktool.exception.UnexpectedException
import com.hchen.hooktool.hook.AbsHook
import com.hchen.hooktool.hook.HookBridge
import com.hchen.hooktool.log.LogExpand
import com.hchen.hooktool.log.XposedLog
import com.hchen.hooktool.utils.PrefsTool
import com.hchen.hooktool.utils.ResInjectTool
import de.robv.android.xposed.XposedHelpers
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.time.Duration
import java.time.Instant
import java.util.Objects

/**
 * 核心工具
 * 
 * @author 焕晨HChen
 */
@Suppress("unused")
open class CoreTool : XposedLog() {
    private companion object {
        // -------------------------------- class ---------------------------------
        /**
         * 检查指定类是否存在
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @return 是否存在该类
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasClass(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findClassIfExists(this, classLoader)
            )
        }

        /**
         * 查找指定类
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @return 找到的类
         */
        @JvmStatic
        @JvmOverloads
        fun String.findClass(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Class<*> {
            return XposedHelpers.findClass(this, classLoader)
        }

        /**
         * 查找指定类，如果不存在则返回 null
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @return 找到的类，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.findClassIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Class<*>? {
            return XposedHelpers.findClassIfExists(this, classLoader)
        }

        // ---------------------------------- method -----------------------------------

        /**
         * 检查指定类是否包含指定方法
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param exactMatch 是否精确匹配，默认为 true
         * @param parameterTypes 参数类型
         * @return 是否存在该方法
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            exactMatch: Boolean = true,
            vararg parameterTypes: Any
        ): Boolean {
            return this.findClassIfExists(classLoader)?.hasMethod(methodName, exactMatch, parameterTypes) ?: false
        }

        /**
         * 检查当前类是否包含指定方法
         *
         * @param methodName 方法名
         * @param exactMatch 是否精确匹配，默认为 true
         * @param parameterTypes 参数类型
         * @return 是否存在该方法
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.hasMethod(
            methodName: String,
            exactMatch: Boolean = true,
            vararg parameterTypes: Any
        ): Boolean {
            return if (exactMatch) {
                Objects.nonNull(
                    XposedHelpers.findMethodExactIfExists(this, methodName, parameterTypes)
                )
            } else {
                this.declaredMethods.any { method ->
                    methodName.contentEquals(method.name)
                }
            }
        }

        /**
         * 查找指定类的指定方法
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return 找到的方法
         */
        @JvmStatic
        @JvmOverloads
        fun String.findMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return XposedHelpers.findMethodExact(this, classLoader, methodName, parameterTypes)
        }

        /**
         * 查找当前类的指定方法
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return 找到的方法
         */
        @JvmStatic
        fun Class<*>.findMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return XposedHelpers.findMethodExact(this, methodName, parameterTypes)
        }

        /**
         * 查找指定类的指定方法，如果不存在则返回 null
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return 找到的方法，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.findMethodIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return XposedHelpers.findMethodExactIfExists(this, classLoader, methodName, parameterTypes)
        }

        /**
         * 查找当前类的指定方法，如果不存在则返回 null
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return 找到的方法，如果不存在则返回 null
         */
        @JvmStatic
        fun Class<*>.findMethodIfExists(
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return XposedHelpers.findMethodExactIfExists(this, methodName, parameterTypes)
        }

        /**
         * 查找指定类的所有方法，可选择按方法名过滤
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名，为null时返回所有方法
         * @return 找到的方法数组
         */
        @JvmStatic
        @JvmOverloads
        fun String.findAllMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String? = null
        ): Array<Method> {
            return this.findClass(classLoader).findAllMethod(methodName)
        }

        /**
         * 查找当前类的所有方法，可选择按方法名过滤
         *
         * @param methodName 方法名，为null时返回所有方法
         * @return 找到的方法数组
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.findAllMethod(
            methodName: String? = null
        ): Array<Method> {
            return this.declaredMethods.filter {
                methodName?.contentEquals(it.name) ?: true
            }.toTypedArray()
        }

        // -------------------------------- constructor ---------------------------------
        /**
         * 检查指定类是否包含指定构造函数
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型
         * @return 是否存在该构造函数
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findConstructorExactIfExists(this, classLoader, parameterTypes)
            )
        }

        /**
         * 检查当前类是否包含指定构造函数
         *
         * @param parameterTypes 参数类型
         * @return 是否存在该构造函数
         */
        @JvmStatic
        fun Class<*>.hasConstructor(
            vararg parameterTypes: Any
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findConstructorExactIfExists(this, parameterTypes)
            )
        }

        /**
         * 查找指定类的指定构造函数
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型
         * @return 找到的构造函数
         */
        @JvmStatic
        @JvmOverloads
        fun String.findConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExact(this, classLoader, parameterTypes)
        }

        /**
         * 查找当前类的指定构造函数
         *
         * @param parameterTypes 参数类型
         * @return 找到的构造函数
         */
        @JvmStatic
        fun Class<*>.findConstructor(
            vararg parameterTypes: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExact(this, parameterTypes)
        }

        /**
         * 查找指定类的指定构造函数，如果不存在则返回 null
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型
         * @return 找到的构造函数，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.findConstructorIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Constructor<*>? {
            return XposedHelpers.findConstructorExactIfExists(this, classLoader, parameterTypes)
        }

        /**
         * 查找当前类的指定构造函数，如果不存在则返回 null
         *
         * @param parameterTypes 参数类型
         * @return 找到的构造函数，如果不存在则返回 null
         */
        @JvmStatic
        fun Class<*>.findConstructorIfExists(
            vararg parameterTypes: Any
        ): Constructor<*>? {
            return XposedHelpers.findConstructorExactIfExists(this, parameterTypes)
        }

        /**
         * 查找指定类的所有构造函数
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @return 找到的构造函数数组
         */
        @JvmStatic
        @JvmOverloads
        fun String.findAllConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Array<Constructor<*>> {
            return this.findClass(classLoader).findAllConstructor()
        }

        /**
         * 查找当前类的所有构造函数
         *
         * @return 找到的构造函数数组
         */
        @JvmStatic
        fun Class<*>.findAllConstructor(): Array<Constructor<*>> {
            return this.declaredConstructors
        }

        // --------------------------------- field ----------------------------------

        /**
         * 检查指定类是否包含指定字段
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param fieldName 字段名
         * @return 是否存在该字段
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Boolean {
            return Objects.nonNull(
                this.findClassIfExists(classLoader)?.hasField(fieldName)
            )
        }

        /**
         * 检查当前类是否包含指定字段
         *
         * @param fieldName 字段名
         * @return 是否存在该字段
         */
        @JvmStatic
        fun Class<*>.hasField(
            fieldName: String
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findFieldIfExists(this, fieldName)
            )
        }

        /**
         * 查找指定类的指定字段
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param fieldName 字段名
         * @return 找到的字段
         */
        @JvmStatic
        @JvmOverloads
        fun String.findField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Field {
            return this.findClass(classLoader).findField(fieldName)
        }

        /**
         * 查找当前类的指定字段
         *
         * @param fieldName 字段名
         * @return 找到的字段
         */
        @JvmStatic
        fun Class<*>.findField(
            fieldName: String
        ): Field {
            return XposedHelpers.findField(this, fieldName)
        }

        /**
         * 查找指定类的指定字段，如果不存在则返回 null
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param fieldName 字段名
         * @return 找到的字段，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.findFieldIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Field? {
            return this.findClassIfExists(classLoader)?.findFieldIfExists(fieldName)
        }

        /**
         * 查找当前类的指定字段，如果不存在则返回 null
         *
         * @param fieldName 字段名
         * @return 找到的字段，如果不存在则返回 null
         */
        @JvmStatic
        fun Class<*>.findFieldIfExists(
            fieldName: String
        ): Field? {
            return XposedHelpers.findFieldIfExists(this, fieldName)
        }

        // -------------------------------- non static ---------------------------------
        /**
         * 调用对象的指定方法
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型数组，默认为空数组
         * @param args 方法参数
         * @return 方法返回值
         */
        @JvmStatic
        @JvmOverloads
        fun Any.callMethod(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return if (parameterTypes.isEmpty()) {
                XposedHelpers.callMethod(this, methodName, args)
            } else {
                XposedHelpers.callMethod(this, methodName, parameterTypes, args)
            }
        }

        /**
         * 调用指定实例的方法
         *
         * @param instance 实例对象
         * @param args 方法参数
         * @return 方法返回值
         */
        @JvmStatic
        fun Method.callMethod(
            instance: Any,
            vararg args: Any?
        ): Any? {
            this.isAccessible = true
            return this.getInvoker().invoke(instance, args)
        }

        /**
         * 获取对象的指定字段值
         *
         * @param fieldName 字段名
         * @return 字段值
         */
        @JvmStatic
        fun Any.getField(
            fieldName: String,
        ): Any? {
            return XposedHelpers.getObjectField(this, fieldName)
        }

        /**
         * 获取指定实例的字段值
         *
         * @param instance 实例对象
         * @return 字段值
         */
        @JvmStatic
        fun Field.getField(
            instance: Any,
        ): Any? {
            this.isAccessible = true
            return this.get(instance)
        }

        /**
         * 设置对象的指定字段值
         *
         * @param fieldName 字段名
         * @param value 新的字段值
         */
        @JvmStatic
        fun Any.setField(
            fieldName: String,
            value: Any?
        ) {
            XposedHelpers.setObjectField(this, fieldName, value)
        }

        /**
         * 设置指定实例的字段值
         *
         * @param instance 实例对象
         * @param value 新的字段值
         */
        @JvmStatic
        fun Field.setField(
            instance: Any,
            value: Any?
        ) {
            this.isAccessible = true
            this.set(instance, value)
        }

        /**
         * 为对象设置额外的实例字段
         *
         * @param key 字段键
         * @param value 字段值
         * @return 之前的值，如果不存在则返回 null
         */
        @JvmStatic
        fun Any.setAdditionalInstanceField(
            key: String,
            value: Any?
        ): Any? {
            return XposedHelpers.setAdditionalInstanceField(this, key, value)
        }

        /**
         * 获取对象的额外实例字段
         *
         * @param key 字段键
         * @return 字段值，如果不存在则返回 null
         */
        @JvmStatic
        fun Any.getAdditionalInstanceField(
            key: String
        ): Any? {
            return XposedHelpers.getAdditionalInstanceField(this, key)
        }

        /**
         * 移除对象的额外实例字段
         *
         * @param key 字段键
         * @return 被移除的值，如果不存在则返回 null
         */
        @JvmStatic
        fun Any.removeAdditionalInstanceField(
            key: String
        ): Any? {
            return XposedHelpers.removeAdditionalInstanceField(this, key)
        }

        // ------------------------------- static ------------------------------------

        /**
         * 创建指定类的实例
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型数组，默认为空数组
         * @param args 构造函数参数
         * @return 创建的实例
         */
        @JvmStatic
        @JvmOverloads
        fun String.newInstance(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any {
            return this.findClass(classLoader).newInstance(parameterTypes, args)
        }

        /**
         * 创建当前类的实例
         *
         * @param parameterTypes 参数类型数组，默认为空数组
         * @param args 构造函数参数
         * @return 创建的实例
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.newInstance(
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any {
            return if (parameterTypes.isEmpty()) {
                XposedHelpers.newInstance(this, args)
            } else {
                XposedHelpers.newInstance(this, parameterTypes, args)
            }
        }

        /**
         * 调用指定类的静态方法
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param parameterTypes 参数类型数组，默认为空数组
         * @param args 方法参数
         * @return 方法返回值
         */
        @JvmStatic
        @JvmOverloads
        fun String.callStaticMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return this.findClass(classLoader).callStaticMethod(methodName, parameterTypes, args)
        }

        /**
         * 调用当前类的静态方法
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型数组，默认为空数组
         * @param args 方法参数
         * @return 方法返回值
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.callStaticMethod(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return if (parameterTypes.isEmpty()) {
                XposedHelpers.callStaticMethod(this, methodName, args)
            } else {
                XposedHelpers.callStaticMethod(this, methodName, parameterTypes, args)
            }
        }

        /**
         * 调用静态方法
         *
         * @param args 方法参数
         * @return 方法返回值
         */
        @JvmStatic
        fun Method.callStaticMethod(
            vararg args: Any?
        ): Any? {
            this.isAccessible = true
            return this.getInvoker().invoke(null, args)
        }

        /**
         * 获取指定类的静态字段值
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param fieldName 字段名
         * @return 字段值
         */
        @JvmStatic
        @JvmOverloads
        fun String.getStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Any? {
            return this.findClass(classLoader).getStaticField(fieldName)
        }

        /**
         * 获取当前类的静态字段值
         *
         * @param fieldName 字段名
         * @return 字段值
         */
        @JvmStatic
        fun Class<*>.getStaticField(
            fieldName: String
        ): Any? {
            return XposedHelpers.getStaticObjectField(this, fieldName)
        }

        /**
         * 获取静态字段值
         *
         * @return 字段值
         */
        @JvmStatic
        fun Field.getStaticField(): Any? {
            this.isAccessible = true
            return this.get(null)
        }

        /**
         * 设置指定类的静态字段值
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param fieldName 字段名
         * @param value 新的字段值
         */
        @JvmStatic
        @JvmOverloads
        fun String.setStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String,
            value: Any?
        ) {
            this.findClass(classLoader).setStaticField(fieldName, value)
        }

        /**
         * 设置当前类的静态字段值
         *
         * @param fieldName 字段名
         * @param value 新的字段值
         */
        @JvmStatic
        fun Class<*>.setStaticField(
            fieldName: String,
            value: Any?
        ) {
            XposedHelpers.setStaticObjectField(this, fieldName, value)
        }

        /**
         * 设置静态字段值
         *
         * @param value 新的字段值
         */
        @JvmStatic
        fun Field.setStaticField(
            value: Any?
        ) {
            this.isAccessible = true
            this.set(null, value)
        }

        /**
         * 为指定类设置额外的静态字段
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param key 字段键
         * @param value 字段值
         * @return 之前的值，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.setAdditionalStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            key: String,
            value: Any?
        ): Any? {
            return this.findClass(classLoader).setAdditionalStaticField(key, value)
        }

        /**
         * 为当前类设置额外的静态字段
         *
         * @param key 字段键
         * @param value 字段值
         * @return 之前的值，如果不存在则返回 null
         */
        @JvmStatic
        fun Class<*>.setAdditionalStaticField(
            key: String,
            value: Any?
        ): Any? {
            return XposedHelpers.setAdditionalStaticField(this, key, value)
        }

        /**
         * 获取指定类的额外静态字段
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param key 字段键
         * @return 字段值，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.getAdditionalStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            key: String,
        ): Any? {
            return this.findClass(classLoader).getAdditionalStaticField(key)
        }

        /**
         * 获取当前类的额外静态字段
         *
         * @param key 字段键
         * @return 字段值，如果不存在则返回 null
         */
        @JvmStatic
        fun Class<*>.getAdditionalStaticField(
            key: String,
        ): Any? {
            return XposedHelpers.getAdditionalStaticField(this, key)
        }

        /**
         * 移除指定类的额外静态字段
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param key 字段键
         * @return 被移除的值，如果不存在则返回 null
         */
        @JvmStatic
        @JvmOverloads
        fun String.removeAdditionalStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            key: String,
        ): Any? {
            return this.findClass(classLoader).removeAdditionalStaticField(key)
        }

        /**
         * 移除当前类的额外静态字段
         *
         * @param key 字段键
         * @return 被移除的值，如果不存在则返回 null
         */
        @JvmStatic
        fun Class<*>.removeAdditionalStaticField(
            key: String,
        ): Any? {
            return XposedHelpers.removeAdditionalStaticField(this, key)
        }

        // --------------------------------- hook ------------------------------------

        /**
         * 钩子指定类的指定方法
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return HookBridge 实例
         */
        @JvmStatic
        @JvmOverloads
        fun String.hook(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findMethod(classLoader, methodName, parameterTypes).hook()
        }

        /**
         * 钩子当前类的指定方法
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return HookBridge 实例
         */
        @JvmStatic
        fun Class<*>.hook(
            methodName: String,
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findMethod(methodName, parameterTypes).hook()
        }

        /**
         * 钩子指定类的指定构造函数
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型
         * @return HookBridge 实例
         */
        @JvmStatic
        @JvmOverloads
        fun String.hook(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findConstructor(classLoader, parameterTypes).hook()
        }

        /**
         * 钩子当前类的指定构造函数
         *
         * @param parameterTypes 参数类型
         * @return HookBridge 实例
         */
        @JvmStatic
        fun Class<*>.hook(
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findConstructor(parameterTypes).hook()
        }

        /**
         * 钩子可执行对象
         *
         * @return HookBridge 实例
         */
        @JvmStatic
        fun Executable.hook(): HookBridge {
            return HookBridge(ModuleData.getWrapper().hook(this))
        }

        /**
         * 钩子指定类的类初始化器
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @return HookBridge 实例
         */
        @JvmStatic
        @JvmOverloads
        fun String.hookClassInitializer(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): HookBridge {
            return this.findClass(classLoader).hookClassInitializer()
        }

        /**
         * 钩子当前类的类初始化器
         *
         * @return HookBridge 实例
         */
        @JvmStatic
        fun Class<*>.hookClassInitializer(): HookBridge {
            return HookBridge(ModuleData.getWrapper().hookClassInitializer(this))
        }

        /**
         * 创建拦截并返回指定结果的钩子
         *
         * @param result 要返回的结果
         * @return AbsHook 实例
         */
        @JvmStatic
        fun returnResult(result: Any?): AbsHook {
            return object : AbsHook() {
                override fun before() {
                    this.result = result
                }
            }
        }

        /**
         * 创建拦截方法的钩子
         *
         * @return AbsHook 实例
         */
        @JvmStatic
        fun doNothing(): AbsHook {
            return returnResult(null)
        }

        /**
         * 创建替换参数的钩子
         *
         * @param index 参数索引
         * @param value 新的参数值
         * @return AbsHook 实例
         */
        @JvmStatic
        fun setArg(index: Int, value: Any?): AbsHook {
            return object : AbsHook() {
                override fun before() {
                    setArg(index, value)
                }
            }
        }

        // ------------------------------- invoker -----------------------------------
        /**
         * 获取指定类指定方法的调用器
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return 方法调用器
         */
        @JvmStatic
        @JvmOverloads
        fun String.getInvoker(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(classLoader, methodName, parameterTypes).getInvoker()
        }

        /**
         * 获取当前类指定方法的调用器
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         * @return 方法调用器
         */
        @JvmStatic
        fun Class<*>.getInvoker(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(methodName, parameterTypes).getInvoker()
        }

        /**
         * 获取方法的调用器
         *
         * @return 方法调用器
         */
        @JvmStatic
        fun Method.getInvoker(): XposedInterface.Invoker<*, Method> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        /**
         * 获取指定类指定构造函数的调用器
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型
         * @return 构造函数调用器
         */
        @JvmStatic
        @JvmOverloads
        fun String.getInvoker(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(classLoader, parameterTypes).getInvoker()
        }

        /**
         * 获取当前类指定构造函数的调用器
         *
         * @param parameterTypes 参数类型
         * @return 构造函数调用器
         */
        @JvmStatic
        fun Class<*>.getInvoker(
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(parameterTypes).getInvoker()
        }

        /**
         * 获取构造函数的调用器
         *
         * @return 构造函数调用器
         */
        @JvmStatic
        fun Constructor<*>.getInvoker(): XposedInterface.CtorInvoker<*> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        // ------------------------------ deoptimize --------------------------------

        /**
         * 反优化指定类的指定方法
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         */
        @JvmStatic
        @JvmOverloads
        fun String.deoptimize(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.findMethod(classLoader, methodName, parameterTypes).deoptimize()
        }

        /**
         * 反优化当前类的指定方法
         *
         * @param methodName 方法名
         * @param parameterTypes 参数类型
         */
        @JvmStatic
        fun Class<*>.deoptimize(
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.findMethod(methodName, parameterTypes).deoptimize()
        }

        /**
         * 反优化指定类的指定构造函数
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型
         */
        @JvmStatic
        @JvmOverloads
        fun String.deoptimize(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ) {
            this.findConstructor(classLoader, parameterTypes).deoptimize()
        }

        /**
         * 反优化当前类的指定构造函数
         *
         * @param parameterTypes 参数类型
         */
        @JvmStatic
        fun Class<*>.deoptimize(
            vararg parameterTypes: Any
        ) {
            this.findConstructor(parameterTypes).deoptimize()
        }

        /**
         * 反优化可执行对象
         */
        @JvmStatic
        fun Executable.deoptimize() {
            ModuleData.getWrapper().deoptimize(this)
        }

        // ----------------------------------- res ---------------------------------------
        /**
         * 创建假的资源ID
         *
         * @param resName 资源名称
         * @return 假的资源ID
         */
        @JvmStatic
        fun createFakeResId(resName: String): Int {
            return ResInjectTool.createFakeResId(resName)
        }

        /**
         * 创建假的资源ID
         *
         * @param resources 资源对象
         * @param resId 原始资源ID
         * @return 假的资源ID
         */
        @JvmStatic
        fun createFakeResId(resources: Resources, @IdRes resId: Int): Int {
            return ResInjectTool.createFakeResId(resources, resId)
        }

        /**
         * 设置资源替换
         *
         * @param packageName 包名
         * @param type 资源类型
         * @param resName 资源名称
         * @param replacementResId 替换的资源ID
         */
        @JvmStatic
        fun setResReplacement(packageName: String, type: String, resName: String, replacementResId: Int) {
            ResInjectTool.setResReplacement(packageName, type, resName, replacementResId)
        }

        /**
         * 设置密度资源替换
         *
         * @param packageName 包名
         * @param type 资源类型
         * @param resName 资源名称
         * @param replacementResValue 替换的资源值
         */
        @JvmStatic
        fun setDensityReplacement(packageName: String, type: String, resName: String, replacementResValue: Float) {
            ResInjectTool.setDensityReplacement(packageName, type, resName, replacementResValue)
        }

        /**
         * 设置对象资源替换
         *
         * @param packageName 包名
         * @param type 资源类型
         * @param resName 资源名称
         * @param replacementResValue 替换的资源值
         */
        @JvmStatic
        fun setObjectReplacement(packageName: String, type: String, resName: String, replacementResValue: Any?) {
            ResInjectTool.setObjectReplacement(packageName, type, resName, replacementResValue)
        }
        // -------------------------------- prefs --------------------------------------

        /**
         * 获取 SharedPreferences 操作接口
         *
         * @param prefsName 偏好设置名称，默认为空
         * @return IPrefsApply 接口
         */
        @JvmStatic
        @JvmOverloads
        fun Context.prefs(
            prefsName: String = ""
        ): IPrefsApply {
            return PrefsTool.prefs(this, prefsName)
        }

        /**
         * 获取 SharedPreferences 操作接口
         *
         * @param prefsName 偏好设置名称，默认为空
         * @return IPrefsApply 接口
         */
        @JvmStatic
        @JvmOverloads
        fun prefs(
            prefsName: String = ""
        ): IPrefsApply {
            return PrefsTool.prefs(prefsName)
        }

        /**
         * 异步获取 SharedPreferences
         *
         * @param prefsName 偏好设置名称，默认为空
         * @param asyncPrefs 异步回调接口
         */
        @JvmStatic
        @JvmOverloads
        fun asyncPrefs(
            prefsName: String = "",
            asyncPrefs: IAsyncPrefs
        ) {
            PrefsTool.asyncPrefs(prefsName, asyncPrefs)
        }

        // -------------------------------- other -------------------------------
        /**
         * 获取当前堆栈跟踪信息
         *
         * @return 堆栈跟踪信息字符串
         */
        @JvmStatic
        fun getStackTrace(): String {
            return LogExpand.getStackTrace()
        }

        /**
         * 计算代码执行时间
         *
         * @param runnable 要执行的代码
         * @return 执行时间（毫秒），如果发生异常则返回-1
         */
        @JvmStatic
        fun timeConsumption(runnable: Runnable): Long {
            try {
                val start = Instant.now()
                runnable.run()
                val end = Instant.now()
                return Duration.between(start, end).toMillis()
            } catch (_: Throwable) {
                return -1L
            }
        }

        /**
         * 获取参数类型数组
         *
         * @param classLoader 类加载器，默认为 ModuleData.getClassLoader()
         * @param parameterTypes 参数类型列表
         * @return 类类型数组
         * @throws UnexpectedException 如果参数类型未知
         */
        @JvmStatic
        @JvmOverloads
        fun getParameterTypes(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Array<Class<*>> {
            val classes = mutableListOf<Class<*>>()
            for (any in parameterTypes) {
                Objects.requireNonNull(any, "Parameter types must not be null.")
                when (any) {
                    is String -> {
                        classes.add(any.findClass(classLoader))
                    }

                    is Class<*> -> {
                        classes.add(any)
                    }

                    else -> {
                        throw UnexpectedException("unknown parameter types.")
                    }
                }
            }
            return classes.toTypedArray()
        }
    }
}