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
package com.hchen.hooktool.hook;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Executable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.github.libxposed.api.XposedInterface;

/**
 * Hook 状态机测试。
 * <p>
 * 通过伪造的 HookBuilder 与 Chain 驱动 AbsHook 的完整生命周期。
 *
 * @author 焕晨HChen
 */
class AbsHookTest {

    static class Target {
        public String run(String value) {
            return "original:" + value;
        }
    }

    private static Executable targetMethod() throws NoSuchMethodException {
        return Target.class.getDeclaredMethod("run", String.class);
    }

    // ------------------------------ framework fakes ------------------------------

    static final class FakeChain implements XposedInterface.Chain {
        private final Executable mExecutable;
        private Object mThisObject;
        private Object[] mArgs;
        private final Object mResult = "original";
        boolean proceeded;
        Object[] proceededArgs;

        FakeChain(Executable executable, Object thisObject, Object... args) {
            mExecutable = executable;
            mThisObject = thisObject;
            mArgs = args;
        }

        @Override
        public Executable getExecutable() {
            return mExecutable;
        }

        @Override
        public Object getThisObject() {
            return mThisObject;
        }

        @Override
        public List<Object> getArgs() {
            return Arrays.asList(mArgs);
        }

        @Override
        public Object getArg(int index) {
            return mArgs[index];
        }

        @Override
        public Object proceed() {
            proceeded = true;
            proceededArgs = mArgs;
            return mResult;
        }

        @Override
        public Object proceed(Object[] args) {
            proceeded = true;
            proceededArgs = args;
            mArgs = args;
            return mResult;
        }

        @Override
        public Object proceedWith(Object thisObject) {
            mThisObject = thisObject;
            return proceed();
        }

        @Override
        public Object proceedWith(Object thisObject, Object[] args) {
            mThisObject = thisObject;
            return proceed(args);
        }
    }

    static final class FakeHandle implements XposedInterface.HookHandle {
        private final Executable mExecutable;

        FakeHandle(Executable executable) {
            mExecutable = executable;
        }

        @Override
        public Executable getExecutable() {
            return mExecutable;
        }

        @Override
        public void unhook() {
            // no-op
        }

        @Override
        public String getId() {
            return null;
        }

        @Override
        public XposedInterface.HookHandle replaceHook(XposedInterface.Hooker hooker) {
            return this;
        }
    }

    static final class FakeBuilder implements XposedInterface.HookBuilder {
        private final Executable mExecutable;
        XposedInterface.Hooker hooker;
        int priority = XposedInterface.PRIORITY_DEFAULT;
        XposedInterface.ExceptionMode mode;
        String id;

        FakeBuilder(Executable executable) {
            mExecutable = executable;
        }

        @Override
        public XposedInterface.HookBuilder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        @Override
        public XposedInterface.HookBuilder setExceptionMode(XposedInterface.ExceptionMode mode) {
            this.mode = mode;
            return this;
        }

        @Override
        public XposedInterface.HookHandle intercept(XposedInterface.Hooker hooker) {
            this.hooker = hooker;
            return new FakeHandle(mExecutable);
        }

        @Override
        public XposedInterface.HookBuilder setId(String id) {
            this.id = id;
            return this;
        }
    }

    private static FakeBuilder install(AbsHook hook) throws NoSuchMethodException {
        FakeBuilder builder = new FakeBuilder(targetMethod());
        new HookBridge(builder).intercept(hook);
        return builder;
    }

    // -------------------------------- ordering --------------------------------

    @Test
    void runsBeforeProceedAfterInOrder() throws Throwable {
        List<String> order = new ArrayList<>();
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                order.add("before");
            }

            @Override
            public Object proceed(XposedInterface.Chain chain) throws Throwable {
                order.add("proceed");
                return super.proceed(chain);
            }

            @Override
            public void after() {
                order.add("after");
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        Object result = builder.hooker.intercept(chain);

        assertEquals(Arrays.asList("before", "proceed", "after"), order);
        assertEquals("original", result);
        assertTrue(chain.proceeded);
    }

    @Test
    void beforeResultSkipsOriginalAndAfter() throws Throwable {
        List<String> order = new ArrayList<>();
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                order.add("before");
                setResult("short-circuit");
            }

            @Override
            public void after() {
                order.add("after");
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        Object result = builder.hooker.intercept(chain);

        assertEquals(Arrays.asList("before"), order);
        assertEquals("short-circuit", result);
        assertTrue(!chain.proceeded);
    }

    @Test
    void proceedOverrideKeepsAfterRunning() throws Throwable {
        List<String> order = new ArrayList<>();
        AbsHook hook = new AbsHook() {
            @Override
            public Object proceed(XposedInterface.Chain chain) {
                order.add("proceed");
                return "replaced";
            }

            @Override
            public void after() {
                order.add("after");
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        Object result = builder.hooker.intercept(chain);

        assertEquals(Arrays.asList("proceed", "after"), order);
        assertEquals("replaced", result);
        assertTrue(!chain.proceeded);
    }

    // -------------------------------- arguments --------------------------------

    @Test
    void setArgIsForwardedToProceed() throws Throwable {
        final Object[] observed = new Object[1];
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                setArg(0, "patched");
            }

            @Override
            public void after() {
                observed[0] = getArg(0);
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        builder.hooker.intercept(chain);

        assertArrayEquals(new Object[]{"patched"}, chain.proceededArgs);
        assertEquals("patched", observed[0]);
    }

    @Test
    void getArgsReturnsCopy() throws Throwable {
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                Object[] copy = getArgs();
                copy[0] = "ignored";
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        builder.hooker.intercept(chain);

        assertEquals("x", chain.proceededArgs[0]);
    }

    // -------------------------------- failures ---------------------------------

    @Test
    void unconsumedProceedFailureStillRunsAfter() throws Throwable {
        List<String> order = new ArrayList<>();
        AbsHook hook = new AbsHook() {
            @Override
            public Object proceed(XposedInterface.Chain chain) {
                order.add("proceed");
                throw new IllegalStateException("boom");
            }

            @Override
            public void after() {
                order.add("after");
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        IllegalStateException error = assertThrows(IllegalStateException.class,
            () -> builder.hooker.intercept(chain));
        assertEquals("boom", error.getMessage());
        assertEquals(Arrays.asList("proceed", "after"), order);
    }

    @Test
    void consumedProceedFailureIsSwallowed() throws Throwable {
        AbsHook hook = new AbsHook() {
            @Override
            public Object proceed(XposedInterface.Chain chain) {
                throw new IllegalStateException("boom");
            }

            @Override
            public boolean onThrow(StageEnum stage, Throwable e) {
                return stage == StageEnum.PROCEED;
            }

            @Override
            public void after() {
                setResult("recovered");
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        Object result = builder.hooker.intercept(chain);
        assertEquals("recovered", result);
    }

    @Test
    void beforeFailureSkipsAfter() throws Throwable {
        List<String> order = new ArrayList<>();
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                order.add("before");
                throw new IllegalStateException("boom");
            }

            @Override
            public void after() {
                order.add("after");
            }
        };
        FakeBuilder builder = install(hook);
        FakeChain chain = new FakeChain(targetMethod(), this, "x");

        assertThrows(IllegalStateException.class, () -> builder.hooker.intercept(chain));
        assertEquals(Arrays.asList("before"), order);
    }

    // ------------------------------- frame data -------------------------------

    @Test
    void frameDataTravelsFromBeforeToAfter() throws Throwable {
        final Object[] seen = new Object[1];
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                setFrameData("payload");
            }

            @Override
            public void after() {
                seen[0] = getFrameData();
            }
        };
        FakeBuilder builder = install(hook);
        builder.hooker.intercept(new FakeChain(targetMethod(), this, "x"));

        assertEquals("payload", seen[0]);
    }

    @Test
    void frameDataIsPerInvocation() throws Throwable {
        final List<Object> observed = new ArrayList<>();
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                observed.add(getFrameData());
                setFrameData("payload");
            }
        };
        FakeBuilder builder = install(hook);
        builder.hooker.intercept(new FakeChain(targetMethod(), this, "a"));
        builder.hooker.intercept(new FakeChain(targetMethod(), this, "b"));

        assertEquals(Arrays.asList(null, null), observed);
    }

    @Test
    void frameDataSurvivesNestedInvocation() throws Throwable {
        final List<Object> observed = new ArrayList<>();
        final FakeBuilder[] holder = new FakeBuilder[1];
        final int[] depth = {0};
        AbsHook hook = new AbsHook() {
            @Override
            public void before() {
                if (depth[0] == 0) {
                    setFrameData("outer");
                    depth[0]++;
                    try {
                        holder[0].hooker.intercept(new FakeChain(targetMethod(), this, "nested"));
                    } catch (Throwable ignored) {
                        // 不应发生
                    }
                    observed.add(getFrameData());
                } else {
                    setFrameData("inner");
                }
            }

            @Override
            public void after() {
                observed.add(getFrameData());
            }
        };
        FakeBuilder builder = install(hook);
        holder[0] = builder;
        builder.hooker.intercept(new FakeChain(targetMethod(), this, "outer"));

        assertEquals(Arrays.asList("inner", "outer", "outer"), observed);
    }

    @Test
    void unhookSelfFailsWithoutHandle() {
        AbsHook hook = new AbsHook() {
        };
        assertThrows(IllegalStateException.class, hook::unhookSelf);
        assertEquals(0, hook.getHookHandles().length);
    }

    @Test
    void observeCallDescribesInvocation() throws Throwable {
        final String[] text = new String[1];
        AbsHook hook = new AbsHook() {
            @Override
            public void after() {
                text[0] = observeCall();
            }
        };
        FakeBuilder builder = install(hook);
        builder.hooker.intercept(new FakeChain(targetMethod(), this, "x"));

        assertTrue(text[0].contains("Target"));
        assertTrue(text[0].contains("run"));
    }
}