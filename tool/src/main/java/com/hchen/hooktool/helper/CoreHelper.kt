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
 * Xposed 辅助方法的内部反射实现类。替代原 Xposed 框架中 XposedHelpers 工具类的功能，
 * 提供类查找、字段访问、方法/构造函数解析、反射调用以及额外实例/静态字段附加等功能。
 *
 * 所有已解析的 [Field]、[Method] 和 [Constructor] 引均使用 [ConcurrentHashMap] 缓存，
 * 避免重复的反射查找。
 *
 * 此类标记为 internal，仅在 tool 模块内部可访问，不属于公开 API。
 *
 * @author 焕晨HChen
 * @see java.lang.reflect.Field
 * @see java.lang.reflect.Method
 * @see java.lang.reflect.Constructor
 */
internal object CoreHelper {
    private val fieldCache = ConcurrentHashMap<FieldCacheKey, Optional<Field>>()
    private val methodCache = ConcurrentHashMap<MethodCacheKey, Optional<Method>>()
    private val constructorCache = ConcurrentHashMap<ConstructorCacheKey, Optional<Constructor<*>>>()

    /**
     * 字段查找的缓存键，由声明该字段的 [Class] 和字段名组成。
     */
    private data class FieldCacheKey(val clazz: Class<*>, val name: String)

    /**
     * 方法查找的缓存键，由声明该方法的 [Class]、方法名、精确参数类型以及
     * 是否为精确匹配或最佳匹配搜索组成。
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
     * 构造函数查找的缓存键，由声明该构造函数的 [Class]、精确参数类型以及
     * 是否为精确匹配或最佳匹配搜索组成。
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
     * 用于缓存的轻量级 Optional 包装类。存储已解析的值或 `null`（表示之前的查找失败），
     * 以避免对已知不存在的成员进行重复的反射查找。
     */
    private class Optional<T>(val value: T?) {
        fun isPresent(): Boolean = value != null
        fun orElseThrow(lazyError: () -> Throwable): T {
            if (value == null) throw lazyError()
            return value
        }

        companion object {
            fun <T> of(v: T): Optional<T> = Optional(v)
            fun <T> empty(): Optional<T> = Optional(null)
        }
    }

    /**
     * 将参数类型描述符数组解析为对应的 [Class] 对象。
     *
     * [parameterTypes] 中的每个元素可以是：
     * - 一个 [Class] 引用（原样返回），或
     * - 一个包含完整类名的 [String]（通过 [findClass] 解析）。
     *
     * @param classLoader    用于解析字符串类名的类加载器。
     * @param parameterTypes 参数类型描述符数组；每个元素必须是 [Class] 或 [String]。
     * @return 与每个输入描述符对应的已解析 [Class] 对象数组。
     * @throws IllegalArgumentException 如果任何元素为 `null` 或既不是 [Class] 也不是 [String]。
     */
    private fun getParameterClasses(classLoader: ClassLoader, parameterTypes: Array<*>): Array<Class<*>> {
        val classes = arrayOfNulls<Class<*>>(parameterTypes.size)
        for (i in parameterTypes.indices) {
            val type = parameterTypes[i]
                ?: throw IllegalArgumentException("parameter type must not be null")
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
     * 在 [clazz] 及其所有父类（不包括 `java.lang.Object`）中递归搜索指定 [fieldName] 的字段。
     *
     * @param clazz     开始搜索的类。
     * @param fieldName 要查找的字段名。
     * @return 已设置为可访问的 [Field] 引用。
     * @throws NoSuchFieldError 如果在 [clazz] 或其任何父类中未找到该字段。
     */
    private fun findFieldRecursiveImpl(clazz: Class<*>, fieldName: String): Field {
        try {
            return clazz.getDeclaredField(fieldName)
        } catch (e: NoSuchFieldException) {
            var clz: Class<*>? = clazz.superclass
            while (clz != null && clz != Any::class.java) {
                try {
                    return clz.getDeclaredField(fieldName)
                } catch (_: NoSuchFieldException) {
                    clz = clz.superclass
                }
            }
            throw e
        }
    }

    /**
     * 检查 [from] 参数类型是否可以逐元素赋值给 [to] 参数类型。
     * 考虑基本类型与包装类之间的自动转换。
     * `from` 中的 `null` 元素表示未知类型（如推断自 `null` 参数），
     * 仅当对应 `to` 元素为基本类型时才返回 `false`。
     *
     * @param from 要检查的源参数类型（元素可为 `null`）。
     * @param to   要检查的目标参数类型。
     * @return 如果 [from] 中的每个元素都可以赋值给 [to] 中的对应元素，则返回 `true`。
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
     * 检查单个 [from] 类型是否可以赋值给单个 [to] 类型，
     * 包括基本类型与包装类之间的自动转换。
     *
     * @param from 源类型。
     * @param to   目标类型。
     * @return 如果 [from] 可以赋值给 [to] 类型的变量，则返回 `true`。
     */
    private fun isAssignable(from: Class<*>, to: Class<*>): Boolean {
        if (to.isAssignableFrom(from)) return true
        if (from.isPrimitive && to.isPrimitive) {
            val fromIndex = WIDENING_PRIMITIVE_TYPES.indexOf(from)
            val toIndex = WIDENING_PRIMITIVE_TYPES.indexOf(to)
            return fromIndex >= 0 && toIndex >= 0 && fromIndex <= toIndex
        }
        if (from.isPrimitive) {
            return to == getWrapperType(from)
        }
        if (to.isPrimitive) {
            val wrapper = getWrapperType(to)
            if (from == wrapper) return true
            val unboxedFrom = getPrimitiveType(from)
            if (unboxedFrom != null) {
                val fromIndex = WIDENING_PRIMITIVE_TYPES.indexOf(unboxedFrom)
                val toIndex = WIDENING_PRIMITIVE_TYPES.indexOf(to)
                return fromIndex >= 0 && toIndex >= 0 && fromIndex <= toIndex
            }
            return false
        }
        return false
    }

    /**
     * 返回给定基本类型对应的包装类类型。
     *
     * 例如，[java.lang.Integer.TYPE]（`int`）映射到 [java.lang.Integer]。
     *
     * @param type 要查找的基本类型。
     * @return 对应的包装类类型，如果 [type] 不是基本类型则返回 [type] 本身。
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
     * 返回给定包装类类型对应的基本类型。
     *
     * 例如，[java.lang.Integer] 映射到 [java.lang.Integer.TYPE]（`int`）。
     *
     * @param type 要查找的包装类类型。
     * @return 对应的基本类型，如果 [type] 不是包装类类型则返回 `null`。
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
     * 基本类型宽化转换的有序列表，按 Java 语言规范中的拓宽基本类型转换顺序排列。
     *
     * 用于计算类型转换代价：从源类型到目标类型在列表中的距离越远，转换代价越高。
     */
    private val WIDENING_PRIMITIVE_TYPES = arrayOf(
        Byte::class.javaPrimitiveType, Short::class.javaPrimitiveType, Char::class.javaPrimitiveType,
        Int::class.javaPrimitiveType, Long::class.javaPrimitiveType, Float::class.javaPrimitiveType, Double::class.javaPrimitiveType
    )

    /**
     * 计算从 [srcType] 到 [destType] 的基本类型宽化转换代价。
     *
     * 如果源类型不是基本类型，则先进行拆箱（代价 0.1），然后沿 [WIDENING_PRIMITIVE_TYPES]
     * 列表逐步宽化，每步代价 0.1。如果源类型无法宽化到目标类型，返回累加代价。
     *
     * @param srcType  源类型（可以是包装类或基本类型）。
     * @param destType 目标基本类型。
     * @return 转换代价，值越小表示转换越精确。
     */
    private fun getPrimitivePromotionCost(srcType: Class<*>, destType: Class<*>): Float {
        var cost = 0.0f
        var cls: Class<*>? = srcType
        if (cls != null && !cls.isPrimitive) {
            cost += 0.1f
            cls = getPrimitiveType(cls)
        }
        if (cls == destType) return cost
        val srcIndex = WIDENING_PRIMITIVE_TYPES.indexOf(cls)
        val destIndex = WIDENING_PRIMITIVE_TYPES.indexOf(destType)
        if (srcIndex < 0 || destIndex < 0 || srcIndex > destIndex) return Float.MAX_VALUE
        cost += 0.1f * (destIndex - srcIndex)
        return cost
    }

    /**
     * 计算从 [srcType] 到 [destType] 的对象类型转换代价。
     *
     * 处理以下情况：
     * - 目标为基本类型：通过 [getPrimitivePromotionCost] 计算拆箱 + 宽化代价。
     * - 源为 `null`：返回 1.5（表示完全未知类型）。
     * - 目标为接口且源可赋值：返回 0.25（接口实现的代价较低）。
     * - 否则沿继承链向上遍历，每层父类代价 +1；若遍历到 `null`（未找到目标），额外加 1.5。
     *
     * @param srcType  源类型，可以为 `null`（表示推断自 `null` 参数）。
     * @param destType 目标类型。
     * @return 转换代价，值越小表示转换越精确。
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
     * 计算所有参数类型的总转换代价。
     *
     * 遍历 [srcArgs] 和 [destArgs]，对每对参数调用 [getObjectTransformationCost] 累加总代价。
     * 用于在多个候选方法/构造函数中选择最佳匹配。
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
     * 在 [clazz] 及其父类中收集所有名称为 [name] 且参数类型与 [parameterTypes] 兼容的方法。
     *
     * 搜索范围包括 [clazz] 自身声明的方法以及所有父类（不包括 `java.lang.Object`）中声明的方法。
     * 父类的私有方法会被排除。通过签名去重，避免因继承关系导致同一方法被重复添加。
     *
     * @param clazz          开始搜索的类。
     * @param name           要查找的方法名。
     * @param parameterTypes 期望的参数类型（元素可为 `null`，表示匹配任意非基本类型）。
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
            considerPrivate = false
            clz = clz.superclass
        }
    }

    /**
     * 比较候选方法/构造函数的形参相对于当前最佳匹配与给定 [parameterTypes] 的匹配程度。
     * 使用与 Apache Commons Lang `MemberUtils.compareMethodFit` 一致的继承距离算法。
     * 如果 candidate 更优则返回负值，如果 current 更优则返回正值，两者同等合适则返回零。
     */
    private fun compareFit(
        candidateParams: Array<Class<*>>,
        currentParams: Array<Class<*>>,
        parameterTypes: Array<Class<*>?>
    ): Int {
        val candidateCost = getTotalTransformationCost(parameterTypes, candidateParams)
        val currentCost = getTotalTransformationCost(parameterTypes, currentParams)
        return candidateCost.compareTo(currentCost)
    }

    // ==================== Class ====================

    /**
     * 将类名转换为 JVM 内部规范名称。
     *
     * 处理数组后缀 `[]`（如 `java.lang.String[]` → `[Ljava.lang.String;`）
     * 以及基本类型数组（如 `int[]` → `[I`）。
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

    /**
     * 基本类型名称到对应 [Class] 对象的映射表。
     *
     * 包含 `boolean`、`byte`、`char`、`short`、`int`、`long`、`float`、`double` 和 `void`
     * 九种基本类型。用于 [findClass] 中快速解析基本类型名称。
     */
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
     * 通过完整类名和指定的类加载器查找类。
     *
     * 支持多种类名格式：
     * - `java.lang.String`
     * - `java.lang.String[]`（数组）
     * - `android.app.ActivityThread.ResourcesKey`（内部类用 `.` 分隔）
     * - `android.app.ActivityThread$ResourcesKey`（内部类用 `$` 分隔）
     * - `int`、`void` 等基本类型名称
     *
     * 内部类解析会逐步从右到左尝试将 `.` 替换为 `$`，与 Apache Commons Lang
     * `ClassUtils.getClass` 行为一致，支持深层内部类（如 `a.b.C.D` → `a.b.C$D`
     * → `a.b$C$D` → `a$b$C$D`）。
     *
     * [classLoader] 为 `null` 时使用启动类加载器（Bootstrap ClassLoader）。
     *
     * @param className   完整类名。
     * @param classLoader 用于解析的类加载器，或 `null` 表示使用启动类加载器。
     * @return 已解析的 [Class] 对象。
     * @throws NoClassDefFoundError 如果无法找到该类。
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
     * 通过完整类名查找类，如果类不存在则返回 `null` 而不是抛出异常。
     *
     * @param className   完整类名。
     * @param classLoader 用于解析的类加载器，或 `null` 表示使用启动类加载器。
     * @return 已解析的 [Class] 对象，如果未找到则返回 `null`。
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
     * 在给定类及其父类中按名称查找字段，将其设置为可访问并返回。结果会被缓存以供后续查找使用。
     *
     * @param clazz     声明或继承该字段的类。
     * @param fieldName 要查找的字段名。
     * @return 已设置为可访问的 [Field] 引用。
     * @throws NoSuchFieldError 如果在 [clazz] 或其任何父类中未找到该字段。
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
     * 按名称查找字段，如果字段不存在则返回 `null` 而不是抛出异常。
     *
     * @param clazz     声明或继承该字段的类。
     * @param fieldName 要查找的字段名。
     * @return 已设置为可访问的 [Field] 引用，如果未找到则返回 `null`。
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
     * 在 [clazz] 中按名称和参数类型查找精确匹配的方法，将其设置为可访问并返回。结果会被缓存。
     * 仅搜索 [clazz] 自身声明的方法（使用 `getDeclaredMethod`），不搜索父类。
     *
     * [parameterTypes] 中的参数类型可以指定为 [Class] 引用或完整类名 [String]
     * （将使用该类自身的类加载器进行解析）。
     *
     * @param clazz          声明该方法的类。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 方法的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Method] 引用。
     * @throws NoClassDefFoundError 如果字符串参数类型无法解析。
     * @throws NoSuchMethodError    如果不存在具有给定名称和参数类型的方法。
     */
    @JvmStatic
    fun findMethodExact(clazz: Class<*>, methodName: String, vararg parameterTypes: Any): Method {
        val resolvedTypes = getParameterClasses(
            clazz.classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader(),
            parameterTypes as Array<*>
        )
        return findMethodExactWithClasses(clazz, methodName, *resolvedTypes)
    }

    /**
     * 通过类名、类加载器、方法名和参数类型查找精确匹配的方法。
     * 这是一个便捷重载，先解析类，然后委托给 [findMethodExact]。
     *
     * @param className      声明该方法的完整类名。
     * @param classLoader    用于解析的类加载器，或 `null` 表示使用系统类加载器。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 方法的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Method] 引用。
     * @throws NoClassDefFoundError 如果类或参数类型无法解析。
     * @throws NoSuchMethodError    如果不存在具有给定名称和参数类型的方法。
     */
    @JvmStatic
    fun findMethodExact(className: String, classLoader: ClassLoader?, methodName: String, vararg parameterTypes: Any): Method {
        val clazz = findClass(className, classLoader)
        val resolvedTypes = getParameterClasses(
            classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader(),
            parameterTypes as Array<*>
        )
        return findMethodExactWithClasses(clazz, methodName, *resolvedTypes)
    }

    /**
     * 通过已解析的 [Class] 参数类型查找精确匹配的方法。这是其他重载使用的核心精确方法解析入口。结果会被缓存。
     * 仅搜索 [clazz] 自身声明的方法（使用 `getDeclaredMethod`），不搜索父类。
     *
     * @param clazz          声明该方法的类。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 已解析的方法参数类型。
     * @return 已设置为可访问的 [Method] 引用。
     * @throws NoSuchMethodError 如果不存在具有给定名称和精确参数类型的方法。
     */
    @JvmStatic
    fun findMethodExactWithClasses(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>): Method {
        val key = MethodCacheKey(clazz, methodName, arrayOf(*parameterTypes), true)
        return methodCache.computeIfAbsent(key) { k ->
            try {
                @Suppress("UNCHECKED_CAST")
                val method = k.clazz.getDeclaredMethod(k.name, *(k.parameters as Array<Class<*>>))
                method.isAccessible = true
                Optional.of(method)
            } catch (_: NoSuchMethodException) {
                Optional.empty()
            }
        }.orElseThrow { NoSuchMethodError("${clazz.name}#$methodName(${parameterTypes.joinToString { it.name }})") }
    }

    /**
     * 查找精确匹配的方法，如果方法或其声明类不存在则返回 `null` 而不是抛出异常。
     *
     * @param clazz          声明该方法的类。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 方法的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Method] 引用，如果未找到则返回 `null`。
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
     * 通过类名查找精确匹配的方法，如果方法或类不存在则返回 `null` 而不是抛出异常。
     *
     * @param className      声明该方法的完整类名。
     * @param classLoader    用于解析的类加载器，或 `null` 表示使用系统类加载器。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 方法的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Method] 引用，如果未找到则返回 `null`。
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
     * 在 [clazz]（及其父类）中按名称和参数类型查找最佳匹配的方法。首先尝试精确匹配；
     * 如果失败，则搜索给定参数类型可赋值给方法形式参数的最兼容方法。
     *
     * 会考虑继承的方法。父类的私有方法在最佳匹配搜索中会被排除。结果会被缓存。
     *
     * @param clazz          声明、继承或覆盖该方法的类。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 期望的参数类型。
     * @return 已设置为可访问的最佳匹配 [Method] 引用。
     * @throws NoSuchMethodError 如果未找到合适的方法。
     */
    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg parameterTypes: Class<*>?): Method {
        if (parameterTypes.all { it != null }) {
            try {
                @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
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
     * 通过从给定 [args] 推断参数类型来查找最佳匹配的方法。
     * 对于每个非 `null` 参数，使用其运行时类作为参数类型；`null` 参数的类型保留为 `null`，
     * 可匹配任意非基本类型形参。
     *
     * @param clazz      声明、继承或覆盖该方法的类。
     * @param methodName 要查找的方法名。
     * @param args       用于推断参数类型的参数实例。
     * @return 已设置为可访问的最佳匹配 [Method] 引用。
     * @throws NoSuchMethodError 如果未找到合适的方法。
     */
    @JvmStatic
    fun findMethodBestMatch(clazz: Class<*>, methodName: String, vararg args: Any?): Method {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findMethodBestMatch(clazz, methodName, *paramTypes)
    }

    /**
     * 使用显式 [parameterTypes] 查找最佳匹配的方法，对于 `null` 类型的条目，
     * 回退使用 [args] 的运行时类型。当某些参数类型在编译时已知但其他参数依赖运行时值时，
     * 此方法非常有用。
     *
     * @param clazz          声明、继承或覆盖该方法的类。
     * @param methodName     要查找的方法名。
     * @param parameterTypes 声明的参数类型；`null` 条目将使用对应 [args] 元素的
     *                       运行时类型进行细化。
     * @param args           用于类型细化的参数实例。
     * @return 已设置为可访问的最佳匹配 [Method] 引用。
     * @throws NoSuchMethodError 如果未找到合适的方法。
     */
    @JvmStatic
    fun findMethodBestMatchWithTypes(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>>, args: Array<Any?>): Method {
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
     * 在 [clazz] 中按参数类型查找精确匹配的构造函数，将其设置为可访问并返回。结果会被缓存。
     * 仅搜索 [clazz] 自身声明的构造函数（使用 `getDeclaredConstructor`），不搜索父类。
     *
     * [parameterTypes] 中的参数类型可以指定为 [Class] 引用或完整类名 [String]
     * （将使用该类自身的类加载器进行解析）。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 构造函数的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Constructor] 引用。
     * @throws NoClassDefFoundError 如果字符串参数类型无法解析。
     * @throws NoSuchMethodError    如果不存在具有给定参数类型的构造函数。
     */
    @JvmStatic
    fun findConstructorExact(clazz: Class<*>, vararg parameterTypes: Any): Constructor<*> {
        val resolvedTypes = getParameterClasses(
            clazz.classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader(),
            parameterTypes as Array<*>
        )
        return findConstructorExactWithClasses(clazz, *resolvedTypes)
    }

    /**
     * 通过类名、类加载器和参数类型查找精确匹配的构造函数。
     * 这是一个便捷重载，先解析类，然后委托给 [findConstructorExact]。
     *
     * @param className      要查找构造函数的完整类名。
     * @param classLoader    用于解析的类加载器，或 `null` 表示使用系统类加载器。
     * @param parameterTypes 构造函数的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Constructor] 引用。
     * @throws NoClassDefFoundError 如果类或参数类型无法解析。
     * @throws NoSuchMethodError    如果不存在具有给定参数类型的构造函数。
     */
    @JvmStatic
    fun findConstructorExact(className: String, classLoader: ClassLoader?, vararg parameterTypes: Any): Constructor<*> {
        val clazz = findClass(className, classLoader)
        val resolvedTypes = getParameterClasses(
            classLoader ?: Thread.currentThread().contextClassLoader ?: ClassLoader.getSystemClassLoader(),
            parameterTypes as Array<*>
        )
        return findConstructorExactWithClasses(clazz, *resolvedTypes)
    }

    /**
     * 通过已解析的 [Class] 参数类型查找精确匹配的构造函数。这是其他重载使用的核心精确构造函数解析入口。结果会被缓存。
     * 仅搜索 [clazz] 自身声明的构造函数（使用 `getDeclaredConstructor`），不搜索父类。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 已解析的构造函数参数类型。
     * @return 已设置为可访问的 [Constructor] 引用。
     * @throws NoSuchMethodError 如果不存在具有给定精确参数类型的构造函数。
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
     * 查找精确匹配的构造函数，如果构造函数或其声明类不存在则返回 `null` 而不是抛出异常。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 构造函数的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Constructor] 引用，如果未找到则返回 `null`。
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
     * 通过类名查找精确匹配的构造函数，如果构造函数或类不存在则返回 `null` 而不是抛出异常。
     *
     * @param className      要查找构造函数的完整类名。
     * @param classLoader    用于解析的类加载器，或 `null` 表示使用系统类加载器。
     * @param parameterTypes 构造函数的参数类型；每个元素可以是 [Class] 或 [String]。
     * @return 已设置为可访问的 [Constructor] 引用，如果未找到则返回 `null`。
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
     * 在 [clazz] 中按参数类型查找最佳匹配的构造函数。首先尝试精确匹配；如果失败，
     * 则搜索给定参数类型可赋值给构造函数形式参数的最兼容构造函数。结果会被缓存。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 期望的参数类型。
     * @return 已设置为可访问的最佳匹配 [Constructor] 引用。
     * @throws NoSuchMethodError 如果未找到合适的构造函数。
     */
    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg parameterTypes: Class<*>?): Constructor<*> {
        if (parameterTypes.all { it != null }) {
            try {
                @Suppress("UNCHECKED_CAST", "KotlinConstantConditions")
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
                        @Suppress("UNCHECKED_CAST")
                        bestMatch = constructor as Constructor<*>
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
     * 通过从给定 [args] 推断参数类型来查找最佳匹配的构造函数。
     * 对于每个非 `null` 参数，使用其运行时类作为参数类型；`null` 参数的类型保留为 `null`，
     * 可匹配任意非基本类型形参。
     *
     * @param clazz 要查找构造函数的类。
     * @param args  用于推断参数类型的参数实例。
     * @return 已设置为可访问的最佳匹配 [Constructor] 引用。
     * @throws NoSuchMethodError 如果未找到合适的构造函数。
     */
    @JvmStatic
    fun findConstructorBestMatch(clazz: Class<*>, vararg args: Any?): Constructor<*> {
        val paramTypes: Array<Class<*>?> = Array(args.size) { i -> args[i]?.javaClass }
        return findConstructorBestMatch(clazz, *paramTypes)
    }

    /**
     * 使用显式 [parameterTypes] 查找最佳匹配的构造函数，对于 `null` 类型的条目，
     * 回退使用 [args] 的运行时类型。
     *
     * @param clazz          要查找构造函数的类。
     * @param parameterTypes 声明的参数类型；`null` 条目将使用对应 [args] 元素的
     *                       运行时类型进行细化。
     * @param args           用于类型细化的参数实例。
     * @return 已设置为可访问的最佳匹配 [Constructor] 引用。
     * @throws NoSuchMethodError 如果未找到合适的构造函数。
     */
    @JvmStatic
    fun findConstructorBestMatchWithTypes(clazz: Class<*>, parameterTypes: Array<Class<*>>, args: Array<Any?>): Constructor<*> {
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
     * 在 [obj] 上按名称调用实例方法，使用 [args] 的运行时类型自动解析最佳匹配的方法。
     *
     * @param obj        要在其上调用方法的对象实例。不能是类引用。
     * @param methodName 要调用的方法名。
     * @param args       传递给方法的参数。
     * @return 方法调用的返回值，如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  如果不存在具有给定名称的合适方法。
     * @throws IllegalAccessError 如果方法不可访问。
     * @throws Throwable          被调用方法本身抛出的任何异常（从 [InvocationTargetException] 中解包）。
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
     * 在 [obj] 上按名称和显式参数类型调用实例方法。当多个方法共享相同名称时，
     * 此重载可用于消歧，特别是在传递 `null` 参数时。
     *
     * @param obj            要在其上调用方法的对象实例。
     * @param methodName     要调用的方法名。
     * @param parameterTypes 用于方法解析的显式参数类型。
     * @param args           传递给方法的参数。
     * @return 方法调用的返回值，如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  如果不存在具有给定名称和参数类型的合适方法。
     * @throws IllegalAccessError 如果方法不可访问。
     * @throws Throwable          被调用方法本身抛出的任何异常。
     */
    @JvmStatic
    fun callMethod(obj: Any, methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? {
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
     * 在 [clazz] 上按名称调用静态方法，使用 [args] 的运行时类型自动解析最佳匹配的方法。
     *
     * @param clazz      包含静态方法的类。
     * @param methodName 要调用的静态方法名。
     * @param args       传递给方法的参数。
     * @return 方法调用的返回值，如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  如果不存在具有给定名称的合适方法。
     * @throws IllegalAccessError 如果方法不可访问。
     * @throws Throwable          被调用方法本身抛出的任何异常。
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
     * 在 [clazz] 上按名称和显式参数类型调用静态方法。当多个静态方法共享相同名称时，
     * 此重载可用于消歧。
     *
     * @param clazz          包含静态方法的类。
     * @param methodName     要调用的静态方法名。
     * @param parameterTypes 用于方法解析的显式参数类型。
     * @param args           传递给方法的参数。
     * @return 方法调用的返回值，如果方法返回 `void` 则返回 `null`。
     * @throws NoSuchMethodError  如果不存在具有给定名称和参数类型的合适方法。
     * @throws IllegalAccessError 如果方法不可访问。
     * @throws Throwable          被调用方法本身抛出的任何异常。
     */
    @JvmStatic
    fun callStaticMethod(clazz: Class<*>, methodName: String, parameterTypes: Array<Class<*>>, vararg args: Any?): Any? {
        return try {
            val method = findMethodBestMatch(clazz, methodName, *parameterTypes)
            method.invoke(null, *args)
        } catch (e: IllegalAccessException) {
            throw IllegalAccessError(e.message)
        } catch (e: InvocationTargetException) {
            throw e.cause ?: e
        }
    }

    // ==================== Object Field ====================

    /**
     * 返回给定对象中实例字段的值。按名称在对象的类（及父类）中查找字段，
     * 并在读取前将其设置为可访问。
     *
     * @param obj       要获取字段值的对象实例。不能是类引用。
     * @param fieldName 要读取的字段名。
     * @return 字段的值，如果字段值为 `null` 则返回 `null`。
     * @throws NoSuchFieldError   如果字段在对象的类层次结构中不存在。
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
     * 设置给定对象中实例字段的值。按名称在对象的类（及父类）中查找字段，
     * 并在写入前将其设置为可访问。
     *
     * @param obj       要修改字段的对象实例。不能是类引用。
     * @param fieldName 要写入的字段名。
     * @param value     要赋给字段的新值。
     * @throws NoSuchFieldError   如果字段在对象的类层次结构中不存在。
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

    // ==================== Static Field ====================

    /**
     * 返回给定类中静态字段的值。按名称在类（及父类）中查找字段，并在读取前将其设置为可访问。
     *
     * @param clazz     包含静态字段的类。
     * @param fieldName 要读取的静态字段名。
     * @return 静态字段的值，如果字段值为 `null` 则返回 `null`。
     * @throws NoSuchFieldError   如果字段在类层次结构中不存在。
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
     * 设置给定类中静态字段的值。按名称在类（及父类）中查找字段，并在写入前将其设置为可访问。
     *
     * @param clazz     包含静态字段的类。
     * @param fieldName 要写入的静态字段名。
     * @param value     要赋给静态字段的新值。
     * @throws NoSuchFieldError   如果字段在类层次结构中不存在。
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

    // ==================== New Instance ====================

    /**
     * 通过从 [args] 的运行时类型解析最佳匹配的构造函数来创建 [clazz] 的新实例。
     *
     * @param clazz 要实例化的类。
     * @param args  传递给构造函数的参数。
     * @return 新创建的实例。
     * @throws NoSuchMethodError   如果未找到合适的构造函数。
     * @throws IllegalAccessError  如果构造函数不可访问。
     * @throws InstantiationError  如果类无法实例化（例如它是抽象的）。
     * @throws Throwable           构造函数本身抛出的任何异常。
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
     * 使用显式 [parameterTypes] 创建 [clazz] 的新实例。当多个构造函数具有相似签名，
     * 或者 `null` 参数需要显式类型消歧时，此重载非常有用。
     *
     * @param clazz          要实例化的类。
     * @param parameterTypes 用于构造函数解析的显式参数类型。
     * @param args           传递给构造函数的参数。
     * @return 新创建的实例。
     * @throws NoSuchMethodError   如果未找到合适的构造函数。
     * @throws IllegalAccessError  如果构造函数不可访问。
     * @throws InstantiationError  如果类无法实例化（例如它是抽象的）。
     * @throws Throwable           构造函数本身抛出的任何异常。
     */
    @JvmStatic
    fun newInstance(clazz: Class<*>, parameterTypes: Array<Class<*>>, vararg args: Any?): Any {
        return try {
            val constructor = findConstructorBestMatchWithTypes(clazz, parameterTypes, args as Array<Any?>)
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
     * 额外实例字段的后备存储。将对象实例映射到一个可变的字符串键值对映射。
     * 使用 [java.util.WeakHashMap] 以便在键对象不再被强引用时允许条目被垃圾回收。
     *
     * **注意：** 当作为键的对象不再被任何强引用持有时（即可被垃圾回收时），
     * 其对应的附加数据映射会被自动清除。这意味着如果使用者仅通过弱引用或软引用持有对象，
     * 附加数据可能在意想不到的时刻丢失。在长时间运行的场景中，请确保对需要保留附加数据的
     * 对象保持强引用，或在数据丢失前主动读取。
     */
    private val additionalFields = WeakHashMap<Any, MutableMap<String, Any?>>()

    /**
     * 将任意值附加到对象实例上，模拟一个额外的实例字段。
     * 该值可以随后通过 [getAdditionalInstanceField] 获取，或通过
     * [removeAdditionalInstanceField] 移除。
     *
     * 内部使用 [ConcurrentHashMap] 存储每个对象的附加字段映射，保证线程安全。
     *
     * @param obj   要附加值的对象实例。不能为 `null`。
     * @param key   标识此额外字段的键。不能为 `null`。
     * @param value 要存储的值，或 `null` 以存储显式的 null 映射。
     * @return 此实例/键组合之前存储的值，如果之前没有映射则返回 `null`。
     * @throws NullPointerException 如果 [obj] 或 [key] 为 `null`。
     */
    @JvmStatic
    fun setAdditionalInstanceField(obj: Any, key: String, value: Any?): Any? {
        val map: MutableMap<String, Any?>
        synchronized(additionalFields) {
            map = additionalFields.getOrPut(obj) { ConcurrentHashMap() }
        }
        return map.put(key, value)
    }

    /**
     * 返回之前通过 [setAdditionalInstanceField] 存储的值。
     *
     * @param obj 存储值的对象实例。不能为 `null`。
     * @param key 标识额外字段的键。不能为 `null`。
     * @return 存储的值，如果此实例/键组合没有存储值（或显式存储了 `null`）则返回 `null`。
     * @throws NullPointerException 如果 [obj] 或 [key] 为 `null`。
     */
    @JvmStatic
    fun getAdditionalInstanceField(obj: Any, key: String): Any? {
        val map = synchronized(additionalFields) {
            additionalFields[obj] ?: return null
        }
        return map[key]
    }

    /**
     * 移除并返回之前通过 [setAdditionalInstanceField] 存储的值。
     *
     * @param obj 存储值的对象实例。不能为 `null`。
     * @param key 标识额外字段的键。不能为 `null`。
     * @return 之前存储的值，如果此实例/键组合没有存储值则返回 `null`。
     * @throws NullPointerException 如果 [obj] 或 [key] 为 `null`。
     */
    @JvmStatic
    fun removeAdditionalInstanceField(obj: Any, key: String): Any? {
        val map = synchronized(additionalFields) {
            additionalFields[obj] ?: return null
        }
        return map.remove(key)
    }

    /**
     * 将任意值附加到类上，模拟一个额外的静态字段。
     * 委托给 [setAdditionalInstanceField]，使用 [clazz] 对象本身作为键。
     *
     * @param clazz 要附加值的类。不能为 `null`。
     * @param key   标识此额外静态字段的键。不能为 `null`。
     * @param value 要存储的值，或 `null` 以存储显式的 null 映射。
     * @return 此类/键组合之前存储的值，如果之前没有映射则返回 `null`。
     */
    @JvmStatic
    fun setAdditionalStaticField(clazz: Class<*>, key: String, value: Any?): Any? {
        return setAdditionalInstanceField(clazz, key, value)
    }

    /**
     * 返回之前通过 [setAdditionalStaticField] 存储的值。
     *
     * @param clazz 存储值的类。
     * @param key   标识额外静态字段的键。
     * @return 存储的值，如果没有存储则返回 `null`。
     */
    @JvmStatic
    fun getAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return getAdditionalInstanceField(clazz, key)
    }

    /**
     * 移除并返回之前通过 [setAdditionalStaticField] 存储的值。
     *
     * @param clazz 存储值的类。
     * @param key   标识额外静态字段的键。
     * @return 之前存储的值，如果没有存储则返回 `null`。
     */
    @JvmStatic
    fun removeAdditionalStaticField(clazz: Class<*>, key: String): Any? {
        return removeAdditionalInstanceField(clazz, key)
    }
}
