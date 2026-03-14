package hcmute.edu.vn.nguyenthetan.receiver;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import hcmute.edu.vn.nguyenthetan.MainActivity;
import hcmute.edu.vn.nguyenthetan.service.ReminderService;

/**
 * ReminderReceiver: Nhận alarm từ AlarmManager và hiển thị notification nhắc nhở.
 */
public class ReminderReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int reminderId = intent.getIntExtra("reminder_id", -1);
        String taskName = intent.getStringExtra("task_name");
        int taskId = intent.getIntExtra("task_id", -1);

        if (taskName == null) taskName = "Nhiệm vụ";

        // Intent mở app khi nhấn notification
        Intent openIntent = new Intent(context, MainActivity.class);
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, reminderId, openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Tạo notification nhắc nhở
        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                context, ReminderService.CHANNEL_ID_REMINDER)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ Sắp đến hạn!")
                .setContentText(taskName + " - Nhiệm vụ sắp đến hạn!")
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
}
