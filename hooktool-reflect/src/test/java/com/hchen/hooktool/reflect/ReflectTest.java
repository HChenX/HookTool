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
package com.hchen.hooktool.reflect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 反射工具类的行为测试。
 *
 * @author 焕晨HChen
 */
class ReflectTest {

    // ------------------------------ class ------------------------------

    @Test
    void findsClassBySimpleName() {
        assertEquals(Fixtures.Child.class, Reflect.findClass(Fixtures.Child.class.getName()));
    }

    @Test
    void findsInnerClassUsingDotNotation() {
        assertEquals(Fixtures.Child.Inner.class,
            Reflect.findClass(Fixtures.Child.class.getName() + ".Inner"));
    }

    @Test
    void findsPrimitiveByName() {
        assertEquals(int.class, Reflect.findClass("int"));
        assertEquals(void.class, Reflect.findClass("void"));
    }

    @Test
    void findsArrayByName() {
        assertEquals(int[].class, Reflect.findClass("int[]"));
        assertEquals(String[].class, Reflect.findClass("java.lang.String[]"));
    }

    @Test
    void missingClassCarriesKind() {
        ReflectException error = assertThrows(ReflectException.class,
            () -> Reflect.findClass("com.hchen.hooktool.reflect.DoesNotExist"));
        assertEquals(ReflectException.Kind.CLASS_NOT_FOUND, error.kind());
        assertNull(Reflect.findClassIfExists("com.hchen.hooktool.reflect.DoesNotExist"));
    }

    @Test
    void reflectEnvDefaultLoaderIsUsed() {
        ClassLoader previous = ReflectEnv.defaultClassLoaderOrNull();
        try {
            ReflectEnv.setDefaultClassLoader(getClass().getClassLoader());
            assertSame(getClass().getClassLoader(), ReflectEnv.defaultClassLoader());
        } finally {
            ReflectEnv.setDefaultClassLoader(previous);
        }
    }

    // ------------------------------ field ------------------------------

    @Test
    void readsInheritedPrivateField() throws Exception {
        Fixtures.Child child = new Fixtures.Child();
        Field field = Reflect.findField(Fixtures.Child.class, "parentField");
        assertEquals("parent", field.get(child));
    }

    @Test
    void readsInterfaceConstant() {
        assertEquals(42, Reflect.getStaticObjectField(Fixtures.Child.class, "INTERFACE_CONST"));
    }

    @Test
    void writesInstanceField() {
        Fixtures.Child child = new Fixtures.Child();
        Reflect.setObjectField(child, "secret", "changed");
        assertEquals("changed", Reflect.getObjectField(child, "secret"));
    }

    @Test
    void readsStaticField() {
        assertEquals("const", Reflect.getStaticObjectField(Fixtures.Child.class, "CONST"));
    }

    @Test
    void rejectsInstanceFieldAsStatic() {
        assertThrows(IllegalArgumentException.class,
            () -> Reflect.getStaticObjectField(Fixtures.Child.class, "count"));
    }

    @Test
    void missingFieldCarriesKind() {
        ReflectException error = assertThrows(ReflectException.class,
            () -> Reflect.findField(Fixtures.Child.class, "nope"));
        assertEquals(ReflectException.Kind.FIELD_NOT_FOUND, error.kind());
        assertNull(Reflect.findFieldIfExists(Fixtures.Child.class, "nope"));
    }

    // --------------------------- method exact ---------------------------

    @Test
    void findsMethodWithClassTokens() {
        Method method = Reflect.findMethodExact(Fixtures.Child.class, "greet", String.class);
        assertNotNull(method);
        assertTrue(method.isAccessible());
    }

    @Test
    void findsMethodWithNameTokens() {
        Method method = Reflect.findMethodExact(Fixtures.Child.class, "greet", "java.lang.String");
        assertEquals("greet", method.getName());
    }

    @Test
    void findsInheritedMethod() {
        Method method = Reflect.findMethodExact(Fixtures.Child.class, "overload", String.class);
        assertEquals(Fixtures.Parent.class, method.getDeclaringClass());
    }

    @Test
    void findsInterfaceDefaultMethod() {
        Method method = Reflect.findMethodExact(Fixtures.Child.class, "hello");
        assertEquals("default", Reflect.callMethod(new Fixtures.Child(), "hello"));
        assertEquals(Fixtures.HasDefault.class, method.getDeclaringClass());
    }

    @Test
    void missingMethodCarriesKind() {
        ReflectException error = assertThrows(ReflectException.class,
            () -> Reflect.findMethodExact(Fixtures.Child.class, "nope"));
        assertEquals(ReflectException.Kind.METHOD_NOT_FOUND, error.kind());
        assertNull(Reflect.findMethodExactIfExists(Fixtures.Child.class, "nope"));
    }

    @Test
    void lookupReportsReasonInsteadOfThrowing() {
        Lookup<Method> lookup = Reflect.lookupMethod(Fixtures.Child.class, "nope");
        assertFalse(lookup.isPresent());
        assertEquals(ReflectException.Kind.METHOD_NOT_FOUND, lookup.error().kind());
        assertNull(lookup.getOrNull());
        assertThrows(ReflectException.class, lookup::get);
    }

    // ---------------------------- method best ----------------------------

    @Test
    void prefersPrimitiveOverloadOverObject() {
        Method method = Reflect.findMethodByArgs(Fixtures.Child.class, "overload", 1);
        assertEquals(int.class, method.getParameterTypes()[0]);
    }

    @Test
    void prefersWideningOverBoxingToObject() {
        Method method = Reflect.findMethodBestMatch(Fixtures.Child.class, "overload", long.class);
        assertEquals(long.class, method.getParameterTypes()[0]);
    }

    @Test
    void nullWildcardIsAnyReferenceType() {
        Method method = Reflect.findMethodBestMatch(Fixtures.Child.class, "overload",
            new Class<?>[]{null});
        assertNotNull(method);
        assertEquals("overload", method.getName());
    }

    @Test
    void fillsNullSlotsFromArguments() {
        Method method = Reflect.findMethodBestMatch(Fixtures.Child.class, "overload",
            new Class<?>[]{null}, new Object[]{"x"});
        assertEquals(String.class, method.getParameterTypes()[0]);
    }

    // --------------------------- constructors ----------------------------

    @Test
    void findsPrivateConstructorBestMatch() {
        Constructor<?> constructor = Reflect.findConstructorByArgs(Fixtures.Child.class, 5);
        assertEquals(int.class, constructor.getParameterTypes()[0]);
    }

    @Test
    void missingConstructorCarriesKind() {
        ReflectException error = assertThrows(ReflectException.class,
            () -> Reflect.findConstructorExact(Fixtures.Child.class, String.class));
        assertEquals(ReflectException.Kind.CONSTRUCTOR_NOT_FOUND, error.kind());
    }

    @Test
    void honoursParameterTypeTokens() {
        ReflectException error = assertThrows(ReflectException.class,
            () -> Reflect.findMethodExact(Fixtures.Child.class, "greet", 42));
        assertEquals(ReflectException.Kind.PARAM_MISMATCH, error.kind());
    }

    // ------------------------------ calls -------------------------------

    @Test
    void callsInstanceMethod() {
        assertEquals("hi bob", Reflect.callMethod(new Fixtures.Child(), "greet", "bob"));
    }

    @Test
    void callsStaticMethod() {
        assertEquals(3, Reflect.callStaticMethod(Fixtures.Child.class, "add", 1, 2));
    }

    @Test
    void rejectsStaticMethodViaInstanceCall() {
        assertThrows(IllegalArgumentException.class,
            () -> Reflect.callMethod(new Fixtures.Child(), "add", 1, 2));
    }

    @Test
    void unwrapsInvocationTargetException() {
        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> Reflect.callMethod(new Fixtures.Child(), "throwIt"));
        assertEquals("boom", error.getMessage());
    }

    @Test
    void createsInstance() {
        Object instance = Reflect.newInstance(Fixtures.Child.class);
        assertTrue(instance instanceof Fixtures.Child);
        assertEquals("hi x", Reflect.callMethod(instance, "greet", "x"));
    }

    // ------------------------- additional fields ------------------------

    @Test
    void attachesAdditionalFields() {
        Fixtures.Child child = new Fixtures.Child();
        assertNull(Reflect.setAdditionalInstanceField(child, "k", "v"));
        assertEquals("v", Reflect.getAdditionalInstanceField(child, "k"));
        assertEquals("v", Reflect.removeAdditionalInstanceField(child, "k"));
        assertNull(Reflect.getAdditionalInstanceField(child, "k"));
    }

    @Test
    void attachesAdditionalStaticFields() {
        assertNull(Reflect.setAdditionalStaticField(Fixtures.Child.class, "s", 1));
        assertEquals(1, Reflect.getAdditionalStaticField(Fixtures.Child.class, "s"));
        assertEquals(1, Reflect.removeAdditionalStaticField(Fixtures.Child.class, "s"));
    }

    // --------------------------- resolved members ------------------------

    @Test
    void callsWithResolvedMethod() {
        Fixtures.Child child = new Fixtures.Child();
        Method greet = Reflect.findMethodExact(Fixtures.Child.class, "greet", String.class);
        assertEquals("hi world", Reflect.callMethod(greet, child, "world"));

        Method add = Reflect.findMethodExact(Fixtures.Child.class, "add", int.class, int.class);
        assertEquals(3, Reflect.callStaticMethod(add, 1, 2));
    }

    @Test
    void createsInstanceWithResolvedConstructor() {
        Constructor<?> constructor = Reflect.findConstructorExact(Fixtures.Child.class, int.class);
        Fixtures.Child child = (Fixtures.Child) Reflect.newInstance(constructor, 5);
        assertEquals(5, child.count);
    }

    @Test
    void accessesFieldsWithResolvedField() {
        Fixtures.Child child = new Fixtures.Child();
        Field count = Reflect.findField(Fixtures.Child.class, "count");
        assertEquals(7, Reflect.getObjectField(count, child));

        Reflect.setObjectField(count, child, 9);
        assertEquals(9, child.count);

        Field constant = Reflect.findField(Fixtures.Child.class, "CONST");
        assertEquals("const", Reflect.getStaticObjectField(constant));
    }

    @Test
    void rejectsFieldOfWrongKind() {
        Field count = Reflect.findField(Fixtures.Child.class, "count");
        assertThrows(IllegalArgumentException.class, () -> Reflect.getStaticObjectField(count));

        Field constant = Reflect.findField(Fixtures.Child.class, "CONST");
        assertThrows(IllegalArgumentException.class, () -> Reflect.getObjectField(constant, new Fixtures.Child()));
    }

    // ------------------------------ caching ------------------------------

    @Test
    void cachesResolvedMembers() {
        Method first = Reflect.findMethodExact(Fixtures.Child.class, "greet", String.class);
        Method second = Reflect.findMethodExact(Fixtures.Child.class, "greet", String.class);
        assertSame(first, second);
    }

    @Test
    void cachesNegativeResults() {
        assertThrows(ReflectException.class,
            () -> Reflect.findMethodExact(Fixtures.Child.class, "absent"));
        assertThrows(ReflectException.class,
            () -> Reflect.findMethodExact(Fixtures.Child.class, "absent"));
    }
}