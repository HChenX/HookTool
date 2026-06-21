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
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.IdRes
import com.hchen.hooktool.ModuleConfig
import com.hchen.hooktool.ModuleData
import com.hchen.hooktool.exception.UnexpectedException
import com.hchen.hooktool.hook.AbsHook
import com.hchen.hooktool.hook.HookBridge
import com.hchen.hooktool.log.LogExpand
import com.hchen.hooktool.log.LogExpand.getTag
import com.hchen.hooktool.log.XposedLog
import com.hchen.hooktool.utils.PrefsTool
import com.hchen.hooktool.utils.ResInjectTool
import com.hchen.hooktool.helper.CoreHelper
import io.github.libxposed.api.XposedInterface
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.time.Duration
import java.time.Instant
import java.util.Objects

/**
 * HookTool 框架的核心工具类，封装了 Xposed Hook 开发中常用的全部操作。
 *
 * 该类以 Kotlin 扩展函数的形式，为 [String]（类名）、[Class]、[Field]、[Method]、
 * [Executable]、[Any] 等类型提供统一的 API 调用入口，涵盖以下功能领域：
 * - **类探测**：[hasClass]、[findClass]、[findClassIfExists]
 * - **方法查找与 Hook**：[findMethod]、[hookMethod]、[hookAllMethod]、[hookMethodIfExists]
 * - **构造函数查找与 Hook**：[findConstructor]、[hookConstructor]、[hookAllConstructor]
 * - **字段读写**：[findField]、[getField]、[setField]、[getStaticField]、[setStaticField]
 * - **附加字段生命周期管理**：[setAdditionalInstanceField]、[setAdditionalStaticField]
 * - **反射调用与实例化**：[callStaticMethod]、[newInstance]
 * - **方法反优化**：[deoptimizeMethod]、[deoptimizeConstructor]
 * - **链式 Hook 构建**：[buildChain]
 * - **资源注入与替换**：[createFakeResId]、[setResReplacement]
 * - **SharedPreferences 访问**：[prefs]
 * - **辅助工具**：[getStackTrace]、[timeConsumption]、[getParameterTypes]
 *
 * @author 焕晨HChen
 * @see AbsModule
 * @see CoreHelper
 */
@Suppress("unused")
open class CoreTool : XposedLog() {
     companion object {
        // -------------------------------- class ---------------------------------
        /**
         * 检测给定类名所对应的类是否可以被加载。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @return 类存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasClass(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
        ): Boolean {
            return CoreHelper.findClassIfExists(this, classLoader) != null
        }

        /**
         * 加载指定类名所对应的 [Class] 对象，若类不存在则抛出异常。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @return 加载成功的 [Class] 对象。
         * @throws NoClassDefFoundError 当目标类无法被找到时抛出。
         */
        @JvmStatic
        @JvmOverloads
        fun String.findClass(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Class<*> {
            return CoreHelper.findClass(this, classLoader)
        }

        /**
         * 尝试加载指定类名所对应的 [Class] 对象，加载失败时返回 `null` 而非抛出异常。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @return 对应的 [Class] 对象；若类不存在则返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun String.findClassIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Class<*>? {
            return CoreHelper.findClassIfExists(this, classLoader)
        }

        // ---------------------------------- method -----------------------------------

        /**
         * 判定指定类中是否存在与给定名称及参数签名完全匹配的方法。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待检测的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return 方法存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun String.hasMethod(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ): Boolean {
            return this.findClassIfExists(classLoader)?.hasMethod(methodName, *parameterTypes) ?: false
        }

        /**
         * 判定指定类中是否存在与给定名称及参数签名完全匹配的方法，使用默认 ClassLoader。
         *
         * @param methodName 待检测的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return 方法存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun String.hasMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): Boolean {
            return this.hasMethod(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 判定当前 [Class] 中是否存在与给定名称及参数签名完全匹配的方法。
         *
         * @param methodName 待检测的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return 方法存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun Class<*>.hasMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): Boolean {
            return CoreHelper.findMethodExactIfExists(this, methodName, *parameterTypes) != null
        }

        /**
         * 判定指定类中是否存在任意一个与给定名称匹配的方法（忽略参数签名差异）。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param methodName 待检测的方法名称。
         * @return 存在任意同名方法时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasAnyMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String
        ): Boolean {
            return this.findClassIfExists(classLoader)?.hasAnyMethod(methodName) ?: false
        }

        /**
         * 判定当前 [Class] 中是否存在任意一个与给定名称匹配的方法（忽略参数签名差异）。
         *
         * @param methodName 待检测的方法名称。
         * @return 存在任意同名方法时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun Class<*>.hasAnyMethod(
            methodName: String
        ): Boolean {
            return this.declaredMethods.any { method ->
                methodName.contentEquals(method.name)
            }
        }

        /**
         * 在指定类中精确查找与给定名称及参数签名匹配的方法，并将其设为可访问。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待查找的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return 已设为可访问状态的 [Method] 对象。
         * @throws NoSuchMethodError 未找到匹配方法时抛出。
         */
        @JvmStatic
        fun String.findMethod(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return CoreHelper.findMethodExact(this, classLoader, methodName, *parameterTypes)
        }

        /**
         * 在指定类中精确查找与给定名称及参数签名匹配的方法，并将其设为可访问，使用默认 ClassLoader。
         *
         * @param methodName 待查找的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return 已设为可访问状态的 [Method] 对象。
         * @throws NoSuchMethodError 未找到匹配方法时抛出。
         */
        @JvmStatic
        fun String.findMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return this.findMethod(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 在当前 [Class] 中精确查找与给定名称及参数签名匹配的方法，并将其设为可访问。
         *
         * @param methodName 待查找的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return 已设为可访问状态的 [Method] 对象。
         * @throws NoSuchMethodError 未找到匹配方法时抛出。
         */
        @JvmStatic
        fun Class<*>.findMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return CoreHelper.findMethodExact(this, methodName, *parameterTypes)
        }

        /**
         * 尝试在指定类中精确查找方法，未找到时返回 `null` 而非抛出异常。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待查找的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return [Method] 对象；方法不存在时返回 `null`。
         */
        @JvmStatic
        fun String.findMethodIfExists(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return CoreHelper.findMethodExactIfExists(this, classLoader, methodName, *parameterTypes)
        }

        /**
         * 尝试在指定类中精确查找方法，未找到时返回 `null`，使用默认 ClassLoader。
         *
         * @param methodName 待查找的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return [Method] 对象；方法不存在时返回 `null`。
         */
        @JvmStatic
        fun String.findMethodIfExists(
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return this.findMethodIfExists(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 尝试在当前 [Class] 中精确查找方法，未找到时返回 `null` 而非抛出异常。
         *
         * @param methodName 待查找的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return [Method] 对象；方法不存在时返回 `null`。
         */
        @JvmStatic
        fun Class<*>.findMethodIfExists(
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return CoreHelper.findMethodExactIfExists(this, methodName, *parameterTypes)
        }

        /**
         * 获取指定类中声明的全部方法，可选按名称进行过滤。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param methodName 用以过滤的方法名称；为 `null` 时不过滤，返回全部方法。
         * @return 匹配条件的 [Method] 数组。
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
         * 获取指定类中与给定名称匹配的方法（使用默认 ClassLoader）。
         *
         * 此重载供 Java 调用方使用，等价于 `findAllMethod(defaultClassLoader, methodName)`。
         *
         * @param methodName 用以过滤的方法名称。
         * @return 匹配条件的 [Method] 数组。
         */
        @JvmStatic
        @JvmName("findAllMethod")
        fun String.findAllMethodByName(
            methodName: String
        ): Array<Method> {
            return this.findClass().findAllMethod(methodName)
        }

        /**
         * 获取当前 [Class] 中声明的全部方法，可选按名称进行过滤。
         *
         * @param methodName 用以过滤的方法名称；为 `null` 时不过滤，返回全部方法。
         * @return 匹配条件的 [Method] 数组。
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
         * 判定指定类中是否存在与给定参数类型列表匹配的构造函数。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列。
         * @return 构造函数存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun String.hasConstructor(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ): Boolean {
            return CoreHelper.findConstructorExactIfExists(this, classLoader, *parameterTypes) != null
        }

        /**
         * 判定指定类中是否存在与给定参数类型列表匹配的构造函数，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return 构造函数存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun String.hasConstructor(
            vararg parameterTypes: Any
        ): Boolean {
            return this.hasConstructor(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 判定当前 [Class] 中是否存在与给定参数类型列表匹配的构造函数。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return 构造函数存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun Class<*>.hasConstructor(
            vararg parameterTypes: Any
        ): Boolean {
            return CoreHelper.findConstructorExactIfExists(this, *parameterTypes) != null
        }

        /**
         * 在指定类中精确查找与给定参数类型列表匹配的构造函数，并将其设为可访问。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列。
         * @return 已设为可访问状态的 [Constructor] 对象。
         * @throws NoSuchMethodError 未找到匹配的构造函数时抛出。
         */
        @JvmStatic
        fun String.findConstructor(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ): Constructor<*> {
            return CoreHelper.findConstructorExact(this, classLoader, *parameterTypes)
        }

        /**
         * 在指定类中精确查找与给定参数类型列表匹配的构造函数，并将其设为可访问，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return 已设为可访问状态的 [Constructor] 对象。
         * @throws NoSuchMethodError 未找到匹配的构造函数时抛出。
         */
        @JvmStatic
        fun String.findConstructor(
            vararg parameterTypes: Any
        ): Constructor<*> {
            return this.findConstructor(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 在当前 [Class] 中精确查找与给定参数类型列表匹配的构造函数，并将其设为可访问。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return 已设为可访问状态的 [Constructor] 对象。
         * @throws NoSuchMethodError 未找到匹配的构造函数时抛出。
         */
        @JvmStatic
        fun Class<*>.findConstructor(
            vararg parameterTypes: Any
        ): Constructor<*> {
            return CoreHelper.findConstructorExact(this, *parameterTypes)
        }

        /**
         * 尝试在指定类中查找构造函数，未找到时返回 `null` 而非抛出异常。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列。
         * @return [Constructor] 对象；构造函数不存在时返回 `null`。
         */
        @JvmStatic
        fun String.findConstructorIfExists(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ): Constructor<*>? {
            return CoreHelper.findConstructorExactIfExists(this, classLoader, *parameterTypes)
        }

        /**
         * 尝试在指定类中查找构造函数，未找到时返回 `null`，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return [Constructor] 对象；构造函数不存在时返回 `null`。
         */
        @JvmStatic
        fun String.findConstructorIfExists(
            vararg parameterTypes: Any
        ): Constructor<*>? {
            return this.findConstructorIfExists(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 尝试在当前 [Class] 中查找构造函数，未找到时返回 `null` 而非抛出异常。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return [Constructor] 对象；构造函数不存在时返回 `null`。
         */
        @JvmStatic
        fun Class<*>.findConstructorIfExists(
            vararg parameterTypes: Any
        ): Constructor<*>? {
            return CoreHelper.findConstructorExactIfExists(this, *parameterTypes)
        }

        /**
         * 获取指定类中声明的全部构造函数。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @return 该类声明的所有 [Constructor] 组成的数组。
         */
        @JvmStatic
        @JvmOverloads
        fun String.findAllConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Array<Constructor<*>> {
            return this.findClass(classLoader).findAllConstructor()
        }

        /**
         * 获取当前 [Class] 中声明的全部构造函数。
         *
         * @return 该类声明的所有 [Constructor] 组成的数组。
         */
        @JvmStatic
        fun Class<*>.findAllConstructor(): Array<Constructor<*>> {
            return this.declaredConstructors
        }

        // --------------------------------- field ----------------------------------

        /**
         * 判定指定类中是否存在指定名称的字段。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待检测的字段名称。
         * @return 字段存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hasField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Boolean {
            return this.findClassIfExists(classLoader)?.hasField(fieldName) ?: false
        }

        /**
         * 判定当前 [Class] 中是否存在指定名称的字段。
         *
         * @param fieldName 待检测的字段名称。
         * @return 字段存在时返回 `true`，否则返回 `false`。
         */
        @JvmStatic
        fun Class<*>.hasField(
            fieldName: String
        ): Boolean {
            return CoreHelper.findFieldIfExists(this, fieldName) != null
        }

        /**
         * 在指定类中查找指定名称的字段，并将其设为可访问。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待查找的字段名称。
         * @return 已设为可访问状态的 [Field] 对象。
         * @throws NoSuchFieldError 未找到该字段时抛出。
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
         * 在当前 [Class] 中查找指定名称的字段，并将其设为可访问。
         *
         * @param fieldName 待查找的字段名称。
         * @return 已设为可访问状态的 [Field] 对象。
         * @throws NoSuchFieldError 未找到该字段时抛出。
         */
        @JvmStatic
        fun Class<*>.findField(
            fieldName: String
        ): Field {
            return CoreHelper.findField(this, fieldName)
        }

        /**
         * 尝试在指定类中查找字段，未找到时返回 `null` 而非抛出异常。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待查找的字段名称。
         * @return [Field] 对象；字段不存在时返回 `null`。
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
         * 尝试在当前 [Class] 中查找字段，未找到时返回 `null` 而非抛出异常。
         *
         * @param fieldName 待查找的字段名称。
         * @return [Field] 对象；字段不存在时返回 `null`。
         */
        @JvmStatic
        fun Class<*>.findFieldIfExists(
            fieldName: String
        ): Field? {
            return CoreHelper.findFieldIfExists(this, fieldName)
        }

        // -------------------------------- non static ---------------------------------
        /**
         * 通过反射在当前对象上调用指定名称的实例方法。
         *
         * @param methodName 待调用的方法名称。
         * @param parameterTypes 方法的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun Any.callMethod(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return if (parameterTypes.isEmpty()) {
                CoreHelper.callMethod(this, methodName, *args)
            } else {
                CoreHelper.callMethod(this, methodName, parameterTypes, *args)
            }
        }

        /**
         * 尝试通过反射在当前对象上调用指定名称的实例方法，若方法不存在则返回 `null`。
         *
         * @param methodName 待调用的方法名称。
         * @param parameterTypes 方法的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法不存在或返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun Any.callMethodIfExists(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            val method = if (parameterTypes.isEmpty()) {
                this.javaClass.findMethodIfExists(methodName)
            } else {
                this.javaClass.findMethodIfExists(methodName, *parameterTypes)
            } ?: return null
            return method.callMethod(this, *args)
        }

        /**
         * 使用当前 [Method] 对象在指定实例上调用该方法。
         *
         * @param instance 目标对象实例。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        fun Method.callMethod(
            instance: Any,
            vararg args: Any?
        ): Any? {
            this.isAccessible = true
            return this.getMethodInvoker().invoke(instance, *args)
        }

        /**
         * 获取当前对象上指定名称字段的值。
         *
         * @param fieldName 字段名称。
         * @return 字段的当前值。
         */
        @JvmStatic
        fun Any.getField(
            fieldName: String,
        ): Any? {
            return CoreHelper.getObjectField(this, fieldName)
        }

        /**
         * 尝试获取当前对象上指定名称字段的值，若字段不存在则返回 `null`。
         *
         * @param fieldName 字段名称。
         * @return 字段的当前值；若字段不存在则返回 `null`。
         */
        @JvmStatic
        fun Any.getFieldIfExists(
            fieldName: String,
        ): Any? {
            return this.javaClass.findFieldIfExists(fieldName)?.getField(this)
        }

        /**
         * 使用当前 [Field] 对象从指定实例中读取该字段的值。
         *
         * @param instance 目标对象实例。
         * @return 字段的当前值。
         */
        @JvmStatic
        fun Field.getField(
            instance: Any,
        ): Any? {
            this.isAccessible = true
            return this.get(instance)
        }

        /**
         * 设置当前对象上指定名称字段的值。
         *
         * @param fieldName 字段名称。
         * @param value 待写入的新值。
         */
        @JvmStatic
        fun Any.setField(
            fieldName: String,
            value: Any?
        ) {
            CoreHelper.setObjectField(this, fieldName, value)
        }

        /**
         * 尝试设置当前对象上指定名称字段的值，若字段不存在则静默跳过。
         *
         * @param fieldName 字段名称。
         * @param value 待写入的新值。
         */
        @JvmStatic
        fun Any.setFieldIfExists(
            fieldName: String,
            value: Any?
        ) {
            this.javaClass.findFieldIfExists(fieldName)?.setField(this, value)
        }

        /**
         * 使用当前 [Field] 对象向指定实例中写入该字段的值。
         *
         * @param instance 目标对象实例。
         * @param value 待写入的新值。
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
         * 在当前对象上动态附加一个键值对形式的实例字段。
         *
         * @param key 附加字段的键名。
         * @param value 待关联的值。
         * @return 该键先前关联的值；若此前未设置过则返回 `null`。
         */
        @JvmStatic
        fun Any.setAdditionalInstanceField(
            key: String,
            value: Any?
        ): Any? {
            return CoreHelper.setAdditionalInstanceField(this, key, value)
        }

        /**
         * 读取当前对象上已附加的实例字段值。
         *
         * @param key 附加字段的键名。
         * @return 该键关联的值；若未设置则返回 `null`。
         */
        @JvmStatic
        fun Any.getAdditionalInstanceField(
            key: String
        ): Any? {
            return CoreHelper.getAdditionalInstanceField(this, key)
        }

        /**
         * 移除当前对象上已附加的实例字段。
         *
         * @param key 附加字段的键名。
         * @return 被移除的值；若该键不存在则返回 `null`。
         */
        @JvmStatic
        fun Any.removeAdditionalInstanceField(
            key: String
        ): Any? {
            return CoreHelper.removeAdditionalInstanceField(this, key)
        }

        // ------------------------------- static ------------------------------------

        /**
         * 根据指定类名创建该类的新实例。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给构造函数的实际参数。
         * @return 创建成功的新实例对象。
         */
        @JvmStatic
        fun String.newInstance(
            classLoader: ClassLoader?,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any {
            return this.findClass(classLoader).newInstance(parameterTypes, *args)
        }

        /**
         * 根据指定类名创建该类的新实例，使用默认 ClassLoader 与自动推断参数类型。
         *
         * @param args 传递给构造函数的实际参数。
         * @return 创建成功的新实例对象。
         */
        @JvmStatic
        fun String.newInstance(
            vararg args: Any?
        ): Any {
            return this.newInstance(classLoader = ModuleData.getClassLoader(), emptyArray(), *args)
        }

        /**
         * 使用当前 [Class] 创建该类的新实例。
         *
         * @param parameterTypes 构造函数的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给构造函数的实际参数。
         * @return 创建成功的新实例对象。
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.newInstance(
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any {
            return if (parameterTypes.isEmpty()) {
                CoreHelper.newInstance(this, *args)
            } else {
                CoreHelper.newInstance(this, parameterTypes, *args)
            }
        }

        /**
         * 调用指定类名所对应类上的静态方法。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待调用的静态方法名称。
         * @param parameterTypes 方法的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        fun String.callStaticMethod(
            classLoader: ClassLoader?,
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return this.findClass(classLoader).callStaticMethod(methodName, parameterTypes, *args)
        }

        /**
         * 调用指定类名所对应类上的静态方法，使用默认 ClassLoader 与自动推断参数类型。
         *
         * @param methodName 待调用的静态方法名称。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        fun String.callStaticMethod(
            methodName: String,
            vararg args: Any?
        ): Any? {
            return this.callStaticMethod(ModuleData.getClassLoader(), methodName, emptyArray(), *args)
        }

        /**
         * 调用当前 [Class] 上的静态方法。
         *
         * @param methodName 待调用的静态方法名称。
         * @param parameterTypes 方法的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.callStaticMethod(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return if (parameterTypes.isEmpty()) {
                CoreHelper.callStaticMethod(this, methodName, *args)
            } else {
                CoreHelper.callStaticMethod(this, methodName, parameterTypes, *args)
            }
        }

        /**
         * 使用当前 [Method] 对象以 `null` 作为接收者调用静态方法。
         *
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        fun Method.callStaticMethod(
            vararg args: Any?
        ): Any? {
            this.isAccessible = true
            return this.getMethodInvoker().invoke(null, *args)
        }

        /**
         * 尝试调用当前 [Class] 上的静态方法，若方法不存在则返回 `null`。
         *
         * @param methodName 待调用的静态方法名称。
         * @param parameterTypes 方法的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当方法不存在或返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.callStaticMethodIfExists(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            val method = if (parameterTypes.isEmpty()) {
                this.findMethodIfExists(methodName)
            } else {
                this.findMethodIfExists(methodName, *parameterTypes)
            } ?: return null
            return method.callStaticMethod(*args)
        }

        /**
         * 尝试调用指定类名所对应类上的静态方法，若类或方法不存在则返回 `null`。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待调用的静态方法名称。
         * @param parameterTypes 方法的参数类型数组，默认为空数组时由框架自动推断。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当类/方法不存在或返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        fun String.callStaticMethodIfExists(
            classLoader: ClassLoader?,
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any?
        ): Any? {
            return this.findClassIfExists(classLoader)?.callStaticMethodIfExists(
                methodName, parameterTypes, *args
            )
        }

        /**
         * 尝试调用指定类名所对应类上的静态方法，若类或方法不存在则返回 `null`，使用默认 ClassLoader 与自动推断参数类型。
         *
         * @param methodName 待调用的静态方法名称。
         * @param args 传递给方法的实际参数。
         * @return 方法的返回值；当类/方法不存在或返回类型为 `void` 时返回 `null`。
         */
        @JvmStatic
        fun String.callStaticMethodIfExists(
            methodName: String,
            vararg args: Any?
        ): Any? {
            return this.callStaticMethodIfExists(ModuleData.getClassLoader(), methodName, emptyArray(), *args)
        }

        /**
         * 获取指定类名所对应类中静态字段的值。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待获取的字段名称。
         * @return 该静态字段的值。
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
         * 获取当前 [Class] 中静态字段的值。
         *
         * @param fieldName 待获取的字段名称。
         * @return 该静态字段的值。
         */
        @JvmStatic
        fun Class<*>.getStaticField(
            fieldName: String
        ): Any? {
            return CoreHelper.getStaticObjectField(this, fieldName)
        }

        /**
         * 使用当前 [Field] 对象以 `null` 作为目标读取静态字段的值。
         *
         * @return 该静态字段的值。
         */
        @JvmStatic
        fun Field.getStaticField(): Any? {
            this.isAccessible = true
            return this.get(null)
        }

        /**
         * 尝试获取当前 [Class] 中静态字段的值，若字段不存在则返回 `null`。
         *
         * @param fieldName 待获取的字段名称。
         * @return 该静态字段的值；若字段不存在则返回 `null`。
         */
        @JvmStatic
        fun Class<*>.getStaticFieldIfExists(
            fieldName: String
        ): Any? {
            return this.findFieldIfExists(fieldName)?.getStaticField()
        }

        /**
         * 尝试获取指定类名所对应类中静态字段的值，若类或字段不存在则返回 `null`。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待获取的字段名称。
         * @return 该静态字段的值；若类/字段不存在则返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun String.getStaticFieldIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Any? {
            return this.findClassIfExists(classLoader)?.getStaticFieldIfExists(fieldName)
        }

        /**
         * 设置指定类名所对应类中静态字段的值。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待设置的字段名称。
         * @param value 待写入的新值。
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
         * 设置当前 [Class] 中静态字段的值。
         *
         * @param fieldName 待设置的字段名称。
         * @param value 待写入的新值。
         */
        @JvmStatic
        fun Class<*>.setStaticField(
            fieldName: String,
            value: Any?
        ) {
            CoreHelper.setStaticObjectField(this, fieldName, value)
        }

        /**
         * 使用当前 [Field] 对象以 `null` 作为目标设置静态字段的值。
         *
         * @param value 待写入的新值。
         */
        @JvmStatic
        fun Field.setStaticField(
            value: Any?
        ) {
            this.isAccessible = true
            this.set(null, value)
        }

        /**
         * 尝试设置当前 [Class] 中静态字段的值，若字段不存在则静默跳过。
         *
         * @param fieldName 待设置的字段名称。
         * @param value 待写入的新值。
         */
        @JvmStatic
        fun Class<*>.setStaticFieldIfExists(
            fieldName: String,
            value: Any?
        ) {
            this.findFieldIfExists(fieldName)?.setStaticField(value)
        }

        /**
         * 尝试设置指定类名所对应类中静态字段的值，若类或字段不存在则静默跳过。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param fieldName 待设置的字段名称。
         * @param value 待写入的新值。
         */
        @JvmStatic
        @JvmOverloads
        fun String.setStaticFieldIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String,
            value: Any?
        ) {
            this.findClassIfExists(classLoader)?.setStaticFieldIfExists(fieldName, value)
        }

        /**
         * 为指定类名所对应类附加一个键值对形式的静态字段。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param key 附加字段的键名。
         * @param value 待关联的值。
         * @return 该键先前关联的值；若此前未设置过则返回 `null`。
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
         * 在当前 [Class] 上附加一个键值对形式的静态字段。
         *
         * @param key 附加字段的键名。
         * @param value 待关联的值。
         * @return 该键先前关联的值；若此前未设置过则返回 `null`。
         */
        @JvmStatic
        fun Class<*>.setAdditionalStaticField(
            key: String,
            value: Any?
        ): Any? {
            return CoreHelper.setAdditionalStaticField(this, key, value)
        }

        /**
         * 读取指定类名所对应类上附加的静态字段值。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param key 附加字段的键名。
         * @return 该键关联的值；若未设置则返回 `null`。
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
         * 读取当前 [Class] 上附加的静态字段值。
         *
         * @param key 附加字段的键名。
         * @return 该键关联的值；若未设置则返回 `null`。
         */
        @JvmStatic
        fun Class<*>.getAdditionalStaticField(
            key: String,
        ): Any? {
            return CoreHelper.getAdditionalStaticField(this, key)
        }

        /**
         * 移除指定类名所对应类上附加的静态字段。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param key 附加字段的键名。
         * @return 被移除的值；若该键不存在则返回 `null`。
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
         * 移除当前 [Class] 上附加的静态字段。
         *
         * @param key 附加字段的键名。
         * @return 被移除的值；若该键不存在则返回 `null`。
         */
        @JvmStatic
        fun Class<*>.removeAdditionalStaticField(
            key: String,
        ): Any? {
            return CoreHelper.removeAdditionalStaticField(this, key)
        }

        // --------------------------------- hook ------------------------------------

        /**
         * 对指定类中的指定方法执行 Hook 操作。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待 Hook 的方法名称。
         * @param parameterTypes 方法的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         * @throws IllegalArgumentException 若最后一个参数不是 [AbsHook] 实例时抛出。
         */
        @JvmStatic
        fun String.hookMethod(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle {
            return this.findClass(classLoader).hookMethod(methodName, *parameterTypes)
        }

        /**
         * 对指定类中的指定方法执行 Hook 操作，使用默认 ClassLoader。
         *
         * @param methodName 待 Hook 的方法名称。
         * @param parameterTypes 方法的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         * @throws IllegalArgumentException 若最后一个参数不是 [AbsHook] 实例时抛出。
         */
        @JvmStatic
        fun String.hookMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle {
            return this.hookMethod(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 对当前 [Class] 的指定方法执行 Hook 操作。
         *
         * @param methodName 待 Hook 的方法名称。
         * @param parameterTypes 方法的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         * @throws IllegalArgumentException 若最后一个参数不是 [AbsHook] 实例时抛出。
         */
        @JvmStatic
        fun Class<*>.hookMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle {
            require(parameterTypes.isNotEmpty() && parameterTypes.last() is AbsHook) {
                "The last element of parameterTypes must be an instance of AbsHook"
            }

            val absHook = parameterTypes.last() as AbsHook
            val realParameterTypes = parameterTypes.dropLast(1).toTypedArray()
            return this.findMethod(methodName, *realParameterTypes).hook(absHook)
        }

        /**
         * 尝试对指定类的方法执行 Hook，若目标类不存在则返回 `null`。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待 Hook 的方法名称。
         * @param parameterTypes 方法的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若目标类不存在则返回 `null`。
         */
        @JvmStatic
        fun String.hookMethodIfExists(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle? {
            return this.findClassIfExists(classLoader)?.hookMethod(methodName, *parameterTypes)
        }

        /**
         * 尝试对指定类的方法执行 Hook，若目标类不存在则返回 `null`，使用默认 ClassLoader。
         *
         * @param methodName 待 Hook 的方法名称。
         * @param parameterTypes 方法的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若目标类不存在则返回 `null`。
         */
        @JvmStatic
        fun String.hookMethodIfExists(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle? {
            return this.hookMethodIfExists(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 尝试对当前 [Class] 的方法执行 Hook，若方法不存在则返回 `null`。
         *
         * @param methodName 待 Hook 的方法名称。
         * @param parameterTypes 方法的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若方法不存在则返回 `null`。
         */
        @JvmStatic
        fun Class<*>.hookMethodIfExists(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle? {
            require(parameterTypes.isNotEmpty() && parameterTypes.last() is AbsHook) {
                "The last element of parameterTypes must be an instance of AbsHook"
            }

            val absHook = parameterTypes.last() as AbsHook
            val realParameterTypes = parameterTypes.dropLast(1).toTypedArray()
            return this.findMethodIfExists(methodName, *realParameterTypes)?.hook(absHook)
        }

        /**
         * 对指定类的构造函数执行 Hook 操作。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         * @throws IllegalArgumentException 若最后一个参数不是 [AbsHook] 实例时抛出。
         */
        @JvmStatic
        fun String.hookConstructor(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle {
            return this.findClass(classLoader).hookConstructor(*parameterTypes)
        }

        /**
         * 对指定类的构造函数执行 Hook 操作，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         * @throws IllegalArgumentException 若最后一个参数不是 [AbsHook] 实例时抛出。
         */
        @JvmStatic
        fun String.hookConstructor(
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle {
            return this.hookConstructor(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 对当前 [Class] 的构造函数执行 Hook 操作。
         *
         * @param parameterTypes 构造函数的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         * @throws IllegalArgumentException 若最后一个参数不是 [AbsHook] 实例时抛出。
         */
        @JvmStatic
        fun Class<*>.hookConstructor(
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle {
            require(parameterTypes.isNotEmpty() && parameterTypes.last() is AbsHook) {
                "The last element of parameterTypes must be an instance of AbsHook"
            }

            val absHook = parameterTypes.last() as AbsHook
            val realParameterTypes = parameterTypes.dropLast(1).toTypedArray()
            return this.findConstructor(*realParameterTypes).hook(absHook)
        }

        /**
         * 尝试对指定类的构造函数执行 Hook，若目标类不存在则返回 `null`。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若目标类不存在则返回 `null`。
         */
        @JvmStatic
        fun String.hookConstructorIfExists(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle? {
            return this.findClassIfExists(classLoader)?.hookConstructor(*parameterTypes)
        }

        /**
         * 尝试对指定类的构造函数执行 Hook，若目标类不存在则返回 `null`，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若目标类不存在则返回 `null`。
         */
        @JvmStatic
        fun String.hookConstructorIfExists(
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle? {
            return this.hookConstructorIfExists(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 尝试对当前 [Class] 的构造函数执行 Hook，若构造函数不存在则返回 `null`。
         *
         * @param parameterTypes 构造函数的参数类型序列，最后一个元素必须为 [AbsHook] 实例。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若构造函数不存在则返回 `null`。
         */
        @JvmStatic
        fun Class<*>.hookConstructorIfExists(
            vararg parameterTypes: Any
        ): XposedInterface.HookHandle? {
            require(parameterTypes.isNotEmpty() && parameterTypes.last() is AbsHook) {
                "The last element of parameterTypes must be an instance of AbsHook"
            }

            val absHook = parameterTypes.last() as AbsHook
            val realParameterTypes = parameterTypes.dropLast(1).toTypedArray()
            return this.findConstructorIfExists(*realParameterTypes)?.hook(absHook)
        }

        /**
         * 对当前 [Executable]（方法或构造函数）执行 Hook 操作。
         * <p>
         * 在注册钩子的同时，会将声明类的全限定类名作为键存入 [AbsHook] 中，
         * 并判断目标是否为静态方法。热重载时，同一类的所有实例方法共享相同的
         * 类级键去重存储 [thisObject]，静态方法则不参与此流程。
         *
         * @param absHook Hook 回调的实现对象。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         */
        @JvmStatic
        fun Executable.hook(absHook: AbsHook): XposedInterface.HookHandle {
            return runCatching {
                HookBridge(
                    ModuleData.getWrapper().hook(this),
                    declaringClass.name,
                    java.lang.reflect.Modifier.isStatic(modifiers)
                ).intercept(absHook)
            }.onSuccess {
                if (ModuleConfig.isShowHookSuccessLog()) {
                    logI(getTag(), "Success to hook: $this")
                }
            }.getOrThrow()
        }

        /**
         * 对指定类中所有与给定名称匹配的方法执行 Hook 操作。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param methodName 待 Hook 的方法名称。
         * @param absHook Hook 回调的实现对象。
         * @return 所有匹配方法的 [XposedInterface.HookHandle] 数组。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hookAllMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            absHook: AbsHook
        ): Array<XposedInterface.HookHandle> {
            return this.findAllMethod(classLoader, methodName).hookAll(absHook)
        }

        /**
         * 对当前 [Class] 中所有与给定名称匹配的方法执行 Hook 操作。
         *
         * @param methodName 待 Hook 的方法名称。
         * @param absHook Hook 回调的实现对象。
         * @return 所有匹配方法的 [XposedInterface.HookHandle] 数组。
         */
        @JvmStatic
        fun Class<*>.hookAllMethod(
            methodName: String,
            absHook: AbsHook
        ): Array<XposedInterface.HookHandle> {
            return this.findAllMethod(methodName).hookAll(absHook)
        }

        /**
         * 对指定类的全部构造函数执行 Hook 操作。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param absHook Hook 回调的实现对象。
         * @return 所有构造函数的 [XposedInterface.HookHandle] 数组。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hookAllConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            absHook: AbsHook
        ): Array<XposedInterface.HookHandle> {
            return this.findAllConstructor(classLoader).hookAll(absHook)
        }

        /**
         * 对当前 [Class] 的全部构造函数执行 Hook 操作。
         *
         * @param absHook Hook 回调的实现对象。
         * @return 所有构造函数的 [XposedInterface.HookHandle] 数组。
         */
        @JvmStatic
        fun Class<*>.hookAllConstructor(absHook: AbsHook): Array<XposedInterface.HookHandle> {
            return this.findAllConstructor().hookAll(absHook)
        }

        /**
         * 对当前 [Executable] 数组中的所有元素逐一执行 Hook 操作。
         *
         * @param absHook Hook 回调的实现对象。
         * @return 所有元素对应的 [XposedInterface.HookHandle] 数组。
         */
        @JvmStatic
        fun Array<out Executable>.hookAll(absHook: AbsHook): Array<XposedInterface.HookHandle> {
            return this.map { it.hook(absHook) }.toTypedArray()
        }

        /**
         * 对指定类的类初始化器（`<clinit>`）执行 Hook 操作。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param absHook Hook 回调的实现对象。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hookClassInitializer(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            absHook: AbsHook,
        ): XposedInterface.HookHandle {
            return this.findClass(classLoader).hookClassInitializer(absHook)
        }

        /**
         * 尝试对指定类的类初始化器执行 Hook，若目标类不存在则返回 `null`。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param absHook Hook 回调的实现对象。
         * @return [XposedInterface.HookHandle] Hook 句柄对象；若目标类不存在则返回 `null`。
         */
        @JvmStatic
        @JvmOverloads
        fun String.hookClassInitializerIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            absHook: AbsHook,
        ): XposedInterface.HookHandle? {
            return this.findClassIfExists(classLoader)?.hookClassInitializer(absHook)
        }

        /**
         * 对当前 [Class] 的类初始化器（`<clinit>`）执行 Hook 操作。
         * <p>
         * 在注册钩子的同时，会以类名作为键存入 [AbsHook]，
         * 并将静态性标志设为 [true]。类初始化器为静态上下文，
         * 在热重载时不参与 [thisObject] 的自动存储与恢复。
         *
         * @param absHook Hook 回调的实现对象。
         * @return [XposedInterface.HookHandle] Hook 句柄对象。
         */
        @JvmStatic
        fun Class<*>.hookClassInitializer(absHook: AbsHook): XposedInterface.HookHandle {
            return runCatching {
                HookBridge(
                    ModuleData.getWrapper().hookClassInitializer(this),
                    name, true
                ).intercept(absHook)
            }.onSuccess {
                if (ModuleConfig.isShowHookSuccessLog()) {
                    logI(getTag(), "Success to hook: $this")
                }
            }.getOrThrow()
        }

        /**
         * 创建一个在方法执行前拦截调用并强制返回指定结果的 [AbsHook] 实例。
         *
         * @param result 强制返回的结果值。
         * @return 配置完毕的 [AbsHook] 实例。
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
         * 创建一个拦截方法执行并直接返回 `null` 的 [AbsHook] 实例，等效于取消原方法执行。
         *
         * @return 配置完毕的 [AbsHook] 实例。
         */
        @JvmStatic
        fun doNothing(): AbsHook {
            return returnResult(null)
        }

        /**
         * 创建一个在方法执行前替换指定位置参数值的 [AbsHook] 实例。
         *
         * @param index 待替换参数的索引位置（从 0 开始计数）。
         * @param value 替换后的参数值。
         * @return 配置完毕的 [AbsHook] 实例。
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
         * 获取指定类中指定方法的调用器，用于绕过 Hook 直接调用原始方法。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return [XposedInterface.Invoker] 方法调用器对象。
         */
        @JvmStatic
        fun String.getMethodInvoker(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(classLoader, methodName, *parameterTypes).getMethodInvoker()
        }

        /**
         * 获取指定类中指定方法的调用器，用于绕过 Hook 直接调用原始方法，使用默认 ClassLoader。
         *
         * @param methodName 方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return [XposedInterface.Invoker] 方法调用器对象。
         */
        @JvmStatic
        fun String.getMethodInvoker(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.getMethodInvoker(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 获取当前 [Class] 中指定方法的调用器，用于绕过 Hook 直接调用原始方法。
         *
         * @param methodName 方法名称。
         * @param parameterTypes 方法的参数类型序列。
         * @return [XposedInterface.Invoker] 方法调用器对象。
         */
        @JvmStatic
        fun Class<*>.getMethodInvoker(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(methodName, *parameterTypes).getMethodInvoker()
        }

        /**
         * 获取当前 [Method] 对应的调用器，用于绕过 Hook 直接调用原始方法。
         *
         * @return [XposedInterface.Invoker] 方法调用器对象。
         */
        @JvmStatic
        fun Method.getMethodInvoker(): XposedInterface.Invoker<*, Method> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        /**
         * 获取指定类中指定构造函数的调用器，用于绕过 Hook 直接调用原始构造逻辑。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列。
         * @return [XposedInterface.CtorInvoker] 构造函数调用器对象。
         */
        @JvmStatic
        fun String.getConstructorInvoker(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(classLoader, *parameterTypes).getConstructorInvoker()
        }

        /**
         * 获取指定类中指定构造函数的调用器，用于绕过 Hook 直接调用原始构造逻辑，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return [XposedInterface.CtorInvoker] 构造函数调用器对象。
         */
        @JvmStatic
        fun String.getConstructorInvoker(
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.getConstructorInvoker(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 获取当前 [Class] 中指定构造函数的调用器，用于绕过 Hook 直接调用原始构造逻辑。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         * @return [XposedInterface.CtorInvoker] 构造函数调用器对象。
         */
        @JvmStatic
        fun Class<*>.getConstructorInvoker(
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(*parameterTypes).getConstructorInvoker()
        }

        /**
         * 获取当前 [Constructor] 对应的调用器，用于绕过 Hook 直接调用原始构造逻辑。
         *
         * @return [XposedInterface.CtorInvoker] 构造函数调用器对象。
         */
        @JvmStatic
        fun Constructor<*>.getConstructorInvoker(): XposedInterface.CtorInvoker<*> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        // ------------------------------ deoptimize --------------------------------

        /**
         * 反优化指定类中的指定方法，使其每次调用时均进入解释执行模式。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param methodName 待反优化的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         */
        @JvmStatic
        fun String.deoptimizeMethod(
            classLoader: ClassLoader?,
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.findMethod(classLoader, methodName, *parameterTypes).deoptimize()
        }

        /**
         * 反优化指定类中的指定方法，使其每次调用时均进入解释执行模式，使用默认 ClassLoader。
         *
         * @param methodName 待反优化的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         */
        @JvmStatic
        fun String.deoptimizeMethod(
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.deoptimizeMethod(ModuleData.getClassLoader(), methodName, *parameterTypes)
        }

        /**
         * 反优化当前 [Class] 中的指定方法，使其每次调用时均进入解释执行模式。
         *
         * @param methodName 待反优化的方法名称。
         * @param parameterTypes 方法的参数类型序列。
         */
        @JvmStatic
        fun Class<*>.deoptimizeMethod(
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.findMethod(methodName, *parameterTypes).deoptimize()
        }

        /**
         * 批量反优化指定类中的方法，可选按名称过滤。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @param methodName 用以过滤的方法名称；为 `null` 时反优化全部方法。
         */
        @JvmStatic
        @JvmOverloads
        fun String.deoptimizeAllMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String? = null
        ) {
            this.findAllMethod(classLoader, methodName).deoptimizeAll()
        }

        /**
         * 批量反优化指定类中与给定名称匹配的方法（使用默认 ClassLoader）。
         *
         * 此重载供 Java 调用方使用，等价于 `deoptimizeAllMethod(defaultClassLoader, methodName)`。
         *
         * @param methodName 用以过滤的方法名称。
         */
        @JvmStatic
        @JvmName("deoptimizeAllMethod")
        fun String.deoptimizeAllMethodByName(
            methodName: String
        ) {
            this.findClass().findAllMethod(methodName).deoptimizeAll()
        }

        /**
         * 批量反优化当前 [Class] 中的方法，可选按名称过滤。
         *
         * @param methodName 用以过滤的方法名称；为 `null` 时反优化全部方法。
         */
        @JvmStatic
        @JvmOverloads
        fun Class<*>.deoptimizeAllMethod(
            methodName: String? = null
        ) {
            this.findAllMethod(methodName).deoptimizeAll()
        }

        /**
         * 反优化指定类中的指定构造函数。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 构造函数的参数类型序列。
         */
        @JvmStatic
        fun String.deoptimizeConstructor(
            classLoader: ClassLoader?,
            vararg parameterTypes: Any
        ) {
            this.findConstructor(classLoader, *parameterTypes).deoptimize()
        }

        /**
         * 反优化指定类中的指定构造函数，使用默认 ClassLoader。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         */
        @JvmStatic
        fun String.deoptimizeConstructor(
            vararg parameterTypes: Any
        ) {
            this.deoptimizeConstructor(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 反优化当前 [Class] 中的指定构造函数。
         *
         * @param parameterTypes 构造函数的参数类型序列。
         */
        @JvmStatic
        fun Class<*>.deoptimizeConstructor(
            vararg parameterTypes: Any
        ) {
            this.findConstructor(*parameterTypes).deoptimize()
        }

        /**
         * 反优化指定类中的全部构造函数。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         */
        @JvmStatic
        @JvmOverloads
        fun String.deoptimizeAllConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
        ) {
            this.findAllConstructor(classLoader).deoptimizeAll()
        }

        /**
         * 反优化当前 [Class] 的全部构造函数。
         */
        @JvmStatic
        fun Class<*>.deoptimizeAllConstructor() {
            this.findAllConstructor().deoptimizeAll()
        }

        /**
         * 反优化当前 [Executable]（方法或构造函数），使其进入解释执行模式。
         */
        @JvmStatic
        fun Executable.deoptimize() {
            ModuleData.getWrapper().deoptimize(this)
        }

        /**
         * 批量反优化当前 [Executable] 数组中的所有元素。
         */
        @JvmStatic
        fun Array<out Executable>.deoptimizeAll() {
            this.forEach {
                it.deoptimize()
            }
        }

        // --------------------------------- chain -----------------------------------

        /**
         * 为指定类创建链式 Hook 构建器，支持流式 API 进行方法与构造函数的批量 Hook 配置。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]，默认值取自 [ModuleData.getClassLoader]。
         * @return [ChainTool] 链式构建器实例。
         */
        @JvmStatic
        @JvmOverloads
        fun String.buildChain(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): ChainTool {
            return ChainTool.buildChain(this, classLoader)
        }

        /**
         * 为当前 [Class] 创建链式 Hook 构建器，支持流式 API 进行方法与构造函数的批量 Hook 配置。
         *
         * @return [ChainTool] 链式构建器实例。
         */
        @JvmStatic
        fun Class<*>.buildChain(): ChainTool {
            return ChainTool.buildChain(this)
        }

        // ----------------------------------- res ---------------------------------------
        /**
         * 根据资源名称创建一个伪造的资源 ID，用于模块资源注入场景。
         *
         * @param resName 资源名称。
         * @return 生成的伪造资源 ID。
         */
        @JvmStatic
        fun createFakeResId(resName: String): Int {
            return ResInjectTool.createFakeResId(resName)
        }

        /**
         * 根据已有资源对象与原始资源 ID 创建一个伪造的资源 ID。
         *
         * @param resources 资源对象。
         * @param resId 原始资源 ID。
         * @return 生成的伪造资源 ID。
         */
        @JvmStatic
        fun createFakeResId(resources: Resources, @IdRes resId: Int): Int {
            return ResInjectTool.createFakeResId(resources, resId)
        }

        /**
         * 注册一条资源替换规则，将目标包中的指定资源替换为模块中的资源。
         *
         * @param packageName 目标应用的包名。
         * @param type 资源类型（如 `drawable`、`layout` 等）。
         * @param resName 资源名称。
         * @param replacementResId 用于替换的模块资源 ID。
         */
        @JvmStatic
        fun setResReplacement(packageName: String, type: String, resName: String, replacementResId: Int) {
            ResInjectTool.setResReplacement(packageName, type, resName, replacementResId)
        }

        /**
         * 注册一条密度值替换规则，将目标包中的指定密度资源替换为自定义浮点值。
         *
         * @param packageName 目标应用的包名。
         * @param type 资源类型。
         * @param resName 资源名称。
         * @param replacementResValue 用于替换的密度浮点值。
         */
        @JvmStatic
        fun setDensityReplacement(packageName: String, type: String, resName: String, replacementResValue: Float) {
            ResInjectTool.setDensityReplacement(packageName, type, resName, replacementResValue)
        }

        /**
         * 注册一条对象级资源替换规则，将目标包中的指定资源替换为任意类型的自定义值。
         *
         * @param packageName 目标应用的包名。
         * @param type 资源类型。
         * @param resName 资源名称。
         * @param replacementResValue 用于替换的自定义值。
         */
        @JvmStatic
        fun setObjectReplacement(packageName: String, type: String, resName: String, replacementResValue: Any?) {
            ResInjectTool.setObjectReplacement(packageName, type, resName, replacementResValue)
        }
        // -------------------------------- prefs --------------------------------------

        /**
         * 获取当前 [Context] 关联的 [SharedPreferences] 实例。
         *
         * @param prefsName 偏好设置文件名称，默认为空字符串时使用默认文件名。
         * @return [SharedPreferences] 操作接口实例。
         */
        @JvmStatic
        @JvmOverloads
        fun Context.prefs(
            prefsName: String = ""
        ): SharedPreferences {
            return PrefsTool.prefs(this, prefsName)
        }

        /**
         * 获取模块默认上下文关联的 [SharedPreferences] 实例。
         *
         * @param prefsName 偏好设置文件名称，默认为空字符串时使用默认文件名。
         * @return [SharedPreferences] 操作接口实例。
         */
        @JvmStatic
        @JvmOverloads
        fun prefs(
            prefsName: String = ""
        ): SharedPreferences {
            return PrefsTool.prefs(prefsName)
        }

        // -------------------------------- other -------------------------------
        /**
         * 获取当前调用位置的完整堆栈跟踪信息。
         *
         * @return 以字符串形式表示的堆栈跟踪信息。
         */
        @JvmStatic
        fun getStackTrace(): String {
            return LogExpand.getStackTrace()
        }

        /**
         * 测量指定代码块的执行耗时。
         *
         * @param runnable 待测量执行时间的代码块。
         * @return 执行耗时（单位：毫秒）；若执行过程中发生异常则返回 `-1`。
         */
        @JvmStatic
        fun timeConsumption(
            runnable: Runnable
        ): Long {
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
         * 将混合类型（类名字符串或 [Class] 对象）的参数类型声明统一转换为 [Class] 数组。
         *
         * @param classLoader 用以加载目标类的 [ClassLoader]。
         * @param parameterTypes 可包含 [String]（类名）或 [Class] 对象的参数类型列表。
         * @return 转换后的 [Class] 数组。
         * @throws UnexpectedException 当参数类型既非 [String] 也非 [Class] 时抛出。
         */
        @JvmStatic
        fun getParameterTypes(
            classLoader: ClassLoader?,
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
                        throw UnexpectedException("Unknown parameter types.")
                    }
                }
            }
            return classes.toTypedArray()
        }

        /**
         * 将混合类型的参数类型声明统一转换为 [Class] 数组，使用默认 ClassLoader。
         *
         * @param parameterTypes 可包含 [String]（类名）或 [Class] 对象的参数类型列表。
         * @return 转换后的 [Class] 数组。
         * @throws UnexpectedException 当参数类型既非 [String] 也非 [Class] 时抛出。
         */
        @JvmStatic
        fun getParameterTypes(
            vararg parameterTypes: Any
        ): Array<Class<*>> {
            return getParameterTypes(classLoader = ModuleData.getClassLoader(), *parameterTypes)
        }

        /**
         * 将当前 [Throwable] 作为异常直接抛出，便于在链式调用中传播错误。
         */
        @JvmStatic
        fun Throwable.throwIt() {
            throw this
        }
    }
}
