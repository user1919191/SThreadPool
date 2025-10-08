package com.Thread.superthreadpool.RejectHandler;

import com.Thread.superthreadpool.CommonThreadPool;

/**
 * 拒接接口
 */
    public interface RejectedExecutionHandler {
        void rejectedExecution(Runnable r, CommonThreadPool executor);
    }