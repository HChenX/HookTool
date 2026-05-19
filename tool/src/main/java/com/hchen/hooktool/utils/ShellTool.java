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
package com.hchen.hooktool.utils;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

import com.hchen.hooktool.callback.ICommandListener;
import com.hchen.hooktool.callback.IExecListener;
import com.hchen.hooktool.data.ShellResult;
import com.hchen.hooktool.exception.UnexpectedException;
import com.hchen.hooktool.log.AndroidLog;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shell 命令执行工具类。
 * <p>
 * 提供同步和异步两种 Shell 命令执行能力，支持 Root（{@code su}）和普通（{@code sh}）两种模式。
 * 内部维护持久化的 Shell 进程流，通过 UUID 标记每条命令的输出边界，实现多命令并发执行时的
 * 结果正确分离。支持命令拼接模式，可将多条命令合并为一条执行。
 * <p>
 * 使用示例：
 * <pre>{@code
 *         ShellTool shellTool = ShellTool.obtain(true);
 *         ShellResult shellResult = shellTool.cmd("ls").exec();
 *         if (shellResult != null) {
 *             boolean result = shellResult.isSuccess();
 *         }
 *         shellTool.cmd("""
 *             if [[ 1 == 1 ]]; then
 *                 echo hello;
 *             elif [[ 1 == 2 ]]; then
 *                 echo world;
 *             fi
 *             """).exec();
 *         shellTool.enableSplicingMode()
 *             .cmd("if [[ true == true ]]; then")
 *             .cmd("  echo hello               ")
 *             .cmd("fi                         ")
 *             .exec();
 *         shellTool.cmd("echo hello").async();
 *         shellTool.cmd("echo world").async(new IExecListener() {
 *             @Override
 *             public void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
 *                 IExecListener.super.output(command, exitCode, outputs);
 *             }
 *         });
 *         shellTool.addExecListener(new IExecListener() {
 *             @Override
 *             public void output(@NonNull String command, @NonNull String exitCode, @NonNull String[] outputs) {
 *                 IExecListener.super.output(command, exitCode, outputs);
 *             }
 *
 *             @Override
 *             public void error(@NonNull String command, @NonNull String exitCode, @NonNull String[] errors) {
 *                 IExecListener.super.error(command, exitCode, errors);
 *             }
 *
 *             @Override
 *             public void rootResult(boolean hasRoot, @NonNull String exitCode) {
 *                 IExecListener.super.rootResult(hasRoot, exitCode);
 *             }
 *
 *             @Override
 *             public void brokenPip(@NonNull String reason, @NonNull String[] errors) {
 *                 IExecListener.super.brokenPip(reason, errors);
 *             }
 *         });
 *         ShellTool.close();
 * }
 * @author 焕晨HChen
 */
public final class ShellTool {
    private static final String TAG = "ShellTool";
    private static final String END_UUID = UUID.randomUUID().toString();
    private static final byte[] LINE_BREAK = "\n".getBytes(StandardCharsets.UTF_8);
    private static final ShellTool shellTool = new ShellTool();
    private static volatile boolean isRoot = false;
    private static volatile String[] shellCommands = new String[]{"su", "sh"};
    private static volatile IExecListener iGlobalExecListeners;
    private static volatile ICommandListener iGlobalCommandListener;
    private static volatile ShellImpl shellImpl;

    private ShellTool() {
        shellImpl = new ShellImpl(this);
    }

    /**
     * 获取 {@link ShellTool} 单例并初始化 Shell 进程流。
     * <p>
     * 使用当前通过 {@link #setRoot(boolean)} 设定的模式启动 Shell 进程。
     *
     * @return {@link ShellTool} 单例实例
     */
    @NonNull
    public static ShellTool obtain() {
        shellImpl.init();
        return shellTool;
    }

    /**
     * 获取 {@link ShellTool} 单例，同时指定 Root 模式并初始化 Shell 进程流。
     *
     * @param isRoot {@code true} 使用 {@code su}（Root 模式），{@code false} 使用 {@code sh}（普通模式）
     * @return {@link ShellTool} 单例实例
     */
    @NonNull
    public static ShellTool obtain(boolean isRoot) {
        setRoot(isRoot);
        return obtain();
    }

    /**
     * 设置 Shell 的 Root 模式。
     *
     * @param isRoot {@code true} 启用 Root 模式，{@code false} 使用普通模式
     * @return {@link ShellTool} 单例实例，支持链式调用
     */
    @NonNull
    public static ShellTool setRoot(boolean isRoot) {
        ShellTool.isRoot = isRoot;
        return shellTool;
    }

    /**
     * 自定义 Shell 启动命令。
     * <p>
     * 数组长度必须为 2：第一个元素为 Root 模式命令，第二个为普通模式命令。
     *
     * @param commands Shell 命令数组，长度必须为 2
     * @return {@link ShellTool} 单例实例，支持链式调用
     */
    @NonNull
    public static ShellTool setShellCommands(@Size(2) String[] commands) {
        shellCommands = commands;
        return shellTool;
    }

    /**
     * 设置全局执行监听器，用于接收所有同步命令的输出和错误回调。
     *
     * @param iExecListener 执行监听器实例；传 {@code null} 可取消监听
     * @return {@link ShellTool} 单例实例，支持链式调用
     */
    @NonNull
    public static ShellTool setExecListener(@Nullable IExecListener iExecListener) {
        iGlobalExecListeners = iExecListener;
        return shellTool;
    }

    /**
     * 设置全局命令监听器，在命令执行前进行拦截。
     * <p>
     * 监听器的 {@code onCommand} 方法返回 {@code false} 时将阻止该命令的执行。
     *
     * @param listener 命令监听器实例；传 {@code null} 可取消监听
     * @return {@link ShellTool} 单例实例，支持链式调用
     */
    @NonNull
    public static ShellTool setCommandListener(@Nullable ICommandListener listener) {
        iGlobalCommandListener = listener;
        return shellTool;
    }

    /**
     * 判断当前 Shell 进程流是否处于活跃状态。
     *
     * @return Shell 进程存活且读取线程正常运行时返回 {@code true}
     */
    public static boolean isActive() {
        return shellImpl.isActive();
    }

    /**
     * 关闭当前 Shell 进程流并释放所有相关资源。
     * <p>
     * 关闭流程包括：发送 {@code exit} 命令、等待进程退出、关闭输出流、终止读取线程。
     */
    public static void close() {
        shellImpl.close();
    }

    /**
     * 启用命令拼接模式。
     * <p>
     * 启用后，通过 {@link #cmd(String)} 添加的多条命令将以换行符连接后拼接为一条命令一次性执行。
     * 拼接模式在执行一次后自动关闭。
     *
     * @return {@link ShellTool} 单例实例，支持链式调用
     */
    @NonNull
    public ShellTool enableSplicingMode() {
        return shellImpl.enableSplicingMode();
    }

    /**
     * 添加一条待执行的 Shell 命令。
     * <p>
     * 若处于拼接模式，命令将被暂存到拼接列表中；否则直接覆盖当前待执行命令。
     *
     * @param cmd 命令字符串，不可为 {@code null}
     * @return {@link ShellTool} 单例实例，支持链式调用
     */
    @NonNull
    public ShellTool cmd(@NonNull String cmd) {
        return shellImpl.cmd(cmd);
    }

    /**
     * 同步执行已添加的命令，阻塞当前线程直到命令执行完毕并返回结果。
     *
     * @return 命令执行结果；若未添加命令则返回 {@code null}
     */
    @Nullable
    public ShellResult exec() {
        return shellImpl.exec();
    }

    /**
     * 异步执行已添加的命令，不阻塞当前线程。
     * <p>
     * 执行结果通过全局执行监听器（{@link #setExecListener}）回调返回。
     */
    public void async() {
        shellImpl.async(null);
    }

    /**
     * 异步执行已添加的命令，并通过指定的监听器接收结果。
     *
     * @param iExecListener 用于接收本次命令执行结果的监听器
     */
    public void async(@NonNull IExecListener iExecListener) {
        shellImpl.async(iExecListener);
    }

    // --------------------------------------- Root Check -------------------------------------------

    /**
     * 同步检查当前设备是否具备 Root 权限。
     *
     * @return 具备 Root 权限返回 {@code true}
     */
    public static boolean isRootAvailable() {
        return isRootAvailable(true, null);
    }

    /**
     * 同步检查当前设备是否具备 Root 权限，并通过监听器返回检测结果。
     *
     * @param iExecListener 接收 Root 检测结果的监听器
     * @return 具备 Root 权限返回 {@code true}
     */
    public static boolean isRootAvailable(@NonNull IExecListener iExecListener) {
        return isRootAvailable(true, iExecListener);
    }

    /**
     * 检查当前设备是否具备 Root 权限。
     * <p>
     * 通过执行 {@code su -c true} 命令并检查退出码来判断 Root 可用性。支持同步和异步两种模式。
     *
     * @param sync          {@code true} 为同步检测，{@code false} 为异步检测
     * @param iExecListener 接收 Root 检测结果的监听器，可为 {@code null}
     * @return 同步模式下返回是否具备 Root 权限；异步模式下固定返回 {@code false}，结果通过监听器回调
     */
    public static boolean isRootAvailable(boolean sync, @Nullable IExecListener iExecListener) {
        Callable<Integer> callable = new Callable<Integer>() {
            @Override
            public Integer call() throws Exception {
                Process process = null;
                try {
                    process = Runtime.getRuntime().exec("su -c true");
                    int exitCode = process.waitFor();
                    if (iExecListener != null) {
                        iExecListener.rootResult(exitCode == 0, String.valueOf(exitCode));
                    }
                    return exitCode;
                } catch (IOException | InterruptedException e) {
                    AndroidLog.logE(TAG, "Error checking if root permission is supported!!", e);
                    return -1;
                } finally {
                    if (process != null)
                        process.destroy();
                }
            }
        };

        if (sync) {
            try {
                return callable.call() == 0;
            } catch (Exception e) {
                return false;
            }
        } else {
            ExecutorService service = null;
            try {
                // noinspection resource
                service = Executors.newSingleThreadExecutor();
                service.submit(callable);
            } finally {
                if (service != null) {
                    service.shutdown();
                }
            }
            return false;
        }
    }
    // ----------------------------------------------------------------------------------------------

    /**
     * Shell 流内部实现类。
     * <p>
     * 负责管理 Shell 进程的完整生命周期，包括进程启动、命令写入、输出读取以及同步/异步执行结果的分发。
     */
    final class ShellImpl {
        @NonNull
        private final ShellTool shellTool;
        private boolean isSplicingMode = false;
        private final ArrayList<String> waitSplicingCommandList = new ArrayList<>();
        private String command = null;
        private Process process = null;
        private StreamThread streamThread = null;
        private DataOutputStream os = null;

        private ShellImpl(@NonNull ShellTool shellTool) {
            this.shellTool = shellTool;
        }

        private volatile boolean resultReady = false;
        private volatile int shellGeneration = 0;

        private synchronized void init() {
            try {
                if (isActive()) return;

                command = null;
                process = Runtime.getRuntime().exec(isRoot ? shellCommands[0] : shellCommands[1]);
                os = new DataOutputStream(process.getOutputStream());

                shellGeneration++;
                streamThread = new StreamThread(this, process.getInputStream(), process.getErrorStream());
                streamThread.run();
            } catch (IOException e) {
                throw new UnexpectedException("Error initializing shell stream.");
            }
        }

        @NonNull
        private synchronized ShellTool enableSplicingMode() {
            this.isSplicingMode = true;
            return shellTool;
        }

        @NonNull
        private synchronized ShellTool cmd(@NonNull String cmd) {
            if (!isActive())
                throw new UnexpectedException("Shell stream is dead.");

            if (isSplicingMode) waitSplicingCommandList.add(cmd);
            else command = cmd;
            return shellTool;
        }

        @Nullable
        private synchronized ShellResult exec() {
            if (!isActive())
                throw new UnexpectedException("Shell stream is dead.");

            splicingCommandIfNeed();
            callbackCommandListener();
            if (command == null) return null;

            String[] commands = command.split("\n");
            final byte[] END_CMD = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s; echo %1$s,$__RET,%2$s 1>&2; unset __RET",
                    END_UUID, command.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            streamThread.shellSyncMap.put(String.valueOf(command.hashCode()), command);
            resultReady = false;
            int generation = shellGeneration;
            write("{");
            writeAll(commands);
            write("}");
            write(END_CMD);
            command = null;

            try {
                while (!resultReady && isActive() && generation == shellGeneration) {
                    wait();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                AndroidLog.logE(TAG, "Shell exec interrupted while waiting for result!!", e);
                return null;
            }

            if (!resultReady) return null;

            return streamThread.getResult();
        }

        private synchronized void async(@Nullable IExecListener iExecListener) {
            if (!isActive())
                throw new UnexpectedException("Shell stream is dead.");

            splicingCommandIfNeed();
            callbackCommandListener();
            if (command == null) return;

            String[] commands = command.split("\n");
            final byte[] END_CMD_ID = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s,1; echo %1$s,$__RET,%2$s,1 1>&2; unset __RET",
                    END_UUID, command.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            streamThread.shellAsyncMap.put(String.valueOf(command.hashCode()), new Pair<>(command, iExecListener));

            write("{");
            writeAll(commands);
            write("}");
            write(END_CMD_ID);
            command = null;
        }

        private void write(@NonNull String command) {
            write(command.getBytes(StandardCharsets.UTF_8));
        }

        private void write(@NonNull byte[] bytes) {
            try {
                os.write(bytes);
                os.write(LINE_BREAK);
                os.flush();
            } catch (IOException e) {
                AndroidLog.logE(TAG, "Error writing data to shell stream!!", e);
            }
        }

        private void writeAll(@NonNull String[] commands) {
            try {
                for (String cmd : commands) {
                    final byte[] bytes = cmd.getBytes(StandardCharsets.UTF_8);
                    os.write(bytes);
                    os.write(LINE_BREAK);
                }
                os.flush();
            } catch (IOException e) {
                AndroidLog.logE(TAG, "Error writing data to shell stream!!", e);
            }
        }

        public synchronized void close() {
            try {
                boolean abnormal = streamThread != null && streamThread.isAbnormalExit();
                if (isActive() || abnormal) {
                    if (!abnormal) {
                        write("exit");
                    }

                    if (process != null) {
                        process.waitFor(3, TimeUnit.SECONDS);
                        process.destroy();
                    }

                    if (os != null && !abnormal) {
                        try {
                            os.close();
                        } catch (IOException e) {
                            AndroidLog.logE(TAG, "Error closing OS!!", e);
                        }
                    }

                    if (streamThread != null) {
                        streamThread.close();
                    }
                }
            } catch (InterruptedException e) {
                AndroidLog.logE(TAG, "Error closing shell stream!!", e);
            } finally {
                streamThread = null;
                command = null;
                process = null;
                os = null;
                resultReady = false;
                notifyAll();
            }
        }

        private synchronized boolean isActive() {
            if (streamThread == null || process == null) return false;
            return streamThread.isActive() && process.isAlive();
        }

        private void splicingCommandIfNeed() {
            if (!isSplicingMode) return;
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < waitSplicingCommandList.size(); i++) {
                if (i == waitSplicingCommandList.size() - 1)
                    stringBuilder.append(waitSplicingCommandList.get(i));
                else stringBuilder.append(waitSplicingCommandList.get(i)).append("\n");
            }
            isSplicingMode = false;
            command = stringBuilder.toString();
            waitSplicingCommandList.clear();
        }

        private void callbackCommandListener() {
            if (command == null) return;
            if (iGlobalCommandListener == null) return;
            if (!iGlobalCommandListener.onCommand(command)) command = null;
        }
    }

    /**
     * Shell 标准输出和错误输出的读取线程管理类。
     * <p>
     * 通过两个独立的线程分别读取标准输出流和错误输出流，利用 UUID 标记识别命令边界，
     * 实现多条命令结果的正确分离。支持同步命令的结果通知和异步命令的回调分发。
     */
    final class StreamThread {
        private final Object lock = new Object();
        private static final int SHELL_ID_OUTPUT = 0;
        private static final int SHELL_ID_ERROR = 1;
        private final ExecutorService outputService = Executors.newSingleThreadExecutor();
        private final ExecutorService errorService = Executors.newSingleThreadExecutor();
        private final ConcurrentHashMap<String, Pair<String, IExecListener>> shellAsyncMap = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, String> shellSyncMap = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<String, ShellData> shellDataMap = new ConcurrentHashMap<>();
        private final CopyOnWriteArrayList<String> outputList = new CopyOnWriteArrayList<>();
        private final CopyOnWriteArrayList<String> errorList = new CopyOnWriteArrayList<>();
        private ShellResult shellResult = null;
        private Future<?> outputFuture = null;
        private Future<?> errorFuture = null;
        @NonNull
        private final ShellImpl shellImpl;
        @NonNull
        private final InputStream input;
        @NonNull
        private final InputStream error;
        private volatile boolean isAbnormalExit = false;

        private StreamThread(@NonNull ShellImpl shellImpl, @NonNull InputStream inputStream, @NonNull InputStream errorStream) {
            this.shellImpl = shellImpl;
            input = inputStream;
            error = errorStream;
        }

        private void run() {
            outputFuture = outputService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                if (filterContent(line, SHELL_ID_OUTPUT)) {
                                    continue;
                                }

                                outputList.add(line);
                            }
                        } catch (Throwable e) {
                            AndroidLog.logE(TAG, "Error reading shell standard output stream!!", e);
                        }
                    }
                }
            );

            errorFuture = errorService.submit(
                new Runnable() {
                    @Override
                    public void run() {
                        try (BufferedReader br = new BufferedReader(new InputStreamReader(error))) {
                            String line;
                            while ((line = br.readLine()) != null) {
                                if (filterContent(line, SHELL_ID_ERROR)) {
                                    continue;
                                }

                                errorList.add(line);
                            }

                            // Shell 管道异常破裂
                            if (!errorList.isEmpty()) {
                                isAbnormalExit = true;

                                onBrokenPip();
                                shellImpl.close();
                                shellImpl.init();
                            }
                        } catch (Throwable e) {
                            AndroidLog.logE(TAG, "Error reading shell standard error stream!!", e);
                        }
                    }
                }
            );
        }

        private boolean isAbnormalExit() {
            return isAbnormalExit;
        }

        @Nullable
        private ShellResult getResult() {
            return shellResult;
        }

        private boolean isActive() {
            if (outputFuture == null || errorFuture == null) return false;
            return !outputFuture.isDone() && !errorFuture.isDone() && !isAbnormalExit;
        }

        /**
         * 过滤并处理输出流中的命令结束标记行。
         * <p>
         * 当检测到以 UUID 开头的行时，表示一条命令的输出已结束。通过 hashCode 匹配同步/异步命令，
         * 等待标准输出和错误输出均到达后组装结果并唤醒等待线程。
         *
         * @param content 当前读取的行内容
         * @param id      输出流标识（{@code 0} = 标准输出，{@code 1} = 错误输出）
         * @return 是结束标记行返回 {@code true}（已处理，不应加入输出列表）
         */
        private boolean filterContent(@NonNull String content, int id) {
            synchronized (lock) {
                if (!content.startsWith(END_UUID)) return false;

                String[] split = content.split(",");
                String hashCode = split[2].trim();
                if (shellDataMap.get(hashCode) == null) {
                    ShellData shellData = new ShellData();
                    shellData.exitCode = split[1].trim();
                    shellData.isAsyncCommand = split.length == 4;
                    shellData.command = shellData.isAsyncCommand ?
                        Objects.requireNonNull(shellAsyncMap.get(hashCode)).first :
                        shellSyncMap.get(hashCode);
                    shellData.iExecListener = shellData.isAsyncCommand ?
                        Objects.requireNonNull(shellAsyncMap.get(hashCode)).second :
                        null;
                    if (id == SHELL_ID_OUTPUT) {
                        shellData.isOutputDone = true;
                        shellData.outputs = toArray(outputList);
                        outputList.clear();
                    }
                    if (id == SHELL_ID_ERROR) {
                        shellData.isErrorDone = true;
                        shellData.errors = toArray(errorList);
                        errorList.clear();
                    }
                    shellDataMap.put(hashCode, shellData);

                    try {
                        lock.wait();
                    } catch (InterruptedException ignore) {
                    }
                    return true;
                } else {
                    ShellData shellData = shellDataMap.get(hashCode);
                    assert shellData != null;
                    if ((shellData.isOutputDone || id == SHELL_ID_OUTPUT) && (shellData.isErrorDone || id == SHELL_ID_ERROR)) {
                        if (shellData.outputs == null) shellData.outputs = toArray(outputList);
                        if (shellData.errors == null) shellData.errors = toArray(errorList);
                        shellData.onShellDone();
                        shellDataMap.remove(hashCode);
                        shellSyncMap.remove(hashCode);
                        shellAsyncMap.remove(hashCode);
                        outputList.clear();
                        errorList.clear();

                        lock.notify();
                        return true;
                    }
                }
            }

            return false;
        }

        private void onBrokenPip() {
            if (iGlobalExecListeners == null) return;

            try {
                iGlobalExecListeners.brokenPip(
                    "Incorrect shell code causing pipeline rupture!!" +
                        " Shell code list: sync: " + shellSyncMap.values()
                        + ", async: " +
                        shellAsyncMap.values()
                            .stream()
                            .map(new Function<Pair<String, IExecListener>, String>() {
                                @Override
                                public String apply(Pair<String, IExecListener> p) {
                                    return p.first;
                                }
                            }).collect(Collectors.toCollection(ArrayList::new)),
                    toArray(errorList)
                );
            } catch (Throwable e) {
                AndroidLog.logE(TAG, "Error during callback!!", e);
            }
        }

        private String[] toArray(List<String> list) {
            return list.toArray(new String[0]);
        }

        private void close() {
            if (outputService != null)
                outputService.shutdownNow();
            if (errorService != null)
                errorService.shutdownNow();

            shellAsyncMap.clear();
            shellSyncMap.clear();
            shellDataMap.clear();
            outputList.clear();
            errorList.clear();
            shellResult = null;
            isAbnormalExit = false;
        }

        /**
         * 单条命令的执行数据封装类。
         * <p>
         * 包含命令文本、退出码、标准输出和错误输出内容，以及命令完成后的结果组装与回调处理逻辑。
         */
        private final class ShellData {
            private boolean isAsyncCommand = false;
            private boolean isOutputDone = false;
            private boolean isErrorDone = false;
            private String command = null;
            private String exitCode = "-1";
            private String[] outputs = null;
            private String[] errors = null;
            private IExecListener iExecListener;

            private void onShellDone() {
                createResult();
                callbackSyncListener();
                callbackAsyncListenerIfNeed();
                notifyImpl();
            }

            private void createResult() {
                shellResult = new ShellResult(command, exitCode, outputs, errors);
            }

            private void callbackSyncListener() {
                if (isAsyncCommand) return;
                if (iGlobalExecListeners == null) return;

                try {
                    if ("0".equals(exitCode))
                        iGlobalExecListeners.output(command, exitCode, outputs);
                    else iGlobalExecListeners.error(command, exitCode, errors);
                } catch (Throwable e) {
                    AndroidLog.logE(TAG, "Error during callback!!", e);
                }
            }

            private void notifyImpl() {
                if (isAsyncCommand) return;

                synchronized (shellImpl) {
                    try {
                        shellImpl.resultReady = true;
                        shellImpl.notify();
                    } catch (IllegalMonitorStateException ignore) {
                    }
                }
            }

            private void callbackAsyncListenerIfNeed() {
                if (!isAsyncCommand) return;
                if (iExecListener != null) {
                    try {
                        if ("0".equals(exitCode)) iExecListener.output(command, exitCode, outputs);
                        else iExecListener.error(command, exitCode, errors);
                    } catch (Throwable e) {
                        AndroidLog.logE(TAG, "Error during callback!!", e);
                    }
                }
            }
        }
    }
}
