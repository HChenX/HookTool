package com.hchen.hooktool.core

import android.content.Context
import android.content.res.Resources
import androidx.annotation.IdRes
import com.hchen.hooktool.ModuleData
import com.hchen.hooktool.callback.IAsyncPrefs
import com.hchen.hooktool.callback.IPrefsApply
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

@Suppress("unused")
open class CoreTool : XposedLog() {
    private companion object {
        // -------------------------------- class ---------------------------------
        @JvmStatic
        @JvmOverloads
        fun String.hasClass(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findClassIfExists(this, classLoader)
            )
        }

        @JvmStatic
        @JvmOverloads
        fun String.findClass(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Class<*> {
            return XposedHelpers.findClass(this, classLoader)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findClassIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Class<*>? {
            return XposedHelpers.findClassIfExists(this, classLoader)
        }

        // ---------------------------------- method -----------------------------------

        @JvmStatic
        @JvmOverloads
        fun String.hasMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            exactMatch: Boolean = true,
            vararg parameterTypes: Any = emptyArray()
        ): Boolean {
            return this.findClassIfExists(classLoader)?.hasMethod(methodName, exactMatch, parameterTypes) ?: false
        }

        @JvmStatic
        @JvmOverloads
        fun Class<*>.hasMethod(
            methodName: String,
            exactMatch: Boolean = true,
            vararg parameterTypes: Any = emptyArray()
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

        @JvmStatic
        @JvmOverloads
        fun String.findMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return XposedHelpers.findMethodExact(this, classLoader, methodName, parameterTypes)
        }

        @JvmStatic
        fun Class<*>.findMethod(
            methodName: String,
            vararg parameterTypes: Any
        ): Method {
            return XposedHelpers.findMethodExact(this, methodName, parameterTypes)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findMethodIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return XposedHelpers.findMethodExactIfExists(this, classLoader, methodName, parameterTypes)
        }

        @JvmStatic
        fun Class<*>.findMethodIfExists(
            methodName: String,
            vararg parameterTypes: Any
        ): Method? {
            return XposedHelpers.findMethodExactIfExists(this, methodName, parameterTypes)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findAllMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String? = null
        ): Array<Method> {
            return this.findClass(classLoader).findAllMethod(methodName)
        }

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

        @JvmStatic
        fun Class<*>.hasConstructor(
            vararg parameterTypes: Any
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findConstructorExactIfExists(this, parameterTypes)
            )
        }

        @JvmStatic
        @JvmOverloads
        fun String.findConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExact(this, classLoader, parameterTypes)
        }

        @JvmStatic
        fun Class<*>.findConstructor(
            vararg parameterTypes: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExact(this, parameterTypes)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findConstructorIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExactIfExists(this, classLoader, parameterTypes)
        }

        @JvmStatic
        fun Class<*>.findConstructorIfExists(
            vararg parameterTypes: Any
        ): Constructor<*>? {
            return XposedHelpers.findConstructorExactIfExists(this, parameterTypes)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findAllConstructor(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): Array<Constructor<*>> {
            return this.findClass(classLoader).findAllConstructor()
        }

        @JvmStatic
        fun Class<*>.findAllConstructor(): Array<Constructor<*>> {
            return this.declaredConstructors
        }

        // --------------------------------- field ----------------------------------

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

        @JvmStatic
        fun Class<*>.hasField(
            fieldName: String
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findFieldIfExists(this, fieldName)
            )
        }

        @JvmStatic
        @JvmOverloads
        fun String.findField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Field {
            return this.findClass(classLoader).findField(fieldName)
        }

        @JvmStatic
        fun Class<*>.findField(
            fieldName: String
        ): Field {
            return XposedHelpers.findField(this, fieldName)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findFieldIfExists(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Field? {
            return this.findClassIfExists(classLoader)?.findFieldIfExists(fieldName)
        }

        @JvmStatic
        fun Class<*>.findFieldIfExists(
            fieldName: String
        ): Field? {
            return XposedHelpers.findFieldIfExists(this, fieldName)
        }

        // -------------------------------- non static ---------------------------------
        @JvmStatic
        @JvmOverloads
        fun Any.callMethod(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any
        ): Any? {
            return if (parameterTypes.isEmpty()) {
                XposedHelpers.callMethod(this, methodName, args)
            } else {
                XposedHelpers.callMethod(this, methodName, parameterTypes, args)
            }
        }

        @JvmStatic
        fun Method.callMethod(
            instance: Any,
            vararg args: Any
        ): Any? {
            this.isAccessible = true
            return this.getInvoker().invoke(instance, args)
        }

        @JvmStatic
        fun Any.getField(
            fieldName: String,
        ): Any? {
            return XposedHelpers.getObjectField(this, fieldName)
        }

        @JvmStatic
        fun Field.getField(
            instance: Any,
        ): Any? {
            this.isAccessible = true
            return this.get(instance)
        }

        @JvmStatic
        fun Any.setField(
            fieldName: String,
            value: Any?
        ) {
            XposedHelpers.setObjectField(this, fieldName, value)
        }

        @JvmStatic
        fun Field.setField(
            instance: Any,
            value: Any?
        ) {
            this.isAccessible = true
            this.set(instance, value)
        }

        @JvmStatic
        fun Any.setAdditionalInstanceField(
            key: String,
            value: Any?
        ): Any? {
            return XposedHelpers.setAdditionalInstanceField(this, key, value)
        }

        @JvmStatic
        fun Any.getAdditionalInstanceField(
            key: String
        ): Any? {
            return XposedHelpers.getAdditionalInstanceField(this, key)
        }

        @JvmStatic
        fun Any.removeAdditionalInstanceField(
            key: String
        ): Any? {
            return XposedHelpers.removeAdditionalInstanceField(this, key)
        }

        // ------------------------------- static ------------------------------------

        @JvmStatic
        @JvmOverloads
        fun String.newInstance(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any
        ): Any {
            return this.findClass(classLoader).newInstance(parameterTypes, args)
        }

        @JvmStatic
        @JvmOverloads
        fun Class<*>.newInstance(
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any
        ): Any {
            return if (parameterTypes.isEmpty()) {
                XposedHelpers.newInstance(this, args)
            } else {
                XposedHelpers.newInstance(this, parameterTypes, args)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun String.callStaticMethod(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any
        ): Any? {
            return this.findClass(classLoader).callStaticMethod(methodName, parameterTypes, args)
        }

        @JvmStatic
        @JvmOverloads
        fun Class<*>.callStaticMethod(
            methodName: String,
            parameterTypes: Array<Class<*>> = emptyArray(),
            vararg args: Any
        ): Any? {
            return if (parameterTypes.isEmpty()) {
                XposedHelpers.callStaticMethod(this, methodName, args)
            } else {
                XposedHelpers.callStaticMethod(this, methodName, parameterTypes, args)
            }
        }

        @JvmStatic
        fun Method.callStaticMethod(
            vararg args: Any
        ): Any? {
            this.isAccessible = true
            return this.getInvoker().invoke(null, args)
        }

        @JvmStatic
        @JvmOverloads
        fun String.getStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String
        ): Any? {
            return this.findClass(classLoader).getStaticField(fieldName)
        }

        @JvmStatic
        fun Class<*>.getStaticField(
            fieldName: String
        ): Any? {
            return XposedHelpers.getStaticObjectField(this, fieldName)
        }

        @JvmStatic
        fun Field.getStaticField(): Any? {
            this.isAccessible = true
            return this.get(null)
        }

        @JvmStatic
        @JvmOverloads
        fun String.setStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            fieldName: String,
            value: Any?
        ) {
            this.findClass(classLoader).setStaticField(fieldName, value)
        }

        @JvmStatic
        fun Class<*>.setStaticField(
            fieldName: String,
            value: Any?
        ) {
            XposedHelpers.setStaticObjectField(this, fieldName, value)
        }

        @JvmStatic
        fun Field.setStaticField(
            value: Any?
        ) {
            this.isAccessible = true
            this.set(null, value)
        }

        @JvmStatic
        @JvmOverloads
        fun String.setAdditionalStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            key: String,
            value: Any?
        ): Any? {
            return this.findClass(classLoader).setAdditionalStaticField(key, value)
        }

        @JvmStatic
        fun Class<*>.setAdditionalStaticField(
            key: String,
            value: Any?
        ): Any? {
            return XposedHelpers.setAdditionalStaticField(this, key, value)
        }

        @JvmStatic
        @JvmOverloads
        fun String.getAdditionalStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            key: String,
        ): Any? {
            return this.findClass(classLoader).getAdditionalStaticField(key)
        }

        @JvmStatic
        fun Class<*>.getAdditionalStaticField(
            key: String,
        ): Any? {
            return XposedHelpers.getAdditionalStaticField(this, key)
        }

        @JvmStatic
        @JvmOverloads
        fun String.removeAdditionalStaticField(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            key: String,
        ): Any? {
            return this.findClass(classLoader).removeAdditionalStaticField(key)
        }

        @JvmStatic
        fun Class<*>.removeAdditionalStaticField(
            key: String,
        ): Any? {
            return XposedHelpers.removeAdditionalStaticField(this, key)
        }

        // --------------------------------- hook ------------------------------------

        @JvmStatic
        @JvmOverloads
        fun String.hook(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findMethod(classLoader, methodName, parameterTypes).hook()
        }

        @JvmStatic
        fun Class<*>.hook(
            methodName: String,
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findMethod(methodName, parameterTypes).hook()
        }

        @JvmStatic
        @JvmOverloads
        fun String.hook(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findConstructor(classLoader, parameterTypes).hook()
        }

        @JvmStatic
        fun Class<*>.hook(
            vararg parameterTypes: Any
        ): HookBridge {
            return this.findConstructor(parameterTypes).hook()
        }

        @JvmStatic
        fun Executable.hook(): HookBridge {
            return HookBridge(ModuleData.getWrapper().hook(this))
        }

        @JvmStatic
        @JvmOverloads
        fun String.hookClassInitializer(
            classLoader: ClassLoader? = ModuleData.getClassLoader()
        ): HookBridge {
            return this.findClass(classLoader).hookClassInitializer()
        }

        @JvmStatic
        fun Class<*>.hookClassInitializer(): HookBridge {
            return HookBridge(ModuleData.getWrapper().hookClassInitializer(this))
        }

        @JvmStatic
        fun returnResult(result: Any?): AbsHook {
            return object : AbsHook() {
                override fun before() {
                    this.result = result
                }
            }
        }

        @JvmStatic
        fun doNothing(): AbsHook {
            return returnResult(null)
        }

        @JvmStatic
        fun setArg(index: Int, value: Any?): AbsHook {
            return object : AbsHook() {
                override fun before() {
                    setArg(index, value)
                }
            }
        }

        // ------------------------------- invoker -----------------------------------
        @JvmStatic
        @JvmOverloads
        fun String.getInvoker(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(classLoader, methodName, parameterTypes).getInvoker()
        }

        @JvmStatic
        fun Class<*>.getInvoker(
            methodName: String,
            vararg parameterTypes: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(methodName, parameterTypes).getInvoker()
        }

        @JvmStatic
        fun Method.getInvoker(): XposedInterface.Invoker<*, Method> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        @JvmStatic
        @JvmOverloads
        fun String.getInvoker(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(classLoader, parameterTypes).getInvoker()
        }

        @JvmStatic
        fun Class<*>.getInvoker(
            vararg parameterTypes: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(parameterTypes).getInvoker()
        }

        @JvmStatic
        fun Constructor<*>.getInvoker(): XposedInterface.CtorInvoker<*> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        // ------------------------------ deoptimize --------------------------------

        @JvmStatic
        @JvmOverloads
        fun String.deoptimize(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.findMethod(classLoader, methodName, parameterTypes).deoptimize()
        }

        @JvmStatic
        fun Class<*>.deoptimize(
            methodName: String,
            vararg parameterTypes: Any
        ) {
            this.findMethod(methodName, parameterTypes).deoptimize()
        }

        @JvmStatic
        @JvmOverloads
        fun String.deoptimize(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ) {
            this.findConstructor(classLoader, parameterTypes).deoptimize()
        }

        @JvmStatic
        fun Class<*>.deoptimize(
            vararg parameterTypes: Any
        ) {
            this.findConstructor(parameterTypes).deoptimize()
        }

        @JvmStatic
        fun Executable.deoptimize() {
            ModuleData.getWrapper().deoptimize(this)
        }

        // ----------------------------------- res ---------------------------------------
        @JvmStatic
        fun createFakeResId(resName: String): Int {
            return ResInjectTool.createFakeResId(resName)
        }

        @JvmStatic
        fun createFakeResId(resources: Resources, @IdRes resId: Int): Int {
            return ResInjectTool.createFakeResId(resources, resId)
        }

        @JvmStatic
        fun setResReplacement(packageName: String, type: String, resName: String, replacementResId: Int) {
            ResInjectTool.setResReplacement(packageName, type, resName, replacementResId)
        }

        @JvmStatic
        fun setDensityReplacement(packageName: String, type: String, resName: String, replacementResValue: Float) {
            ResInjectTool.setDensityReplacement(packageName, type, resName, replacementResValue)
        }

        @JvmStatic
        fun setObjectReplacement(packageName: String, type: String, resName: String, replacementResValue: Any?) {
            ResInjectTool.setObjectReplacement(packageName, type, resName, replacementResValue)
        }
        // -------------------------------- prefs --------------------------------------

        @JvmStatic
        @JvmOverloads
        fun Context.prefs(
            prefsName: String = ""
        ): IPrefsApply {
            return PrefsTool.prefs(this, prefsName)
        }

        @JvmStatic
        @JvmOverloads
        fun prefs(
            prefsName: String = ""
        ): IPrefsApply {
            return PrefsTool.prefs(prefsName)
        }

        @JvmStatic
        @JvmOverloads
        fun asyncPrefs(
            prefsName: String = "",
            asyncPrefs: IAsyncPrefs
        ) {
            PrefsTool.asyncPrefs(prefsName, asyncPrefs)
        }

        // -------------------------------- other -------------------------------
        @JvmStatic
        fun getStackTrace(): String {
            return LogExpand.getStackTrace()
        }

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

        @JvmStatic
        @JvmOverloads
        fun getParameterTypes(
            classLoader: ClassLoader? = ModuleData.getClassLoader(),
            vararg parameterTypes: Any
        ): Array<Class<*>> {
            val classes = mutableListOf<Class<*>>()
            for (any in parameterTypes) {
                Objects.requireNonNull(any, "parameter types must not be null.")
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