package hcmute.edu.vn.nguyenthetan.model.dao;

import android.database.Cursor;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import hcmute.edu.vn.nguyenthetan.model.Task;

@Dao
public interface TaskDao {
    @Insert
    void insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Delete
    void deleteMultiple(List<Task> tasks);

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY id DESC")
    List<Task> getAllTasks();

    @Query("SELECT * FROM tasks WHERE dueDate = 0 AND isCompleted = 0 ORDER BY id DESC")
    List<Task> getInboxTasks();

    @Query("SELECT * FROM tasks WHERE categoryId = :catId AND isCompleted = 0 ORDER BY id DESC")
    List<Task> getTasksByCategoryId(int catId);

    @Query("SELECT * FROM tasks WHERE dueDate >= :start AND dueDate <= :end AND isCompleted = 0 ORDER BY dueDate ASC")
    List<Task> getTasksByDateRange(long start, long end);

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY id DESC")
    List<Task> getCompletedTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(int taskId);

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    Cursor getTasksCursor();
}