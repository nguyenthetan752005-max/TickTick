package hcmute.edu.vn.nguyenthetan.repository;

import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.database.AppDatabase;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.dao.ReminderDao;

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



    /**
     * Cập nhật Timer trong Service.
     */
    public void scheduleReminder(Reminder reminder, String taskName) {
        notifyServiceToUpdate();
    }

    /**
     * Hủy alarm cho reminder.
     */
    public void cancelReminder(int reminderId) {
        notifyServiceToUpdate();
    }

    /**
     * Gửi Intent để ReminderService biết cần tải lại danh sách Timer.
     */
    private void notifyServiceToUpdate() {
        Intent intent = new Intent(context, hcmute.edu.vn.nguyenthetan.service.ReminderService.class);
        intent.setAction(hcmute.edu.vn.nguyenthetan.service.ReminderService.ACTION_UPDATE_REMINDERS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
    }


}
