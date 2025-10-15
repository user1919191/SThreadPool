package com.Thread.superthreadpool.Entry.Enum;

public enum ThreadPoolStatus {

    NO_CREATE("未创建"),
    RUNNING("运行中"),
    SHUTDOWN("已关闭"),
    DESTORY("已销毁");

    private String status;

    ThreadPoolStatus(String status) {
        this.status = status;
    }
}
