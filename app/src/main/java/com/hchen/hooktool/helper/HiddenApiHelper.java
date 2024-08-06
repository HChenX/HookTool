package com.hchen.hooktool.helper;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandleInfo;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Executable;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import sun.misc.Unsafe;

/**
 * 代码来自
 * <a href="https://github.com/LSPosed/AndroidHiddenApiBypass">AndroidHiddenApiBypass</a>
 * 项目， 本项目简单使用其部分代码。
 *
 * @author LSPass
 * @noinspection JavaReflectionMemberAccess, unused
 */
@SuppressLint("DiscouragedPrivateApi")
public class HiddenApiHelper {
    private static final String TAG = "HiddenApiHelper";
    private static final Unsafe unsafe;
    private static final long artOffset;
    private static final long infoOffset;
    private static final long methodsOffset;
    private static final long memberOffset;
    private static final long artMethodSize;
    private static final long artMethodBias;

    static {
        try {
            unsafe = (Unsafe) Unsafe.class.getDeclaredMethod("getUnsafe").invoke(null);
            assert unsafe != null;
            artOffset = unsafe.objectFieldOffset(Helper.MethodHandle.class.getDeclaredField("artFieldOrMethod"));
            infoOffset = unsafe.objectFieldOffset(Helper.MethodHandleImpl.class.getDeclaredField("info"));
            methodsOffset = unsafe.objectFieldOffset(Helper.Class.class.getDeclaredField("methods"));
            memberOffset = unsafe.objectFieldOffset(Helper.HandleInfo.class.getDeclaredField("member"));
            Method mA = Helper.NeverCall.class.getDeclaredMethod("a");
            Method mB = Helper.NeverCall.class.getDeclaredMethod("b");
            mA.setAccessible(true);
            mB.setAccessible(true);
            MethodHandle mhA = MethodHandles.lookup().unreflect(mA);
            MethodHandle mhB = MethodHandles.lookup().unreflect(mB);
            long aAddr = unsafe.getLong(mhA, artOffset);
            long bAddr = unsafe.getLong(mhB, artOffset);
            long aMethods = unsafe.getLong(Helper.NeverCall.class, methodsOffset);
            artMethodSize = bAddr - aAddr;
            artMethodBias = aAddr - aMethods - artMethodSize;
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Method getDeclaredMethod(Class<?> clazz, String methodName, Class<?>... parameterTypes) throws NoSuchMethodException {
        List<Executable> methods = getDeclaredMethods(clazz);
        allMethods:
        for (Executable method : methods) {
            if (!method.getName().equals(methodName)) continue;
            if (!(method instanceof Method)) continue;
            Class<?>[] expectedTypes = method.getParameterTypes();
            if (expectedTypes.length != parameterTypes.length) continue;
            for (int i = 0; i < parameterTypes.length; ++i) {
                if (parameterTypes[i] != expectedTypes[i]) continue allMethods;
            }
            return (Method) method;
        }
        throw new NoSuchMethodException("Cannot find matching method");
    }

    public static List<Executable> getDeclaredMethods(@NonNull Class<?> clazz) {
        ArrayList<Executable> list = new ArrayList<>();
        if (clazz.isPrimitive() || clazz.isArray()) return list;
        MethodHandle mh;
        try {
            Method mA = Helper.NeverCall.class.getDeclaredMethod("a");
            mA.setAccessible(true);
            mh = MethodHandles.lookup().unreflect(mA);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            return list;
        }
        long methods = unsafe.getLong(clazz, methodsOffset);
        if (methods == 0) return list;
        int numMethods = unsafe.getInt(methods);
        for (int i = 0; i < numMethods; i++) {
            long method = methods + i * artMethodSize + artMethodBias;
            unsafe.putLong(mh, artOffset, method);
            unsafe.putObject(mh, infoOffset, null);
            try {
                MethodHandles.lookup().revealDirect(mh);
            } catch (Throwable ignored) {
            }
            MethodHandleInfo info = (MethodHandleInfo) unsafe.getObject(mh, infoOffset);
            Executable member = (Executable) unsafe.getObject(info, memberOffset);
            list.add(member);
        }
        return list;
    }

    /**
     * @author LSPass
     */
    private static class Helper {
        static public class MethodHandle {
            protected final int handleKind = 0;
            // The ArtMethod* or ArtField* associated with this method handle (used by the runtime).
            protected final long artFieldOrMethod = 0;
            private final MethodType type = null;
            private MethodType nominalType;
            private MethodHandle cachedSpreadInvoker;
        }

        static final public class MethodHandleImpl extends MethodHandle {
            private final MethodHandleInfo info = null;
        }

        static final public class HandleInfo {
            private final Member member = null;
            private final MethodHandle handle = null;
        }

        static final public class Class {
            private transient ClassLoader classLoader;
            private transient java.lang.Class<?> componentType;
            private transient Object dexCache;
            private transient Object extData;
            private transient Object[] ifTable;
            private transient String name;
            private transient java.lang.Class<?> superClass;
            private transient Object vtable;
            private transient long iFields;
            private transient long methods;
            private transient long sFields;
            private transient int accessFlags;
            private transient int classFlags;
            private transient int classSize;
            private transient int clinitThreadId;
            private transient int dexClassDefIndex;
            private transient volatile int dexTypeIndex;
            private transient int numReferenceInstanceFields;
            private transient int numReferenceStaticFields;
            private transient int objectSize;
            private transient int objectSizeAllocFastPath;
            private transient int primitiveType;
            private transient int referenceInstanceOffsets;
            private transient int status;
            private transient short copiedMethodsOffset;
            private transient short virtualMethodsOffset;
        }

        static public class AccessibleObject {
            private boolean override;
        }

        static final public class Executable extends AccessibleObject {
            private Class declaringClass;
            private Class declaringClassOfOverriddenMethod;
            private Object[] parameters;
            private long artMethod;
            private int accessFlags;
        }

        @SuppressWarnings("EmptyMethod")
        public static class NeverCall {
            private static int s;
            private static int t;
            private int i;
            private int j;

            private static void a() {
            }

            private static void b() {
            }
        }

        public static class InvokeStub {
            private InvokeStub(Object... args) {
                throw new IllegalStateException("Failed to new a instance");
            }

            private static Object invoke(Object... args) {
                throw new IllegalStateException("Failed to invoke the method");
            }
        }
    }
}
