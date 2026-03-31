package hcmute.edu.vn.nguyenthetan.receiver;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.nguyenthetan.MainActivity;
import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;

/**
 * ReminderBroadcastReceiver: Nhận alarm từ AlarmManager khi đến giờ nhắc nhở.
 * Hiển thị notification và lưu thông báo vào Inbox (AppNotification).
 */
public class ReminderBroadcastReceiver extends BroadcastReceiver {

    public static final String EXTRA_REMINDER_ID = "extra_reminder_id";
    public static final String EXTRA_TASK_ID = "extra_task_id";
    public static final String CHANNEL_ID_REMINDER = "reminder_notification_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        int reminderId = intent.getIntExtra(EXTRA_REMINDER_ID, -1);
        int taskId = intent.getIntExtra(EXTRA_TASK_ID, -1);

        if (reminderId == -1 || taskId == -1) return;

        // Query task từ DB trên background thread (onReceive chạy trên main thread nhưng ngắn)
        AppDatabase db = AppDatabase.getInstance(context);
        Task task = db.taskDao().getTaskById(taskId);

        // Bỏ qua nếu task đã bị xóa hoặc đã hoàn thành
        if (task == null || task.isCompleted()) return;

        // Lấy thông tin reminder để xác định loại thông báo
        Reminder reminder = null;
        try {
            java.util.List<Reminder> reminders = db.reminderDao().getRemindersByTaskId(taskId);
            for (Reminder r : reminders) {
                if (r.getId() == reminderId) {
                    reminder = r;
                    break;
                }
            }
        } catch (Exception ignored) {}

        String taskName = task.getName();
        boolean isAtDeadline = false;
        if (task.getDueDate() > 0 && reminder != null) {
            isAtDeadline = (task.getDueDate() == reminder.getReminderTime());
        }

        String messageTitle = isAtDeadline ? "⏰ Đã đến hạn!" : "⏰ Sắp đến hạn!";
        String messageBody = taskName + (isAtDeadline ? " - Đã đến hạn thực hiện!" : " - Sắp đến hạn thực hiện!");

        // Tạo notification channel (Android O+)
        createNotificationChannel(context);

        // Lưu thông báo vào CSDL (Inbox)
        long timestamp = System.currentTimeMillis();
        AppNotificationRepository notifRepo = new AppNotificationRepository(context);
        AppNotification notification = new AppNotification(taskId, taskName, messageBody, timestamp);
        notifRepo.addNotification(notification);

        // Intent mở app khi nhấn notification
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, reminderId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo và hiện notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REMINDER)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(reminderId, builder.build());
        }
    }

    /**
     * Tạo notification channel cho reminder (Android O+).
     */
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_REMINDER,
                    "Thông báo nhắc nhở",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo khi nhiệm vụ sắp đến hạn");
            channel.enableVibration(true);

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
