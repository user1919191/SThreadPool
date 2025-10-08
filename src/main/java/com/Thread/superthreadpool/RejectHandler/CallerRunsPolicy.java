package com.Thread.superthreadpool.RejectHandler;

import com.Thread.superthreadpool.CommonThreadPool;

/**
 * 调用者运行策略：在调用者线程中执行
 */
public class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, CommonThreadPool executor) {
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }