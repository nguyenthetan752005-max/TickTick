package hcmute.edu.vn.nguyenthetan.model.dao;

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

    // Lấy toàn bộ task
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    List<Task> getAllTasks();

    // QUAN TRỌNG: Lấy danh sách Task thuộc về một Category cụ thể
    @Query("SELECT * FROM tasks WHERE categoryId = :catId ORDER BY id DESC")
    List<Task> getTasksByCategoryId(int catId);
}