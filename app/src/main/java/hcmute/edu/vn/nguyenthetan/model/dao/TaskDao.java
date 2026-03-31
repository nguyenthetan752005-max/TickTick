package hcmute.edu.vn.nguyenthetan.model.dao;

import android.database.Cursor;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.lifecycle.LiveData;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;
import hcmute.edu.vn.nguyenthetan.model.Task;

@Dao
public interface TaskDao {
    @Insert
    long insert(Task task);

    @Update
    void update(Task task);

    @Delete
    void delete(Task task);

    @Delete
    void deleteMultiple(List<Task> tasks);

    @Query("DELETE FROM tasks")
    void deleteAll();

    @Query("SELECT * FROM tasks WHERE isCompleted = 0 ORDER BY id DESC")
    LiveData<List<Task>> getAllTasks();

    @Query("SELECT * FROM tasks WHERE dueDate = 0 AND isCompleted = 0 ORDER BY id DESC")
    LiveData<List<Task>> getInboxTasks();

    @Query("SELECT * FROM tasks WHERE categoryId = :catId AND isCompleted = 0 ORDER BY id DESC")
    LiveData<List<Task>> getTasksByCategoryId(int catId);

    @Query("SELECT * FROM tasks WHERE dueDate >= :start AND dueDate <= :end AND isCompleted = 0 ORDER BY dueDate ASC")
    LiveData<List<Task>> getTasksByDateRange(long start, long end);

    @Query("SELECT * FROM tasks WHERE isCompleted = 1 ORDER BY id DESC")
    LiveData<List<Task>> getCompletedTasks();

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    Task getTaskById(int taskId);

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    Cursor getTasksCursor();

    @Query("SELECT * FROM tasks WHERE name LIKE '%' || :keyword || '%' ORDER BY id DESC")
    LiveData<List<Task>> searchTasks(String keyword);
}