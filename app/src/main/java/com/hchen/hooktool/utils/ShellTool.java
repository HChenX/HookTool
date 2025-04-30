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
 *
 * @author 焕晨HChen
 */
public class ShellTool {
    private static final String TAG = "ShellTool";
    private static final String END_UUID = UUID.randomUUID().toString();
    private static final byte[] LINE_BREAK = "\n".getBytes(StandardCharsets.UTF_8);
    private static final Builder mBuilder = new Builder();
    private final ShellImpl mShellImpl = new ShellImpl(this);
    private final List<IExecListener> mIExecListeners = new ArrayList<>();
    private String[] mEntranceCmds = new String[]{"su", "sh"};
    private boolean isRoot;

    private ShellTool() {
    }

    /**
     * 构建 Shell
     */
    public static Builder builder() {
        return mBuilder;
    }

    /**
     * 获取已经构建的 Shell，不存在会报错
     */
    public static ShellTool obtain() {
        return mBuilder.obtain();
    }

    /**
     * 输入命令
     */
    public ShellTool cmd(@NonNull String cmd) {
        return mShellImpl.cmd(cmd);
    }

    /**
     * 同步执行命令，并获取返回值
     */
    @Nullable
    public ShellResult exec() {
        return mShellImpl.exec();
    }

    /**
     * 异步执行命令
     */
    public void async() {
        mShellImpl.async(null);
    }

    public void async(@NonNull IExecListener iExecListener) {
        mShellImpl.async(iExecListener);
    }

    /**
     * 添加回调，传入 null 则删除全部回调
     */
    public void addExecListener(@Nullable IExecListener iExecListener) {
        if (iExecListener == null) {
            mIExecListeners.clear();
            return;
        }
        mIExecListeners.add(iExecListener);
    }

    /**
     * 移除指定回调
     */
    public void removeExecListener(IExecListener iExecListener) {
        mIExecListeners.remove(iExecListener);
    }

    /**
     * Shell 是否处于活动状态
     */
    public boolean isActive() {
        return mShellImpl.isActive();
    }

    /**
     * 关闭 Shell 流
     */
    public void close() {
        mShellImpl.close();
    }

    private void create() {
        mShellImpl.init();
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
                        iExecListener.notRoot(String.valueOf(exitCode));
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
        private final ShellTool mShellTool;
        private boolean isActive = false;
        private String mCommand = null;
        private Process mProcess = null;
        private StreamThread mStreamThread = null;
        private DataOutputStream os = null;

        private ShellImpl(ShellTool shellTool) {
            mShellTool = shellTool;
        }

        private synchronized void init() {
            try {
                if (isActive()) return;

                mCommand = null;
                mProcess = Runtime.getRuntime().exec(isRoot ? mEntranceCmds[0] : mEntranceCmds[1]);
                os = new DataOutputStream(mProcess.getOutputStream());

                mStreamThread = new StreamThread(this, mProcess.getInputStream(), mProcess.getErrorStream());
                mStreamThread.clearAll();
                mStreamThread.run();

                isActive = true;
            } catch (IOException e) {
                isActive = false;
                AndroidLog.logE(TAG, "Error initializing Shell stream!", e);
            }
            notify();
        }

        private synchronized ShellTool cmd(@NonNull String cmd) {
            if (!isActive()) return mShellTool;

            mCommand = cmd;
            return mShellTool;
        }

        private synchronized ShellResult exec() {
            if (!isActive()) return null;
            if (mCommand == null) return null;

            String[] commands = mCommand.split("\n");
            final byte[] END_CMD = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s; echo %1$s,$__RET,%2$s 1>&2; unset __RET",
                    END_UUID, mCommand.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            mStreamThread.mId2CommandSyncMap.put(String.valueOf(mCommand.hashCode()), mCommand);
            write("{");
            writeAll(commands);
            write("}");
            write(END_CMD);
            sync();

            return mStreamThread.getResult();
        }

        private synchronized void async(IExecListener iExecListener) {
            if (!isActive()) return;
            if (mCommand == null) return;

            String[] commands = mCommand.split("\n");
            final byte[] END_CMD_ID = String.format(
                    "__RET=$?; echo %1$s,$__RET,%2$s,1; echo %1$s,$__RET,%2$s,1 1>&2; unset __RET",
                    END_UUID, mCommand.hashCode()
                )
                .getBytes(StandardCharsets.UTF_8);
            mStreamThread.mId2CommandAsyncMap.put(String.valueOf(mCommand.hashCode()), new Pair<>(mCommand, iExecListener));

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
            if (!isActive() && !mStreamThread.isAbnormalExit()) return;

            try {
                if (!mStreamThread.isAbnormalExit()) {
                    write("exit");
                }

                if (mProcess != null) {
                    mProcess.waitFor(3, TimeUnit.SECONDS);
                    mProcess.destroy();
                }
                if (os != null && !mStreamThread.isAbnormalExit()) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        AndroidLog.logE(TAG, "Error closing OS!", e);
                    }
                }

                if (mStreamThread.mOutputService != null)
                    mStreamThread.mOutputService.shutdown();
                if (mStreamThread.mErrorService != null)
                    mStreamThread.mErrorService.shutdown();
            } catch (InterruptedException e) {
                AndroidLog.logE(TAG, "Error closing shell stream!", e);
            } finally {
                mStreamThread.clearAll();
                mProcess = null;
                os = null;
            }
            isActive = false;
        }

        private synchronized boolean isActive() {
            if (mStreamThread == null || mProcess == null) return false;
            boolean state = isActive && mStreamThread.isActive() && mProcess.isAlive();
            if (!state && isActive) {
                AndroidLog.logW(TAG, "Shell stream has been closed!");
                close();
            }

            return state;
        }
    }

    final class StreamThread {
        private final Object lock = new Object();
        private final ExecutorService mOutputService = Executors.newSingleThreadExecutor();
        private final ExecutorService mErrorService = Executors.newSingleThreadExecutor();
        private final HashMap<String, Pair<String, IExecListener>> mId2CommandAsyncMap = new HashMap<>();
        private final HashMap<String, String> mId2CommandSyncMap = new HashMap<>();
        private final HashMap<String, ShellID> mCommand2IDMap = new HashMap<>();
        private final List<String> mOutputList = new ArrayList<>();
        private final List<String> mErrorList = new ArrayList<>();
        private ShellResult mShellResult = null;
        private Future<?> mOutputFuture = null;
        private Future<?> mErrorFuture = null;
        private String mExitCode = "unknown";
        private final ShellImpl mShellImpl;
        private final InputStream mInput;
        private final InputStream mError;
        private boolean isAbnormalExit = false;
        private boolean isAsyncCommand = false;
        private String mCommandID = "unknown";

        private StreamThread(ShellImpl shellImpl, InputStream inputStream, InputStream errorStream) {
            mShellImpl = shellImpl;
            mInput = inputStream;
            mError = errorStream;
        }

        private void run() {
            mOutputFuture = mOutputService.submit(
                () -> {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(mInput))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (filterID(line)) {
                                shellID(0);
                                continue;
                            }

                            mOutputList.add(line);
                        }
                    } catch (Throwable e) {
                        AndroidLog.logE(TAG, "Error reading shell standard output stream!", e);
                    }
                }
            );

            mErrorFuture = mErrorService.submit(
                () -> {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(mError))) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            if (filterID(line)) {
                                shellID(1);
                                continue;
                            }
                            mErrorList.add(line);
                        }

                        if (!mErrorList.isEmpty()) {
                            isAbnormalExit = true;
                            mExitCode = "-1";

                            onBrokenPip();
                            mShellImpl.close();
                            mShellImpl.init();
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
            return mShellResult;
        }

        private boolean isActive() {
            if (mOutputFuture == null || mErrorFuture == null) return false;
            return !mOutputFuture.isDone() && !mErrorFuture.isDone() && !isAbnormalExit;
        }

        private @Size(4) Object[] createFinalVar() {
            return createFinalVar(mShellImpl.mCommand);
        }

        private @Size(4) Object[] createFinalVar(String cmd) {
            final String command = cmd;
            final String exitCode = mExitCode;
            final ArrayList<String> outputList = new ArrayList<>(mOutputList);
            final ArrayList<String> errorList = new ArrayList<>(mErrorList);
            mOutputList.clear();
            mErrorList.clear();
            mExitCode = "unknown";

            return new Object[]{command, exitCode, outputList, errorList};
        }

        private void createResult(Object[] finalVar) {
            if ("unknown".equals(finalVar[1])) return;

            if ("0".equals(finalVar[1]))
                mShellResult = new ShellResult((String) finalVar[0], toArray((List<String>) finalVar[2]), (String) finalVar[1]);
            else {
                mShellResult = new ShellResult((String) finalVar[0], toArray((List<String>) finalVar[3]), (String) finalVar[1]);
            }
        }

        private void callbackListener(Object[] finalVar) {
            if (isAsyncCommand) return;
            if ("unknown".equals(finalVar[1])) return;
            if (mIExecListeners.isEmpty()) return;

            for (IExecListener iExecListener : mIExecListeners) {
                try {
                    if ("0".equals(finalVar[1])) {
                        iExecListener.output((String) finalVar[0], toArray((List<String>) finalVar[2]), (String) finalVar[1]);
                    } else
                        iExecListener.error((String) finalVar[0], toArray((List<String>) finalVar[3]), (String) finalVar[1]);
                } catch (Throwable e) {
                    AndroidLog.logE(TAG, "Error during callback:", e);
                }
            }
        }

        private void callbackAsyncIfNeed(Object[] finalVar) {
            if (!isAsyncCommand) return;
            if ("unknown".equals(mCommandID)) return;
            if (mId2CommandAsyncMap.get(mCommandID) == null) return;

            String command = mId2CommandAsyncMap.get(mCommandID).first;
            IExecListener iExecListener = mId2CommandAsyncMap.get(mCommandID).second;

            if (iExecListener != null) {
                if ("0".equals(finalVar[1])) {
                    iExecListener.output(command, toArray((List<String>) finalVar[2]), (String) finalVar[1]);
                } else
                    iExecListener.error(command, toArray((List<String>) finalVar[3]), (String) finalVar[1]);
            }
            mId2CommandAsyncMap.remove(mCommandID);
        }

        private void notifyThread(Object[] finalVar) {
            if (isAsyncCommand) return;
            if ("unknown".equals(finalVar[1])) return;

            synchronized (mShellImpl) {
                try {
                    mShellImpl.notify();
                } catch (IllegalMonitorStateException ignore) {
                }
            }
        }

        private void shellID(int stream) {
            if ("unknown".equals(mCommandID)) return;

            if (mCommand2IDMap.get(mCommandID) == null) {
                ShellID shellID = new ShellID();
                if (stream == 0) shellID.mOutputID = true;
                else if (stream == 1) shellID.mErrorID = true;
                mCommand2IDMap.put(mCommandID, shellID);

                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignore) {
                    }
                }
            } else {
                ShellID shellID = mCommand2IDMap.get(mCommandID);
                if ((shellID.mOutputID || stream == 0) && (shellID.mErrorID || stream == 1)) {
                    mCommand2IDMap.remove(mCommandID);
                    shellID.onIDDone(mId2CommandSyncMap.get(mCommandID));
                    mId2CommandSyncMap.remove(mCommandID);
                    mCommandID = "unknown";

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

            mExitCode = split[1].trim();
            if (split.length == 4) {
                isAsyncCommand = true;
                mCommandID = split[2].trim();
            } else if (split.length == 3) {
                isAsyncCommand = false;
                mCommandID = split[2].trim();
            } else return false;

            return true;
        }

        private void onBrokenPip() {
            if (mIExecListeners.isEmpty()) return;

            Object[] finalVar = createFinalVar();
            for (IExecListener iExecListener : mIExecListeners) {
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
            mOutputList.clear();
            mErrorList.clear();
            mShellResult = null;
            isAsyncCommand = false;
            isAbnormalExit = false;
            mExitCode = "unknown";
            mCommandID = "unknown";
        }

        private final class ShellID {
            private boolean mOutputID = false;
            private boolean mErrorID = false;

            private void onIDDone(String cmd) {
                Object[] finalVar = createFinalVar(cmd);
                createResult(finalVar);
                callbackListener(finalVar);
                notifyThread(finalVar);
                callbackAsyncIfNeed(finalVar);
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
            mShell.mEntranceCmds = cmds;
            return this;
        }

        /**
         * 设置 Shell 执行监听
         */
        public Builder addExecListener(@Nullable IExecListener execListener) {
            mShell.addExecListener(execListener);
            return this;
        }

        /**
         * 移除指定的监听
         */
        public Builder removeExecListener(IExecListener execListener) {
            mShell.removeExecListener(execListener);
            return this;
        }

        public ShellTool create() {
            mShell.create();
            return mShell;
        }
    }
}
