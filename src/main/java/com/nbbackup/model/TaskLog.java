package com.nbbackup.model;

import java.util.Date;

public class TaskLog {
    private int id;
    private int taskId;
    private String level;
    private String message;
    private Date createTime;

    public static final String LEVEL_INFO = "INFO";
    public static final String LEVEL_ERROR = "ERROR";
    public static final String LEVEL_SUCCESS = "SUCCESS";
    public static final String LEVEL_WARNING = "WARNING";

    public TaskLog() {
        this.createTime = new Date();
        this.level = LEVEL_INFO;
    }

    public TaskLog(int taskId, String level, String message) {
        this();
        this.taskId = taskId;
        this.level = level;
        this.message = message;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
