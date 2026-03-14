package hcmute.edu.vn.nguyenthetan.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.TaskDialogHelper;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;
import hcmute.edu.vn.nguyenthetan.repository.TaskRepository;

public class MainViewModel extends AndroidViewModel {

    private TaskRepository taskRepository;
    private ReminderRepository reminderRepository;
    private MutableLiveData<List<Task>> tasks = new MutableLiveData<>();
    private int currentFilterMode = 0; 
    private int currentCategoryId = -1;

    public MainViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        reminderRepository = new ReminderRepository(application);
    }

    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    public void setFilterMode(int mode) {
        this.currentFilterMode = mode;
        this.currentCategoryId = -1;
        loadTasks();
    }

    public void setCategoryFilter(int categoryId) {
        this.currentFilterMode = 3;
        this.currentCategoryId = categoryId;
        loadTasks();
    }

    public void loadTasks() {
        new Thread(() -> {
            List<Task> result;
            switch (currentFilterMode) {
                case 1: result = taskRepository.getTasksToday(); break;
                case 2: result = taskRepository.getTasksNext7Days(); break;
                case 3: result = taskRepository.getTasksByCategoryId(currentCategoryId); break;
                case 4: result = taskRepository.getCompletedTasks(); break;
                default: result = taskRepository.getAllTasks(); break;
            }
            tasks.postValue(result);
        }).start();
    }

    /**
     * Thêm task mới và schedule reminders nếu có.
     */
    public void addTaskWithReminders(Task task, List<TaskDialogHelper.PendingReminder> pendingReminders) {
        new Thread(() -> {
            taskRepository.addTask(task);
            // Lấy task vừa insert (có ID) 
            // Vì Room insert trên main thread đã bị cho phép, ta lấy ID từ danh sách
            List<Task> allTasks = taskRepository.getAllTasks();
            if (!allTasks.isEmpty() && pendingReminders != null && !pendingReminders.isEmpty()) {
                // Task mới nhất có ID cao nhất
                Task insertedTask = allTasks.get(0); // ORDER BY id DESC
                for (TaskDialogHelper.PendingReminder pr : pendingReminders) {
                    // Tính lại reminder time dựa trên dueDate thực tế
                    long reminderTime = calculateReminderTime(insertedTask.getDueDate(), pr.value, pr.unitIndex);
                    Reminder reminder = new Reminder(insertedTask.getId(), reminderTime);
                    long id = reminderRepository.addReminder(reminder);
                    reminder.setId((int) id);
                    reminderRepository.scheduleReminder(reminder, insertedTask.getName());
                }
            }
            loadTasks();
        }).start();
    }

    public void addTask(Task task) {
        new Thread(() -> {
            taskRepository.addTask(task);
            loadTasks();
        }).start();
    }

    public void updateTask(Task task) {
        new Thread(() -> {
            taskRepository.updateTask(task);
            loadTasks();
        }).start();
    }

    public void deleteTasks(List<Task> tasksToDelete) {
        new Thread(() -> {
            taskRepository.deleteMultiple(tasksToDelete);
            loadTasks(); // Nạp lại danh sách sau khi xóa hàng loạt
        }).start();
    }

    // Helper tính thời gian nhắc nhở
    private long calculateReminderTime(long dueDate, int value, int unitIndex) {
        long offset;
        switch (unitIndex) {
            case 0: offset = value * 60 * 1000L; break;         // Phút
            case 1: offset = value * 60 * 60 * 1000L; break;    // Giờ
            case 2: offset = value * 24 * 60 * 60 * 1000L; break; // Ngày
            default: offset = 0;
        }
        return dueDate - offset;
    }
}
