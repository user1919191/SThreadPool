package com.Thread.superthreadpool;

import com.Thread.superthreadpool.Entry.MonitorData;
import com.Thread.superthreadpool.RejectHandler.AbortPolicy;
import com.Thread.superthreadpool.RejectHandler.RejectedExecutionHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
public class EnhancedThreadPool {

    // 线程池状态枚举
    public enum ThreadPoolStatus {
        NO_CREATE, RUNNING, SHUTDOWN
    }

    // 配置参数
    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private volatile long keepAliveTime;
    private final TimeUnit unit;
    private final BlockingQueue<Runnable> workQueue;
    private final List<Worker> workers;
    private volatile boolean isShutdown = false;
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    private final RejectedExecutionHandler rejectedExecutionHandler;

    // 统计信息
    private final LongAdder taskCount = new LongAdder();
    private final LongAdder completedTaskCount = new LongAdder();
    private final LongAdder rejectCount = new LongAdder();

    // 状态
    private String threadPoolId;
    private String threadPoolStatus;

    // 工作线程
    class Worker implements Runnable {
        private final Thread thread;
        private Runnable firstTask;
        private volatile boolean running = true;

        public Worker(Runnable firstTask) {
            this.firstTask = firstTask;
            this.thread = new Thread(this, "CommonThreadPool-Worker-" + threadNumber.getAndIncrement());
        }

        public void start() {
            thread.start();
        }

        public void interrupt() {
            running = false;
            thread.interrupt();
        }

        @Override
        public void run() {
            runWorker(this);
        }
    }

    // 构造方法
    public EnhancedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, int queueCapacity) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, queueCapacity, new AbortPolicy());
    }

    public EnhancedThreadPool(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, int queueCapacity,
                            RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("Invalid pool size parameters");
        }
        if(keepAliveTime < 0 || queueCapacity < 0) {
            throw new IllegalArgumentException("Invalid keepAliveTime or queueCapacity parameters");
        }

        this.threadPoolId = UUID.randomUUID().toString();
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.workQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.workers = Collections.synchronizedList(new ArrayList<>());
        this.rejectedExecutionHandler = handler;

        // 预先启动核心线程
        for (int i = 0; i < corePoolSize; i++) {
            addWorker(null);
        }

        threadPoolStatus = ThreadPoolStatus.RUNNING.name();
        log.info("ThreadPool created: {}, corePoolSize: {}, maximumPoolSize: {}",
                threadPoolId, corePoolSize, maximumPoolSize);
    }

    /**
     * 执行任务
     */
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException("Task cannot be null");
        }
        if (isShutdown) {
            throw new IllegalStateException("ThreadPool is shutdown");
        }

        taskCount.increment();

        // 1. 如果运行的线程数小于核心线程数，尝试创建新线程
        if (workers.size() < corePoolSize) {
            if (addWorker(task)) {
                return;
            }
        }

        // 2. 尝试将任务加入队列
        if (workQueue.offer(task)) {
            return;
        }

        // 3. 尝试创建非核心线程
        if (workers.size() < maximumPoolSize) {
            if (addWorker(task)) {
                return;
            }
        }

        // 4. 执行拒绝策略
        rejectCount.increment();
        rejectedExecutionHandler.rejectedExecution(task, this);
    }

    /**
     * 添加工作线程
     */
    private boolean addWorker(Runnable firstTask) {
        if (workers.size() >= maximumPoolSize) {
            return false;
        }

        Worker worker = new Worker(firstTask);
        workers.add(worker);
        worker.start();
        return true;
    }

    /**
     * 工作线程执行逻辑
     */
    private void runWorker(Worker worker) {
        Runnable task = worker.firstTask;
        worker.firstTask = null;

        while (worker.running) {
            try {
                // 优先取firstTask，然后从队列中获取
                if (task == null) {
                    task = workQueue.poll(keepAliveTime, unit);
                    if (task == null) {
                        // 超时后如果线程数超过核心线程数，则退出
                        if (workers.size() > corePoolSize) {
                            break;
                        }
                        continue;
                    }
                }

                // 执行任务
                task.run();
                completedTaskCount.increment();
                task = null;

            } catch (InterruptedException e) {
                // 线程被中断，退出循环
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 任务执行异常，继续处理下一个任务
                log.error("Task execution failed", e);
                task = null;
            }
        }

        // 从工作线程集合中移除
        workers.remove(worker);
    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        isShutdown = true;
        threadPoolStatus = ThreadPoolStatus.SHUTDOWN.name();

        for (Worker worker : workers) {
            worker.interrupt();
        }

        log.info("ThreadPool shutdown: {}", threadPoolId);
    }

    /**
     * 立即关闭线程池
     */
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        threadPoolStatus = ThreadPoolStatus.SHUTDOWN.name();
        List<Runnable> remainingTasks = new ArrayList<>();

        // 清空队列
        workQueue.drainTo(remainingTasks);

        // 中断所有工作线程
        for (Worker worker : workers) {
            worker.interrupt();
        }

        workers.clear();
        log.info("ThreadPool shutdownNow: {}", threadPoolId);

        return remainingTasks;
    }

    public boolean isShutdown() {
        return isShutdown;
    }


    /**
     * 获取监控数据
     */
    public MonitorData getMonitorData() {
        MonitorData monitorData = new MonitorData();
        monitorData.setThreadPoolName(threadPoolId);
        monitorData.setStatus(threadPoolStatus);
        monitorData.setCorePoolSize(corePoolSize);
        monitorData.setMaximumPoolSize(maximumPoolSize);
        monitorData.setActiveThreadCount(workers.size());
        monitorData.setQueueTaskCount(workQueue.size());
        monitorData.setKeepAliveTime(keepAliveTime);
        monitorData.setRejectPolicy(rejectedExecutionHandler.getClass().getSimpleName());
        monitorData.setUnit(unit);
        return monitorData;
    }
}
