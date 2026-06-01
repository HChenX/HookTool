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

import com.hchen.hooktool.helper.CoreHelper.findClass
import com.hchen.hooktool.helper.CoreHelper.findConstructorBestMatch
import com.hchen.hooktool.helper.CoreHelper.findConstructorExact
import com.hchen.hooktool.helper.CoreHelper.findConstructorExactWithClasses
import com.hchen.hooktool.helper.CoreHelper.findField
import com.hchen.hooktool.helper.CoreHelper.findMethodBestMatch
import com.hchen.hooktool.helper.CoreHelper.findMethodExact
import com.hchen.hooktool.helper.CoreHelper.findMethodExactWithClasses
import com.hchen.hooktool.helper.CoreHelper.getAdditionalInstanceField
import com.hchen.hooktool.helper.CoreHelper.getObjectTransformationCost
import com.hchen.hooktool.helper.CoreHelper.getPrimitivePromotionCost
import com.hchen.hooktool.helper.CoreHelper.removeAdditionalInstanceField
import com.hchen.hooktool.helper.CoreHelper.setAdditionalInstanceField
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

/**
 * HookTool 框架的底层反射工具核心实现。
 *
 * 本类提供了替代原生 Xposed 框架 `XposedHelpers` 的全套反射操作能力，涵盖：
 * - 查找类（自动处理内部类的 `$` 分隔符解析）
 * - 读写字段（实例字段、静态字段、接口常量）
 * - 解析方法与构造函数（支持精确匹配和基于继承距离的最佳匹配）
 * - 反射调用实例方法与静态方法
 * - 为任意对象 / 类动态附加额外的键值对字段
 *
 * 内部使用 [ConcurrentHashMap] 对已解析的 [Field]、[Method]、[Constructor] 进行缓存，
 * 避免重复的反射查找开销。类型兼容性判断严格遵循 Apache Commons Lang 的规范，
 * 并完整支持 Java 8+ 接口 default 方法的深度查找。
 *
 * 本类为 `internal` 可见性，仅供 `tool` 模块内部使用，不属于对外公开的 API。
 *
 * @author 焕晨HChen
 */
object CoreHelper {
    /** 字段缓存，以 [Class] 为键，字段名到 [Optional] 包装的 [Field] 的并发映射为值。 */
    private val fieldCache = WeakHashMap<Class<*>, ConcurrentHashMap<String, Optional<Field>>>()

    /** 方法缓存，以 [Class] 为键，方法签名到 [Optional] 包装的 [Method] 的并发映射为值。 */
    private val methodCache = WeakHashMap<Class<*>, ConcurrentHashMap<String, Optional<Method>>>()

    /** 构造函数缓存，以 [Class] 为键，构造函数签名到 [Optional] 包装的 [Constructor] 的并发映射为值。 */
    private val constructorCache = WeakHashMap<Class<*>, ConcurrentHashMap<String, Optional<Constructor<*>>>>()

    /**
     * 获取指定类对应的字段缓存映射。
     *
     * 如果缓存中不存在该类的映射，则创建一个新的 [ConcurrentHashMap] 并存入缓存。
     * 通过 [synchronized] 保证并发安全。
     *
     * @param clazz 目标类。
     * @return 该类对应的字段缓存映射。
     */
    private fun getFieldCacheMap(clazz: Class<*>): ConcurrentHashMap<String, Optional<Field>> {
        synchronized(fieldCache) {
            return fieldCache.getOrPut(clazz) { ConcurrentHashMap() }
        }
    }

    /**
     * 获取指定类对应的方法缓存映射。
     *
     * 如果缓存中不存在该类的映射，则创建一个新的 [ConcurrentHashMap] 并存入缓存。
     * 通过 [synchronized] 保证并发安全。
     *
     * @param clazz 目标类。
     * @return 该类对应的方法缓存映射。
     */
    private fun getMethodCacheMap(clazz: Class<*>): ConcurrentHashMap<String, Optional<Method>> {
        synchronized(methodCache) {
            return methodCache.getOrPut(clazz) { ConcurrentHashMap() }
        }
    }

    /**
     * 获取指定类对应的构造函数缓存映射。
     *
     * 如果缓存中不存在该类的映射，则创建一个新的 [ConcurrentHashMap] 并存入缓存。
     * 通过 [synchronized] 保证并发安全。
     *
     * @param clazz 目标类。
     * @return 该类对应的构造函数缓存映射。
     */
    private fun getConstructorCacheMap(clazz: Class<*>): ConcurrentHashMap<String, Optional<Constructor<*>>> {
        synchronized(constructorCache) {
            return constructorCache.getOrPut(clazz) { ConcurrentHashMap() }
        }
    }

    /**
     * 轻量级可选值包装器，用于在缓存中区分"从未查找"与"查找失败"两种状态。
     *
     * 与 `java.util.Optional` 不同，本实现允许将 `null` 作为有效值以外的"空"状态存储，
     * 以适配 [ConcurrentHashMap.computeIfAbsent] 的缓存语义。
     *
     * @param T 被包装值的类型。
     * @property value 被包装的值，可为 `null`。
     */
    private class Optional<T>(val value: T?) {
        /**
         * 判断当前包装器中是否存在有效值。
         *
         * @return 如果 [value] 不为 `null` 则返回 `true`。
         */
        fun isPresent(): Boolean = value != null

        /**
         * 获取有效值；若不存在，则抛出由 [lazyError] 提供的异常。
         *
         * @param lazyError 生成异常的延迟工厂函数。
         * @return 包装的有效值。
         * @throws Throwable 当 [value] 为 `null` 时，抛出由 [lazyError] 创建的异常。
         */
        fun orElseThrow(lazyError: () -> Throwable): T {
            if (value == null) throw lazyError()
            return value
        }

        companion object {
            /** 全局共享的空 [Optional] 单例，避免重复创建空实例。 */
            private val EMPTY = Optional<Any>(null)

            /**
             * 创建一个包含指定值的 [Optional] 实例。
             *
             * @param v 需要包装的值。
             * @return 包含 [v] 的 [Optional] 实例。
             */
            fun <T> of(v: T): Optional<T> = Optional(v)

            /**
             * 返回全局共享的空 [Optional] 实例。
             *
             * @return 空的 [Optional] 实例。
             */
            @Suppress("UNCHECKED_CAST")
            fun <T> empty(): Optional<T> = EMPTY as Optional<T>
        }
    }

    /**
     * 获取一个保证可用的类加载器。
     *
     * 按以下优先级依次尝试：传入的 [classLoader] → 当前线程上下文类加载器 → 系统类加载器。
     *
     * @param classLoader 优先使用的类加载器，可为 `null`。
     * @return 一个非 `null` 的可用类加载器实例。
     */
    private fun getSafeClassLoader(classLoader: ClassLoader?): ClassLoader {
        return classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
    }

    /**
     * 将混合类型参数数组中的每个元素解析为对应的 [Class] 对象。
     *
     * 数组元素可以是 [Class] 实例或类名 [String]（后者通过 [findClass] 加载）。
     *
     * @param classLoader    用于加载字符串类名的类加载器。
     * @param parameterTypes 参数类型数组，元素为 [Class] 或 [String]。
     * @return 解析完成的 [Class] 数组。
     * @throws IllegalArgumentException 如果数组中包含 `null` 元素，或元素既非 [Class] 也非 [String]。
     * @throws NoClassDefFoundError     如果某个字符串类名无法被解析为有效的类。
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
     * 递归地在类的继承链及接口树中查找指定名称的字段。
     *
     * 搜索顺序为：当前类直接声明的字段 → 当前类实现的接口树中的常量字段 → 父类（逐级向上）。
     * 一旦找到即返回，全部遍历完毕仍未找到则抛出异常。
     *
     * @param clazz     开始搜索的类。
     * @param fieldName 需要查找的字段名称。
     * @return 匹配的 [Field] 对象（尚未设置可访问性）。
     * @throws NoSuchFieldException 在整个继承链和接口树中均未找到该字段时抛出。
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
     * 在接口及其父接口中递归查找指定名称的字段（即接口常量）。
     *
     * @param iface     需要搜索的接口。
     * @param fieldName 需要查找的字段名称。
     * @return 匹配的 [Field] 对象；未找到时返回 `null`。
     */
    private fun findInterfaceFieldRecursive(iface: Class<*>, fieldName: String): Field? {
        try {
            val f = iface.getDeclaredField(fieldName)
            if (Modifier.isStatic(f.modifiers)) return f
        } catch (_: NoSuchFieldException) {
        }
        for (superIface in iface.interfaces) {
            val f = findInterfaceFieldRecursive(superIface, fieldName)
            if (f != null) return f
        }
        return null
    }

    /**
     * 判断源参数类型数组 [from] 是否可逐一赋值给目标参数类型数组 [to]。
     *
     * 两个数组长度必须一致。对于 [from] 中为 `null` 的元素，仅当 [to] 对应位置为非基本类型时才视为兼容。
     *
     * @param from 源参数类型数组（元素可为 `null`）。
     * @param to   目标参数类型数组。
     * @return 所有位置均可赋值时返回 `true`。
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
     * 判断源类型 [from] 是否可以赋值给目标类型 [to]。
     *
     * 综合考虑以下情形：
     * - 直接的 `isAssignableFrom` 继承 / 实现关系
     * - 基本类型之间的宽化转换（如 `int` → `long`）
     * - 基本类型与对应包装类型之间的装箱 / 拆箱（如 `int` ↔ `Integer`）
     *
     * @param from 源类型。
     * @param to   目标类型。
     * @return [from] 可赋值给 [to] 时返回 `true`。
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
     * 判断基本类型 [from] 是否可以通过 Java 规范定义的宽化转换赋值给 [to]。
     *
     * 例如 `int` 可宽化为 `long`、`float`、`double`，但不可宽化为 `short`。
     *
     * @param from 源基本类型。
     * @param to   目标基本类型。
     * @return 若可宽化则返回 `true`。
     */
    private fun isPrimitiveAssignable(from: Class<*>, to: Class<*>): Boolean {
        if (from == to) return true
        return getPrimitiveWideningSteps(from.name, to.name) != -1
    }

    /**
     * 获取给定基本类型对应的包装类型。
     *
     * 例如：`int` → `Integer`，`boolean` → `Boolean`。
     * 如果传入的类型本身不是基本类型，则原样返回。
     *
     * @param type 基本类型或其他类型。
     * @return 对应的包装类型；如果 [type] 非基本类型则返回 [type] 自身。
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
     * 获取给定包装类型对应的基本类型。
     *
     * 例如：`Integer` → `int`，`Boolean` → `boolean`。
     * 如果传入的类型不是包装类型，则返回 `null`。
     *
     * @param type 包装类型或其他类型。
     * @return 对应的基本类型；如果无法转换则返回 `null`。
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
     * 计算从基本类型 [srcName] 到 [destName] 所需的宽化转换步数。
     *
     * 遵循 Java 语言规范定义的拓宽基本类型转换链：
     * - `byte` → `short` → `int` → `long` → `float` → `double`
     * - `char` → `int` → `long` → `float` → `double`
     *
     * @param srcName  源基本类型名称（如 `"int"`）。
     * @param destName 目标基本类型名称（如 `"long"`）。
     * @return 所需的宽化步数；无法宽化时返回 `-1`。
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
     * 计算源类型到目标基本类型之间的类型提升代价。
     *
     * 如果 [srcType] 为包装类型，先进行拆箱（代价 0.1），再累加宽化步数的代价（每步 0.1）。
     * `void` 和 `boolean` 类型之间的转换代价为 [Float.MAX_VALUE]，表示不可转换。
     *
     * @param srcType  源类型（包装类型或基本类型均可）。
     * @param destType 目标基本类型。
     * @return 转换代价，数值越小表示兼容性越好；不可转换时返回 [Float.MAX_VALUE]。
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
     * 处理逻辑按优先级排列：
     * - 目标类型为基本类型时，委托给 [getPrimitivePromotionCost]；若源类型为 `null` 则返回 [Float.MAX_VALUE]
     * - 源类型为 `null`（非基本类型目标）时代价固定为 1.5
     * - 源类型与目标类型完全相同时代价为 0
     * - 双方均为数组类型时，递归计算组件类型的转换代价并加收 0.1 的数组维度附加代价
     * - 否则沿继承链向上遍历，若目标为接口则委托 [getInterfaceDistance] 计算精确的接口距离代价，
     *   每向上一级父类的代价为 1.0
     *
     * @param srcType  源类型，为 `null` 表示运行时值为 `null`。
     * @param destType 目标类型。
     * @return 转换代价，数值越小表示兼容性越好；不可转换时返回 [Float.MAX_VALUE]。
     */
    private fun getObjectTransformationCost(srcType: Class<*>?, destType: Class<*>): Float {
        if (destType.isPrimitive) {
            if (srcType == null) return Float.MAX_VALUE
            return getPrimitivePromotionCost(srcType, destType)
        }
        if (srcType == null) return 1.5f
        if (srcType == destType) return 0.0f

        if (srcType.isArray && destType.isArray) {
            return getObjectTransformationCost(srcType.componentType, destType.componentType!!) + 0.1f
        }

        var cost = 0f
        var src: Class<*>? = srcType
        while (src != null && src != destType) {
            if (destType.isInterface && destType.isAssignableFrom(src)) {
                cost += getInterfaceDistance(src, destType)
                break
            }
            cost++
            src = src.superclass
        }
        if (src == null) cost += 1.5f
        return cost
    }

    /**
     * 递归计算源类型到目标接口之间的继承距离代价。
     *
     * 沿源类型的接口和父类递归搜索，记录到达目标接口的最短路径：
     * - 每经过一个直接接口的代价为 0.25
     * - 每经过一个父类的代价为 1.0
     *
     * @param src  源类型。
     * @param dest 目标接口类型。
     * @return 到达目标接口的最小转换代价；无法到达时返回 [Float.MAX_VALUE]。
     */
    private fun getInterfaceDistance(src: Class<*>, dest: Class<*>): Float {
        if (src == dest) return 0f
        var minCost = Float.MAX_VALUE
        for (iface in src.interfaces) {
            if (dest.isAssignableFrom(iface)) {
                val cost = 0.25f + getInterfaceDistance(iface, dest)
                if (cost < minCost) minCost = cost
            }
        }
        val superclass = src.superclass
        if (superclass != null && dest.isAssignableFrom(superclass)) {
            val cost = 1.0f + getInterfaceDistance(superclass, dest)
            if (cost < minCost) minCost = cost
        }
        return minCost
    }

    /**
     * 计算参数列表的总类型转换代价。
     *
     * 逐一对比 [srcArgs] 和 [destArgs] 中每个位置的类型，累加 [getObjectTransformationCost] 的结果。
     *
     * @param srcArgs  源参数类型数组（元素可为 `null`）。
     * @param destArgs 目标参数类型数组。
     * @return 所有参数的总转换代价。
     */
    private fun getTotalTransformationCost(srcArgs: Array<Class<*>?>, destArgs: Array<Class<*>>): Float {
        var totalCost = 0f
        for (i in srcArgs.indices) {
            totalCost += getObjectTransformationCost(srcArgs[i], destArgs[i])
        }
        return totalCost
    }

    /**
     * 在类的继承链及接口树中收集与指定名称和参数类型兼容的所有方法。
     *
     * 搜索分两轮进行：
     * 1. 沿继承链向上遍历（当前类 → 父类），收集直接声明的方法；bridge 方法和 synthetic 方法会被过滤，
     *    仅第一轮（当前类）包含 `private` 方法。
     * 2. 从当前类出发沿继承链向上遍历接口树（深度优先递归），收集接口中声明的 default 方法，
     *    排除静态、私有、bridge 和 synthetic 方法。
     *
     * 通过 [seen] 集合对方法签名进行去重，避免同一方法被重复收集。
     *
     * @param clazz          开始搜索的类。
     * @param name           需要查找的方法名称。
     * @param parameterTypes 期望的参数类型数组（元素可为 `null`）。
     * @param result         用于收集匹配方法的可变列表。
     */
    private fun collectMethodsBestMatch(clazz: Class<*>, name: String, parameterTypes: Array<Class<*>?>, result: MutableList<Method>) {
        val seen = HashSet<String>()

        var clz: Class<*>? = clazz
        var considerPrivate = true
        while (clz != null) {
            for (method in clz.declaredMethods) {
                if (method.isBridge || method.isSynthetic) continue
                if (!considerPrivate && Modifier.isPrivate(method.modifiers)) continue
                if (method.name == name && isAssignable(parameterTypes, method.parameterTypes)) {
                    val sig = method.name + method.parameterTypes.contentToString()
                    if (seen.add(sig)) {
                        result.add(method)
                    }
                }
            }
            considerPrivate = false
            clz = clz.superclass
        }

        clz = clazz
        while (clz != null) {
            for (iface in clz.interfaces) {
                collectInterfaceMethodsRecursive(iface, name, parameterTypes, seen, result)
            }
            clz = clz.superclass
        }
    }

    /**
     * 在接口及其父接口中递归收集与指定名称和参数类型兼容的方法。
     *
     * 用于支持 Java 8+ 接口 default 方法的深度查找。接口中的静态方法、私有方法、
     * bridge 方法和 synthetic 方法会被跳过，因为它们不可被实现类继承或为编译器生成的辅助方法。
     * 通过 [seen] 集合对方法签名进行去重。
     *
     * @param iface          需要搜索的接口。
     * @param name           需要查找的方法名称。
     * @param parameterTypes 期望的参数类型数组（元素可为 `null`）。
     * @param seen           已收集过的方法签名集合，用于去重。
     * @param result         用于收集匹配方法的可变列表。
     */
    private fun collectInterfaceMethodsRecursive(iface: Class<*>, name: String, parameterTypes: Array<Class<*>?>, seen: HashSet<String>, result: MutableList<Method>) {
        for (method in iface.declaredMethods) {
            if (Modifier.isStatic(method.modifiers) || Modifier.isPrivate(method.modifiers) || method.isBridge || method.isSynthetic) continue
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
     * 比较候选方法 / 构造函数相对于当前最佳匹配的参数匹配优劣。
     *
     * 使用与 Apache Commons Lang `MemberUtils.compareMethodFit` 一致的继承距离算法。
     *
     * @param candidateParams 候选者的参数类型数组。
     * @param currentParams   当前最佳匹配的参数类型数组。
     * @param parameterTypes  期望的参数类型数组（元素可为 `null`）。
     * @return 负值表示候选者更优，正值表示当前匹配更优，零表示两者同等。
     */
    private fun compareFit(candidateParams: Array<Class<*>>, currentParams: Array<Class<*>>, parameterTypes: Array<Class<*>?>): Int {
        val candidateCost = getTotalTransformationCost(parameterTypes, candidateParams)
        val currentCost = getTotalTransformationCost(parameterTypes, currentParams)
        return candidateCost.compareTo(currentCost)
    }

    // ==================== Class ====================

    /**
     * 将 Java 源码风格的类名转换为 JVM 内部规范描述符。
     *
     * 处理数组类型的转换，例如：`"int[]"` → `"[I"`，`"java.lang.String[]"` → `"[Ljava.lang.String;"`。
     * 非数组类型则去除首尾空白后原样返回。
     *
     * @param className 需要转换的类名。
     * @return JVM 内部规范描述符格式的类名。
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

    /** 基本类型名称到对应 [Class] 对象的静态映射表。 */
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
     * 根据类名查找对应的 [Class] 对象。
     *
     * 支持以下格式：
     * - 基本类型名称（如 `"int"`、`"void"`）
     * - 数组类型（如 `"int[]"`、`"java.lang.String[]"`）
     * - 普通类名及内部类（如果直接查找失败，会自动尝试将 `.` 替换为 `$` 以解析内部类）
     *
     * @param className   需要查找的类名。
     * @param classLoader 用于加载类的类加载器，为 `null` 时使用安全类加载器。
     * @return 对应的 [Class] 对象。
     * @throws NoClassDefFoundError 无法找到指定类时抛出。
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
     * 根据类名查找对应的 [Class] 对象，查找失败时返回 `null`。
     *
     * 是 [findClass] 的安全版本，不会抛出异常。
     *
     * @param className   需要查找的类名。
     * @param classLoader 用于加载类的类加载器，可为 `null`。
     * @return 对应的 [Class] 对象；如果类不存在则返回 `null`。
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
     * 在指定类中查找具有给定名称的字段，支持沿继承链及接口树向上递归查找。
     *
     * 查找结果会被缓存。找到的字段会自动设置为可访问。
     *
     * @param clazz     需要查找字段的类。
     * @param fieldName 字段的名称。
     * @return 已设置可访问标志的 [Field] 对象。
     * @throws NoSuchFieldError 在类、父类及接口树中均未找到该字段时抛出。
     */
    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        return getFieldCacheMap(clazz).computeIfAbsent(fieldName) { k ->
            try {
                val field = findFieldRecursiveImpl(clazz, k)
                field.isAccessible = true
                Optional.of(field)
            } catch (_: NoSuchFieldException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchFieldError("${clazz.name}#$fieldName") }
    }

    /**
     * 在指定类中查找具有给定名称的字段，查找失败时返回 `null`。
     *
     * 是 [findField] 的安全版本，不会抛出异常。
     *
     * @param clazz     需要查找字段的类。
     * @param fieldName 字段的名称。
     * @return 已设置可访问标志的 [Field] 对象；如果字段不存在则返回 `null`。
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
     * 参数类型可以是 [Class] 对象或类名 [String]（通过 [findClass] 解析）。
     * 查找结果会被缓存。
     *
     * @param clazz          需要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Method] 对象。
     * @throws NoClassDefFoundError 某个字符串参数类型无法解析为有效类时抛出。
     * @throws NoSuchMethodError    类中未找到精确匹配的方法时抛出。
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
     * @param className      需要查找方法的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Method] 对象。
     * @throws NoClassDefFoundError 类名或某个字符串参数类型无法解析为有效类时抛出。
     * @throws NoSuchMethodError    类中未找到精确匹配的方法时抛出。
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
     * 支持从实体类及其实现的接口树中查找继承的 default 方法。
     * 查找结果会被缓存。
     *
     * @param clazz          需要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 精确匹配的参数类型数组。
     * @return 已设置可访问标志的 [Method] 对象。
     * @throws NoSuchMethodError 类中未找到精确匹配的方法时抛出。
     */
    @JvmStatic
    fun findMethodExactWithClasses(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val sig = "$methodName#${parameterTypes.joinToString { it.name }}#exact"
        return getMethodCacheMap(clazz).computeIfAbsent(sig) {
            try {
                val method = findMethodExactRecursive(clazz, methodName, arrayOf(*parameterTypes))
                method.isAccessible = true
                Optional.of(method)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}#$methodName(${parameterTypes.joinToString { it.name }})") }
    }

    /**
     * 递归查找精确匹配的方法，支持从实体类及接口树中查找继承的 default 方法。
     *
     * 搜索分两轮进行：
     * 1. 沿继承链向上遍历（当前类 → 父类），在直接声明的方法中查找。
     * 2. 从当前类出发沿继承链向上遍历接口树（深度优先），在接口声明的方法中查找，
     *    排除静态和私有方法。
     *
     * @param clazz          开始搜索的类。
     * @param name           需要查找的方法名称。
     * @param parameterTypes 精确匹配的参数类型数组。
     * @return 匹配的 [Method] 对象。
     * @throws NoSuchMethodException 在类及所有接口中均未找到该方法时抛出。
     */
    private fun findMethodExactRecursive(clazz: Class<*>, name: String, parameterTypes: Array<Class<*>>): Method {
        var clz: Class<*>? = clazz
        while (clz != null) {
            try {
                return clz.getDeclaredMethod(name, *parameterTypes)
            } catch (_: NoSuchMethodException) {
            }
            clz = clz.superclass
        }

        clz = clazz
        while (clz != null) {
            for (iface in clz.interfaces) {
                val m = findInterfaceMethodExactRecursive(iface, name, parameterTypes)
                if (m != null) return m
            }
            clz = clz.superclass
        }
        throw NoSuchMethodException()
    }

    /**
     * 在接口及其父接口中递归查找精确匹配的方法。
     *
     * 用于支持 Java 8+ 接口 default 方法的查找。接口中的静态方法和私有方法会被排除，
     * 因为静态方法不可被实现类继承，私有方法对外不可见。
     *
     * @param iface          需要搜索的接口。
     * @param name           需要查找的方法名称。
     * @param parameterTypes 精确匹配的参数类型数组。
     * @return 匹配的 [Method] 对象；未找到时返回 `null`。
     */
    private fun findInterfaceMethodExactRecursive(iface: Class<*>, name: String, parameterTypes: Array<Class<*>>): Method? {
        try {
            val m = iface.getDeclaredMethod(name, *parameterTypes)
            if (!Modifier.isStatic(m.modifiers) && !Modifier.isPrivate(m.modifiers)) return m
        } catch (_: NoSuchMethodException) {
        }
        for (superIface in iface.interfaces) {
            val m = findInterfaceMethodExactRecursive(superIface, name, parameterTypes)
            if (m != null) return m
        }
        return null
    }

    /**
     * 在指定类中查找精确匹配的方法，查找失败时返回 `null`。
     *
     * 是 [findMethodExact] 的安全版本，不会抛出异常。
     *
     * @param clazz          需要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Method] 对象；如果方法不存在则返回 `null`。
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
     * 在指定类名对应的类中查找精确匹配的方法，查找失败时返回 `null`。
     *
     * 是 [findMethodExact]（接受类名版本）的安全版本，不会抛出异常。
     *
     * @param className      需要查找方法的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Method] 对象；如果方法不存在则返回 `null`。
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
     * 当所有参数类型均非 `null` 时，优先尝试精确匹配（[findMethodExactWithClasses]）。
     * 精确匹配失败则回退到最佳匹配模式：收集所有兼容的候选方法，
     * 通过基于继承距离的评分算法选出最优解。
     * 查找结果会被缓存。
     *
     * @param clazz          需要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示该位置通配）。
     * @return 已设置可访问标志的最佳匹配 [Method] 对象。
     * @throws NoSuchMethodError 类中未找到任何兼容方法时抛出。
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

        val sig = "$methodName#${parameterTypes.joinToString { it?.name ?: "null" }}#best"
        return getMethodCacheMap(clazz).computeIfAbsent(sig) {
            val candidates = mutableListOf<Method>()
            collectMethodsBestMatch(clazz, methodName, arrayOf(*parameterTypes), candidates)
            var bestMatch: Method? = null
            for (method in candidates) {
                if (bestMatch == null || compareFit(method.parameterTypes, bestMatch.parameterTypes, arrayOf(*parameterTypes)) < 0) {
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
     * 根据实际参数值在指定类中查找最匹配的方法。
     *
     * 通过运行时参数的实际类型（`javaClass`）推断参数类型，
     * 然后委托给 [findMethodBestMatch]（接受类型数组版本）。
     *
     * @param clazz      需要查找方法的类。
     * @param methodName 方法名称。
     * @param args       实际参数值（可为 `null`）。
     * @return 已设置可访问标志的最佳匹配 [Method] 对象。
     * @throws NoSuchMethodError 类中未找到任何兼容方法时抛出。
     */
    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findMethodBestMatch(clazz, methodName, *paramTypes)
    }

    /**
     * 根据指定的参数类型和实际参数值在指定类中查找最匹配的方法。
     *
     * [parameterTypes] 中为 `null` 的位置会使用 [args] 中对应元素的实际类型进行填充，
     * 然后委托给 [findMethodBestMatch]（接受类型数组版本）。
     *
     * @param clazz          需要查找方法的类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可为 `null`）。
     * @param args           实际参数值数组。
     * @return 已设置可访问标志的最佳匹配 [Method] 对象。
     * @throws NoSuchMethodError 类中未找到任何兼容方法时抛出。
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
     * 参数类型可以是 [Class] 对象或类名 [String]（通过 [findClass] 解析）。
     * 查找结果会被缓存。
     *
     * @param clazz          需要查找构造函数的类。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Constructor] 对象。
     * @throws NoClassDefFoundError 某个字符串参数类型无法解析为有效类时抛出。
     * @throws NoSuchMethodError    类中未找到精确匹配的构造函数时抛出。
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
     * @param className      需要查找构造函数的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Constructor] 对象。
     * @throws NoClassDefFoundError 类名或某个字符串参数类型无法解析为有效类时抛出。
     * @throws NoSuchMethodError    类中未找到精确匹配的构造函数时抛出。
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
     * 查找结果会被缓存。找到的构造函数会自动设置为可访问。
     *
     * @param clazz          需要查找构造函数的类。
     * @param parameterTypes 精确匹配的参数类型数组。
     * @return 已设置可访问标志的 [Constructor] 对象。
     * @throws NoSuchMethodError 类中未找到精确匹配的构造函数时抛出。
     */
    @JvmStatic
    fun findConstructorExactWithClasses(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        val sig = "${parameterTypes.joinToString { it.name }}#exact"
        return getConstructorCacheMap(clazz).computeIfAbsent(sig) {
            try {
                val constructor = clazz.getDeclaredConstructor(*parameterTypes)
                constructor.isAccessible = true
                Optional.of(constructor)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}<init>(${parameterTypes.joinToString { it.name }})") }
    }

    /**
     * 在指定类中查找精确匹配的构造函数，查找失败时返回 `null`。
     *
     * 是 [findConstructorExact] 的安全版本，不会抛出异常。
     *
     * @param clazz          需要查找构造函数的类。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Constructor] 对象；如果构造函数不存在则返回 `null`。
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
     * 在指定类名对应的类中查找精确匹配的构造函数，查找失败时返回 `null`。
     *
     * 是 [findConstructorExact]（接受类名版本）的安全版本，不会抛出异常。
     *
     * @param className      需要查找构造函数的类名。
     * @param classLoader    用于加载类的类加载器，可为 `null`。
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）。
     * @return 已设置可访问标志的 [Constructor] 对象；如果构造函数不存在则返回 `null`。
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
     * 当所有参数类型均非 `null` 时，优先尝试精确匹配（[findConstructorExactWithClasses]）。
     * 精确匹配失败则回退到最佳匹配模式：遍历所有已声明的构造函数，
     * 通过基于继承距离的评分算法选出最优解。
     * 查找结果会被缓存。
     *
     * @param clazz          需要查找构造函数的类。
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示该位置通配）。
     * @return 已设置可访问标志的最佳匹配 [Constructor] 对象。
     * @throws NoSuchMethodError 类中未找到任何兼容构造函数时抛出。
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

        val sig = "${parameterTypes.joinToString { it?.name ?: "null" }}#best"
        return getConstructorCacheMap(clazz).computeIfAbsent(sig) {
            var bestMatch: Constructor<*>? = null
            for (constructor in clazz.declaredConstructors) {
                if (isAssignable(arrayOf(*parameterTypes), constructor.parameterTypes)) {
                    if (bestMatch == null || compareFit(constructor.parameterTypes, bestMatch.parameterTypes, arrayOf(*parameterTypes)) < 0) {
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
     * 根据实际参数值在指定类中查找最匹配的构造函数。
     *
     * 通过运行时参数的实际类型（`javaClass`）推断参数类型，
     * 然后委托给 [findConstructorBestMatch]（接受类型数组版本）。
     *
     * @param clazz 需要查找构造函数的类。
     * @param args  实际参数值（可为 `null`）。
     * @return 已设置可访问标志的最佳匹配 [Constructor] 对象。
     * @throws NoSuchMethodError 类中未找到任何兼容构造函数时抛出。
     */
    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg args: Any?): Constructor<*> {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findConstructorBestMatch(clazz, *paramTypes)
    }

    /**
     * 根据指定的参数类型和实际参数值在指定类中查找最匹配的构造函数。
     *
     * [parameterTypes] 中为 `null` 的位置会使用 [args] 中对应元素的实际类型进行填充，
     * 然后委托给 [findConstructorBestMatch]（接受类型数组版本）。
     *
     * @param clazz          需要查找构造函数的类。
     * @param parameterTypes 参数类型数组（元素可为 `null`）。
     * @param args           实际参数值数组。
     * @return 已设置可访问标志的最佳匹配 [Constructor] 对象。
     * @throws NoSuchMethodError 类中未找到任何兼容构造函数时抛出。
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
     * 通过 [findMethodBestMatch] 根据实际参数值自动查找最佳匹配的方法并反射调用。
     *
     * @param obj        目标对象实例。
     * @param methodName 方法名称。
     * @param args       方法参数值（可为 `null`）。
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  未找到兼容方法时抛出。
     * @throws IllegalAccessError 方法无法访问时抛出。
     * @throws Throwable          方法执行过程中抛出的原始异常。
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
     * 参数类型数组中可传入 `null` 作为通配符，通过最佳匹配查找并反射调用。
     *
     * @param obj            目标对象实例。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）。
     * @param args           方法参数值（可为 `null`）。
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  未找到兼容方法时抛出。
     * @throws IllegalAccessError 方法无法访问时抛出。
     * @throws Throwable          方法执行过程中抛出的原始异常。
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
     * 通过 [findMethodBestMatch] 根据实际参数值自动查找最佳匹配的方法，
     * 并以 `null` 作为接收者进行反射调用。
     *
     * @param clazz      目标类。
     * @param methodName 方法名称。
     * @param args       方法参数值（可为 `null`）。
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  未找到兼容方法时抛出。
     * @throws IllegalAccessError 方法无法访问时抛出。
     * @throws Throwable          方法执行过程中抛出的原始异常。
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
     * 参数类型数组中可传入 `null` 作为通配符，通过最佳匹配查找，
     * 并以 `null` 作为接收者进行反射调用。
     *
     * @param clazz          目标类。
     * @param methodName     方法名称。
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）。
     * @param args           方法参数值（可为 `null`）。
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  未找到兼容方法时抛出。
     * @throws IllegalAccessError 方法无法访问时抛出。
     * @throws Throwable          方法执行过程中抛出的原始异常。
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
     * 读取指定对象的实例字段值。
     *
     * 通过 [findField] 查找字段并反射读取其值。
     *
     * @param obj       目标对象实例。
     * @param fieldName 字段名称。
     * @return 字段的值，可能为 `null`。
     * @throws NoSuchFieldError   未找到该字段时抛出。
     * @throws IllegalAccessError 字段无法访问时抛出。
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
     * 通过 [findField] 查找字段并反射设置其值。
     *
     * @param obj       目标对象实例。
     * @param fieldName 字段名称。
     * @param value     需要设置的值，可为 `null`。
     * @throws NoSuchFieldError   未找到该字段时抛出。
     * @throws IllegalAccessError 字段无法访问时抛出。
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
     * 读取指定类的静态字段值（支持接口静态常量）。
     *
     * 通过 [findField] 查找字段，校验其为静态字段后以 `null` 作为接收者反射读取值。
     * 如果字段存在但不是静态字段，将抛出 [IllegalArgumentException]。
     *
     * @param clazz     目标类或接口。
     * @param fieldName 字段名称。
     * @return 字段的值，可能为 `null`。
     * @throws NoSuchFieldError      未找到该字段时抛出。
     * @throws IllegalAccessError    字段无法访问时抛出。
     * @throws IllegalArgumentException 字段不是静态字段时抛出。
     */
    @JvmStatic
    fun getStaticObjectField(clazz: Class<*>, fieldName: String): Any? {
        return try {
            val field = findField(clazz, fieldName)
            if (!Modifier.isStatic(field.modifiers)) {
                throw IllegalArgumentException("Expected static field, but ${clazz.name}#$fieldName is an instance field.")
            }
            field.get(null)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 设置指定类的静态字段值。
     *
     * 通过 [findField] 查找字段，校验其为静态字段后以 `null` 作为接收者反射设置值。
     * 如果字段存在但不是静态字段，将抛出 [IllegalArgumentException]。
     *
     * @param clazz     目标类。
     * @param fieldName 字段名称。
     * @param value     需要设置的值，可为 `null`。
     * @throws NoSuchFieldError      未找到该字段时抛出。
     * @throws IllegalAccessError    字段无法访问时抛出。
     * @throws IllegalArgumentException 字段不是静态字段时抛出。
     */
    @JvmStatic
    fun setStaticObjectField(clazz: Class<*>, fieldName: String, value: Any?) {
        try {
            val field = findField(clazz, fieldName)
            if (!Modifier.isStatic(field.modifiers)) {
                throw IllegalArgumentException("Expected static field, but ${clazz.name}#$fieldName is an instance field.")
            }
            field.set(null, value)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        }
    }

    /**
     * 通过最佳匹配的构造函数创建指定类的新实例。
     *
     * 通过 [findConstructorBestMatch] 根据实际参数值自动查找最佳匹配的构造函数并反射创建实例。
     *
     * @param clazz 目标类。
     * @param args  构造函数参数值（可为 `null`）。
     * @return 创建的新实例。
     * @throws NoSuchMethodError    未找到兼容构造函数时抛出。
     * @throws IllegalAccessError   构造函数无法访问时抛出。
     * @throws InstantiationError   类为抽象类或接口、无法实例化时抛出。
     * @throws Throwable            构造函数执行过程中抛出的原始异常。
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
     * 参数类型数组中可传入 `null` 作为通配符。
     *
     * @param clazz          目标类。
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）。
     * @param args           构造函数参数值（可为 `null`）。
     * @return 创建的新实例。
     * @throws NoSuchMethodError    未找到兼容构造函数时抛出。
     * @throws IllegalAccessError   构造函数无法访问时抛出。
     * @throws InstantiationError   类为抽象类或接口、无法实例化时抛出。
     * @throws Throwable            构造函数执行过程中抛出的原始异常。
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
     * 附加实例字段的存储容器。
     *
     * 以 [WeakHashMap] 弱引用持有对象键，当对象被 GC 回收后，
     * 其对应的附加字段会自动清理，从而避免内存泄漏。
     * 所有访问均通过 [synchronized] 保证线程安全。
     */
    private val additionalFields = WeakHashMap<Any, HashMap<String, Any?>>()

    /**
     * 为指定对象设置一个附加的实例字段。
     *
     * 附加字段不依赖于类的字段定义，可为任意对象动态关联键值对数据。
     * 如果指定的键已存在，则更新其值并返回旧值。
     *
     * @param obj   目标对象实例。
     * @param key   字段键名。
     * @param value 需要设置的值，可为 `null`。
     * @return 该键先前关联的值；如果之前未设置则返回 `null`。
     */
    @JvmStatic
    fun setAdditionalInstanceField(obj: Any, key: String, value: Any?): Any? {
        synchronized(additionalFields) {
            val map = additionalFields.getOrPut(obj) { HashMap() }
            return map.put(key, value)
        }
    }

    /**
     * 读取指定对象的附加实例字段值。
     *
     * @param obj 目标对象实例。
     * @param key 字段键名。
     * @return 该键关联的值；如果未设置则返回 `null`。
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
     * 如果移除后该对象不再拥有任何附加字段，则同时清理该对象的映射条目。
     *
     * @param obj 目标对象实例。
     * @param key 字段键名。
     * @return 被移除的值；如果该键不存在则返回 `null`。
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
     * 内部以 [Class] 对象作为键，委托给 [setAdditionalInstanceField] 实现。
     *
     * @param clazz 目标类。
     * @param key   字段键名。
     * @param value 需要设置的值，可为 `null`。
     * @return 该键先前关联的值；如果之前未设置则返回 `null`。
     */
    @JvmStatic
    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? {
        return setAdditionalInstanceField(clazz, key, value)
    }

    /**
     * 读取指定类的附加静态字段值。
     *
     * 内部以 [Class] 对象作为键，委托给 [getAdditionalInstanceField] 实现。
     *
     * @param clazz 目标类。
     * @param key   字段键名。
     * @return 该键关联的值；如果未设置则返回 `null`。
     */
    @JvmStatic
    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return getAdditionalInstanceField(clazz, key)
    }

    /**
     * 移除指定类的附加静态字段。
     *
     * 内部以 [Class] 对象作为键，委托给 [removeAdditionalInstanceField] 实现。
     *
     * @param clazz 目标类。
     * @param key   字段键名。
     * @return 被移除的值；如果该键不存在则返回 `null`。
     */
    @JvmStatic
    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return removeAdditionalInstanceField(clazz, key)
    }
}
