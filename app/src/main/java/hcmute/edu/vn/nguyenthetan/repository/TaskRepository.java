package hcmute.edu.vn.nguyenthetan.repository;

import android.content.Context;
import java.util.Calendar;
import java.util.List;
import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.model.dao.TaskDao;

/**
 * TaskRepository: Quản lý logic dữ liệu cho các nhiệm vụ (Task).
 */
public class TaskRepository {
    private TaskDao taskDao;

    public TaskRepository(Context context) {
        taskDao = AppDatabase.getInstance(context).taskDao();
    }

    public void addTask(Task task) { taskDao.insert(task); }
    public void updateTask(Task task) { taskDao.update(task); }
    public void deleteTask(Task task) { taskDao.delete(task); }
    public void deleteMultiple(List<Task> tasks) { taskDao.deleteMultiple(tasks); }

    // Lấy Task theo Category
    public List<Task> getTasksByCategoryId(int catId) {
        return taskDao.getTasksByCategoryId(catId);
    }

    // Lấy Task trong Hộp thư đến (không có deadline)
    public List<Task> getInboxTasks() {
        return taskDao.getInboxTasks();
    }

    // Hàm lấy Task theo khoảng thời gian linh hoạt (Dùng cho cả Today và 7 Days)
    public List<Task> getTasksByDateRange(long start, long end) {
        return taskDao.getTasksByDateRange(start, end);
    }

    // Lọc Task cho "Hôm nay"
    public List<Task> getTasksToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        long start = cal.getTimeInMillis();

        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        long end = cal.getTimeInMillis();

        return getTasksByDateRange(start, end);
    }

    // Lọc Task cho "7 ngày tới"
    public List<Task> getTasksNext7Days() {
        Calendar cal = Calendar.getInstance();
        long start = cal.getTimeInMillis();
        cal.add(Calendar.DAY_OF_YEAR, 7);
        long end = cal.getTimeInMillis();

        return getTasksByDateRange(start, end);
    }

    public List<Task> getAllTasks() {
        return taskDao.getAllTasks();
    }

    public List<Task> getCompletedTasks() {
        return taskDao.getCompletedTasks();
    }

    public List<Task> searchTasks(String keyword) {
        return taskDao.searchTasks(keyword);
    }
}