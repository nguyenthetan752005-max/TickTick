package hcmute.edu.vn.nguyenthetan.view;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.TaskDialogHelper;
import hcmute.edu.vn.nguyenthetan.adapter.HeaderItem;
import hcmute.edu.vn.nguyenthetan.adapter.InboxItem;
import hcmute.edu.vn.nguyenthetan.adapter.NotificationItemWrapper;
import hcmute.edu.vn.nguyenthetan.adapter.TaskItemWrapper;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;
import hcmute.edu.vn.nguyenthetan.repository.TaskRepository;
import hcmute.edu.vn.nguyenthetan.MainActivity;
import hcmute.edu.vn.nguyenthetan.service.ReminderService;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;
import hcmute.edu.vn.nguyenthetan.repository.TaskRepository;

public class MainViewModel extends AndroidViewModel {

    private TaskRepository taskRepository;
    private ReminderRepository reminderRepository;
    private AppNotificationRepository notificationRepository;

    private MutableLiveData<List<Task>> tasks = new MutableLiveData<>();
    private MutableLiveData<List<InboxItem>> inboxData = new MutableLiveData<>();

    private int currentFilterMode = 0;
    private int currentCategoryId = -1;

    public MainViewModel(@NonNull Application application) {
        super(application);
        taskRepository = new TaskRepository(application);
        reminderRepository = new ReminderRepository(application);
        notificationRepository = new AppNotificationRepository(application);
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
            if (currentFilterMode == 0) {
                // Load dữ liệu cho Inbox
                List<Task> inboxTasks = taskRepository.getInboxTasks();
                List<AppNotification> notifications = notificationRepository.getAllNotifications();

                List<InboxItem> items = new java.util.ArrayList<>();

                if (!inboxTasks.isEmpty()) {
                    items.add(new HeaderItem("Nhiệm vụ chưa lên lịch"));
                    for (Task t : inboxTasks)
                        items.add(new TaskItemWrapper(t));
                }

                if (!notifications.isEmpty()) {
                    items.add(new HeaderItem("Thông báo nhắc nhở"));
                    for (AppNotification n : notifications)
                        items.add(new NotificationItemWrapper(n));
                }

                if (items.isEmpty()) {
                    items.add(new HeaderItem("Hộp thư đến trống"));
                }

                inboxData.postValue(items);
                tasks.postValue(new java.util.ArrayList<>()); // Clear tasks view
            } else {
                List<Task> result;
                switch (currentFilterMode) {
                    case 1:
                        result = taskRepository.getTasksToday();
                        break;
                    case 2:
                        result = taskRepository.getTasksNext7Days();
                        break;
                    case 3:
                        result = taskRepository.getTasksByCategoryId(currentCategoryId);
                        break;
                    case 4:
                        result = taskRepository.getCompletedTasks();
                        break;
                    case 5:
                        result = taskRepository.getAllTasks();
                        break; // Tất cả task
                    default:
                        result = taskRepository.getAllTasks();
                        break;
                }
                tasks.postValue(result);
            }
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

    public void completeTask(Task task) {
        new Thread(() -> {
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
            Context context = getApplication().getApplicationContext();
            Intent openIntent = new Intent(context, MainActivity.class);
            openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(
                    context, task.getId(), openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(
                    context, ReminderService.CHANNEL_ID_REMINDER)
                    .setSmallIcon(android.R.drawable.ic_menu_agenda)
                    .setContentTitle("🎉 Hoàn thành nhiệm vụ!")
                    .setContentText(task.getName() + " " + congratMsg)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(pendingIntent)
                    .setDefaults(NotificationCompat.DEFAULT_ALL);

            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null) {
                notificationManager.notify(task.getId(), builder.build());
            }

            loadTasks();
        }).start();
    }

    public void deleteTasks(List<Task> tasksToDelete) {
        new Thread(() -> {
            taskRepository.deleteMultiple(tasksToDelete);
            loadTasks(); // Nạp lại danh sách sau khi xóa hàng loạt
        }).start();
    }

    public void updateNotification(AppNotification notification) {
        new Thread(() -> {
            notificationRepository.updateNotification(notification);
            loadTasks();
        }).start();
    }

    public void deleteNotification(AppNotification notification) {
        new Thread(() -> {
            notificationRepository.deleteNotification(notification);
            loadTasks();
        }).start();
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
}
