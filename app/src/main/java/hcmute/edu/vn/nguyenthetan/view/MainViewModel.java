package hcmute.edu.vn.nguyenthetan.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import hcmute.edu.vn.nguyenthetan.TaskDialogHelper;
import hcmute.edu.vn.nguyenthetan.adapter.HeaderItem;
import hcmute.edu.vn.nguyenthetan.adapter.InboxItem;
import hcmute.edu.vn.nguyenthetan.adapter.NotificationItemWrapper;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;
import hcmute.edu.vn.nguyenthetan.repository.TaskRepository;
import hcmute.edu.vn.nguyenthetan.util.AppExecutors;
import hcmute.edu.vn.nguyenthetan.util.NotificationHelper;

public class MainViewModel extends AndroidViewModel {

    private static class FilterState {
        final int mode;
        final int categoryId;
        final String keyword;
        final int sortMode; // -1 = không sort

        FilterState(int mode, int categoryId, String keyword, int sortMode) {
            this.mode = mode;
            this.categoryId = categoryId;
            this.keyword = keyword;
            this.sortMode = sortMode;
        }
    }

    private TaskRepository taskRepository;
    private ReminderRepository reminderRepository;
    private AppNotificationRepository notificationRepository;

    private int currentFilterMode = 0;
    private int currentCategoryId = -1;
    private String currentSearchKeyword = "";
    private int currentSortMode = -1;

    private final AppExecutors appExecutors;

    private final MutableLiveData<FilterState> filterStateLiveData = new MutableLiveData<>();
    private final LiveData<List<Task>> tasks;
    private final LiveData<List<InboxItem>> inboxData;

    public MainViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        reminderRepository = new ReminderRepository(application);
        notificationRepository = new AppNotificationRepository(application);
        appExecutors = AppExecutors.getInstance();

        // Trạng thái ban đầu (mode mặc định: Inbox)
        publishState();

        tasks = Transformations.switchMap(filterStateLiveData, state -> {
            if (state == null) return new MutableLiveData<>(Collections.emptyList());

            // Search ưu tiên hơn filter mode
            String keyword = state.keyword;
            LiveData<List<Task>> source;
            if (keyword != null && !keyword.trim().isEmpty()) {
                source = taskRepository.searchTasks(keyword);
            } else {
                switch (state.mode) {
                    case 1:
                        source = taskRepository.getTasksToday();
                        break;
                    case 2:
                        source = taskRepository.getTasksNext7Days();
                        break;
                    case 3:
                        source = taskRepository.getTasksByCategoryId(state.categoryId);
                        break;
                    case 4:
                        source = taskRepository.getCompletedTasks();
                        break;
                    case 5:
                        source = taskRepository.getAllTasks();
                        break;
                    case 6:
                        source = taskRepository.getInboxTasks();
                        break;
                    case 0:
                    default:
                        source = new MutableLiveData<>(Collections.emptyList());
                        break;
                }
            }

            return Transformations.map(source, list -> sortTasks(list, state.sortMode));
        });

        inboxData = Transformations.switchMap(filterStateLiveData, state -> {
            if (state == null || state.mode != 0) {
                return new MutableLiveData<>(Collections.emptyList());
            }

            return Transformations.map(notificationRepository.getAllNotifications(), notifications -> {
                List<InboxItem> items = new ArrayList<>();

                if (notifications != null && !notifications.isEmpty()) {
                    items.add(new HeaderItem("Thông báo nhắc nhở"));
                    for (AppNotification n : notifications) {
                        items.add(new NotificationItemWrapper(n));
                    }
                }

                if (items.isEmpty()) {
                    items.add(new HeaderItem("Hộp thư thông báo trống"));
                }

                return items;
            });
        });
    }

    public LiveData<List<Task>> getTasks() {
        return tasks;
    }

    public LiveData<List<InboxItem>> getInboxData() {
        return inboxData;
    }

    public int getCurrentFilterMode() {
        return currentFilterMode;
    }

    private void publishState() {
        filterStateLiveData.postValue(
                new FilterState(currentFilterMode, currentCategoryId, currentSearchKeyword, currentSortMode)
        );
    }

    public void setFilterMode(int mode) {
        this.currentFilterMode = mode;
        this.currentCategoryId = -1;
        this.currentSearchKeyword = "";
        this.currentSortMode = -1;
        publishState();
    }

    public void setCategoryFilter(int categoryId) {
        this.currentFilterMode = 3;
        this.currentCategoryId = categoryId;
        this.currentSearchKeyword = "";
        this.currentSortMode = -1;
        publishState();
    }

    public void loadTasks() {
        // Chỉ cần đẩy lại state để Room LiveData tự refresh.
        publishState();
    }

    /**
     * Thêm task mới và schedule reminders nếu có.
     */
    public void addTaskWithReminders(Task task, List<TaskDialogHelper.PendingReminder> pendingReminders) {
        appExecutors.diskIO().execute(() -> {
            long insertedTaskId = taskRepository.addTask(task);

            if (pendingReminders != null && !pendingReminders.isEmpty()) {
                for (TaskDialogHelper.PendingReminder pr : pendingReminders) {
                    // Tính lại reminder time dựa trên dueDate thực tế
                    long reminderTime = calculateReminderTime(task.getDueDate(), pr.value, pr.unitIndex);
                    Reminder reminder = new Reminder((int) insertedTaskId, reminderTime);
                    long id = reminderRepository.addReminder(reminder);
                    reminder.setId((int) id);
                    reminderRepository.scheduleReminder(reminder, task.getName());
                }
            }

            // Sau mutation thì reset sort/search để giống behavior cũ (loadTasks())
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    public void addTask(Task task) {
        appExecutors.diskIO().execute(() -> {
            taskRepository.addTask(task);
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    public void updateTask(Task task) {
        appExecutors.diskIO().execute(() -> {
            taskRepository.updateTask(task);
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    public void completeTask(Task task) {
        appExecutors.diskIO().execute(() -> {
            taskRepository.updateTask(task);

            // Xóa tất cả nhắc nhở của task này để không báo chuông nữa
            reminderRepository.deleteByTaskId(task.getId());

            // Lưu thông báo chúc mừng vào Hộp thư đến
            String congratMsg = "🎉 Chúc mừng bạn đã hoàn thành nhiệm vụ - " + task.getName() + " - này !";
            AppNotification congratNotification = new AppNotification(
                    task.getId(),
                    task.getName(),
                    congratMsg,
                    System.currentTimeMillis());
            notificationRepository.addNotification(congratNotification);

            // Hiện notification của hệ thống để người dùng thấy ngay
            NotificationHelper.showTaskCompletedNotification(
                    getApplication().getApplicationContext(),
                    task,
                    congratMsg
            );

            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    public void deleteTasks(List<Task> tasksToDelete) {
        appExecutors.diskIO().execute(() -> {
            taskRepository.deleteMultiple(tasksToDelete);
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    public void updateNotification(AppNotification notification) {
        appExecutors.diskIO().execute(() -> {
            notificationRepository.updateNotification(notification);
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    public void deleteNotification(AppNotification notification) {
        appExecutors.diskIO().execute(() -> {
            notificationRepository.deleteNotification(notification);
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }

    // Helper tính thời gian nhắc nhở
    private long calculateReminderTime(long dueDate, int value, int unitIndex) {
        long offset;
        switch (unitIndex) {
            case 0:
                offset = value * 60 * 1000L;
                break; // Phút
            case 1:
                offset = value * 60 * 60 * 1000L;
                break; // Giờ
            case 2:
                offset = value * 24 * 60 * 60 * 1000L;
                break; // Ngày
            default:
                offset = 0;
        }
        return dueDate - offset;
    }

    /**
     * Tìm kiếm task theo từ khóa.
     */
    public void searchTasks(String keyword) {
        currentSearchKeyword = keyword == null ? "" : keyword.trim();
        currentSortMode = -1;
        publishState();
    }

    /**
     * Sắp xếp danh sách task hiện tại theo tiêu chí.
     * 0 = theo tên (A-Z), 1 = theo ngày (gần nhất trước), 2 = theo ngày tạo (mới nhất)
     */
    public void sortCurrentTasks(int sortMode) {
        currentSortMode = sortMode;
        publishState();
    }

    private static List<Task> sortTasks(List<Task> input, int sortMode) {
        if (input == null) return Collections.emptyList();
        if (sortMode < 0) return input;

        ArrayList<Task> sorted = new ArrayList<>(input);
        switch (sortMode) {
            case 0: // Tên A-Z
                Collections.sort(sorted,
                        (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
                break;
            case 1: // Deadline gần nhất
                Collections.sort(sorted,
                        (a, b) -> Long.compare(a.getDueDate(), b.getDueDate()));
                break;
            case 2: // Mới tạo nhất (ID giảm dần)
                Collections.sort(sorted,
                        (a, b) -> Integer.compare(b.getId(), a.getId()));
                break;
        }
        return sorted;
    }

    /**
     * Xóa tất cả dữ liệu từ database.
     */
    public void clearAllData() {
        appExecutors.diskIO().execute(() -> {
            // Xóa tất cả tasks (sẽ cascade delete reminders)
            taskRepository.deleteAllTasks();
            
            // Xóa tất cả notifications
            notificationRepository.deleteAllNotifications();
            
            // Reset filter states
            currentFilterMode = 0;
            currentCategoryId = -1;
            currentSearchKeyword = "";
            currentSortMode = -1;
            publishState();
        });
    }
}

