package com.hchen.hooktool.hook;

import androidx.annotation.NonNull;

import java.util.Objects;

import io.github.libxposed.api.XposedInterface;

public class HookBridge {
    private final XposedInterface.HookBuilder builder;

    public HookBridge(@NonNull XposedInterface.HookBuilder builder) {
        this.builder = builder;
    }

    public HookBridge setPriority(int priority) {
        Objects.requireNonNull(builder);
        builder.setPriority(priority);
        return this;
    }

    public XposedInterface.HookHandle intercept(@NonNull AbsHook absHook) {
        Objects.requireNonNull(builder);
        XposedInterface.HookHandle handle = builder.intercept(new XposedInterface.Hooker() {
            @Override
            public Object intercept(@NonNull XposedInterface.Chain chain) throws Throwable {
                absHook.setChain(chain);
                absHook.reset();

                try {
                    absHook.before();
                } catch (Throwable throwable) {
                    if (!absHook.onThrow(AbsHook.StageEnum.BEFORE, throwable)) {
                        throw throwable;
                    }
                }

                try {
                    absHook.callProceed();
                } catch (Throwable throwable) {
                    if (!absHook.onThrow(AbsHook.StageEnum.PROCEED, throwable)) {
                        throw throwable;
                    }
                }

                try {
                    absHook.after();
                } catch (Throwable throwable) {
                    if (!absHook.onThrow(AbsHook.StageEnum.AFTER, throwable)) {
                        throw throwable;
                    }
                }

                return absHook.getResult();
            }
        });
        absHook.setHandle(handle);
        return handle;
    }
}
