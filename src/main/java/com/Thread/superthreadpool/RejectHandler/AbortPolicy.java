package com.Thread.superthreadpool.RejectHandler;

import com.Thread.superthreadpool.CommonThreadPool;

/**
 * 默认拒绝策略：直接抛出异常
 */
public class AbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, CommonThreadPool executor) {
            throw new RuntimeException("Task " + r.toString() + " rejected from " + executor.toString());
        }
    }