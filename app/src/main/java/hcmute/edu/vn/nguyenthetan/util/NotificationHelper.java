package hcmute.edu.vn.nguyenthetan.util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.nguyenthetan.MainActivity;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.service.ReminderService;

/**
 * Helper tạo và hiển thị notification hệ thống cho Task.
 */
public class NotificationHelper {

    public static void showTaskCompletedNotification(Context context, Task task, String message) {
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                task.getId(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context,
                ReminderService.CHANNEL_ID_REMINDER
        )
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("🎉 Hoàn thành nhiệm vụ!")
                .setContentText(task.getName() + " " + message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.notify(task.getId(), builder.build());
        }
    }
}

