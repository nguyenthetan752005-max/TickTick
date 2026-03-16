package hcmute.edu.vn.nguyenthetan.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notifications")
public class AppNotification {
    @PrimaryKey(autoGenerate = true)
    private int id;
    
    private int taskId;
    private String taskName;
    private String message;
    private long timestamp;
    private boolean isRead;

    public AppNotification(int taskId, String taskName, String message, long timestamp) {
        this.taskId = taskId;
        this.taskName = taskName;
        this.message = message;
        this.timestamp = timestamp;
        this.isRead = false;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public String getTaskName() { return taskName; }
    public void setTaskName(String taskName) { this.taskName = taskName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}
