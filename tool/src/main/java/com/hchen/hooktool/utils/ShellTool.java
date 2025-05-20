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
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
 *             public void output(String command, @NonNull String[] outputs, String exitCode) {
 *                 IExecListener.super.output(command, outputs, exitCode);
 *             }
 *         });
 *         shellTool.addExecListener(new IExecListener() {
 *             @Override
 *             public void output(String command, @NonNull String[] outputs, String exitCode) {
 *                 IExecListener.super.output(command, outputs, exitCode);
 *             }
 *
 *             @Override
 *             public void error(String command, @NonNull String[] errors, String exitCode) {
 *                 IExecListener.super.error(command, errors, exitCode);
 *             }
 *
 *             @Override
 *             public void notRoot(String exitCode) {
 *                 IExecListener.super.notRoot(exitCode);
 *             }
 *
 *             @Override
 *             public void brokenPip(String command, @NonNull String[] errors, String reason) {
 *                 IExecListener.super.brokenPip(command, errors, reason);
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
    private String[] entranceCmds = new String[]{"su", "sh"};
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
                if (exitCode != 0 && !sync) {
                    if (iExecListener != null) {
                        iExecListener.rootResult(String.valueOf(exitCode));
                    }
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
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.submit(callable);
            service.shutdown();
            return false;
        }
    }

    final class ShellImpl {
        private final ShellTool shellTool;
        private boolean isActive = false;
        private String command = null;
        private Process process = null;
        private StreamThread streamThread = null;
        private DataOutputStream os = null;

        private ShellImpl(ShellTool shellTool) {
            this.shellTool = shellTool;
        }

        private synchronized void init() {
            try {
                if (isActive()) return;

                command = null;
                process = Runtime.getRuntime().exec(isRoot ? entranceCmds[0] : entranceCmds[1]);
                os = new DataOutputStream(process.getOutputStream());

                streamThread = new StreamThread(this, process.getInputStream(), process.getErrorStream());
                streamThread.clearAll();
                streamThread.run();

                isActive = true;
            } catch (IOException e) {
                isActive = false;
                AndroidLog.logE(TAG, "Error initializing Shell stream!", e);
            }
            notify();
        }

        private synchronized ShellTool cmd(String cmd) {
            if (!isActive()) return shellTool;

            command = cmd;
            return shellTool;
        }

        private synchronized ShellResult exec() {
            if (!isActive()) return null;
            if (command == null) return null;

            String[] commands = command.split("\n");
            final byte[] END_CMD = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s; echo %1$s,$__RET,%2$s 1>&2; unset __RET",
                    END_UUID, command.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            streamThread.id2CommandSyncMap.put(String.valueOf(command.hashCode()), command);
            write("{");
            writeAll(commands);
            write("}");
            write(END_CMD);
            sync();

            return streamThread.getResult();
        }

        private synchronized void async(IExecListener iExecListener) {
            if (!isActive()) return;
            if (command == null) return;

            String[] commands = command.split("\n");
            final byte[] END_CMD_ID = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s,1; echo %1$s,$__RET,%2$s,1 1>&2; unset __RET",
                    END_UUID, command.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            streamThread.id2CommandAsyncMap.put(String.valueOf(command.hashCode()), new Pair<>(command, iExecListener));

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

        private void write(String cmd) {
            write(cmd.getBytes(StandardCharsets.UTF_8));
        }

        private void write(byte[] bytes) {
            if (!isActive() || os == null) return;
            try {
                os.write(bytes);
                os.write(LINE_BREAK);
                os.flush();
            } catch (IOException e) {
                AndroidLog.logE(TAG, "Error writing data to shell stream!", e);
            }
        }

        private void writeAll(String[] cmds) {
            if (!isActive() || os == null) return;

            try {
                for (String cmd : cmds) {
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
            if (!isActive() && !streamThread.isAbnormalExit()) return;

            try {
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

                if (streamThread.outputService != null)
                    streamThread.outputService.shutdown();
                if (streamThread.errorService != null)
                    streamThread.errorService.shutdown();
            } catch (InterruptedException e) {
                AndroidLog.logE(TAG, "Error closing shell stream!", e);
            } finally {
                streamThread.clearAll();
                process = null;
                os = null;
            }
            isActive = false;
        }

        private synchronized boolean isActive() {
            if (streamThread == null || process == null) return false;
            boolean state = isActive && streamThread.isActive() && process.isAlive();
            if (!state && isActive) {
                AndroidLog.logW(TAG, "Shell stream has been closed!");
                close();
            }

            return state;
        }
    }

    final class StreamThread {
        private final Object lock = new Object();
        private final ExecutorService outputService = Executors.newSingleThreadExecutor();
        private final ExecutorService errorService = Executors.newSingleThreadExecutor();
        private final HashMap<String, Pair<String, IExecListener>> id2CommandAsyncMap = new HashMap<>();
        private final HashMap<String, String> id2CommandSyncMap = new HashMap<>();
        private final HashMap<String, ShellID> command2IDMap = new HashMap<>();
        private final List<String> outputList = new ArrayList<>();
        private final List<String> errorList = new ArrayList<>();
        private ShellResult shellResult = null;
        private Future<?> outputFuture = null;
        private Future<?> errorFuture = null;
        private String exitCode = "unknown";
        private final ShellImpl shellImpl;
        private final InputStream input;
        private final InputStream error;
        private boolean isAbnormalExit = false;
        private boolean isAsyncCommand = false;
        private String commandID = "unknown";

        private StreamThread(ShellImpl shellImpl, InputStream inputStream, InputStream errorStream) {
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
                            if (filterID(line)) {
                                shellID(0);
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
                            if (filterID(line)) {
                                shellID(1);
                                continue;
                            }
                            errorList.add(line);
                        }

                        if (!errorList.isEmpty()) {
                            isAbnormalExit = true;
                            exitCode = "-1";

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

        private ShellResult getResult() {
            return shellResult;
        }

        private boolean isActive() {
            if (outputFuture == null || errorFuture == null) return false;
            return !outputFuture.isDone() && !errorFuture.isDone() && !isAbnormalExit;
        }

        private @Size(4) Object[] createFinalData() {
            return createFinalData(shellImpl.command);
        }

        private @Size(4) Object[] createFinalData(String cmd) {
            final String command = cmd;
            final String exitCode = this.exitCode;
            final ArrayList<String> outputList = new ArrayList<>(this.outputList);
            final ArrayList<String> errorList = new ArrayList<>(this.errorList);
            this.outputList.clear();
            this.errorList.clear();
            this.exitCode = "unknown";

            return new Object[]{command, exitCode, outputList, errorList};
        }

        private void createResult(Object[] finalData) {
            if ("unknown".equals(finalData[1])) return;

            if ("0".equals(finalData[1]))
                shellResult = new ShellResult((String) finalData[0], toArray((List<String>) finalData[2]), (String) finalData[1]);
            else {
                shellResult = new ShellResult((String) finalData[0], toArray((List<String>) finalData[3]), (String) finalData[1]);
            }
        }

        private void callbackListener(Object[] finalData) {
            if (isAsyncCommand) return;
            if ("unknown".equals(finalData[1])) return;
            if (iExecListeners.isEmpty()) return;

            for (IExecListener iExecListener : iExecListeners) {
                try {
                    if ("0".equals(finalData[1])) {
                        iExecListener.output((String) finalData[0], toArray((List<String>) finalData[2]), (String) finalData[1]);
                    } else
                        iExecListener.error((String) finalData[0], toArray((List<String>) finalData[3]), (String) finalData[1]);
                } catch (Throwable e) {
                    AndroidLog.logE(TAG, "Error during callback:", e);
                }
            }
        }

        private void callbackAsyncIfNeed(Object[] finalData) {
            if (!isAsyncCommand) return;
            if ("unknown".equals(commandID)) return;
            if (id2CommandAsyncMap.get(commandID) == null) return;

            String command = id2CommandAsyncMap.get(commandID).first;
            IExecListener iExecListener = id2CommandAsyncMap.get(commandID).second;

            if (iExecListener != null) {
                if ("0".equals(finalData[1])) {
                    iExecListener.output(command, toArray((List<String>) finalData[2]), (String) finalData[1]);
                } else
                    iExecListener.error(command, toArray((List<String>) finalData[3]), (String) finalData[1]);
            }
            id2CommandAsyncMap.remove(commandID);
        }

        private void notifyThread(Object[] finalData) {
            if (isAsyncCommand) return;
            if ("unknown".equals(finalData[1])) return;

            synchronized (shellImpl) {
                try {
                    shellImpl.notify();
                } catch (IllegalMonitorStateException ignore) {
                }
            }
        }

        private void shellID(int stream) {
            if ("unknown".equals(commandID)) return;

            if (command2IDMap.get(commandID) == null) {
                ShellID shellID = new ShellID();
                if (stream == 0) shellID.outputID = true;
                else if (stream == 1) shellID.errorID = true;
                command2IDMap.put(commandID, shellID);

                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            } else {
                ShellID shellID = command2IDMap.get(commandID);
                if ((shellID.outputID || stream == 0) && (shellID.errorID || stream == 1)) {
                    command2IDMap.remove(commandID);
                    shellID.onIDDone(id2CommandSyncMap.get(commandID));
                    id2CommandSyncMap.remove(commandID);
                    commandID = "unknown";

                    synchronized (lock) {
                        lock.notifyAll();
                    }
                }
            }
        }

        private synchronized boolean filterID(String content) {
            if (content == null) return false;
            if (!content.contains(",")) return false;
            if (!content.startsWith(END_UUID)) return false;

            String[] split = content.split(",");
            if (!END_UUID.equals(split[0].trim())) return false;

            exitCode = split[1].trim();
            if (split.length == 4) {
                isAsyncCommand = true;
                commandID = split[2].trim();
            } else if (split.length == 3) {
                isAsyncCommand = false;
                commandID = split[2].trim();
            } else return false;

            return true;
        }

        private void onBrokenPip() {
            if (iExecListeners.isEmpty()) return;

            Object[] finalVar = createFinalData();
            for (IExecListener iExecListener : iExecListeners) {
                try {
                    iExecListener.brokenPip((String) finalVar[0], toArray((List<String>) finalVar[3]), "Incorrect shell code causing pipeline rupture!");
                } catch (Throwable e) {
                    AndroidLog.logE(TAG, "Error during callback!", e);
                }
            }
        }

        private String[] toArray(List<String> list) {
            return list.toArray(new String[0]);
        }

        private synchronized void clearAll() {
            outputList.clear();
            errorList.clear();
            shellResult = null;
            isAsyncCommand = false;
            isAbnormalExit = false;
            exitCode = "unknown";
            commandID = "unknown";
        }

        private final class ShellID {
            private boolean outputID = false;
            private boolean errorID = false;

            private void onIDDone(String cmd) {
                Object[] finalData = createFinalData(cmd);
                createResult(finalData);
                callbackListener(finalData);
                notifyThread(finalData);
                callbackAsyncIfNeed(finalData);
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
            mShell.entranceCmds = cmds;
            return this;
        }

        public ShellTool create() {
            mShell.create();
            return mShell;
        }
    }
}
