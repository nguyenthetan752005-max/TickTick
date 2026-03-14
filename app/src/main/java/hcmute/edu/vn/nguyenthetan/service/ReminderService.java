package hcmute.edu.vn.nguyenthetan.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.nguyenthetan.MainActivity;
import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;

/**
 * ReminderService: Foreground Service để theo dõi và quản lý nhắc nhở.
 * Service chạy nền với notification hiển thị trạng thái đang theo dõi.
 */
public class ReminderService extends Service {

    public static final String CHANNEL_ID_FOREGROUND = "reminder_foreground_channel";
    public static final String CHANNEL_ID_REMINDER = "reminder_notification_channel";
    private static final int FOREGROUND_NOTIFICATION_ID = 9999;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Đặt lại tất cả alarms khi service start
        new Thread(() -> {
            ReminderRepository repo = new ReminderRepository(getApplicationContext());
            repo.rescheduleAllReminders();
        }).start();

        // Tạo notification cho Foreground Service
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                .setContentTitle("TickTick - Nhắc nhở")
                .setContentText("Đang theo dõi nhắc nhở nhiệm vụ")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();

        startForeground(FOREGROUND_NOTIFICATION_ID, notification);

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel cho Foreground Service (im lặng, ưu tiên thấp)
            NotificationChannel foregroundChannel = new NotificationChannel(
                    CHANNEL_ID_FOREGROUND,
                    "Dịch vụ nhắc nhở",
                    NotificationManager.IMPORTANCE_LOW
            );
            foregroundChannel.setDescription("Thông báo dịch vụ đang chạy nền");

            // Channel cho Reminder notifications (ưu tiên cao, có âm thanh)
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
