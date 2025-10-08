package com.Thread.superthreadpool.RejectHandler;

import com.Thread.superthreadpool.CommonThreadPool;
import lombok.extern.slf4j.Slf4j;

/**
* 丢弃策略
*/
@Slf4j
public class DiscardPolicy implements RejectedExecutionHandler {
    @Override
    public void rejectedExecution(Runnable r, CommonThreadPool executor) {
        // 直接丢弃任务
        log.warn("Task discarded: " + r.toString());
    }
}