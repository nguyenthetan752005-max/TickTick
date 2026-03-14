package hcmute.edu.vn.nguyenthetan.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import hcmute.edu.vn.nguyenthetan.service.ReminderService;

/**
 * BootReceiver: Đăng ký lại tất cả reminders khi thiết bị khởi động lại.
 * AlarmManager bị mất alarm sau reboot nên cần reschedule.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Khởi động ReminderService để reschedule tất cả alarms
            Intent serviceIntent = new Intent(context, ReminderService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}
