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
package com.hchen.hooktool.utils;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.Size;

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
import java.util.stream.Collectors;

/**
 * Shell 工具
 * <p>
 * 使用方法:
 * <p>
 * <pre>{@code
 *         ShellTool shellTool = ShellTool.builder().isRoot(true).create();
 *         shellTool = ShellTool.obtain();
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
 *         shellTool.close();
 * }
 * @author 焕晨HChen
 */
public class ShellTool {
    private static final String TAG = "ShellTool";
    private static final String END_UUID = UUID.randomUUID().toString();
    private static final byte[] LINE_BREAK = "\n".getBytes(StandardCharsets.UTF_8);
    private static final Builder builder = new Builder();
    private final ShellImpl shellImpl = new ShellImpl(this);
    private final List<IExecListener> iExecListeners = new ArrayList<>();
    private String[] shellCommands = new String[]{"su", "sh"};
    private boolean isRoot;

    private ShellTool() {
    }

    /**
     * 构建 Shell
     */
    @NonNull
    public static Builder builder() {
        return builder;
    }

    /**
     * 获取已经构建的 Shell，不存在会报错
     */
    @NonNull
    public static ShellTool obtain() {
        return builder.obtain();
    }

    /**
     * 输入命令
     */
    @NonNull
    public ShellTool cmd(@NonNull String cmd) {
        return shellImpl.cmd(cmd);
    }

    /**
     * 同步执行命令，并获取返回值
     */
    @Nullable
    public ShellResult exec() {
        return shellImpl.exec();
    }

    /**
     * 异步执行命令
     */
    public void async() {
        shellImpl.async(null);
    }

    public void async(@NonNull IExecListener iExecListener) {
        shellImpl.async(iExecListener);
    }

    /**
     * 添加回调，传入 null 则删除全部回调
     */
    public void addExecListener(@NonNull IExecListener iExecListener) {
        iExecListeners.add(iExecListener);
    }

    /**
     * 移除指定回调
     */
    public void removeExecListener(@NonNull IExecListener iExecListener) {
        iExecListeners.remove(iExecListener);
    }

    /**
     * 清除全部回调
     */
    public void clearExecListener() {
        iExecListeners.clear();
    }

    /**
     * Shell 是否处于活动状态
     */
    public boolean isActive() {
        return shellImpl.isActive();
    }

    /**
     * 关闭 Shell 流
     */
    public void close() {
        shellImpl.close();
    }

    private void create() {
        shellImpl.init();
    }

    /**
     * 检查是否支持 Root
     */
    public static boolean isRootAvailable() {
        return isRootAvailable(true, null);
    }

    /**
     * 检查是否支持 Root
     */
    public static boolean isRootAvailable(boolean sync, @Nullable IExecListener iExecListener) {
        Callable<Integer> callable = () -> {
            Process process = null;
            try {
                process = Runtime.getRuntime().exec("su -c true");
                int exitCode = process.waitFor();
                if (iExecListener != null) {
                    iExecListener.rootResult(exitCode == 0, String.valueOf(exitCode));
                }
                return exitCode;
            } catch (IOException | InterruptedException e) {
                AndroidLog.logE(TAG, "Error checking if root permission is supported!", e);
                return -1;
            } finally {
                if (process != null)
                    process.destroy();
            }
        };

        if (sync) {
            try {
                return callable.call() == 0;
            } catch (Exception e) {
                return false;
            }
        } else {
            Executors.newSingleThreadExecutor().submit(callable);
            return false;
        }
    }

    final class ShellImpl {
        @NonNull
        private final ShellTool shellTool;
        private String command = null;
        private Process process = null;
        private StreamThread streamThread = null;
        private DataOutputStream os = null;

        private ShellImpl(@NonNull ShellTool shellTool) {
            this.shellTool = shellTool;
        }

        private synchronized void init() {
            try {
                if (isActive()) return;

                command = null;
                process = Runtime.getRuntime().exec(isRoot ? shellCommands[0] : shellCommands[1]);
                os = new DataOutputStream(process.getOutputStream());

                streamThread = new StreamThread(this, process.getInputStream(), process.getErrorStream());
                streamThread.run();
            } catch (IOException e) {
                AndroidLog.logE(TAG, "Error initializing Shell stream!", e);
            } finally {
                notify();
            }
        }

        @NonNull
        private synchronized ShellTool cmd(@NonNull String cmd) {
            if (!isActive())
                return shellTool;

            command = cmd;
            return shellTool;
        }

        @Nullable
        private synchronized ShellResult exec() {
            if (!isActive()) return null;
            if (command == null) return null;

            String[] commands = command.split("\n");
            final byte[] END_CMD = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s; echo %1$s,$__RET,%2$s 1>&2; unset __RET",
                    END_UUID, command.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            streamThread.shellSyncMap.put(String.valueOf(command.hashCode()), command);
            write("{");
            writeAll(commands);
            write("}");
            write(END_CMD);
            sync();

            return streamThread.getResult();
        }

        private synchronized void async(@Nullable IExecListener iExecListener) {
            if (!isActive()) return;
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
        }

        private synchronized void sync() {
            if (!isActive()) return;

            try {
                wait();
            } catch (InterruptedException ignore) {
            }
        }

        private void write(@NonNull String command) {
            write(command.getBytes(StandardCharsets.UTF_8));
        }

        private void write(@NonNull byte[] bytes) {
            if (!isActive() || os == null) return;
            try {
                os.write(bytes);
                os.write(LINE_BREAK);
                os.flush();
            } catch (IOException e) {
                AndroidLog.logE(TAG, "Error writing data to shell stream!", e);
            }
        }

        private void writeAll(@NonNull String[] commands) {
            if (!isActive() || os == null) return;

            try {
                for (String cmd : commands) {
                    final byte[] bytes = cmd.getBytes(StandardCharsets.UTF_8);
                    os.write(bytes);
                    os.write(LINE_BREAK);
                }
                os.flush();
            } catch (IOException e) {
                AndroidLog.logE(TAG, "Error writing data to shell stream!", e);
            }
        }

        public synchronized void close() {
            if (!isActive() /* 已关闭 */ && !streamThread.isAbnormalExit() /* 不是异常退出 */)
                return;

            try {
                // 异常退出时 os 流已经死了，不需要再写入 exit 了
                if (!streamThread.isAbnormalExit()) {
                    write("exit");
                }

                if (process != null) {
                    process.waitFor(3, TimeUnit.SECONDS);
                    process.destroy();
                }
                if (os != null && !streamThread.isAbnormalExit()) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        AndroidLog.logE(TAG, "Error closing OS!", e);
                    }
                }

                streamThread.close();
            } catch (InterruptedException e) {
                AndroidLog.logE(TAG, "Error closing shell stream!", e);
            } finally {
                streamThread = null;
                command = null;
                process = null;
                os = null;
            }
        }

        private synchronized boolean isActive() {
            if (streamThread == null || process == null) return false;
            return streamThread.isActive() && process.isAlive();
        }
    }

    final class StreamThread {
        private final Object lock = new Object();
        private final int SHELL_ID_OUTPUT = 0;
        private final int SHELL_ID_ERROR = 1;
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
        private boolean isAbnormalExit = false;

        private StreamThread(@NonNull ShellImpl shellImpl, @NonNull InputStream inputStream, @NonNull InputStream errorStream) {
            this.shellImpl = shellImpl;
            input = inputStream;
            error = errorStream;
        }

        private void run() {
            outputFuture = outputService.submit(
                () -> {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (filterContent(line, SHELL_ID_OUTPUT)) {
                                continue;
                            }

                            outputList.add(line);
                        }
                    } catch (Throwable e) {
                        AndroidLog.logE(TAG, "Error reading shell standard output stream!", e);
                    }
                }
            );

            errorFuture = errorService.submit(
                () -> {
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
                        AndroidLog.logE(TAG, "Error reading shell standard error stream!", e);
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

        private boolean filterContent(@NonNull String content, int id) {
            if (!content.startsWith(END_UUID)) return false;

            String[] split = content.split(",");
            String hashCode = split[2].trim();

            synchronized (lock) {
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
                    } catch (InterruptedException e) {
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

                        lock.notifyAll();
                        return true;
                    }
                }
            }

            return false;
        }

        private void onBrokenPip() {
            if (iExecListeners.isEmpty()) return;

            for (IExecListener iExecListener : iExecListeners) {
                try {
                    iExecListener.brokenPip(
                        "Incorrect shell code causing pipeline rupture!!" +
                            " Shell code list: sync: " + shellSyncMap.values()
                            + ", async: " + shellAsyncMap.values().stream().map(p -> p.first).collect(Collectors.toCollection(ArrayList::new)),
                        toArray(errorList)
                    );
                } catch (Throwable e) {
                    AndroidLog.logE(TAG, "Error during callback!", e);
                }
            }
        }

        private String[] toArray(List<String> list) {
            return list.toArray(new String[0]);
        }

        private synchronized void close() {
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
                notifyThread();
                callbackAsyncListenerIfNeed();
            }

            private void createResult() {
                shellResult = new ShellResult(command, exitCode, outputs, errors);
            }

            private void callbackSyncListener() {
                if (isAsyncCommand) return;
                if (iExecListeners.isEmpty()) return;

                for (IExecListener iExecListener : iExecListeners) {
                    try {
                        if ("0".equals(exitCode)) iExecListener.output(command, exitCode, outputs);
                        else iExecListener.error(command, exitCode, errors);
                    } catch (Throwable e) {
                        AndroidLog.logE(TAG, "Error during callback:", e);
                    }
                }
            }

            private void notifyThread() {
                if (isAsyncCommand) return;

                synchronized (shellImpl) {
                    try {
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
                        AndroidLog.logE(TAG, "Error during callback:", e);
                    }
                }
            }
        }
    }

    public static final class Builder {
        private static final ShellTool mShell = new ShellTool();

        private Builder() {
        }

        private ShellTool obtain() {
            if (mShell.isActive())
                return mShell;
            else {
                throw new UnexpectedException("[ShellTool]: The shell tool has not been initialized, please use it after initialization!");
            }
        }

        /**
         * 是否使用 Root 模式
         */
        public Builder isRoot(boolean isRoot) {
            mShell.isRoot = isRoot;
            return this;
        }

        /**
         * 设置启动时执行的命令。默认: {"su", "sh"}
         */
        public Builder setEntranceCmds(@NonNull @Size(2) String[] cmds) {
            if (cmds.length != 2) return this;
            mShell.shellCommands = cmds;
            return this;
        }

        public ShellTool create() {
            mShell.create();
            return mShell;
        }
    }
}
