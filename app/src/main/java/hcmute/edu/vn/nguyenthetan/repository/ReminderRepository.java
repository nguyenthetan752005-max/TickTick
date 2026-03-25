package hcmute.edu.vn.nguyenthetan.repository;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.dao.ReminderDao;
import hcmute.edu.vn.nguyenthetan.receiver.ReminderBroadcastReceiver;

/**
 * ReminderRepository: Quản lý logic dữ liệu và scheduling cho Reminder.
 * Sử dụng AlarmManager + BroadcastReceiver để đặt báo thức chính xác.
 */
public class ReminderRepository {
    private static final String TAG = "ReminderRepository";

    private ReminderDao reminderDao;
    private Context context;
    private AlarmManager alarmManager;

    public ReminderRepository(Context context) {
        this.context = context.getApplicationContext();
        reminderDao = AppDatabase.getInstance(context).reminderDao();
        alarmManager = (AlarmManager) this.context.getSystemService(Context.ALARM_SERVICE);
    }

    public long addReminder(Reminder reminder) {
        return reminderDao.insert(reminder);
    }

    public void deleteReminder(Reminder reminder) {
        cancelAlarm(reminder.getId(), reminder.getTaskId());
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
            cancelAlarm(r.getId(), r.getTaskId());
        }
        reminderDao.deleteByTaskId(taskId);
    }

    /**
     * Đặt alarm chính xác cho reminder qua AlarmManager.
     * AlarmManager sẽ gửi broadcast đến ReminderBroadcastReceiver khi đến giờ.
     */
    public void scheduleReminder(Reminder reminder, String taskName) {
        if (reminder.getReminderTime() <= System.currentTimeMillis()) {
            Log.d(TAG, "Bỏ qua reminder #" + reminder.getId() + " vì đã quá hạn");
            return;
        }

        PendingIntent pendingIntent = createPendingIntent(reminder.getId(), reminder.getTaskId());

        try {
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
            Log.d(TAG, "Đã đặt alarm cho reminder #" + reminder.getId()
                    + " tại " + new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                    .format(new java.util.Date(reminder.getReminderTime())));
        } catch (SecurityException e) {
            Log.e(TAG, "Không thể đặt exact alarm: " + e.getMessage());
        }
    }

    /**
     * Hủy alarm cho reminder.
     */
    public void cancelAlarm(int reminderId, int taskId) {
        PendingIntent pendingIntent = createPendingIntent(reminderId, taskId);
        alarmManager.cancel(pendingIntent);
        Log.d(TAG, "Đã hủy alarm cho reminder #" + reminderId);
    }

    /**
     * Lên lịch lại tất cả reminders còn trong tương lai.
     * Được gọi từ BootReceiver sau khi thiết bị reboot.
     */
    public void rescheduleAllReminders() {
        List<Reminder> reminders = getAllReminders();
        long currentTime = System.currentTimeMillis();
        int scheduled = 0;

        for (Reminder reminder : reminders) {
            if (reminder.getReminderTime() > currentTime) {
                scheduleReminder(reminder, ""); // taskName không cần thiết cho scheduling
                scheduled++;
            }
        }
        Log.d(TAG, "Đã lên lịch lại " + scheduled + "/" + reminders.size() + " reminders sau reboot");
    }

    /**
     * Tạo PendingIntent trỏ đến ReminderBroadcastReceiver.
     * Dùng reminderId làm requestCode để mỗi reminder có PendingIntent riêng.
     */
    private PendingIntent createPendingIntent(int reminderId, int taskId) {
        Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_REMINDER_ID, reminderId);
        intent.putExtra(ReminderBroadcastReceiver.EXTRA_TASK_ID, taskId);

        return PendingIntent.getBroadcast(
                context,
                reminderId, // requestCode unique cho mỗi reminder
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }
}
