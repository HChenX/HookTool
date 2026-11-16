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

import java.lang.ref.WeakReference
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
 * @author 焕晨HChen
 */
object CoreHelper {
    /** 字段缓存，以 [Class] 为键，字段名到 [Optional] 包装的 [Field] 的并发映射为值。 */
    private val sFieldCache = WeakHashMap<Class<*>, ConcurrentHashMap<String, Optional<Field>>>()

    /** 方法缓存，以 [Class] 为键，方法签名到 [Optional] 包装的 [Method] 的并发映射为值。 */
    private val sMethodCache = WeakHashMap<Class<*>, ConcurrentHashMap<String, Optional<Method>>>()

    /** 构造函数缓存，以 [Class] 为键，构造函数签名到 [Optional] 包装的 [Constructor] 的并发映射为值。 */
    private val sConstructorCache = WeakHashMap<Class<*>, ConcurrentHashMap<String, Optional<Constructor<*>>>>()

    /** 类缓存键：按「类加载器身份哈希 + 类名」隔离不同加载器加载的同名类。 */
    private data class ClassCacheKey(val loaderId: Int, val className: String)

    /**
     * 类缓存：值为 [WeakReference] 包装的 [Class]。
     */
    private val sClassCache = ConcurrentHashMap<ClassCacheKey, WeakReference<Class<*>>>()

    /**
     * 获取指定类对应的字段缓存映射。
     *
     * @param clazz 目标类
     * @return 该类对应的字段缓存映射
     */
    private fun getFieldCacheMap(clazz: Class<*>): ConcurrentHashMap<String, Optional<Field>> {
        synchronized(sFieldCache) {
            return sFieldCache.getOrPut(clazz) { ConcurrentHashMap() }
        }
    }

    /**
     * 获取指定类对应的方法缓存映射。
     *
     * @param clazz 目标类
     * @return 该类对应的方法缓存映射
     */
    private fun getMethodCacheMap(clazz: Class<*>): ConcurrentHashMap<String, Optional<Method>> {
        synchronized(sMethodCache) {
            return sMethodCache.getOrPut(clazz) { ConcurrentHashMap() }
        }
    }

    /**
     * 获取指定类对应的构造函数缓存映射。
     *
     * @param clazz 目标类
     * @return 该类对应的构造函数缓存映射
     */
    private fun getConstructorCacheMap(clazz: Class<*>): ConcurrentHashMap<String, Optional<Constructor<*>>> {
        synchronized(sConstructorCache) {
            return sConstructorCache.getOrPut(clazz) { ConcurrentHashMap() }
        }
    }

    /**
     * 轻量级可选值包装器，用于在缓存中区分"从未查找"与"查找失败"两种状态。
     *
     * @param T 被包装值的类型
     */
    private class Optional<T>(value: T?) {
        private val ref: WeakReference<T>? = value?.let { WeakReference(it) }

        /**
         * 获取有效值；若不存在，则抛出由 [lazyError] 提供的异常。
         *
         * @param lazyError 生成异常的延迟工厂函数
         * @return 包装的有效值
         * @throws Throwable 当弱引用被清除时抛出由 [lazyError] 创建的异常
         */
        fun orElseThrow(lazyError: () -> Throwable): T {
            val value = ref?.get() ?: throw lazyError()
            return value
        }

        /**
         * 获取有效值；若弱引用已被清除则返回 `null`。
         *
         * @return 包装的有效值；弱引用被 GC 清除时返回 `null`
         */
        fun getOrNull(): T? = ref?.get()

        /**
         * 判断本实例是否为"查找失败"的负结果（从未持有有效值）。
         *
         * @return 本实例为负结果时返回 `true`
         */
        fun isEmpty(): Boolean = ref == null

        companion object {
            /** 全局共享的空 [Optional] 单例，避免重复创建空实例。 */
            private val EMPTY = Optional<Any>(null)

            /**
             * 创建一个包含指定值的 [Optional] 实例。
             *
             * @param v 需要包装的值
             * @return 包含 [v] 的 [Optional] 实例
             */
            fun <T> of(v: T): Optional<T> = Optional(v)

            /**
             * 返回全局共享的空 [Optional] 实例。
             *
             * @return 空的 [Optional] 实例
             */
            @Suppress("UNCHECKED_CAST")
            fun <T> empty(): Optional<T> = EMPTY as Optional<T>
        }
    }

    /**
     * 缓存查找并处理弱引用自愈：返回 [key] 对应的缓存值，若弱引用已被 GC 清除则重新计算并原子更新缓存。
     *
     * @param map       目标缓存映射
     * @param key       缓存键
     * @param recompute 重新计算的工厂函数，参数为缓存键
     * @param error     缓存条目为空（查找失败）时生成异常的工厂函数
     * @return 解析出的有效成员；查找失败时抛出 [error] 生成的异常
     */
    private fun <T> cachedOrRecompute(
        map: ConcurrentHashMap<String, Optional<T>>,
        key: String,
        recompute: (String) -> Optional<T>,
        error: () -> Throwable,
    ): T {
        while (true) {
            val cached = map[key]
            if (cached != null) {
                if (cached.isEmpty()) throw error()
                val value = cached.getOrNull()
                if (value != null) return value
                val fresh = recompute(key)
                map.compute(key) { _, _ -> fresh }
                val freshValue = fresh.getOrNull() ?: throw error()
                return freshValue
            }
            val computed = map.computeIfAbsent(key, recompute)
            val value = computed.getOrNull()
            if (value != null) return value
            // computeIfAbsent 返回的条目弱引用可能恰好被清除，此处直接抛错兜底。
            val freshValue = computed.orElseThrow(error)
            return freshValue
        }
    }

    /**
     * 生成方法/构造函数缓存签名：为每个参数类型附加其 ClassLoader 身份（[System.identityHashCode]），
     * 避免不同 ClassLoader 加载的同名类型在缓存中碰撞；以逗号分隔防止边界粘连。
     *
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示通配）
     * @return 用于缓存键的类型签名片段
     */
    private fun typeSigKey(parameterTypes: Array<out Class<*>?>): String {
        return parameterTypes.joinToString(",") {
            it?.let { c -> "${c.name}@${System.identityHashCode(c.classLoader)}" } ?: "null"
        }
    }

    /**
     * 获取一个保证可用的类加载器。
     *
     * 按以下优先级依次尝试：传入的 [classLoader] → 当前线程上下文类加载器 → 系统类加载器。
     *
     * @param classLoader 优先使用的类加载器，可为 `null`
     * @return 可用的类加载器实例
     */
    private fun getSafeClassLoader(classLoader: ClassLoader?): ClassLoader {
        return classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader()
    }

    /**
     * 将混合类型参数数组中的每个元素解析为对应的 [Class] 对象。
     *
     * 数组元素可以是 [Class] 实例或类名 [String]（后者通过 [findClass] 加载）。
     *
     * @param classLoader    用于加载字符串类名的类加载器
     * @param parameterTypes 参数类型数组，元素为 [Class] 或 [String]
     * @return 解析完成的 [Class] 数组
     * @throws IllegalArgumentException 如果数组中包含 `null` 元素，或元素既非 [Class] 也非 [String]
     * @throws NoClassDefFoundError     如果某个字符串类名无法被解析为有效的类
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
     *
     * @param clazz     开始搜索的类
     * @param fieldName 需要查找的字段名称
     * @return 匹配的 [Field] 对象（尚未设置可访问性）
     * @throws NoSuchFieldException 在整个继承链和接口树中均未找到该字段时抛出
     */
    private fun findFieldRecursiveImpl(clazz: Class<*>, fieldName: String): Field {
        var clz: Class<*>? = clazz
        while (clz != null && clz != Any::class.java) {
            try {
                return clz.getDeclaredField(fieldName)
            } catch (_: NoSuchFieldException) {
            }
            clz = clz.superclass
        }

        clz = clazz
        while (clz != null && clz != Any::class.java) {
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
     * @param iface     需要搜索的接口
     * @param fieldName 需要查找的字段名称
     * @return 匹配的 [Field] 对象；未找到时返回 `null`
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
     * @param from 源参数类型数组（元素可为 `null`）
     * @param to   目标参数类型数组
     * @return 所有位置均可赋值时返回 `true`
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
     * @param from 源类型
     * @param to   目标类型
     * @return [from] 可赋值给 [to] 时返回 `true`
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
     * @param from 源基本类型
     * @param to   目标基本类型
     * @return 若可宽化则返回 `true`
     */
    private fun isPrimitiveAssignable(from: Class<*>, to: Class<*>): Boolean {
        if (from == to) return true
        return getPrimitiveWideningSteps(from.name, to.name) != -1
    }

    /**
     * 获取给定基本类型对应的包装类型。
     *
     * 如果传入的类型本身不是基本类型，则原样返回。
     *
     * @param type 基本类型或其他类型
     * @return 对应的包装类型；如果 [type] 非基本类型则返回 [type] 自身
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
     * 如果传入的类型不是包装类型，则返回 `null`。
     *
     * @param type 包装类型或其他类型
     * @return 对应的基本类型；如果无法转换则返回 `null`
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
     * @param srcName  源基本类型名称（如 `"int"`）
     * @param destName 目标基本类型名称（如 `"long"`）
     * @return 所需的宽化步数；无法宽化时返回 `-1`
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
     * @param srcType  源类型（包装类型或基本类型均可）
     * @param destType 目标基本类型
     * @return 转换代价，数值越小表示兼容性越好；不可转换时返回 [Float.MAX_VALUE]
     */
    private fun getPrimitivePromotionCost(srcType: Class<*>, destType: Class<*>): Float {
        var cost = 0.0f
        var cls: Class<*>? = srcType
        if (cls != null && !cls.isPrimitive) {
            cost += 0.1f
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
     * @param srcType  源类型，为 `null` 表示运行时值为 `null`
     * @param destType 目标类型
     * @return 转换代价，数值越小表示兼容性越好；不可转换时返回 [Float.MAX_VALUE]
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

        val effectiveSrc: Class<*>? =
            if (srcType.isPrimitive && !destType.isPrimitive) getWrapperType(srcType) else srcType

        var cost = 0f
        var src: Class<*>? = effectiveSrc
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
     * @param src  源类型
     * @param dest 目标接口类型
     * @return 到达目标接口的最小转换代价；无法到达时返回 [Float.MAX_VALUE]
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
     * @param srcArgs  源参数类型数组（元素可为 `null`）
     * @param destArgs 目标参数类型数组
     * @return 所有参数的总转换代价
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
     * @param clazz          开始搜索的类
     * @param name           需要查找的方法名称
     * @param parameterTypes 期望的参数类型数组（元素可为 `null`）
     * @param result         用于收集匹配方法的可变列表
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
     * @param iface          需要搜索的接口
     * @param name           需要查找的方法名称
     * @param parameterTypes 期望的参数类型数组（元素可为 `null`）
     * @param seen           已收集过的方法签名集合，用于去重
     * @param result         用于收集匹配方法的可变列表
     */
    private fun collectInterfaceMethodsRecursive(iface: Class<*>, name: String, parameterTypes: Array<Class<*>?>, seen: HashSet<String>, result: MutableList<Method>) {
        for (method in iface.declaredMethods) {
            if (Modifier.isStatic(method.modifiers) || Modifier.isPrivate(method.modifiers)
                || Modifier.isAbstract(method.modifiers) || method.isBridge || method.isSynthetic
            ) continue
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
     * @param candidateParams 候选者的参数类型数组
     * @param currentParams   当前最佳匹配的参数类型数组
     * @param parameterTypes  期望的参数类型数组（元素可为 `null`）
     * @return 负值表示候选者更优，正值表示当前匹配更优，零表示两者同等
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
     * 非数组类型则去除首尾空白后原样返回。
     *
     * @param className 需要转换的类名
     * @return JVM 内部规范描述符格式的类名
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
     * @param className   需要查找的类名
     * @param classLoader 用于加载类的类加载器，为 `null` 时使用安全类加载器
     * @return 对应的 [Class] 对象
     * @throws NoClassDefFoundError 无法找到指定类时抛出
     */
    @JvmStatic
    fun findClass(className: String, classLoader: ClassLoader?): Class<*> {
        val loader = getSafeClassLoader(classLoader)
        val key = ClassCacheKey(System.identityHashCode(loader), className)
        val cached = sClassCache[key]?.get()
        if (cached != null && cached.classLoader === loader) {
            return cached
        }
        val clazz = doFindClass(className, loader)
        // 仅缓存成功结果：负结果不缓存，避免运行期动态加载（如后续 DexClassLoader）的类被"找不到"永久遮蔽。
        sClassCache[key] = WeakReference(clazz)
        return clazz
    }

    /**
     * 执行实际的类查找：支持基本类型、数组、普通类与内部类（`.` 与 `$` 互替重试）。
     *
     * @param className   需要查找的类名
     * @param classLoader 类加载器
     * @return 对应的 [Class] 对象
     * @throws NoClassDefFoundError 无法找到指定类时抛出
     */
    private fun doFindClass(className: String, classLoader: ClassLoader): Class<*> {
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
     * @param className   需要查找的类名
     * @param classLoader 用于加载类的类加载器，可为 `null`
     * @return 对应的 [Class] 对象；如果类不存在则返回 `null`
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
     * @param clazz     需要查找字段的类
     * @param fieldName 字段的名称
     * @return 已设置可访问标志的 [Field] 对象
     * @throws NoSuchFieldError 在类、父类及接口树中均未找到该字段时抛出
     */
    @JvmStatic
    fun findField(clazz: Class<*>, fieldName: String): Field {
        return cachedOrRecompute(
            getFieldCacheMap(clazz),
            fieldName,
            { k ->
                try {
                    val field = findFieldRecursiveImpl(clazz, k)
                    field.isAccessible = true
                    Optional.of(field)
                } catch (_: NoSuchFieldException) {
                    Optional.empty()
                }
            },
            { NoSuchFieldError("${clazz.name}#$fieldName") }
        )
    }

    /**
     * 在指定类中查找具有给定名称的字段，查找失败时返回 `null`。
     *
     * @param clazz     需要查找字段的类
     * @param fieldName 字段的名称
     * @return 已设置可访问标志的 [Field] 对象；如果字段不存在则返回 `null`
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
     *
     * @param clazz          需要查找方法的类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Method] 对象
     * @throws NoClassDefFoundError 某个字符串参数类型无法解析为有效类时抛出
     * @throws NoSuchMethodError    类中未找到精确匹配的方法时抛出
     */
    @JvmStatic
    fun findMethodExact(clazz: Class<*>, methodName: String, vararg parameterTypes: Any): Method {
        val resolvedTypes = getParameterClasses(getSafeClassLoader(clazz.classLoader), parameterTypes as Array<*>)
        return findMethodExactWithClasses(clazz, methodName, *resolvedTypes)
    }

    /**
     * 在指定类名对应的类中查找精确匹配的方法。
     *
     * @param className      需要查找方法的类名
     * @param classLoader    用于加载类的类加载器，可为 `null`
     * @param methodName     方法名称
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Method] 对象
     * @throws NoClassDefFoundError 类名或某个字符串参数类型无法解析为有效类时抛出
     * @throws NoSuchMethodError    类中未找到精确匹配的方法时抛出
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
     * @param clazz          需要查找方法的类
     * @param methodName     方法名称
     * @param parameterTypes 精确匹配的参数类型数组
     * @return 已设置可访问标志的 [Method] 对象
     * @throws NoSuchMethodError 类中未找到精确匹配的方法时抛出
     */
    @JvmStatic
    fun findMethodExactWithClasses(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val sig = "$methodName#${typeSigKey(parameterTypes)}#exact"
        return cachedOrRecompute(
            getMethodCacheMap(clazz),
            sig,
            {
                try {
                    val method = findMethodExactRecursive(clazz, methodName, arrayOf(*parameterTypes))
                    method.isAccessible = true
                    Optional.of(method)
                } catch (_: NoSuchMethodException) {
                    Optional.empty()
                }
            },
            { NoSuchMethodError("${clazz.name}#$methodName(${parameterTypes.joinToString { it.name }})") }
        )
    }

    /**
     * 递归查找精确匹配的方法，支持从实体类及接口树中查找继承的 default 方法。
     *
     * @param clazz          开始搜索的类
     * @param name           需要查找的方法名称
     * @param parameterTypes 精确匹配的参数类型数组
     * @return 匹配的 [Method] 对象
     * @throws NoSuchMethodException 在类及所有接口中均未找到该方法时抛出
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
     * @param iface          需要搜索的接口
     * @param name           需要查找的方法名称
     * @param parameterTypes 精确匹配的参数类型数组
     * @return 匹配的 [Method] 对象；未找到时返回 `null`
     */
    private fun findInterfaceMethodExactRecursive(iface: Class<*>, name: String, parameterTypes: Array<Class<*>>): Method? {
        try {
            val m = iface.getDeclaredMethod(name, *parameterTypes)
            if (!Modifier.isStatic(m.modifiers) && !Modifier.isPrivate(m.modifiers) && !Modifier.isAbstract(m.modifiers)) return m
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
     * @param clazz          需要查找方法的类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Method] 对象；如果方法不存在则返回 `null`
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
     * @param className      需要查找方法的类名
     * @param classLoader    用于加载类的类加载器，可为 `null`
     * @param methodName     方法名称
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Method] 对象；如果方法不存在则返回 `null`
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
     *
     * @param clazz          需要查找方法的类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示该位置通配）
     * @return 已设置可访问标志的最佳匹配 [Method] 对象
     * @throws NoSuchMethodError 类中未找到任何兼容方法时抛出
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

        val sig = "$methodName#${typeSigKey(parameterTypes)}#best"
        return cachedOrRecompute(
            getMethodCacheMap(clazz),
            sig,
            {
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
            },
            { NoSuchMethodError("${clazz.name}#$methodName(${parameterTypes.joinToString { it?.name ?: "null" }})") }
        )
    }

    /**
     * 根据实际参数值在指定类中查找最匹配的方法。
     *
     * @param clazz      需要查找方法的类
     * @param methodName 方法名称
     * @param args       实际参数值（可为 `null`）
     * @return 已设置可访问标志的最佳匹配 [Method] 对象
     * @throws NoSuchMethodError 类中未找到任何兼容方法时抛出
     */
    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findMethodBestMatch(clazz, methodName, *paramTypes)
    }

    /**
     * 根据指定的参数类型和实际参数值在指定类中查找最匹配的方法。
     *
     * [parameterTypes] 中为 `null` 的位置会使用 [args] 中对应元素的实际类型填充。
     *
     * @param clazz          需要查找方法的类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组（元素可为 `null`）
     * @param args           实际参数值数组
     * @return 已设置可访问标志的最佳匹配 [Method] 对象
     * @throws NoSuchMethodError 类中未找到任何兼容方法时抛出
     */
    @JvmStatic
    fun findMethodBestMatchWithTypes(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>?>, args: Array<out Any?>): Method {
        require(parameterTypes.size == args.size) {
            "parameterTypes size (${parameterTypes.size}) must match args size (${args.size})"
        }
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
     *
     * @param clazz          需要查找构造函数的类
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Constructor] 对象
     * @throws NoClassDefFoundError 某个字符串参数类型无法解析为有效类时抛出
     * @throws NoSuchMethodError    类中未找到精确匹配的构造函数时抛出
     */
    @JvmStatic
    fun findConstructorExact(clazz: Class<*>, vararg parameterTypes: Any): Constructor<*> {
        val resolvedTypes = getParameterClasses(getSafeClassLoader(clazz.classLoader), parameterTypes as Array<*>)
        return findConstructorExactWithClasses(clazz, *resolvedTypes)
    }

    /**
     * 在指定类名对应的类中查找精确匹配的构造函数。
     *
     * @param className      需要查找构造函数的类名
     * @param classLoader    用于加载类的类加载器，可为 `null`
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Constructor] 对象
     * @throws NoClassDefFoundError 类名或某个字符串参数类型无法解析为有效类时抛出
     * @throws NoSuchMethodError    类中未找到精确匹配的构造函数时抛出
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
     * @param clazz          需要查找构造函数的类
     * @param parameterTypes 精确匹配的参数类型数组
     * @return 已设置可访问标志的 [Constructor] 对象
     * @throws NoSuchMethodError 类中未找到精确匹配的构造函数时抛出
     */
    @JvmStatic
    fun findConstructorExactWithClasses(clazz: Class<*>, vararg parameterTypes: Class<*>): Constructor<*> {
        val sig = "${typeSigKey(parameterTypes)}#exact"
        return cachedOrRecompute(
            getConstructorCacheMap(clazz),
            sig,
            {
                try {
                    val constructor = clazz.getDeclaredConstructor(*parameterTypes)
                    constructor.isAccessible = true
                    Optional.of(constructor)
                } catch (_: NoSuchMethodException) {
                    Optional.empty()
                }
            },
            { NoSuchMethodError("${clazz.name}<init>(${parameterTypes.joinToString { it.name }})") }
        )
    }

    /**
     * 在指定类中查找精确匹配的构造函数，查找失败时返回 `null`。
     *
     * @param clazz          需要查找构造函数的类
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Constructor] 对象；如果构造函数不存在则返回 `null`
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
     * @param className      需要查找构造函数的类名
     * @param classLoader    用于加载类的类加载器，可为 `null`
     * @param parameterTypes 参数类型列表（可为 [Class] 或 [String]）
     * @return 已设置可访问标志的 [Constructor] 对象；如果构造函数不存在则返回 `null`
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
     *
     * @param clazz          需要查找构造函数的类
     * @param parameterTypes 参数类型数组（元素可为 `null`，表示该位置通配）
     * @return 已设置可访问标志的最佳匹配 [Constructor] 对象
     * @throws NoSuchMethodError 类中未找到任何兼容构造函数时抛出
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

        val sig = "${typeSigKey(parameterTypes)}#best"
        return cachedOrRecompute(
            getConstructorCacheMap(clazz),
            sig,
            {
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
            },
            { NoSuchMethodError("${clazz.name}<init>(${parameterTypes.joinToString { it?.name ?: "null" }})") }
        )
    }

    /**
     * 根据实际参数值在指定类中查找最匹配的构造函数。
     *
     * @param clazz 需要查找构造函数的类
     * @param args  实际参数值（可为 `null`）
     * @return 已设置可访问标志的最佳匹配 [Constructor] 对象
     * @throws NoSuchMethodError 类中未找到任何兼容构造函数时抛出
     */
    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg args: Any?): Constructor<*> {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findConstructorBestMatch(clazz, *paramTypes)
    }

    /**
     * 根据指定的参数类型和实际参数值在指定类中查找最匹配的构造函数。
     *
     * [parameterTypes] 中为 `null` 的位置会使用 [args] 中对应元素的实际类型填充。
     *
     * @param clazz          需要查找构造函数的类
     * @param parameterTypes 参数类型数组（元素可为 `null`）
     * @param args           实际参数值数组
     * @return 已设置可访问标志的最佳匹配 [Constructor] 对象
     * @throws NoSuchMethodError 类中未找到任何兼容构造函数时抛出
     */
    @JvmStatic
    fun findConstructorBestMatchWithTypes(clazz: Class<*>, parameterTypes: Array<Class<*>?>, args: Array<out Any?>): Constructor<*> {
        require(parameterTypes.size == args.size) {
            "parameterTypes size (${parameterTypes.size}) must match args size (${args.size})"
        }
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
     * @param obj        目标对象实例
     * @param methodName 方法名称
     * @param args       方法参数值（可为 `null`）
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`
     * @throws NoSuchMethodError  未找到兼容方法时抛出
     * @throws IllegalAccessError 方法无法访问时抛出
     * @throws Throwable          方法执行过程中抛出的原始异常
     */
    @JvmStatic
    fun callMethod(obj: Any, methodName: String, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(obj.javaClass, methodName, *args)
            if (Modifier.isStatic(method.modifiers)) {
                throw IllegalArgumentException(
                    "Method $methodName is static, call with callStaticMethod instead."
                )
            }
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
     * 参数类型数组中可传入 `null` 作为通配符。
     *
     * @param obj            目标对象实例
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）
     * @param args           方法参数值（可为 `null`）
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`
     * @throws NoSuchMethodError  未找到兼容方法时抛出
     * @throws IllegalAccessError 方法无法访问时抛出
     * @throws Throwable          方法执行过程中抛出的原始异常
     */
    @JvmStatic
    fun callMethod(obj: Any, methodName: String, parameterTypes: Array<Class<*>?>, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(obj.javaClass, methodName, *parameterTypes)
            if (Modifier.isStatic(method.modifiers)) {
                throw IllegalArgumentException(
                    "Method $methodName is static, call with callStaticMethod instead."
                )
            }
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
     * @param clazz      目标类
     * @param methodName 方法名称
     * @param args       方法参数值（可为 `null`）
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`
     * @throws NoSuchMethodError  未找到兼容方法时抛出
     * @throws IllegalAccessError 方法无法访问时抛出
     * @throws Throwable          方法执行过程中抛出的原始异常
     */
    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(clazz, methodName, *args)
            if (!Modifier.isStatic(method.modifiers)) {
                throw IllegalArgumentException(
                    "Method $methodName is not static, call with callMethod instead."
                )
            }
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
     * 参数类型数组中可传入 `null` 作为通配符。
     *
     * @param clazz          目标类
     * @param methodName     方法名称
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）
     * @param args           方法参数值（可为 `null`）
     * @return 方法的返回值；如果方法返回 `void` 则返回 `null`
     * @throws NoSuchMethodError  未找到兼容方法时抛出
     * @throws IllegalAccessError 方法无法访问时抛出
     * @throws Throwable          方法执行过程中抛出的原始异常
     */
    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>?>, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(clazz, methodName, *parameterTypes)
            if (!Modifier.isStatic(method.modifiers)) {
                throw IllegalArgumentException(
                    "Method $methodName is not static, call with callMethod instead."
                )
            }
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
     * @param obj       目标对象实例
     * @param fieldName 字段名称
     * @return 字段的值，可能为 `null`
     * @throws NoSuchFieldError   未找到该字段时抛出
     * @throws IllegalAccessError 字段无法访问时抛出
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
     * @param obj       目标对象实例
     * @param fieldName 字段名称
     * @param value     需要设置的值，可为 `null`
     * @throws NoSuchFieldError   未找到该字段时抛出
     * @throws IllegalAccessError 字段无法访问时抛出
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
     * @param clazz     目标类或接口
     * @param fieldName 字段名称
     * @return 字段的值，可能为 `null`
     * @throws NoSuchFieldError      未找到该字段时抛出
     * @throws IllegalAccessError    字段无法访问时抛出
     * @throws IllegalArgumentException 字段不是静态字段时抛出
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
     * @param clazz     目标类
     * @param fieldName 字段名称
     * @param value     需要设置的值，可为 `null`
     * @throws NoSuchFieldError      未找到该字段时抛出
     * @throws IllegalAccessError    字段无法访问时抛出
     * @throws IllegalArgumentException 字段不是静态字段时抛出
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
     * @param clazz 目标类
     * @param args  构造函数参数值（可为 `null`）
     * @return 创建的新实例
     * @throws NoSuchMethodError    未找到兼容构造函数时抛出
     * @throws IllegalAccessError   构造函数无法访问时抛出
     * @throws InstantiationError   类为抽象类或接口、无法实例化时抛出
     * @throws Throwable            构造函数执行过程中抛出的原始异常
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
     * @param clazz          目标类
     * @param parameterTypes 参数类型数组（元素可包含 `null` 表示通配）
     * @param args           构造函数参数值（可为 `null`）
     * @return 创建的新实例
     * @throws NoSuchMethodError    未找到兼容构造函数时抛出
     * @throws IllegalAccessError   构造函数无法访问时抛出
     * @throws InstantiationError   类为抽象类或接口、无法实例化时抛出
     * @throws Throwable            构造函数执行过程中抛出的原始异常
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
     */
    private val sAdditionalFields = WeakHashMap<Any, HashMap<String, Any?>>()

    /**
     * 为指定对象设置一个附加的实例字段。
     *
     * 如果指定的键已存在，则更新其值并返回旧值。
     *
     * @param obj   目标对象实例
     * @param key   字段键名
     * @param value 需要设置的值，可为 `null`
     * @return 该键先前关联的值；如果之前未设置则返回 `null`
     */
    @JvmStatic
    fun setAdditionalInstanceField(obj: Any, key: String, value: Any?): Any? {
        synchronized(sAdditionalFields) {
            val map = sAdditionalFields.getOrPut(obj) { HashMap() }
            return map.put(key, value)
        }
    }

    /**
     * 读取指定对象的附加实例字段值。
     *
     * @param obj 目标对象实例
     * @param key 字段键名
     * @return 该键关联的值；如果未设置则返回 `null`
     */
    @JvmStatic
    fun getAdditionalInstanceField(obj: Any, key: String): Any? {
        synchronized(sAdditionalFields) {
            val map = sAdditionalFields[obj] ?: return null
            return map[key]
        }
    }

    /**
     * 移除指定对象的附加实例字段。
     *
     * @param obj 目标对象实例
     * @param key 字段键名
     * @return 被移除的值；如果该键不存在则返回 `null`
     */
    @JvmStatic
    fun removeAdditionalInstanceField(obj: Any, key: String): Any? {
        synchronized(sAdditionalFields) {
            val map = sAdditionalFields[obj] ?: return null
            val removed = map.remove(key)
            if (map.isEmpty()) {
                sAdditionalFields.remove(obj)
            }
            return removed
        }
    }

    /**
     * 为指定类设置一个附加的静态字段。
     *
     * @param clazz 目标类
     * @param key   字段键名
     * @param value 需要设置的值，可为 `null`
     * @return 该键先前关联的值；如果之前未设置则返回 `null`
     */
    @JvmStatic
    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? {
        return setAdditionalInstanceField(clazz, key, value)
    }

    /**
     * 读取指定类的附加静态字段值。
     *
     * @param clazz 目标类
     * @param key   字段键名
     * @return 该键关联的值；如果未设置则返回 `null`
     */
    @JvmStatic
    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return getAdditionalInstanceField(clazz, key)
    }

    /**
     * 移除指定类的附加静态字段。
     *
     * @param clazz 目标类
     * @param key   字段键名
     * @return 被移除的值；如果该键不存在则返回 `null`
     */
    @JvmStatic
    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return removeAdditionalInstanceField(clazz, key)
    }
}
