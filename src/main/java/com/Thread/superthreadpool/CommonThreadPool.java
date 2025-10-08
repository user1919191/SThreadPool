package com.Thread.superthreadpool;

import com.Thread.superthreadpool.Entry.MonitorData;
import com.Thread.superthreadpool.RejectHandler.RejectedExecutionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

import com.Thread.superthreadpool.RejectHandler.AbortPolicy;

/**
 * 通用线程池
 */
public class CommonThreadPool {
    
    // 核心线程数
    private final int corePoolSize;
    // 最大线程数
    private final int maximumPoolSize;
    //线程存活时间
    private volatile long keepAliveTime;
    //存活时间单位
    private volatile TimeUnit unit;
    // 任务队列
    private final BlockingQueue<Runnable> workQueue;
    // 工作线程集合
    private final List<Worker> workers;
    // 线程池状态
    private volatile boolean isShutdown = false;
    // 线程编号
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    // 拒绝策略
    private final RejectedExecutionHandler rejectedExecutionHandler;
    //处理任务数
    private volatile LongAdder taskCount = new LongAdder();
    //拒绝任务数
    private volatile LongAdder rejectCount = new LongAdder();

    // 工作线程
    class Worker extends AbstractQueuedSynchronizer implements Runnable {
        private final Thread thread;
        private Runnable firstTask;
        private volatile AtomicInteger status = new AtomicInteger(1);
        private volatile LongAdder completedTask = new LongAdder();

        public Worker(Runnable firstTask) {
            this.firstTask = firstTask;
            this.thread = new Thread(this, "CommonThreadPool-Worker-" + threadNumber.getAndIncrement());
        }

        public void start() {
            thread.start();
        }

        public void interrupt() {
            thread.interrupt();
        }

        @Override
        public void run() {
            runWorker(this);
        }
    }
    
    // 构造方法
    public CommonThreadPool(int corePoolSize, int maximumPoolSize,long keepAliveTime,TimeUnit unit, int queueCapacity) {
        this(corePoolSize, maximumPoolSize,keepAliveTime,unit, queueCapacity, new AbortPolicy());
    }
    
    public CommonThreadPool(int corePoolSize, int maximumPoolSize,long keepAliveTime,TimeUnit unit,int queueCapacity,
                            RejectedExecutionHandler handler) {
        if (corePoolSize < 0 || maximumPoolSize <= 0 || maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("Invalid pool size parameters");
        }
        if(keepAliveTime < 0 || queueCapacity < 0) {
            throw new IllegalArgumentException("Invalid keepAliveTime or queueCapacity parameters");
        }
        
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        this.workQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.workers = new ArrayList<>(maximumPoolSize);
        this.rejectedExecutionHandler = handler;
        
        // 预先启动核心线程
        for (int i = 0; i < corePoolSize; i++) {
            addWorker(null);
        }
    }
    
    /**
     * 执行任务
     */
    public void execute(Runnable task) {
        if (task == null) {
            throw new NullPointerException();
        }
        if (isShutdown) {
            throw new IllegalStateException("ThreadPool is shutdown");
        }
        
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
        
        while (worker.status.equals(1)) {
            try {
                // 优先取firstTask，然后从队列中获取
                if (task == null) {
                    task = workQueue.take();
                }
                
                // 执行任务
                if (task != null) {
                    task.run();
                    task = null;
                }
            } catch (InterruptedException e) {
                // 线程被中断，退出循环
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                // 任务执行异常，继续处理下一个任务
                e.printStackTrace();
                task = null;
            }finally {
                worker.completedTask.increment();
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
        for (Worker worker : workers) {
            boolean compared = worker.status.compareAndSet(1, 0);
            if (compared) {
                worker.interrupt();
            }
            //Todo 失败措施
        }
        workers.clear();
    }
    
    /**
     * 立即关闭线程池
     */
    public List<Runnable> shutdownNow() {
        isShutdown = true;
        List<Runnable> remainingTasks = new ArrayList<>();
        
        // 清空队列
        workQueue.drainTo(remainingTasks);
        
        // 中断所有工作线程
        for (Worker worker : workers) {
            boolean compared = worker.status.compareAndSet(1, 0);
            if( compared){
                worker.interrupt();
            }
            //Todo 失败措施
        }
        workers.clear();
        
        return remainingTasks;
    }

    public boolean isShutdown() {
        return isShutdown;
    }

    public int getPoolSize() {
        return workers.size();
    }
    
    public int getQueueSize() {
        return workQueue.size();
    }

    /**
    * 获取监控数据
    */
    public MonitorData getMonitorData() {
        return null;
    }

}