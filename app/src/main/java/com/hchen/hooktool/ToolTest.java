package com.hchen.hooktool;

import com.hchen.hooktool.hook.IHook;

public class ToolTest extends BaseHC {
    @Override
    protected void init() {
        new IHook() {
            @Override
            public void before() {
            }
        };

        buildChain("").findMethod("").doNothing();
    }
}
