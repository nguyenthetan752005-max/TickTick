package hcmute.edu.vn.nguyenthetan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;

/**
 * BootReceiver: Nhận broadcast BOOT_COMPLETED để lên lịch lại tất cả reminders
 * sau khi thiết bị khởi động lại (vì AlarmManager bị xóa khi reboot).
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || intent.getAction() == null) return;

        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Lên lịch lại tất cả reminders trên background thread
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                ReminderRepository repo = new ReminderRepository(context);
                repo.rescheduleAllReminders();
                executor.shutdown();
            });
        }
    }
}
