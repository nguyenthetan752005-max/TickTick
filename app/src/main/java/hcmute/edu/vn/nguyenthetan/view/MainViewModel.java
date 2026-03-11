package hcmute.edu.vn.nguyenthetan.view;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import java.util.List;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.TaskRepository;

public class MainViewModel extends AndroidViewModel {

    private TaskRepository taskRepository;
    private MutableLiveData<List<Task>> tasks = new MutableLiveData<>();
    private int currentFilterMode = 0; 
    private int currentCategoryId = -1;

    public MainViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
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
}
