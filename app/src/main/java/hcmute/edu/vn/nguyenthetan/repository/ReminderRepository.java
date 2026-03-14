package hcmute.edu.vn.nguyenthetan.repository;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.model.dao.ReminderDao;
import hcmute.edu.vn.nguyenthetan.receiver.ReminderReceiver;

/**
 * ReminderRepository: Quản lý logic dữ liệu và scheduling cho Reminder.
 */
public class ReminderRepository {
    private ReminderDao reminderDao;
    private Context context;

    public ReminderRepository(Context context) {
        this.context = context.getApplicationContext();
        reminderDao = AppDatabase.getInstance(context).reminderDao();
    }

    public long addReminder(Reminder reminder) {
        return reminderDao.insert(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        cancelReminder(reminder.getId());
        reminderDao.delete(reminder);
    }

    public List<Reminder> getRemindersByTaskId(int taskId) {
        return reminderDao.getRemindersByTaskId(taskId);
    }

    public List<Reminder> getAllReminders() {
        return reminderDao.getAll();
    }

    public void deleteByTaskId(int taskId) {
        List<Reminder> reminders = getRemindersByTaskId(taskId);
        for (Reminder r : reminders) {
            cancelReminder(r.getId());
        }
        reminderDao.deleteByTaskId(taskId);
    }

    public Reminder getById(int id) {
        return reminderDao.getById(id);
    }

    /**
     * Đặt alarm cho reminder sử dụng AlarmManager.
     */
    public void scheduleReminder(Reminder reminder, String taskName) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("reminder_id", reminder.getId());
        intent.putExtra("task_name", taskName);
        intent.putExtra("task_id", reminder.getTaskId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminder.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Chỉ schedule nếu thời gian nhắc nhở trong tương lai
        if (reminder.getReminderTime() > System.currentTimeMillis()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        reminder.getReminderTime(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * Hủy alarm cho reminder.
     */
    public void cancelReminder(int reminderId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager == null) return;

        Intent intent = new Intent(context, ReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                reminderId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        alarmManager.cancel(pendingIntent);
    }

    /**
     * Đặt lại tất cả alarms (dùng sau khi reboot).
     */
    public void rescheduleAllReminders() {
        List<Reminder> reminders = getAllReminders();
        AppDatabase db = AppDatabase.getInstance(context);
        for (Reminder reminder : reminders) {
            if (reminder.getReminderTime() > System.currentTimeMillis()) {
                Task task = db.taskDao().getTaskById(reminder.getTaskId());
                String taskName = (task != null) ? task.getName() : "Nhiệm vụ";
                scheduleReminder(reminder, taskName);
            }
        }
    }
}
