package com.Thread.superthreadpool.Entry;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class MonitorData implements Serializable {

    //线程池状态
    private final String status;

    // 核心线程数
    private final int corePoolSize;
    // 最大线程数
    private final int maximumPoolSize;
    // 当前线程数
    private final int activeThreadCount;
    // 队列大小
    private final int queueSize;
    // 队列任务数
    private final int queueTaskCount;
    // 完成任务数
    private final int completeTaskCount;
    // 拒绝任务数
    private final int rejectTaskCount;
    // 总任务数
    private final int totalTaskCount;

    //任务队列扩容次数(可选)
    private final int queueExpandCount = -1;
    //任务队列缩容次数(可选)
    private final int queueReduceCount = -1;
    //达到最大核心线程数频次(可选)
    private final int reachMaxCorePoolSizeCount = -1;
    //任务队列达到阈值频次(可选)
    private final int reachQueueThresholdCount = -1;
}
