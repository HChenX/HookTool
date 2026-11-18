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
package com.hchen.hooktool.kt.internal

import com.hchen.hooktool.kt.ClassScope
import com.hchen.hooktool.kt.HookGroup
import com.hchen.hooktool.kt.MissingPolicy
import com.hchen.hooktool.reflect.ReflectException

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

import java.lang.reflect.Method

/**
 * DSL 声明阶段的测试。
 *
 * @author 焕晨HChen
 */
class DslSpecTest {

    private fun spec(block: ClassScope.() -> Unit): ClassSpec {
        val spec = ClassSpec("com.example.Target", null, null)
        ClassScope(spec).block()
        return spec
    }

    @Test
    fun recordsMethodWithExactParams() {
        val spec = spec {
            method("run", Int::class, String::class) { }
        }
        assertEquals(1, spec.members.size)
        val member = spec.members[0]
        assertEquals(MemberKind.METHOD, member.kind)
        assertEquals("run", member.name)
        assertEquals(2, member.paramTokens.size)
        assertTrue(member.label.contains("#run"))
    }

    @Test
    fun recordsConstructorAndClinit() {
        val spec = spec {
            constructor(String::class) { }
            classInitializer { }
        }
        assertEquals(MemberKind.CONSTRUCTOR, spec.members[0].kind)
        assertTrue(spec.members[0].label.contains("<init>"))
        assertEquals(MemberKind.CLINIT, spec.members[1].kind)
    }

    @Test
    fun recordsBatchDeclarations() {
        val spec = spec {
            allMethods { }
            methods({ method: Method -> method.name.startsWith("on") }) { }
        }
        assertEquals(MemberKind.ALL_METHODS, spec.members[0].kind)
        assertEquals(MemberKind.PREDICATE_METHODS, spec.members[1].kind)
        assertNotNull(spec.members[1].predicate)
        assertTrue(spec.members[1].predicate!!.invoke(stubMethod("onCreate")))
        assertFalse(spec.members[1].predicate!!.invoke(stubMethod("dispatch")))
    }

    @Test
    fun defaultMissingPolicyIsStrict() {
        val spec = spec { method("a") { } }
        assertEquals(MissingPolicy.STRICT, spec.missingPolicy)
    }

    @Test
    fun missingPolicyCanBeOverriddenAnywhere() {
        val spec = spec {
            method("a") { }
            onMissing(MissingPolicy.REPORT)
        }
        assertEquals(MissingPolicy.REPORT, spec.missingPolicy)
    }

    @Test
    fun recordsStageBlocksAndOptions() {
        val spec = spec {
            method("a") {
                before { }
                proceed { null }
                after { }
                onThrow { false }
                id("hooked")
                priority(10)
                logInstall()
                optional()
            }
        }
        val member = spec.members[0]
        assertNotNull(member.before)
        assertNotNull(member.proceed)
        assertNotNull(member.after)
        assertNotNull(member.onThrow)
        assertEquals("hooked", member.id)
        assertEquals(10, member.priority)
        assertTrue(member.logInstall)
        assertTrue(member.optional)
    }

    @Test
    fun recordsReloadHandlers() {
        val spec = spec {
            method("a") { }
            onReloading { it["x"] = 1 }
            onReloaded { }
        }
        assertNotNull(spec.onReloading)
        assertNotNull(spec.onReloaded)
    }

    @Test
    fun groupRunsSaveHandlerOnceAndPrefixesKeys() {
        val group = HookGroup("test")
        var saves = 0
        group.setReloadHandlers({ state ->
            saves++
            state["count"] = 7
        }, null)

        val state = HashMap<String, Any?>()
        group.saveState(state)
        group.saveState(state)

        assertEquals(1, saves)
        assertEquals(7, state["dsl:test:count"])
    }

    @Test
    fun groupRunsRestoreHandlerOnceAndStripsPrefix() {
        val group = HookGroup("test")
        var restores = 0
        var observed: Any? = null
        group.setReloadHandlers(null, { state ->
            restores++
            observed = state["count"]
        })

        group.restoreState(mapOf("dsl:test:count" to 7))
        group.restoreState(mapOf("dsl:test:count" to 7))

        assertEquals(1, restores)
        assertEquals(7, observed)
    }

    @Test
    fun groupIgnoresStateOfOtherGroups() {
        val group = HookGroup("test")
        var observed: Any? = null
        group.setReloadHandlers(null, { state ->
            observed = state["count"]
        })

        group.restoreState(mapOf("dsl:other:count" to 7))
        assertEquals(null, observed)
    }

    @Test
    fun optionalMemberIsSkippedSilently() {
        val spec = ClassSpec(null, Stub::class.java, null)
        ClassScope(spec).method("absent") { optional() }

        val group = Installer.install(spec)

        assertTrue(group.report().installed.isEmpty())
        assertTrue(group.report().missing.isEmpty())
    }

    @Test
    fun reportingPolicyRecordsMissingMember() {
        val spec = ClassSpec(null, Stub::class.java, null)
        ClassScope(spec).apply {
            onMissing(MissingPolicy.REPORT)
            method("absent") { }
        }

        val group = Installer.install(spec)

        assertEquals(1, group.report().missing.size)
    }

    @Test
    fun strictPolicyRejectsMissingMember() {
        val spec = ClassSpec(null, Stub::class.java, null)
        ClassScope(spec).method("absent") { }

        val error = assertThrows(ReflectException::class.java) { Installer.install(spec) }

        assertEquals(ReflectException.Kind.METHOD_NOT_FOUND, error.kind())
    }

    @Test
    fun reportStartsEmpty() {
        val group = HookGroup("test")
        assertTrue(group.report().installed.isEmpty())
        assertTrue(group.report().missing.isEmpty())
    }

    private fun stubMethod(name: String): Method =
        Stub::class.java.getDeclaredMethod(name)

    @Suppress("unused")
    private class Stub {
        fun onCreate() = Unit

        fun dispatch() = Unit
    }
}