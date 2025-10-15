package com.Thread.superthreadpool.RejectHandler;

import com.Thread.superthreadpool.CommonThreadPool;
import com.Thread.superthreadpool.EnhancedThreadPool;

/**
 * 拒接接口
 */
    public interface RejectedExecutionHandler {
        void rejectedExecution(Runnable r, CommonThreadPool executor);

        void rejectedExecution(Runnable r, EnhancedThreadPool executor);
    }