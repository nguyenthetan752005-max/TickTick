package hcmute.edu.vn.nguyenthetan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.nguyenthetan.MainActivity;
import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.AppNotification;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.AppNotificationRepository;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;

/**
 * ReminderService: Foreground Service để theo dõi và quản lý nhắc nhở bằng Timer.
 */
public class ReminderService extends Service {

    public static final String ACTION_UPDATE_REMINDERS = "hcmute.edu.vn.nguyenthetan.ACTION_UPDATE_REMINDERS";
    public static final String CHANNEL_ID_FOREGROUND = "reminder_foreground_channel";
    public static final String CHANNEL_ID_REMINDER = "reminder_notification_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 9999;

    private Timer reminderTimer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Tạo notification cho Foreground Service
        startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());

        // Hủy Timer cũ nếu có và lên lịch lại tất cả báo thức từ CSDL
        scheduleAllReminders();

        return START_STICKY; // Giữ Service phục hồi chạy nền liên tục
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                .setContentTitle("TickTick - Hệ thống Nhắc nhở")
                .setContentText("Đang chạy nền để canh thời gian nhắc nhở nhiệm vụ")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void scheduleAllReminders() {
        executor.execute(() -> {
            if (reminderTimer != null) {
                reminderTimer.cancel();
                reminderTimer.purge();
            }
            reminderTimer = new Timer("ReminderTimerThread");

            ReminderRepository repo = new ReminderRepository(getApplicationContext());
            List<Reminder> reminders = repo.getAllReminders();
            long currentTime = System.currentTimeMillis();

            for (Reminder reminder : reminders) {
                if (reminder.getReminderTime() > currentTime) {
                    TimerTask taskTimer = new TimerTask() {
                        @Override
                        public void run() {
                            handleReminderTrigger(reminder);
                        }
                    };
                    
                    try {
                        reminderTimer.schedule(taskTimer, new Date(reminder.getReminderTime()));
                    } catch (IllegalStateException ignored) {
                        // Bỏ qua nếu timer đã bị cancel
                    }
                }
            }
        });
    }

    private void handleReminderTrigger(Reminder reminder) {
        // Load lại Task từ DB để đảm bảo cờ isCompleted() là mới nhất
        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
        Task task = db.taskDao().getTaskById(reminder.getTaskId());

        // Bỏ qua nếu task đã bị xóa hoặc đã hoàn thành
        if (task == null || task.isCompleted()) {
            return; 
        }

        String taskName = task.getName();
        boolean isAtDeadline = false;
        if (task.getDueDate() > 0) {
            if (task.getDueDate() == reminder.getReminderTime()) {
                isAtDeadline = true;
            }
        }

        String messageTitle = isAtDeadline ? "⏰ Đã đến hạn!" : "⏰ Sắp đến hạn!";
        String messageBody = taskName + (isAtDeadline ? " - Đã đến hạn thực hiện!" : " - Sắp đến hạn thực hiện!");

        // Lưu thông báo vào CSDL (Inbox)
        long timestamp = System.currentTimeMillis();
        AppNotificationRepository notifRepo = new AppNotificationRepository(getApplicationContext());
        AppNotification notification = new AppNotification(reminder.getTaskId(), taskName, messageBody, timestamp);
        notifRepo.addNotification(notification);

        // Intent mở app khi nhấn notification
        Intent openIntent = new Intent(getApplicationContext(), MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                getApplicationContext(), reminder.getId(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo notification nhắc nhở
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                getApplicationContext(), CHANNEL_ID_REMINDER)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(messageTitle)
                .setContentText(messageBody)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(reminder.getId(), builder.build());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (reminderTimer != null) {
            reminderTimer.cancel();
            reminderTimer.purge();
        }
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel foregroundChannel = new NotificationChannel(
                    CHANNEL_ID_FOREGROUND,
                    "Dịch vụ nhắc nhở",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Thông báo dịch vụ đang chạy nền");

            NotificationChannel reminderChannel = new NotificationChannel(
                    CHANNEL_ID_REMINDER,
                    "Thông báo nhắc nhở",
                    NotificationManager.IMPORTANCE_HIGH
            );
            reminderChannel.setDescription("Thông báo khi nhiệm vụ sắp đến hạn");
            reminderChannel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(foregroundChannel);
                manager.createNotificationChannel(reminderChannel);
            }
        }
    }
}
