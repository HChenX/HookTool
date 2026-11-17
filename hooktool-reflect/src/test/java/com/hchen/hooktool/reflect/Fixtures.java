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

/**
 * 反射测试使用的示例类型。
 *
 * @author 焕晨HChen
 */
final class Fixtures {
    private Fixtures() {
    }

    static class Parent {
        private final String parentField = "parent";
        String secret = "secret";

        String overload(Object value) {
            return "parent-object";
        }

        String overload(String value) {
            return "parent-string";
        }
    }

    static class Child extends Parent implements HasDefault {
        int count = 7;
        static final String CONST = "const";

        Child() {
        }

        private Child(int value) {
            this.count = value;
        }

        public String greet(String name) {
            return "hi " + name;
        }

        public static int add(int a, int b) {
            return a + b;
        }

        public String overload(int value) {
            return "int";
        }

        public String overload(long value) {
            return "long";
        }

        public String throwIt() {
            throw new IllegalStateException("boom");
        }

        public static class Inner {
            public String name() {
                return "inner";
            }
        }
    }

    interface HasDefault {
        int INTERFACE_CONST = 42;

        default String hello() {
            return "default";
        }
    }
}
