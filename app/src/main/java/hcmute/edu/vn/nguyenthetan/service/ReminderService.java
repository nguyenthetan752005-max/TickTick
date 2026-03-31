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

import hcmute.edu.vn.nguyenthetan.MainActivity;

/**
 * ReminderService: Foreground Service hiển thị notification "đang chạy nền".
 * Việc scheduling và trigger nhắc nhở đã chuyển sang AlarmManager + BroadcastReceiver.
 * Service này chỉ giữ lại để tạo notification channels và hiện foreground notification.
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
    @android.annotation.SuppressLint("ForegroundServiceType")
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Tạo notification cho Foreground Service
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+ - có thể set foregroundServiceType
                startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification(),
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            } else {
                // API 24-30 - không có foregroundServiceType
                startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
            }
        } catch (Exception e) {
            // Fallback nếu có lỗi runtime
            try {
                startForeground(FOREGROUND_NOTIFICATION_ID, buildForegroundNotification());
            } catch (Exception ignored) {}
        }

        return START_STICKY;
    }

    private Notification buildForegroundNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID_FOREGROUND)
                .setContentTitle("TickTick - Hệ thống Nhắc nhở")
                .setContentText("Đang chạy nền để nhắc nhở nhiệm vụ")
                .setSmallIcon(android.R.drawable.ic_dialog_info)  // Thay icon an toàn hơn
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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
