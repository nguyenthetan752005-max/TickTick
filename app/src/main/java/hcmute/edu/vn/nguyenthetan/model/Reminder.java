package hcmute.edu.vn.nguyenthetan.model;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "reminders",
        foreignKeys = @ForeignKey(entity = Task.class,
                parentColumns = "id",
                childColumns = "taskId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("taskId")})
public class Reminder {
    @PrimaryKey(autoGenerate = true)
    private int id;

    private int taskId;
    private long reminderTime; // Thời điểm nhắc nhở (millis)

    public Reminder(int taskId, long reminderTime) {
        this.taskId = taskId;
        this.reminderTime = reminderTime;
    }

    // Getter và Setter
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }
    public long getReminderTime() { return reminderTime; }
    public void setReminderTime(long reminderTime) { this.reminderTime = reminderTime; }
}
