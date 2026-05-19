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
package com.hchen.hooktool.helper

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * Xposed 辅助方法的内部反射实现类。
 *
 * 替代原 Xposed 框架中 `XposedHelpers` 工具类的功能，提供以下核心能力：
 * - 类查找（支持内部类自动解析）
 * - 字段访问（实例字段、静态字段以及接口常量字段）
 * - 方法 / 构造函数解析（精确匹配与最佳匹配）
 * - 反射调用（实例方法与静态方法）
 * - 额外实例 / 静态字段附加
 *
 * 所有已解析的 [Field]、[Method] 和 [Constructor] 均使用 [ConcurrentHashMap] 缓存，
 * 避免重复的反射查找。此版本在类型匹配上严格对齐 Apache Commons Lang 规范，
 * 并深度支持 Java 8+ 接口 default 方法。
 *
 * 此类标记为 `internal`，仅在 `tool` 模块内部可访问，不属于公开 API。
 *
 * @author 焕晨HChen
 */
internal object CoreHelper {
    /** 已解析字段的缓存，键为 [FieldCacheKey]（类 + 字段名），值为 [Optional] 包装的 [Field]。 */
    private val fieldCache = ConcurrentHashMap<FieldCacheKey, Optional<Field>>()

    /** 已解析方法的缓存，键为 [MethodCacheKey]（类 + 方法名 + 参数类型 + 是否精确），值为 [Optional] 包装的 [Method]。 */
    private val methodCache = ConcurrentHashMap<MethodCacheKey, Optional<Method>>()

    /** 已解析构造函数的缓存，键为 [ConstructorCacheKey]（类 + 参数类型 + 是否精确），值为 [Optional] 包装的 [Constructor]。 */
    private val constructorCache = ConcurrentHashMap<ConstructorCacheKey, Optional<Constructor<*>>>()

    /**
     * 字段缓存的键。
     *
     * @property clazz  字段所属的类。
     * @property name   字段名称。
     */
    private data class FieldCacheKey(val clazz: Class<*>, val name: String)

    /**
     * 方法缓存的键。
     *
     * @property clazz       方法所属的类。
     * @property name        方法名称。
     * @property parameters  参数类型数组（元素可为 `null`，表示通配）。
     * @property isExact     是否为精确匹配模式（`true` 表示精确匹配，`false` 表示最佳匹配）。
     */
    private data class MethodCacheKey(
        val clazz: Class<*>,
        val name: String,
        val parameters: Array<Class<*>?>,
        val isExact: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is MethodCacheKey) return false
            return isExact == other.isExact && clazz == other.clazz && name == other.name &&
                    parameters.contentEquals(other.parameters)
        }

        override fun hashCode(): Int {
            var result = clazz.hashCode()
            result = 31 * result + name.hashCode()
            result = 31 * result + parameters.contentHashCode()
            result = 31 * result + isExact.hashCode()
            return result
        }
    }

    /**
     * 构造函数缓存的键。
     *
     * @property clazz       构造函数所属的类。
     * @property parameters  参数类型数组（元素可为 `null`，表示通配）。
     * @property isExact     是否为精确匹配模式（`true` 表示精确匹配，`false` 表示最佳匹配）。
     */
    private data class ConstructorCacheKey(
        val clazz: Class<*>,
        val parameters: Array<Class<*>?>,
        val isExact: Boolean
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is ConstructorCacheKey) return false
            return isExact == other.isExact && clazz == other.clazz &&
                    parameters.contentEquals(other.parameters)
        }

        override fun hashCode(): Int {
            var result = clazz.hashCode()
            result = 31 * result + parameters.contentHashCode()
            result = 31 * result + isExact.hashCode()
            return result
        }
    }

    /**
     * 轻量级的可选值包装器，用于缓存中区分 "未查找" 与 "查找失败" 两种状态。
     *
     * 与 `java.util.Optional` 不同，此实现允许存储 `null` 值以外的 "空" 状态，
     * 以支持 [ConcurrentHashMap.computeIfAbsent] 的缓存语义。
     *
     * @param T 被包装值的类型。
     * @property value 被包装的值，可为 `null`。
     */
    private class Optional<T>(val value: T?) {
        /** 判断是否存在有效值（即 [value] 不为 `null`）。 */
        fun isPresent(): Boolean = value != null

        /**
         * 返回有效值，如果不存在则抛出由 [lazyError] 提供的异常。
         *
         * @param lazyError 用于生成异常的延迟函数。
         * @return 有效的值。
         * @throws Throwable 由 [lazyError] 生成的异常。
         */
        fun orElseThrow(lazyError: () -> Throwable): T {
            if (value == null) throw lazyError()
            return value
        }

        companion object {
            private val EMPTY = Optional<Any>(null)

            /**
             * 创建一个包含指定值的 [Optional] 实例。
             *
             * @param v 要包装的值。
             * @return 包含指定值的 [Optional] 实例。
             */
            fun <T> of(v: T): Optional<T> = Optional(v)

            /**
             * 返回一个空的 [Optional] 实例。
             *
             * @return 空的 [Optional] 实例。
             */
            @Suppress("UNCHECKED_CAST")
            fun <T> empty(): Optional<T> = EMPTY as Optional<T>
        }
    }

    /**
     * 获取安全的类加载器。
     *
     * 按优先级依次尝试：传入的 [classLoader] → 当前线程上下文类加载器 → 系统类加载器。
     * 确保始终返回一个可用的非 `null` 类加载器。
     *
     * @param classLoader 优先使用的类加载器，可为 `null`。
     * @return 可用的类加载器实例。
     */
    private fun getSafeClassLoader(classLoader: ClassLoader?): ClassLoader {
        return classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
    }

    /**
     * 将参数类型数组中的每个元素解析为对应的 [Class] 对象。
     *
     * 数组中的每个元素可以是 [Class] 实例或类名 [String]。若为 [String]，
     * 则通过 [findClass] 进行查找。不允许出现 `null` 元素。
     *
     * @param classLoader    用于加载类的类加载器。
     * @param parameterTypes 参数类型数组，元素为 [Class] 或 [String]。
     * @return 解析后的 [Class] 类型数组。
     * @throws IllegalArgumentException 如果数组中包含 `null` 元素或非 [Class] / [String] 类型的元素。
     * @throws NoClassDefFoundError     如果某个字符串类名无法解析为有效的类。
     */
    private fun getParameterClasses(classLoader: ClassLoader, parameterTypes: Array<*>): Array<Class<*>> {
        val classes = arrayOfNulls<Class<*>>(parameterTypes.size)
        for (i in parameterTypes.indices) {
            val type = parameterTypes[i] ?: throw IllegalArgumentException("parameter type must not be null")
            classes[i] = when (type) {
                is Class<*> -> type
                is String -> findClass(type, classLoader)
                else -> throw IllegalArgumentException("parameter type must either be specified as Class or String")
            }
        }
        @Suppress("UNCHECKED_CAST")
        return classes as Array<Class<*>>
    }

    /**
     * 递归查找指定类、其父类以及实现的接口树中与给定名称匹配的字段。
     *
     * 搜索顺序：当前类声明的字段 → 当前类声明的接口树常量 → 父类及其接口树（向上逐级遍历）。
     * 首次找到即返回；如果在所有层级均未找到，则抛出 [NoSuchFieldException]。
     *
     * @param clazz     开始搜索的类。
     * @param fieldName 要查找的字段名称。
     * @return 找到的 [Field] 对象（未设置可访问性）。
     * @throws NoSuchFieldException 如果在类、父类及所有接口树中均未找到该字段。
     */
    private fun findFieldRecursiveImpl(clazz: Class<*>, fieldName: String): Field {
        var clz: Class<*>? = clazz
        while (clz != null && clz != Any::class.java) {
            try {
                return clz.getDeclaredField(fieldName)
            } catch (_: NoSuchFieldException) {
            }

            for (iface in clz.interfaces) {
                val f = findInterfaceFieldRecursive(iface, fieldName)
                if (f != null) return f
            }
            clz = clz.superclass
        }
        throw NoSuchFieldException(fieldName)
    }

    /**
     * 递归在接口及其父接口中查找指定名称的字段（接口常量）。
     *
     * @param iface     要搜索的接口。
     * @param fieldName 要查找的字段名称。
     * @return 找到的 [Field] 对象，如果未找到则返回 `null`。
     */
    private fun findInterfaceFieldRecursive(iface: Class<*>, fieldName: String): Field? {
        try {
            return iface.getDeclaredField(fieldName)
        } catch (_: NoSuchFieldException) {
        }
        for (superIface in iface.interfaces) {
            val f = findInterfaceFieldRecursive(superIface, fieldName)
            if (f != null) return f
        }
        return null
    }

    /**
     * 检查参数类型数组 [from] 是否可以逐一赋值给目标参数类型数组 [to]。
     *
     * 两个数组的长度必须相同。对于 [from] 中的 `null` 元素，仅当 [to] 中对应位置
     * 为非基本类型时才视为兼容。
     *
     * @param from 源参数类型数组（元素可为 `null`）。
     * @param to   目标参数类型数组。
     * @return 如果每个元素均可赋值，则返回 `true`。
     */
    private fun isAssignable(from: Array<Class<*>?>, to: Array<Class<*>>): Boolean {
        if (from.size != to.size) return false
        for (i in from.indices) {
            val fromType = from[i]
            if (fromType == null) {
                if (to[i].isPrimitive) return false
            } else if (!isAssignable(fromType, to[i])) return false
        }
        return true
    }

    /**
     * 检查源类型 [from] 是否可以赋值给目标类型 [to]。
     *
     * 综合考虑以下情况：
     * - 直接的 `isAssignableFrom` 关系
     * - 基本类型之间的宽化转换（如 `int` → `long`）
     * - 基本类型与其对应包装类型之间的装箱 / 拆箱（如 `int` → `Integer`）
     *
     * @param from 源类型。
     * @param to   目标类型。
     * @return 如果 [from] 可以赋值给 [to]，则返回 `true`。
     */
    private fun isAssignable(from: Class<*>, to: Class<*>): Boolean {
        if (to.isAssignableFrom(from)) return true

        if (from.isPrimitive && to.isPrimitive) {
            return isPrimitiveAssignable(from, to)
        }

        if (from.isPrimitive) {
            val wrapper = getWrapperType(from)
            return to.isAssignableFrom(wrapper)
        }

        if (to.isPrimitive) {
            val unboxedFrom = getPrimitiveType(from)
            if (unboxedFrom != null) {
                return isPrimitiveAssignable(unboxedFrom, to)
            }
            return false
        }
        return false
    }

    /**
     * 检查基本类型 [from] 是否可以通过 Java 基本类型宽化转换赋值给 [to]。
     *
     * 例如，`int` 可以宽化为 `long`、`float` 或 `double`，但不能宽化为 `short`。
     *
     * @param from 源基本类型。
     * @param to   目标基本类型。
     * @return 如果 [from] 可以通过宽化转换赋值给 [to]，则返回 `true`。
     */
    private fun isPrimitiveAssignable(from: Class<*>, to: Class<*>): Boolean {
        if (from == to) return true
        return getPrimitiveWideningSteps(from.name, to.name) != -1
    }

    /**
     * 获取基本类型对应的包装类型。
     *
     * 例如，`int` → `Integer`，`boolean` → `Boolean`。
     * 如果传入的类型不是基本类型，则原样返回。
     *
     * @param type 基本类型或其他类型。
     * @return 对应的包装类型，或原类型本身。
     */
    private fun getWrapperType(type: Class<*>): Class<*> {
        return when (type) {
            Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
            Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
            Char::class.javaPrimitiveType -> Char::class.javaObjectType
            Short::class.javaPrimitiveType -> Short::class.javaObjectType
            Int::class.javaPrimitiveType -> Int::class.javaObjectType
            Long::class.javaPrimitiveType -> Long::class.javaObjectType
            Float::class.javaPrimitiveType -> Float::class.javaObjectType
            Double::class.javaPrimitiveType -> Double::class.javaObjectType
            Void::class.javaPrimitiveType -> Void::class.javaObjectType
            else -> type
        }
    }

    /**
     * 获取包装类型对应的基本类型。
     *
     * 例如，`Integer` → `int`，`Boolean` → `boolean`。
     * 如果传入的类型不是包装类型，则返回 `null`。
     *
     * @param type 包装类型或其他类型。
     * @return 对应的基本类型，如果无法转换则返回 `null`。
     */
    private fun getPrimitiveType(type: Class<*>): Class<*>? {
        return when (type) {
            Boolean::class.javaObjectType -> Boolean::class.javaPrimitiveType
            Byte::class.javaObjectType -> Byte::class.javaPrimitiveType
            Char::class.javaObjectType -> Char::class.javaPrimitiveType
            Short::class.javaObjectType -> Short::class.javaPrimitiveType
            Int::class.javaObjectType -> Int::class.javaPrimitiveType
            Long::class.javaObjectType -> Long::class.javaPrimitiveType
            Float::class.javaObjectType -> Float::class.javaPrimitiveType
            Double::class.javaObjectType -> Double::class.javaPrimitiveType
            Void::class.javaObjectType -> Void::class.javaPrimitiveType
            else -> null
        }
    }

    /**
     * 计算从基本类型 [srcName] 到 [destName] 的宽化转换步数。
     *
     * 遵循 Java 语言规范中的拓宽基本类型转换顺序：
     * - `byte` → `short` → `int` → `long` → `float` → `double`
     * - `char` → `int` → `long` → `float` → `double`
     *
     * @param srcName  源基本类型的名称（如 `"int"`）。
     * @param destName 目标基本类型的名称（如 `"long"`）。
     * @return 宽化转换的步数，如果无法宽化则返回 `-1`。
     */
    private fun getPrimitiveWideningSteps(srcName: String, destName: String): Int {
        return when (srcName) {
            "byte" -> when (destName) {
                "short" -> 1
                "int" -> 2
                "long" -> 3
                "float" -> 4
                "double" -> 5
                else -> -1
            }

            "short" -> when (destName) {
                "int" -> 1
                "long" -> 2
                "float" -> 3
                "double" -> 4
                else -> -1
            }

            "char" -> when (destName) {
                "int" -> 1
                "long" -> 2
                "float" -> 3
                "double" -> 4
                else -> -1
            }

            "int" -> when (destName) {
                "long" -> 1
                "float" -> 2
                "double" -> 3
                else -> -1
            }

            "long" -> when (destName) {
                "float" -> 1
                "double" -> 2
                else -> -1
            }

            "float" -> when (destName) {
                "double" -> 1
                else -> -1
            }

            else -> -1
        }
    }

    /**
     * 计算源基本类型到目标基本类型之间的类型提升代价。
     *
     * 如果 [srcType] 是包装类型，会先进行拆箱（代价 0.1），然后计算宽化步数代价（每步 0.1）。
     * 对于 `void` 和 `boolean` 类型，返回 [Float.MAX_VALUE] 表示不可转换。
     *
     * @param srcType  源类型（可以是包装类型或基本类型）。
     * @param destType 目标基本类型。
     * @return 类型转换代价，越小表示越兼容。不可转换时返回 [Float.MAX_VALUE]。
     */
    private fun getPrimitivePromotionCost(srcType: Class<*>, destType: Class<*>): Float {
        var cost = 0.0f
        var cls: Class<*>? = srcType
        if (cls != null && !cls.isPrimitive) {
            cost += 0.1f // 拆箱代价
            cls = getPrimitiveType(cls)
        }
        if (cls == null) return 1.5f
        if (cls == destType) return cost
        if (cls == Void::class.javaPrimitiveType || cls == Boolean::class.javaPrimitiveType) return Float.MAX_VALUE

        val steps = getPrimitiveWideningSteps(cls.name, destType.name)
        if (steps == -1) return Float.MAX_VALUE

        cost += 0.1f * steps
        return cost
    }

    /**
     * 计算源类型到目标类型之间的对象转换代价。
     *
     * 如果目标类型是基本类型，委托给 [getPrimitivePromotionCost] 计算。
     * 否则，沿继承链向上遍历，计算类层次距离：
     * - 实现接口的代价为 0.25
     * - 每向上一级父类的代价为 1.0
     * - `null` 源类型的代价固定为 1.5
     *
     * @param srcType  源类型，可为 `null`（表示运行时为 `null` 值）。
     * @param destType 目标类型。
     * @return 类型转换代价，越小表示越兼容。
     */
    private fun getObjectTransformationCost(srcType: Class<*>?, destType: Class<*>): Float {
        if (destType.isPrimitive) {
            if (srcType == null) return 1.5f
            return getPrimitivePromotionCost(srcType, destType)
        }
        if (srcType == null) return 1.5f
        var cost = 0f
        var src: Class<*>? = srcType
        while (src != null && src != destType) {
            if (destType.isInterface && destType.isAssignableFrom(src)) {
                cost += 0.25f
                break
            }
            cost++
            src = src.superclass
        }
        if (src == null) cost += 1.5f
        return cost
    }

    /**
     * 计算所有参数的总类型转换代价。
     *
     * 逐一对比 [srcArgs] 和 [destArgs] 中每个位置的类型，累加 [getObjectTransformationCost] 的结果。
     *
     * @param srcArgs  源参数类型数组（元素可为 `null`）。
     * @param destArgs 目标参数类型数组。
     * @return 总的类型转换代价。
     */
    private fun getTotalTransformationCost(srcArgs: Array<Class<*>?>, destArgs: Array<Class<*>>): Float {
        var totalCost = 0f
        for (i in srcArgs.indices) {
            totalCost += getObjectTransformationCost(srcArgs[i], destArgs[i])
        }
        return totalCost
    }

    /**
     * 收集指定类及其父类、接口中与给定名称和参数类型兼容的所有方法。
     *
     * 搜索顺序：
     * 1. 当前类声明的所有方法（仅第一轮包含私有方法）
     * 2. 当前类实现的接口（深度优先递归，排除静态方法）
     * 3. 父类（向上逐级遍历）
     *
     * 通过 [seen] 集合对方法签名去重，避免同一方法被多次收集。
     *
     * @param clazz          开始搜索的类。
     * @param name           要查找的方法名。
     * @param parameterTypes 期望的参数类型（元素可为 `null`）。
     * @param result         用于收集匹配方法的可变列表。
     */
    private fun collectMethodsBestMatch(clazz: Class<*>, name: String, parameterTypes: Array<Class<*>?>, result: MutableList<Method>) {
        val seen = HashSet<String>()
        var clz: Class<*>? = clazz
        var considerPrivate = true
        while (clz != null) {
            for (method in clz.declaredMethods) {
                if (!considerPrivate && Modifier.isPrivate(method.modifiers)) continue
                if (method.name == name && isAssignable(parameterTypes, method.parameterTypes)) {
                    val sig = method.name + method.parameterTypes.contentToString()
                    if (seen.add(sig)) {
                        result.add(method)
                    }
                }
            }
            for (iface in clz.interfaces) {
                collectInterfaceMethodsRecursive(iface, name, parameterTypes, seen, result)
            }
            considerPrivate = false
            clz = clz.superclass
        }
    }

    /**
     * 递归收集接口及其父接口中与指定名称和参数类型兼容的非静态方法。
     *
     * 用于支持 Java 8+ 接口 default 方法的深度查找。接口中的静态方法会被排除，
     * 因为静态方法不可被类继承。通过 [seen] 集合对方法签名去重。
     *
     * @param iface          要搜索的接口。
     * @param name           要查找的方法名。
     * @param parameterTypes 期望的参数类型（元素可为 `null`）。
     * @param seen           已见过的方法签名集合，用于去重。
     * @param result         用于收集匹配方法的可变列表。
     */
    private fun collectInterfaceMethodsRecursive(iface: Class<*>, name: String, parameterTypes: Array<Class<*>?>, seen: HashSet<String>, result: MutableList<Method>) {
        for (method in iface.declaredMethods) {
            if (Modifier.isStatic(method.modifiers)) continue
            if (method.name == name && isAssignable(parameterTypes, method.parameterTypes)) {
                val sig = method.name + method.parameterTypes.contentToString()
                if (seen.add(sig)) {
                    result.add(method)
                }
            }
        }
        for (superIface in iface.interfaces) {
            collectInterfaceMethodsRecursive(superIface, name, parameterTypes, seen, result)
        }
    }

    /**
     * 比较候选方法 / 构造函数的形参相对于当前最佳匹配与给定 [parameterTypes] 的匹配程度。
     *
     * 使用与 Apache Commons Lang `MemberUtils.compareMethodFit` 一致的继承距离算法。
     * 如果 candidate 更优则返回负值，如果 current 更优则返回正值，两者同等合适则返回零。
     *
     * @param candidateParams 候选方法的参数类型数组。
     * @param currentParams   当前最佳匹配方法的参数类型数组。
     * @param parameterTypes  期望的参数类型数组（元素可为 `null`）。
     * @return 负值表示 candidate 更优，正值表示 current 更优，零表示同等合适。
     */
    private fun compareFit(candidateParams: Array<Class<*>>, currentParams: Array<Class<*>>, parameterTypes: Array<Class<*>?>): Int {
        val candidateCost = getTotalTransformationCost(parameterTypes, candidateParams)
        val currentCost = getTotalTransformationCost(parameterTypes, currentParams)
        return candidateCost.compareTo(currentCost)
    }

    // ==================== Class ====================

    /**
     * 将类名转换为 JVM 内部规范格式。
     *
     * 处理数组类型：将 `"int[]"` 转换为 `"[I"`，将 `"java.lang.String[]"` 转换为 `"[Ljava.lang.String;"`。
     * 非数组类型则去除首尾空白后原样返回。
     *
     * @param className 待转换的类名。
     * @return JVM 内部规范格式的类名。
     */
    private fun toCanonicalName(className: String): String {
        var name = className.trim()
        if (name.endsWith("[]")) {
            val sb = StringBuilder()
            while (name.endsWith("[]")) {
                sb.append("[")
                name = name.substring(0, name.length - 2)
            }
            when (name) {
                "boolean" -> sb.append("Z")
                "byte" -> sb.append("B")
                "char" -> sb.append("C")
                "short" -> sb.append("S")
                "int" -> sb.append("I")
                "long" -> sb.append("J")
                "float" -> sb.append("F")
                "double" -> sb.append("D")
                else -> sb.append("L").append(name).append(";")
            }
            return sb.toString()
        }
        return name
    }

    /** 基本类型名称到其 [Class] 对象的映射表。 */
    private val PRIMITIVE_NAME_MAP = mapOf(
        "boolean" to Boolean::class.javaPrimitiveType,
        "byte" to Byte::class.javaPrimitiveType,
        "char" to Char::class.javaPrimitiveType,
        "short" to Short::class.javaPrimitiveType,
        "int" to Int::class.javaPrimitiveType,
        "long" to Long::class.javaPrimitiveType,
        "float" to Float::class.javaPrimitiveType,
        "double" to Double::class.javaPrimitiveType,
        "void" to Void::class.javaPrimitiveType
    )

    /**
     * 查找指定名称的类。
     *
     * 支持以下格式：
     * - 基本类型名称（如 `"int"`、`"void"`）
     * - 数组类型（如 `"int[]"`、`"java.lang.String[]"`）
     * - 普通类名及内部类（如未找到则自动尝试将 `.` 替换为 `$` 进行内部类解析）
     *
     * @param className   要查找的类名。
     * @param classLoader 用于加载类的类加载器，可为 `null`（将使用安全类加载器）。
     * @return 对应的 [Class] 对象。
     * @throws NoClassDefFoundError 如果无法找到指定的类。
     */
    @JvmStatic
    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        var next = className
        var lastDotIndex = -1
        do {
            try {
                val primitive = PRIMITIVE_NAME_MAP[next]
                if (primitive != null) return primitive
                return Class.forName(toCanonicalName(next), false, classLoader)
            } catch (_: ClassNotFoundException) {
                lastDotIndex = next.lastIndexOf('.')
                if (lastDotIndex != -1) {
                    next = next.substring(0, lastDotIndex) + "$" + next.substring(lastDotIndex + 1)
                }
            }
        } while (lastDotIndex != -1)
        throw NoClassDefFoundError(className)
    }

    /**
     * 查找指定名称的类，如果不存在则返回 `null`。
     *
     * 是 [findClass] 的安全版本，不会抛出异常。
     *
     * @param className   要查找的类名。
     * @param classLoader 用于加载类的类加载器，可为 `null`。
     * @return 对应的 [Class] 对象，如果不存在则返回 `null`。
     */
    @JvmStatic
    fun findClassIfExists(className: String, classLoader: ClassLoader?): Class<*>? {
        return try {
            findClass(className, classLoader)
        } catch (_: NoClassDefFoundError) {
            null
        }
    }

    // ==================== Field ====================

    /**
     * 查找指定类中具有给定名称的字段，支持沿继承链及接口树向上递归查找。
     *
     * 查找结果会被缓存。找到的字段会自动设置为可访问（`isAccessible = true`）。
     *
     * @param clazz     要查找字段的类。
     * @param fieldName 字段名称。
     * @return 已设置为可访问的 [Field] 对象。
     * @throws NoSuchFieldError 如果在类、父类及接口树中均未找到该字段。
     */
    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        val key = FieldCacheKey(clazz, fieldName)
        return fieldCache.computeIfAbsent(key) { k ->
            try {
                val field = findFieldRecursiveImpl(k.clazz, k.name)
                field.isAccessible = true
                Optional.of(field)
            } catch (_: NoSuchFieldException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchFieldError("${clazz.name}#$fieldName") }
    }

    /**
     * 查找指定类中具有给定名称的字段，如果不存在则返回 `null`。
     *
     * 是 [findField] 的安全版本，不会抛出异常。
     *
     * @param clazz     要查找字段的类。
     * @param fieldName 字段名称。
     * @return 已设置为可访问的 [Field] 对象，如果不存在则返回 `null`。
     */
    @JvmStatic
    fun findFieldIfExists(clazz: Class<*>, fieldName: String): Field? {
        return try {
            findField(clazz, fieldName)
        } catch (_: NoSuchFieldError) {
            null
        }
    }

    // ==================== Method Exact ====================

    /**
     * 在指定类中查找精确匹配的方法。
     *
     * 参数类型可以是 [Class] 对象或类名 [String]（将通过 [findClass] 解析）。
     * 查找结果会被缓存。
     *
     * @param clazz          要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Method] 对象。
     * @throws NoClassDefFoundError 如果某个字符串参数类型无法解析为有效的类。
     * @throws NoSuchMethodError    如果在类中未找到精确匹配的方法。
     */
    @JvmStatic
    fun findMethodExact(clazz: Class<*>, methodName: String, vararg parameterTypes: Any): Method {
        val resolvedTypes = getParameterClasses(getSafeClassLoader(clazz.classLoader), parameterTypes as Array<*>)
        return findMethodExactWithClasses(clazz, methodName, *resolvedTypes)
    }

    /**
     * 在指定类名对应的类中查找精确匹配的方法。
     *
     * 先通过 [findClass] 加载类，再解析参数类型并委托给 [findMethodExactWithClasses]。
     * 查找结果会被缓存。
     *
     * @param className      要查找方法的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Method] 对象。
     * @throws NoClassDefFoundError 如果类名或某个字符串参数类型无法解析为有效的类。
     * @throws NoSuchMethodError    如果在类中未找到精确匹配的方法。
     */
    @JvmStatic
    fun findMethodExact(className: String, classLoader: ClassLoader?, methodName: String, vararg parameterTypes: Any): Method {
        val clazz = findClass(className, classLoader)
        val resolvedTypes = getParameterClasses(getSafeClassLoader(classLoader), parameterTypes as Array<*>)
        return findMethodExactWithClasses(clazz, methodName, *resolvedTypes)
    }

    /**
     * 在指定类中查找精确匹配的方法（参数类型已解析为 [Class] 对象）。
     *
     * 支持从实体类及其接口树中查找继承的 default 方法。
     * 查找结果会被缓存。
     *
     * @param clazz          要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 精确的参数类型数组。
     * @return 已设置为可访问的 [Method] 对象。
     * @throws NoSuchMethodError 如果在类中未找到精确匹配的方法。
     */
    @JvmStatic
    fun findMethodExactWithClasses(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val key = MethodCacheKey(clazz, methodName, arrayOf(*parameterTypes), true)
        return methodCache.computeIfAbsent(key) { k ->
            try {
                @Suppress("UNCHECKED_CAST")
                val method = findMethodExactRecursive(k.clazz, k.name, k.parameters as Array<Class<*>>)
                method.isAccessible = true
                Optional.of(method)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}#$methodName(${parameterTypes.joinToString { it.name }})") }
    }

    /**
     * 递归查找精确匹配的方法，支持从实体类及其接口树中查找继承的 default 方法。
     *
     * 搜索顺序：当前类声明的方法 → 当前类实现的接口（深度优先）→ 父类（向上遍历）。
     * 接口中的静态方法会被排除。
     *
     * @param clazz          开始搜索的类。
     * @param name           要查找的方法名。
     * @param parameterTypes 精确的参数类型数组。
     * @return 已设置为可访问的 [Method] 引用。
     * @throws NoSuchMethodException 如果在类及所有接口中均未找到该方法。
     */
    private fun findMethodExactRecursive(clazz: Class<*>, name: String, parameterTypes: Array<Class<*>>): Method {
        var clz: Class<*>? = clazz
        while (clz != null) {
            try {
                return clz.getDeclaredMethod(name, *parameterTypes)
            } catch (_: NoSuchMethodException) {
            }
            for (iface in clz.interfaces) {
                val m = findInterfaceMethodExactRecursive(iface, name, parameterTypes)
                if (m != null) return m
            }
            clz = clz.superclass
        }
        throw NoSuchMethodException()
    }

    /**
     * 递归在接口及其父接口中查找精确匹配的非静态方法。
     *
     * 用于支持 Java 8+ 接口 default 方法的精确查找。接口中的静态方法会被排除，
     * 因为静态方法不可被类继承。
     *
     * @param iface          要搜索的接口。
     * @param name           要查找的方法名。
     * @param parameterTypes 精确的参数类型数组。
     * @return 已设置为可访问的 [Method] 引用，如果未找到则返回 `null`。
     */
    private fun findInterfaceMethodExactRecursive(iface: Class<*>, name: String, parameterTypes: Array<Class<*>>): Method? {
        try {
            val m = iface.getDeclaredMethod(name, *parameterTypes)
            if (!Modifier.isStatic(m.modifiers)) return m
        } catch (_: NoSuchMethodException) {
        }
        for (superIface in iface.interfaces) {
            val m = findInterfaceMethodExactRecursive(superIface, name, parameterTypes)
            if (m != null) return m
        }
        return null
    }

    /**
     * 在指定类中查找精确匹配的方法，如果不存在则返回 `null`。
     *
     * 是 [findMethodExact] 的安全版本，不会抛出异常。
     *
     * @param clazz          要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Method] 对象，如果不存在则返回 `null`。
     */
    @JvmStatic
    fun findMethodExactIfExists(clazz: Class<*>, methodName: String, vararg parameterTypes: Any): Method? {
        return try {
            findMethodExact(clazz, methodName, *parameterTypes)
        } catch (_: NoClassDefFoundError) {
            null
        } catch (_: NoSuchMethodError) {
            null
        }
    }

    /**
     * 在指定类名对应的类中查找精确匹配的方法，如果不存在则返回 `null`。
     *
     * 是 [findMethodExact]（接受类名版本）的安全版本，不会抛出异常。
     *
     * @param className      要查找方法的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Method] 对象，如果不存在则返回 `null`。
     */
    @JvmStatic
    fun findMethodExactIfExists(className: String, classLoader: ClassLoader?, methodName: String, vararg parameterTypes: Any): Method? {
        return try {
            findMethodExact(className, classLoader, methodName, *parameterTypes)
        } catch (_: NoClassDefFoundError) {
            null
        } catch (_: NoSuchMethodError) {
            null
        }
    }

    // ==================== Method Best Match ====================

    /**
     * 在指定类中查找与给定参数类型最匹配的方法。
     *
     * 如果所有参数类型均非 `null`，则优先尝试精确匹配（[findMethodExactWithClasses]）。
     * 精确匹配失败时，回退到最佳匹配模式：收集所有兼容的方法候选，
     * 使用基于继承距离的评分算法选择最优匹配。
     * 查找结果会被缓存。
     *
     * @param clazz          要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示通配）。
     * @return 已设置为可访问的最佳匹配 [Method] 对象。
     * @throws NoSuchMethodError 如果在类中未找到任何兼容的方法。
     */
    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>?): Method {
        if (parameterTypes.all { it != null }) {
            try {
                @Suppress("UNCHECKED_CAST")
                return findMethodExactWithClasses(clazz, methodName, *(parameterTypes as Array<Class<*>>))
            } catch (_: NoSuchMethodError) {
            }
        }

        val key = MethodCacheKey(clazz, methodName, arrayOf(*parameterTypes), false)
        return methodCache.computeIfAbsent(key) { k ->
            val candidates = mutableListOf<Method>()
            collectMethodsBestMatch(k.clazz, k.name, k.parameters, candidates)
            var bestMatch: Method? = null
            for (method in candidates) {
                if (bestMatch == null || compareFit(method.parameterTypes, bestMatch.parameterTypes, k.parameters) < 0) {
                    bestMatch = method
                }
            }
            if (bestMatch != null) {
                bestMatch.isAccessible = true
                Optional.of(bestMatch)
            } else {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}#$methodName(${parameterTypes.joinToString { it?.name ?: "null" }})") }
    }

    /**
     * 在指定类中根据实际参数值查找最匹配的方法。
     *
     * 通过运行时参数的实际类型（`javaClass`）推断参数类型，
     * 然后委托给 [findMethodBestMatch]（接受类型数组版本）。
     *
     * @param clazz      要查找方法的类。
     * @param methodName 方法名称。
     * @param args       实际参数值（可为 `null`）。
     * @return 已设置为可访问的最佳匹配 [Method] 对象。
     * @throws NoSuchMethodError 如果在类中未找到任何兼容的方法。
     */
    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findMethodBestMatch(clazz, methodName, *paramTypes)
    }

    /**
     * 在指定类中根据指定的参数类型和实际参数值查找最匹配的方法。
     *
     * 对于 [parameterTypes] 中为 `null` 的位置，使用 [args] 中对应元素的实际类型替代。
     * 然后委托给 [findMethodBestMatch]（接受类型数组版本）。
     *
     * @param clazz          要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可为 `null`）。
     * @param args           实际参数值数组。
     * @return 已设置为可访问的最佳匹配 [Method] 对象。
     * @throws NoSuchMethodError 如果在类中未找到任何兼容的方法。
     */
    @JvmStatic
    fun findMethodBestMatchWithTypes(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>?>, args: Array<out Any?>): Method {
        val resolvedTypes: Array<Class<*>?> = Array(parameterTypes.size) { parameterTypes[it] }
        for (i in resolvedTypes.indices) {
            if (resolvedTypes[i] == null && args[i] != null) {
                resolvedTypes[i] = args[i]!!.javaClass
            }
        }
        return findMethodBestMatch(clazz, methodName, *resolvedTypes)
    }

    // ==================== Constructor Exact ====================

    /**
     * 在指定类中查找精确匹配的构造函数。
     *
     * 参数类型可以是 [Class] 对象或类名 [String]（将通过 [findClass] 解析）。
     * 查找结果会被缓存。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Constructor] 对象。
     * @throws NoClassDefFoundError 如果某个字符串参数类型无法解析为有效的类。
     * @throws NoSuchMethodError    如果在类中未找到精确匹配的构造函数。
     */
    @JvmStatic
    fun findConstructorExact(clazz: Class<*>, vararg parameterTypes: Any): Constructor<*> {
        val resolvedTypes = getParameterClasses(getSafeClassLoader(clazz.classLoader), parameterTypes as Array<*>)
        return findConstructorExactWithClasses(clazz, *resolvedTypes)
    }

    /**
     * 在指定类名对应的类中查找精确匹配的构造函数。
     *
     * 先通过 [findClass] 加载类，再解析参数类型并委托给 [findConstructorExactWithClasses]。
     * 查找结果会被缓存。
     *
     * @param className      要查找构造函数的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Constructor] 对象。
     * @throws NoClassDefFoundError 如果类名或某个字符串参数类型无法解析为有效的类。
     * @throws NoSuchMethodError    如果在类中未找到精确匹配的构造函数。
     */
    @JvmStatic
    fun findConstructorExact(className: String, classLoader: ClassLoader?, vararg parameterTypes: Any): Constructor<*> {
        val clazz = findClass(className, classLoader)
        val resolvedTypes = getParameterClasses(getSafeClassLoader(classLoader), parameterTypes as Array<*>)
        return findConstructorExactWithClasses(clazz, *resolvedTypes)
    }

    /**
     * 在指定类中查找精确匹配的构造函数（参数类型已解析为 [Class] 对象）。
     *
     * 查找结果会被缓存。找到的构造函数会自动设置为可访问（`isAccessible = true`）。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 精确的参数类型数组。
     * @return 已设置为可访问的 [Constructor] 对象。
     * @throws NoSuchMethodError 如果在类中未找到精确匹配的构造函数。
     */
    @JvmStatic
    fun findConstructorExactWithClasses(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        val key = ConstructorCacheKey(clazz, arrayOf(*parameterTypes), true)
        return constructorCache.computeIfAbsent(key) { k ->
            try {
                @Suppress("UNCHECKED_CAST")
                val constructor = k.clazz.getDeclaredConstructor(*(k.parameters as Array<Class<*>>))
                constructor.isAccessible = true
                Optional.of(constructor)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}<init>(${parameterTypes.joinToString { it.name }})") }
    }

    /**
     * 在指定类中查找精确匹配的构造函数，如果不存在则返回 `null`。
     *
     * 是 [findConstructorExact] 的安全版本，不会抛出异常。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Constructor] 对象，如果不存在则返回 `null`。
     */
    @JvmStatic
    fun findConstructorExactIfExists(clazz: Class<*>, vararg parameterTypes: Any): Constructor<*>? {
        return try {
            findConstructorExact(clazz, *parameterTypes)
        } catch (_: NoClassDefFoundError) {
            null
        } catch (_: NoSuchMethodError) {
            null
        }
    }

    /**
     * 在指定类名对应的类中查找精确匹配的构造函数，如果不存在则返回 `null`。
     *
     * 是 [findConstructorExact]（接受类名版本）的安全版本，不会抛出异常。
     *
     * @param className      要查找构造函数的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param parameterTypes 参数类型（可为 [Class] 或 [String]）。
     * @return 已设置为可访问的 [Constructor] 对象，如果不存在则返回 `null`。
     */
    @JvmStatic
    fun findConstructorExactIfExists(className: String, classLoader: ClassLoader?, vararg parameterTypes: Any): Constructor<*>? {
        return try {
            findConstructorExact(className, classLoader, *parameterTypes)
        } catch (_: NoClassDefFoundError) {
            null
        } catch (_: NoSuchMethodError) {
            null
        }
    }

    // ==================== Constructor Best Match ====================

    /**
     * 在指定类中查找与给定参数类型最匹配的构造函数。
     *
     * 如果所有参数类型均非 `null`，则优先尝试精确匹配（[findConstructorExactWithClasses]）。
     * 精确匹配失败时，回退到最佳匹配模式：遍历所有声明的构造函数，
     * 使用基于继承距离的评分算法选择最优匹配。
     * 查找结果会被缓存。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示通配）。
     * @return 已设置为可访问的最佳匹配 [Constructor] 对象。
     * @throws NoSuchMethodError 如果在类中未找到任何兼容的构造函数。
     */
    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg parameterTypes: Class<*>?): Constructor<*> {
        if (parameterTypes.all { it != null }) {
            try {
                @Suppress("UNCHECKED_CAST")
                return findConstructorExactWithClasses(clazz, *(parameterTypes as Array<Class<*>>))
            } catch (_: NoSuchMethodError) {
            }
        }

        val key = ConstructorCacheKey(clazz, arrayOf(*parameterTypes), false)
        return constructorCache.computeIfAbsent(key) { k ->
            var bestMatch: Constructor<*>? = null
            for (constructor in k.clazz.declaredConstructors) {
                if (isAssignable(k.parameters, constructor.parameterTypes)) {
                    if (bestMatch == null || compareFit(constructor.parameterTypes, bestMatch.parameterTypes, k.parameters) < 0) {
                        bestMatch = constructor
                    }
                }
            }
            if (bestMatch != null) {
                bestMatch.isAccessible = true
                Optional.of(bestMatch)
            } else {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}<init>(${parameterTypes.joinToString { it?.name ?: "null" }})") }
    }

    /**
     * 在指定类中根据实际参数值查找最匹配的构造函数。
     *
     * 通过运行时参数的实际类型（`javaClass`）推断参数类型，
     * 然后委托给 [findConstructorBestMatch]（接受类型数组版本）。
     *
     * @param clazz 要查找构造函数的类。
     * @param args  实际参数值（可为 `null`）。
     * @return 已设置为可访问的最佳匹配 [Constructor] 对象。
     * @throws NoSuchMethodError 如果在类中未找到任何兼容的构造函数。
     */
    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg args: Any?): Constructor<*> {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findConstructorBestMatch(clazz, *paramTypes)
    }

    /**
     * 在指定类中根据指定的参数类型和实际参数值查找最匹配的构造函数。
     *
     * 对于 [parameterTypes] 中为 `null` 的位置，使用 [args] 中对应元素的实际类型替代。
     * 然后委托给 [findConstructorBestMatch]（接受类型数组版本）。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 参数类型数组（元素可为 `null`）。
     * @param args           实际参数值数组。
     * @return 已设置为可访问的最佳匹配 [Constructor] 对象。
     * @throws NoSuchMethodError 如果在类中未找到任何兼容的构造函数。
     */
    @JvmStatic
    fun findConstructorBestMatchWithTypes(clazz: Class<*>, parameterTypes: Array<Class<*>?>, args: Array<out Any?>): Constructor<*> {
        val resolvedTypes: Array<Class<*>?> = Array(parameterTypes.size) { parameterTypes[it] }
        for (i in resolvedTypes.indices) {
            if (resolvedTypes[i] == null && args[i] != null) {
                resolvedTypes[i] = args[i]!!.javaClass
            }
        }
        return findConstructorBestMatch(clazz, *resolvedTypes)
    }

    // ==================== Call Method ====================

    /**
     * 调用指定对象的实例方法。
     *
     * 通过 [findMethodBestMatch] 根据实际参数值自动查找最佳匹配的方法并调用。
     *
     * @param obj        目标对象实例。
     * @param methodName 方法名称。
     * @param args       方法参数值（可为 `null`）。
     * @return 方法的返回值，如果方法返回类型为 `void` 则返回 `null`。
     * @throws NoSuchMethodError   如果未找到兼容的方法。
     * @throws IllegalAccessError  如果方法无法访问。
     * @throws Throwable           方法执行过程中抛出的原始异常。
     */
    @JvmStatic
    fun callMethod(obj: Any, methodName: String, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(obj.javaClass, methodName, *args)
            method.invoke(obj, *args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    /**
     * 使用指定的参数类型调用指定对象的实例方法。
     *
     * 支持在参数类型数组中传入 `null` 作为通配符进行最佳匹配查找并调用。
     *
     * @param obj            目标对象实例。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）。
     * @param args           方法参数值（可为 `null`）。
     * @return 方法的返回值，如果方法返回类型为 `void` 则返回 `null`。
     * @throws NoSuchMethodError   如果未找到兼容的方法。
     * @throws IllegalAccessError  如果方法无法访问。
     * @throws Throwable           方法执行过程中抛出的原始异常。
     */
    @JvmStatic
    fun callMethod(obj: Any, methodName: String, parameterTypes: Array<Class<*>?>, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(obj.javaClass, methodName, *parameterTypes)
            method.invoke(obj, *args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    // ==================== Call Static Method ====================

    /**
     * 调用指定类的静态方法。
     *
     * 通过 [findMethodBestMatch] 根据实际参数值自动查找最佳匹配的方法并以 `null` 作为接收者调用。
     *
     * @param clazz      目标类。
     * @param methodName 方法名称。
     * @param args       方法参数值（可为 `null`）。
     * @return 方法的返回值，如果方法返回类型为 `void` 则返回 `null`。
     * @throws NoSuchMethodError   如果未找到兼容的方法。
     * @throws IllegalAccessError  如果方法无法访问。
     * @throws Throwable           方法执行过程中抛出的原始异常。
     */
    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(clazz, methodName, *args)
            method.invoke(null, *args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    /**
     * 使用指定的参数类型调用指定类的静态方法。
     *
     * 支持在参数类型数组中传入 `null` 作为通配符进行最佳匹配查找并以 `null` 作为接收者调用。
     *
     * @param clazz          目标类。
     * @param methodName     方法名称.
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）。
     * @param args           方法参数值（可为 `null`）。
     * @return 方法的返回值，如果方法返回类型为 `void` 则返回 `null`。
     * @throws NoSuchMethodError   如果未找到兼容的方法。
     * @throws IllegalAccessError  如果方法无法访问。
     * @throws Throwable           方法执行过程中抛出的原始异常。
     */
    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>?>, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(clazz, methodName, *parameterTypes)
            method.invoke(null, *args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    // ==================== Object/Static Field & New Instance ====================

    /**
     * 获取指定对象的实例字段值。
     *
     * 通过 [findField] 查找字段并读取其值。
     *
     * @param obj       目标对象实例。
     * @param fieldName 字段名称。
     * @return 字段的值，可能为 `null`。
     * @throws NoSuchFieldError   如果未找到该字段。
     * @throws IllegalAccessError 如果字段无法访问。
     */
    @JvmStatic
    fun getObjectField(obj: Any, fieldName: String): Any? {
        return try {
            val field = findField(obj.javaClass, fieldName)
            field.get(obj)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 设置指定对象的实例字段值。
     *
     * 通过 [findField] 查找字段并设置其值。
     *
     * @param obj       目标对象实例。
     * @param fieldName 字段名称。
     * @param value     要设置的值，可为 `null`。
     * @throws NoSuchFieldError   如果未找到该字段。
     * @throws IllegalAccessError 如果字段无法访问。
     */
    @JvmStatic
    fun setObjectField(obj: Any, fieldName: String, value: Any?) {
        try {
            val field = findField(obj.javaClass, fieldName)
            field.set(obj, value)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 获取指定类的静态字段值（支持接口静态常量）。
     *
     * 通过 [findField] 查找字段并以 `null` 作为接收者读取其值。
     *
     * @param clazz     目标类或接口。
     * @param fieldName 字段名称。
     * @return 字段的值，可能为 `null`。
     * @throws NoSuchFieldError   如果未找到该字段。
     * @throws IllegalAccessError 如果字段无法访问。
     */
    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return try {
            val field = findField(clazz, fieldName)
            field.get(null)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 设置指定类的静态字段值。
     *
     * 通过 [findField] 查找字段并以 `null` 作为接收者设置其值。
     *
     * @param clazz     目标类。
     * @param fieldName 字段名称。
     * @param value     要设置的值，可为 `null`。
     * @throws NoSuchFieldError   如果未找到该字段。
     * @throws IllegalAccessError 如果字段无法访问。
     */
    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            val field = findField(clazz, fieldName)
            field.set(null, value)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 通过最佳匹配的构造函数创建指定类的新实例。
     *
     * 通过 [findConstructorBestMatch] 根据实际参数值自动查找最佳匹配的构造函数并创建实例。
     *
     * @param clazz 目标类。
     * @param args  构造函数参数值（可为 `null`）。
     * @return 创建的新实例。
     * @throws NoSuchMethodError     如果未找到兼容的构造函数。
     * @throws IllegalAccessError    如果构造函数无法访问。
     * @throws InstantiationError    如果类是抽象类或接口，无法实例化。
     * @throws Throwable             构造函数执行过程中抛出的原始异常。
     */
    @JvmStatic
    fun newInstance(clazz: Class<*>, vararg args: Any?): Any {
        return try {
            val constructor = findConstructorBestMatch(clazz, *args)
            constructor.newInstance(*args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        } catch (e: InstantiationException) {
            throw InstantiationError(e.message)
        }
    }

    /**
     * 使用指定的参数类型通过最佳匹配的构造函数创建指定类的新实例。
     *
     * 支持在参数类型数组中传入 `null` 作为通配符。
     *
     * @param clazz          目标类。
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）。
     * @param args           构造函数参数值（可为 `null`）。
     * @return 创建的新实例。
     * @throws NoSuchMethodError     如果未找到兼容的构造函数。
     * @throws IllegalAccessError    如果构造函数无法访问。
     * @throws InstantiationError    如果类是抽象类或接口，无法实例化。
     * @throws Throwable             构造函数执行过程中抛出的原始异常。
     */
    @JvmStatic
    fun newInstance(clazz: Class<*>, parameterTypes: Array<Class<*>?>, vararg args: Any?): Any {
        return try {
            val constructor = findConstructorBestMatchWithTypes(clazz, parameterTypes, args)
            constructor.newInstance(*args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        } catch (e: InstantiationException) {
            throw InstantiationError(e.message)
        }
    }

    // ==================== Additional Fields ====================

    /**
     * 附加实例字段的存储表。
     *
     * 使用 [WeakHashMap] 以弱引用持有对象键，当对象被 GC 回收后，
     * 对应的附加字段会自动清理，避免内存泄漏。
     * 访问时通过 [synchronized] 保证线程安全。
     */
    private val additionalFields = WeakHashMap<Any, HashMap<String, Any?>>()

    /**
     * 为指定对象设置一个附加的实例字段。
     *
     * 附加字段不依赖于类的字段定义，可以为任意对象动态关联键值对数据。
     * 如果指定的键已存在，则更新其值并返回旧值。
     *
     * @param obj   目标对象实例。
     * @param key   字段键名。
     * @param value 要设置的值，可为 `null`。
     * @return 该键先前关联的值，如果之前未设置则返回 `null`。
     */
    @JvmStatic
    fun setAdditionalInstanceField(obj: Any, key: String, value: Any?): Any? {
        synchronized(additionalFields) {
            val map = additionalFields.getOrPut(obj) { HashMap() }
            return map.put(key, value)
        }
    }

    /**
     * 获取指定对象的附加实例字段值。
     *
     * @param obj 目标对象实例。
     * @param key 字段键名。
     * @return 该键关联的值，如果未设置则返回 `null`。
     */
    @JvmStatic
    fun getAdditionalInstanceField(obj: Any, key: String): Any? {
        synchronized(additionalFields) {
            val map = additionalFields[obj] ?: return null
            return map[key]
        }
    }

    /**
     * 移除指定对象的附加实例字段。
     *
     * 如果移除后该对象不再有任何附加字段，则同时清理该对象的映射条目。
     *
     * @param obj 目标对象实例。
     * @param key 字段键名。
     * @return 被移除的值，如果该键不存在则返回 `null`。
     */
    @JvmStatic
    fun removeAdditionalInstanceField(obj: Any, key: String): Any? {
        synchronized(additionalFields) {
            val map = additionalFields[obj] ?: return null
            val removed = map.remove(key)
            if (map.isEmpty()) {
                additionalFields.remove(obj)
            }
            return removed
        }
    }

    /**
     * 为指定类设置一个附加的静态字段。
     *
     * 实现上将 [Class] 对象作为键委托给 [setAdditionalInstanceField]。
     *
     * @param clazz 目标类。
     * @param key   字段键名。
     * @param value 要设置的值，可为 `null`。
     * @return 该键先前关联的值，如果之前未设置则返回 `null`。
     */
    @JvmStatic
    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? {
        return setAdditionalInstanceField(clazz, key, value)
    }

    /**
     * 获取指定类的附加静态字段值。
     *
     * 实现上将 [Class] 对象作为键委托给 [getAdditionalInstanceField]。
     *
     * @param clazz 目标类。
     * @param key   字段键名。
     * @return 该键关联的值，如果未设置则返回 `null`。
     */
    @JvmStatic
    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return getAdditionalInstanceField(clazz, key)
    }

    /**
     * 移除指定类的附加静态字段。
     *
     * 实现上将 [Class] 对象作为键委托给 [removeAdditionalInstanceField]。
     *
     * @param clazz 目标类。
     * @param key   字段键名。
     * @return 被移除的值，如果该键不存在则返回 `null`。
     */
    @JvmStatic
    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return removeAdditionalInstanceField(clazz, key)
    }
}