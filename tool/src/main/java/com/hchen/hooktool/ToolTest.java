/*
 * This file is part of HookTool.

 * HookTool is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.

 * Copyright (C) 2023-2025 HChenX
 */
package com.hchen.hooktool;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import com.hchen.hooktool.callback.IAppDataGetter;
import com.hchen.hooktool.callback.IAsyncPrefs;
import com.hchen.hooktool.callback.IExecListener;
import com.hchen.hooktool.callback.IPrefsApply;
import com.hchen.hooktool.data.AppData;
import com.hchen.hooktool.data.ShellResult;
import com.hchen.hooktool.exception.NonSingletonException;
import com.hchen.hooktool.hook.IHook;
import com.hchen.hooktool.utils.PackageTool;
import com.hchen.hooktool.utils.PrefsTool;
import com.hchen.hooktool.utils.ShellTool;

import java.util.ArrayList;
import java.util.function.Supplier;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * 测试类
 *
 * @author 焕晨HChen
 */
class ToolTest extends HCBase {
    private ToolTest() {
    }

    @Override
    protected boolean isEnabled() {
        return false; // 是否启用
    }

    @Override
    protected void init() {
        // 链式 Hook
        buildChain("com.hchen.demo")
            .findMethod("test")
            .hook(new IHook() {
                @Override
                public void before() {
                    super.before();
                }
            })
            .findMethod("test1", String.class)
            .hook(new IHook() {
                @Override
                public void after() {
                    super.after();
                }
            })
            .findConstructor()
            .returnResult(false);

        hookMethod("com.hchen.demo", "test", new IHook() {
            @Override
            public void before() {
                super.before();
            }
        }).unhook();

        // 查找模式
        findMethodPro("com.hchen.demo")
            .withMethodName("demo")
            .withParamTypes(int.class)
            .withParamCount(1)
            .withSuper(true)
            .singleOrThrow(new Supplier<NonSingletonException>() {
                @Override
                public NonSingletonException get() {
                    return new NonSingletonException("Non single");
                }
            })
            .hook(new IHook() {
                @Override
                public void after() {
                    // TODO
                }
            });

        // 基本用法
        new IHook() {
            @Override
            public void before() {
                // hook 方法所属的类
                Class<?> c = getMethod().getDeclaringClass();
                Context context = (Context) thisObject();
                String string = (String) getArg(0); // 获取第一个参数值
                setArg(1, 1); // 设置第二个参数值

                // 非静态本类内
                setThisField("demo", 1); // 设置本类内 demo 字段值
                callThisMethod("method"); // 调用本类内 method 方法
                getThisField("test");
                String result = (String) callThisMethod("call", getArg(0));

                // 非静态本类外
                Object o = null;
                setField(o, "demo", 1); // 设置实例 o 的 demo 字段
                callMethod(o, "method");
                getField(o, "test");

                // 静态本类内
                callThisStaticMethod("thisCall", getArg(0));
                int t = (int) getThisStaticField("test");

                // 静态本类外
                callStaticMethod(Object.class, "thisCall", getArg(0));
                callStaticMethod("com.demo.Main", "callStatic", getArg(1)); // 调用静态方法 callStatic
                int i = (int) getStaticField("com.demo.Main", "field");
                setStaticField("com.demo.Main", "test", true); // 设置静态字段 test

                unHookSelf(); // 移除自身
                observeCall(); // 观察调用
                getStackTrace(); // 获取堆栈
            }
        };

        // Shell 工具使用方法
        ShellTool shellTool = ShellTool.builder().isRoot(true).create();
        shellTool = ShellTool.obtain();
        ShellResult shellResult = shellTool.cmd("ls").exec();
        if (shellResult != null) {
            boolean result = shellResult.isSuccess();
        }
        shellTool.cmd("""
            if [[ 1 == 1 ]]; then
                echo hello;
            elif [[ 1 == 2 ]]; then
                echo world;
            fi
            """).exec();
        shellTool.cmd("echo hello").async();
        shellTool.cmd("echo world").async(new IExecListener() {
            @Override
            public void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
                IExecListener.super.output(command, exitCode, outputs);
            }
        });
        shellTool.addExecListener(new IExecListener() {
            @Override
            public void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
                IExecListener.super.output(command, exitCode, outputs);
            }

            @Override
            public void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
                IExecListener.super.error(command, exitCode, errors);
            }

            @Override
            public void rootResult(boolean hasRoot, @NonNull String exitCode) {
                IExecListener.super.rootResult(hasRoot, exitCode);
            }

            @Override
            public void brokenPip(@NonNull String reason, @NonNull String[] errors) {
                IExecListener.super.brokenPip(reason, errors);
            }
        });
        shellTool.close();

        // PackageTool 使用
        Context context = null;
        AppData appData = PackageTool.getAppData(context, new IAppDataGetter() {
            @NonNull
            @Override
            public Parcelable[] getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException {
                PackageInfo packageInfo = null;
                ArrayList<PackageInfo> arrayList = new ArrayList<>();
                arrayList.add(packageInfo);
                return arrayList.toArray(new PackageInfo[0]);
            }
        })[0];
        Bitmap bitmap = appData.icon;

        PackageTool.getAppData(context, true, new IAppDataGetter() {
            @NonNull
            @Override
            public Parcelable[] getPackages(@NonNull PackageManager pm) throws PackageManager.NameNotFoundException {
                PackageInfo packageInfo = null;
                ArrayList<PackageInfo> arrayList = new ArrayList<>();
                arrayList.add(packageInfo);
                return arrayList.toArray(new PackageInfo[0]);
            }

            @Override
            public void getAsyncAppData(@NonNull AppData[] appData) {
                Bitmap bitmap = appData[0].icon;
            }
        });

        // 资源注入
        createFakeResId("test_res"); // 获取 test_res 的虚拟资源 id
        // 设置 pkg 的 string 资源 test_res_str 值为 HC!
        setObjectReplacement("com.hchen.demo", "string", "test_res_str", "HC!");

        // 共享首选项工具使用方法
        prefs().get("test_key", "0"); // 获取 prefs test_key 的值
        prefs().getBoolean("test_key_bool", false); // 获取 prefs test_key_bool 的值

        // xprefs 模式：
        // 注意 xprefs 模式，寄生应用不能修改配置只能读取
        String s = prefs().getString("test", "1");  // 即可读取
        s = prefs("myPrefs").getString("test", "1");  // 可指定读取文件名

        // sprefs 模式：
        // 配置会保存到寄生应用的私有目录，读取也会从寄生应用私有目录读取
        prefs(context).editor().putString("test", "1").commit();
        // 如果没有继承 HCBase 可以这样调用
        PrefsTool.prefs(context).editor().putString("test", "2").commit();
        // 注意 sprefs 模式 是和 xprefs 模式相互独立的，可共同存在

        // 如果不方便获取 context 可用使用此方法，异步获取寄生应用上下文后再设置
        asyncPrefs(new IAsyncPrefs() {
            @Override
            public void async(@NonNull IPrefsApply sPrefs) {
                sPrefs.editor().putString("test", "1").commit();
            }
        });
    }

    @Override
    protected void init(@NonNull ClassLoader classLoader) {
        super.init(classLoader); // 使用自定义的类加载器
    }

    @Override
    protected void initZygote(@NonNull IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam); // zygote 阶段
    }

    @Override
    protected void onApplicationBefore(@NonNull Context context) {
        super.onApplicationBefore(context); // Application 创建前
    }

    @Override
    protected void onApplicationAfter(@NonNull Context context) {
        super.onApplicationAfter(context); // Application 创建后
    }

    @Override
    protected void onThrowable(int flag, @NonNull Throwable e) {
        super.onThrowable(flag, e); // 以上流程内抛错后会回调本方法，可以在此处执行清理操作
    }

    private static class InitHook extends HCEntrance {
        @Override
        @NonNull
        public HCInit.BasicData initHC(@NonNull HCInit.BasicData basicData) {
            return basicData
                .setTag("HookTool")
                .setLogLevel(HCInit.LOG_D)
                .setModulePackageName("com.hchen.demo")
                .setPrefsName("myprefs") // 可选
                .setAutoReload(true) // 可选
                .setLogExpandPath("com.hchen.demo.hook") // 可选
                .setLogExpandIgnoreClassNames("Ignore"); // 可选
        }

        @NonNull
        @Override
        public String[] ignorePackageNameList() {
            return super.ignorePackageNameList(); // 设置忽略的包名列表，会阻止其触发 onLoadPackage 方法
        }

        @Override
        public void onLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
            new ToolTest().onLoadPackage();
            new ToolTest().onApplication().onLoadPackage();
        }

        @Override
        public void onInitZygote(@NonNull StartupParam startupParam) throws Throwable {
            super.onInitZygote(startupParam);
        }

        @Override
        public void onLoadModule(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) {
            super.onLoadModule(loadPackageParam); // 模块本身被 hook 时调用
        }
    }
}
