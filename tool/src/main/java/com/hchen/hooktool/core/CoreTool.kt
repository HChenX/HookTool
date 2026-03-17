package com.hchen.hooktool.core

import android.content.Context
import android.content.res.Resources
import androidx.annotation.IdRes
import com.hchen.hooktool.ModuleData
import com.hchen.hooktool.callback.IAsyncPrefs
import com.hchen.hooktool.callback.IPrefsApply
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
            vararg params: Any = emptyArray()
        ): Boolean {
            return this.findClassIfExists(classLoader)?.hasMethod(methodName, exactMatch, params) ?: false
        }

        @JvmStatic
        @JvmOverloads
        fun Class<*>.hasMethod(
            methodName: String,
            exactMatch: Boolean = true,
            vararg params: Any = emptyArray()
        ): Boolean {
            return if (exactMatch) {
                Objects.nonNull(
                    XposedHelpers.findMethodExactIfExists(this, methodName, params)
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            methodName: String,
            vararg params: Any
        ): Method {
            return XposedHelpers.findMethodExact(this, classLoader, methodName, params)
        }

        @JvmStatic
        fun Class<*>.findMethod(
            methodName: String,
            vararg params: Any
        ): Method {
            return XposedHelpers.findMethodExact(this, methodName, params)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findMethodIfExists(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            methodName: String,
            vararg params: Any
        ): Method? {
            return XposedHelpers.findMethodExactIfExists(this, classLoader, methodName, params)
        }

        @JvmStatic
        fun Class<*>.findMethodIfExists(
            methodName: String,
            vararg params: Any
        ): Method? {
            return XposedHelpers.findMethodExactIfExists(this, methodName, params)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findAllMethod(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            vararg params: Any
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findConstructorExactIfExists(this, classLoader, params)
            )
        }

        @JvmStatic
        fun Class<*>.hasConstructor(
            vararg params: Any
        ): Boolean {
            return Objects.nonNull(
                XposedHelpers.findConstructorExactIfExists(this, params)
            )
        }

        @JvmStatic
        @JvmOverloads
        fun String.findConstructor(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            vararg params: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExact(this, classLoader, params)
        }

        @JvmStatic
        fun Class<*>.findConstructor(
            vararg params: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExact(this, params)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findConstructorIfExists(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            vararg params: Any
        ): Constructor<*> {
            return XposedHelpers.findConstructorExactIfExists(this, classLoader, params)
        }

        @JvmStatic
        fun Class<*>.findConstructorIfExists(
            vararg params: Any
        ): Constructor<*>? {
            return XposedHelpers.findConstructorExactIfExists(this, params)
        }

        @JvmStatic
        @JvmOverloads
        fun String.findAllConstructor(
            classLoader: ClassLoader = ModuleData.getClassLoader()
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            paramTypes: Array<Class<*>> = emptyArray(),
            vararg params: Any
        ): Any? {
            return if (paramTypes.isEmpty()) {
                XposedHelpers.callMethod(this, methodName, params)
            } else {
                XposedHelpers.callMethod(this, methodName, paramTypes, params)
            }
        }

        @JvmStatic
        fun Any.getField(
            fieldName: String,
        ): Any? {
            return XposedHelpers.getObjectField(this, fieldName)
        }

        @JvmStatic
        fun Any.getField(
            field: Field,
        ): Any? {
            return field.run {
                this.isAccessible = true
                field.get(this@getField)
            }
        }

        @JvmStatic
        fun Any.setField(
            fieldName: String,
            value: Any?
        ) {
            XposedHelpers.setObjectField(this, fieldName, value)
        }

        @JvmStatic
        fun Any.setField(
            field: Field,
            value: Any?
        ) {
            field.isAccessible = true
            field.set(this, value)
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            paramTypes: Array<Class<*>> = emptyArray(),
            vararg params: Any
        ): Any {
            return this.findClass(classLoader).newInstance(paramTypes, params)
        }

        @JvmStatic
        @JvmOverloads
        fun Class<*>.newInstance(
            paramTypes: Array<Class<*>> = emptyArray(),
            vararg params: Any
        ): Any {
            return if (paramTypes.isEmpty()) {
                XposedHelpers.newInstance(this, params)
            } else {
                XposedHelpers.newInstance(this, paramTypes, params)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun String.callStaticMethod(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            methodName: String,
            paramTypes: Array<Class<*>> = emptyArray(),
            vararg params: Any
        ): Any? {
            return this.findClass(classLoader).callStaticMethod(methodName, paramTypes, params)
        }

        @JvmStatic
        @JvmOverloads
        fun Class<*>.callStaticMethod(
            methodName: String,
            paramTypes: Array<Class<*>> = emptyArray(),
            vararg params: Any
        ): Any? {
            return if (paramTypes.isEmpty()) {
                XposedHelpers.callStaticMethod(this, methodName, params)
            } else {
                XposedHelpers.callStaticMethod(this, methodName, paramTypes, params)
            }
        }

        @JvmStatic
        @JvmOverloads
        fun String.getStaticField(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            methodName: String,
            vararg params: Any
        ): HookBridge {
            return this.findMethod(classLoader, methodName, params).hook()
        }

        @JvmStatic
        fun Class<*>.hook(
            methodName: String,
            vararg params: Any
        ): HookBridge {
            return this.findMethod(methodName, params).hook()
        }

        @JvmStatic
        @JvmOverloads
        fun String.hook(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            vararg params: Any
        ): HookBridge {
            return this.findConstructor(classLoader, params).hook()
        }

        @JvmStatic
        fun Class<*>.hook(
            vararg params: Any
        ): HookBridge {
            return this.findConstructor(params).hook()
        }

        @JvmStatic
        fun Executable.hook(): HookBridge {
            return HookBridge(ModuleData.getWrapper().hook(this))
        }

        @JvmStatic
        @JvmOverloads
        fun String.hookClassInitializer(
            classLoader: ClassLoader = ModuleData.getClassLoader()
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
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            methodName: String,
            vararg params: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(classLoader, methodName, params).getInvoker()
        }

        @JvmStatic
        fun Class<*>.getInvoker(
            methodName: String,
            vararg params: Any
        ): XposedInterface.Invoker<*, Method> {
            return this.findMethod(methodName, params).getInvoker()
        }

        @JvmStatic
        fun Method.getInvoker(): XposedInterface.Invoker<*, Method> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        @JvmStatic
        @JvmOverloads
        fun String.getInvoker(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            vararg params: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(classLoader, params).getInvoker()
        }

        @JvmStatic
        fun Class<*>.getInvoker(
            vararg params: Any
        ): XposedInterface.CtorInvoker<*> {
            return this.findConstructor(params).getInvoker()
        }

        @JvmStatic
        fun Constructor<*>.getInvoker(): XposedInterface.CtorInvoker<*> {
            return ModuleData.getWrapper().getInvoker(this)
        }

        // ------------------------------ deoptimize --------------------------------

        @JvmStatic
        @JvmOverloads
        fun String.deoptimize(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            methodName: String,
            vararg params: Any
        ) {
            this.findMethod(classLoader, methodName, params).deoptimize()
        }

        @JvmStatic
        fun Class<*>.deoptimize(
            methodName: String,
            vararg params: Any
        ) {
            this.findMethod(methodName, params).deoptimize()
        }

        @JvmStatic
        @JvmOverloads
        fun String.deoptimize(
            classLoader: ClassLoader = ModuleData.getClassLoader(),
            vararg params: Any
        ) {
            this.findConstructor(classLoader, params).deoptimize()
        }

        @JvmStatic
        fun Class<*>.deoptimize(
            vararg params: Any
        ) {
            this.findConstructor(params).deoptimize()
        }

        @JvmStatic
        fun Executable.deoptimize() {
            ModuleData.getWrapper().deoptimize(this)
        }

        // --------------------------------- chain -----------------------------------

        @JvmStatic
        @JvmOverloads
        fun String.buildChain(
            classLoader: ClassLoader = ModuleData.getClassLoader()
        ): ChainTool {
            return ChainTool.buildChain(this, classLoader)
        }

        @JvmStatic
        fun Class<*>.buildChain(): ChainTool {
            return ChainTool.buildChain(this)
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
    }
}