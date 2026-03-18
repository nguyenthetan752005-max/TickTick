package hcmute.edu.vn.nguyenthetan.model.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.model.Reminder;

@Dao
public interface ReminderDao {
    @Insert
    long insert(Reminder reminder);

    @Delete
    void delete(Reminder reminder);

    @Query("SELECT * FROM reminders WHERE taskId = :taskId ORDER BY reminderTime ASC")
    List<Reminder> getRemindersByTaskId(int taskId);

    @Query("SELECT * FROM reminders ORDER BY reminderTime ASC")
    List<Reminder> getAll();

    @Query("DELETE FROM reminders WHERE taskId = :taskId")
    void deleteByTaskId(int taskId);


}
