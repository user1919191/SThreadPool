package com.Thread.superthreadpool.Entry;

import com.Thread.superthreadpool.EnhancedThreadPool;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MonitorData implements Serializable {

    //线程池名称
    private  String threadPoolName;
    //线程池状态
    private  String status;

    // 核心线程数
    private  int corePoolSize;
    // 最大线程数
    private  int maximumPoolSize;
    // 当前线程数
    private  int activeThreadCount;
    //线程存活时间
    private volatile long keepAliveTime;
    //存活时间单位
    private volatile TimeUnit unit;
    // 队列任务数
    private  int queueTaskCount;
    // 完成任务数
    private  int completeTaskCount;
    // 拒绝任务数
    private  int rejectTaskCount;
    // 总任务数
    private  int totalTaskCount;
    //拒绝策略
    private  String rejectPolicy;

    //任务队列扩容次数(可选)
    private  int queueExpandCount = -1;
    //任务队列缩容次数(可选)
    private  int queueReduceCount = -1;
    //达到最大核心线程数频次(可选)
    private  int reachMaxCorePoolSizeCount = -1;
    //任务队列达到阈值频次(可选)
    private  int reachQueueThresholdCount = -1;

    public MonitorData(EnhancedThreadPool enhancedThreadPool) {
    }
}
