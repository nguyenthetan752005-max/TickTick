package hcmute.edu.vn.nguyenthetan.ui.taskdialog;

import android.content.Context;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.TaskDialogHelper;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;

/**
 * Đồng bộ reminders khi edit task:
 * - xóa những reminder trong DB nhưng đã bị remove khỏi UI (existingReminders list)
 * - thêm pending reminders mới và schedule
 *
 * Giữ nguyên hành vi legacy (threading + logic so khớp theo id).
 */
public final class ReminderSyncManager {
    private ReminderSyncManager() {}

    public static void syncEditedTaskRemindersAsync(
            Context context,
            Task task,
            List<Reminder> existingRemindersStillOnUi,
            List<TaskDialogHelper.PendingReminder> pendingRemindersToAdd,
            long deadlineTime
    ) {
        new Thread(() -> {
            ReminderRepository repo = new ReminderRepository(context);

            List<Reminder> currentDbReminders = repo.getRemindersByTaskId(task.getId());
            for (Reminder dbReminder : currentDbReminders) {
                boolean stillExists = false;
                for (Reminder er : existingRemindersStillOnUi) {
                    if (er.getId() == dbReminder.getId()) {
                        stillExists = true;
                        break;
                    }
                }
                if (!stillExists) {
                    repo.deleteReminder(dbReminder);
                }
            }

            for (TaskDialogHelper.PendingReminder pr : pendingRemindersToAdd) {
                long reminderTime = ReminderTimeCalculator.calculateReminderTime(deadlineTime, pr.value, pr.unitIndex);
                Reminder reminder = new Reminder(task.getId(), reminderTime);
                long id = repo.addReminder(reminder);
                reminder.setId((int) id);
                repo.scheduleReminder(reminder, task.getName());
            }
        }).start();
    }
}

