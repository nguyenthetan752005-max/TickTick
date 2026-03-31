/**
 * TaskDialogHelper: Lớp hỗ trợ hiển thị hộp thoại thêm/sửa nhiệm vụ dạng BottomSheet.
 * Bao gồm tính năng chọn ngày+giờ deadline và thêm nhắc nhở (Reminder) cho mỗi task.
 */
package hcmute.edu.vn.nguyenthetan;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.model.Reminder;
import hcmute.edu.vn.nguyenthetan.model.Task;
import hcmute.edu.vn.nguyenthetan.repository.ReminderRepository;
import hcmute.edu.vn.nguyenthetan.ui.taskdialog.ReminderItemViewFactory;
import hcmute.edu.vn.nguyenthetan.ui.taskdialog.ReminderTimeCalculator;
import hcmute.edu.vn.nguyenthetan.ui.taskdialog.AddReminderBottomSheet;
import hcmute.edu.vn.nguyenthetan.ui.taskdialog.BottomSheetConfigurer;
import hcmute.edu.vn.nguyenthetan.ui.taskdialog.ReminderSyncManager;

public class TaskDialogHelper {

    /**
     * Dữ liệu tạm cho reminder chưa lưu (khi tạo task mới).
     */
    public static class PendingReminder {
        public int value;
        public int unitIndex; // 0=phút, 1=giờ, 2=ngày
        public long reminderTime;
        public String displayText;

        public PendingReminder(int value, int unitIndex, long reminderTime, String displayText) {
            this.value = value;
            this.unitIndex = unitIndex;
            this.reminderTime = reminderTime;
            this.displayText = displayText;
        }
    }

    public interface TaskCallback {
        void onSave(Task task, List<PendingReminder> pendingReminders);
    }



    public static void showTaskDialog(Context context, List<Category> categories, Task existingTask, TaskCallback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        dialog.setContentView(view);

        // THIẾT LẬP ĐỂ ĐẨY DIALOG KHI BÀN PHÍM HIỆN LÊN
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetConfigurer.configureExpandedWithImePadding(bottomSheet);
        }

        // Cập nhật tiêu đề dựa trên chế độ (Thêm/Sửa)
        TextView tvTitle = view.findViewById(R.id.tvDialogTitle);
        if (tvTitle != null) {
            tvTitle.setText(existingTask != null ? "Chỉnh sửa nhiệm vụ" : "Nhiệm vụ mới");
        }

        EditText etName = view.findViewById(R.id.etTaskName);
        EditText etDesc = view.findViewById(R.id.etTaskDesc);
        Spinner spCategory = view.findViewById(R.id.spCategory);
        TextView tvDate = view.findViewById(R.id.tvDueDate);
        TextView tvTime = view.findViewById(R.id.tvDueTime);
        View btnDateContainer = view.findViewById(R.id.btnDateContainer);
        View btnTimeContainer = view.findViewById(R.id.btnTimeContainer);
        ImageButton btnSave = view.findViewById(R.id.btnSaveTask);
        View btnCancel = view.findViewById(R.id.btnCancelTask);
        LinearLayout btnAddReminder = view.findViewById(R.id.btnAddReminder);
        LinearLayout layoutReminderList = view.findViewById(R.id.layoutReminderList);
        TextView btnClearDeadline = view.findViewById(R.id.btnClearDeadline); // Nút xoá deadline mới

        String[] catNames = new String[categories.size()];
        for(int i=0; i<categories.size(); i++) catNames[i] = categories.get(i).getName();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_dark, catNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        // Calendar dùng chung để lưu ngày + giờ deadline
        final Calendar selectedCalendar = Calendar.getInstance();
        SimpleDateFormat sdfDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm", Locale.getDefault());
        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        // Danh sách reminders tạm (pending) - cho cả tạo mới và sửa
        final List<PendingReminder> pendingReminders = new ArrayList<>();

        // Danh sách reminders đã lưu trong DB (chỉ khi edit)
        final List<Reminder> existingReminders = new ArrayList<>();

        // Trạng thái lưu deadline
        final boolean[] hasDeadline = {false};

        if (existingTask != null) {
            etName.setText(existingTask.getName());
            etDesc.setText(existingTask.getDescription());
            hasDeadline[0] = (existingTask.getDueDate() > 0);
            if (hasDeadline[0]) {
                selectedCalendar.setTimeInMillis(existingTask.getDueDate());
                tvDate.setText(sdfDate.format(selectedCalendar.getTime()));
                tvTime.setText(sdfTime.format(selectedCalendar.getTime()));
                if (btnClearDeadline != null) btnClearDeadline.setVisibility(View.VISIBLE);
                
                if (existingTask.getDueDate() <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Nhiệm vụ có thời gian hạn trong quá khứ nên không khả dụng, khuyên bạn nên đặt thời gian đến tương lai.", Toast.LENGTH_LONG).show();
                }
            } else {
                tvDate.setText("Không có");
                tvTime.setText("");
                if (btnClearDeadline != null) btnClearDeadline.setVisibility(View.GONE);
            }

            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == existingTask.getCategoryId()) {
                    spCategory.setSelection(i);
                    break;
                }
            }

            // Load reminders đã có cho task này
            new Thread(() -> {
                ReminderRepository repo = new ReminderRepository(context);
                List<Reminder> reminders = repo.getRemindersByTaskId(existingTask.getId());
                existingReminders.addAll(reminders);

                if (layoutReminderList != null) {
                    layoutReminderList.post(() -> {
                        for (Reminder r : reminders) {
                            addExistingReminderToUI(context, layoutReminderList, r, existingReminders, existingTask.getDueDate(), sdfFull);
                        }
                    });
                }
            }).start();
        } else {
            hasDeadline[0] = false; // Mặc định tạo mới không có deadline
            tvDate.setText("Thêm ngày");
            tvTime.setText("");
            if (btnClearDeadline != null) btnClearDeadline.setVisibility(View.GONE);
        }

        if (btnClearDeadline != null) {
            btnClearDeadline.setOnClickListener(v -> {
                hasDeadline[0] = false;
                tvDate.setText("Thêm ngày");
                tvTime.setText("");
                pendingReminders.clear();
                layoutReminderList.removeAllViews();
                btnClearDeadline.setVisibility(View.GONE);
                Toast.makeText(context, "Đã xóa hạn chót & nhắc nhở", Toast.LENGTH_SHORT).show();
            });
        }

        // ===== Chọn ngày =====
        btnDateContainer.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(context, (view1, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);

                if (existingTask == null) {
                    // Kiểm tra nếu ngày+giờ đã chọn nằm trong quá khứ → đẩy giờ về hiện tại
                    adjustIfPast(selectedCalendar);
                } else {
                    if (selectedCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
                        Toast.makeText(context, "Nhiệm vụ có thời gian hạn trong quá khứ nên không khả dụng, khuyên bạn nên đặt thời gian đến tương lai.", Toast.LENGTH_LONG).show();
                    }
                }

                tvDate.setText(sdfDate.format(selectedCalendar.getTime()));
                tvTime.setText(sdfTime.format(selectedCalendar.getTime()));
                hasDeadline[0] = true;
                if (btnClearDeadline != null) btnClearDeadline.setVisibility(View.VISIBLE);
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));

            if (existingTask == null) {
                // Không cho chọn ngày trong quá khứ khi thêm task mới
                dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
            }
            dpd.show();
        });

        // ===== Chọn giờ =====
        btnTimeContainer.setOnClickListener(v -> {
            new TimePickerDialog(context, (view1, hourOfDay, minute) -> {
                Calendar tempCal = (Calendar) selectedCalendar.clone();
                tempCal.set(Calendar.HOUR_OF_DAY, hourOfDay);
                tempCal.set(Calendar.MINUTE, minute);
                tempCal.set(Calendar.SECOND, 0);

                // Kiểm tra thời gian chọn có trong quá khứ không
                if (tempCal.getTimeInMillis() <= System.currentTimeMillis()) {
                    if (existingTask == null) {
                        Toast.makeText(context, "Không thể chọn thời gian trong quá khứ!", Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Toast.makeText(context, "Nhiệm vụ có thời gian hạn trong quá khứ nên không khả dụng, khuyên bạn nên đặt thời gian đến tương lai.", Toast.LENGTH_LONG).show();
                    }
                }

                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedCalendar.set(Calendar.MINUTE, minute);
                selectedCalendar.set(Calendar.SECOND, 0);

                tvTime.setText(sdfTime.format(selectedCalendar.getTime()));
                hasDeadline[0] = true;
                if (btnClearDeadline != null) btnClearDeadline.setVisibility(View.VISIBLE);
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
        });

        // ===== Hủy task =====
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> dialog.dismiss());
        }

        // ===== Xử lý nút "Thêm nhắc nhở" =====
        if (btnAddReminder != null) {
            btnAddReminder.setOnClickListener(v -> {
                if (!hasDeadline[0]) {
                    Toast.makeText(context, "Vui lòng chọn hạn chót trước khi thêm nhắc nhở!", Toast.LENGTH_SHORT).show();
                    return;
                }
                long dueDate = selectedCalendar.getTimeInMillis();

                // Kiểm tra deadline hợp lệ trước khi cho thêm reminder
                if (dueDate <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Vui lòng chọn deadline trong tương lai trước!", Toast.LENGTH_SHORT).show();
                    return;
                }

                showAddReminderDialog(context, dueDate, pendingReminder -> {
                    pendingReminders.add(pendingReminder);
                    addPendingReminderToUI(context, layoutReminderList, pendingReminder, pendingReminders);
                });
            });
        }

        // ===== Lưu task =====
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên");
                return;
            }

            long deadlineTime = hasDeadline[0] ? selectedCalendar.getTimeInMillis() : 0;

            // Kiểm tra: Không cho tạo task mới với deadline trong quá khứ
            if (existingTask == null && hasDeadline[0] && deadlineTime <= System.currentTimeMillis()) {
                Toast.makeText(context, "Không thể tạo nhiệm vụ với thời gian trong quá khứ!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Tự động thêm nhắc nhở tại thời điểm đến hạn nếu chưa có (chỉ áp dụng cho tương lai)
            if (hasDeadline[0] && deadlineTime > System.currentTimeMillis()) {
                boolean hasAtDeadline = false;
                if (existingTask != null) {
                    for (Reminder r : existingReminders) {
                        if (r.getReminderTime() == deadlineTime) hasAtDeadline = true;
                    }
                }
                for (PendingReminder pr : pendingReminders) {
                    if (ReminderTimeCalculator.calculateReminderTime(deadlineTime, pr.value, pr.unitIndex) == deadlineTime) hasAtDeadline = true;
                }
                if (!hasAtDeadline) {
                    PendingReminder implicitReminder = new PendingReminder(0, 0, deadlineTime, "Tại thời điểm đến hạn");
                    pendingReminders.add(implicitReminder);
                }
            }

            // Kiểm tra tất cả pending reminders vẫn hợp lệ với deadline hiện tại
            for (int i = pendingReminders.size() - 1; i >= 0; i--) {
                PendingReminder pr = pendingReminders.get(i);
                long recalculated = ReminderTimeCalculator.calculateReminderTime(deadlineTime, pr.value, pr.unitIndex);
                if (recalculated <= System.currentTimeMillis()) {
                    Toast.makeText(context,
                            "Nhắc nhở \"" + pr.displayText + "\" không còn hợp lệ (trong quá khứ). Vui lòng xóa hoặc chỉnh sửa.",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // Cập nhật lại reminder time theo deadline mới nhất
                pr.reminderTime = recalculated;
            }

            int catId = categories.get(spCategory.getSelectedItemPosition()).getId();
            if (existingTask == null) {
                Task newTask = new Task(name, etDesc.getText().toString(), catId, deadlineTime);
                callback.onSave(newTask, pendingReminders);
            } else {
                existingTask.setName(name);
                existingTask.setDescription(etDesc.getText().toString());
                existingTask.setCategoryId(catId);
                existingTask.setDueDate(deadlineTime);

                ReminderSyncManager.syncEditedTaskRemindersAsync(
                        context,
                        existingTask,
                        existingReminders,
                        pendingReminders,
                        deadlineTime
                );

                callback.onSave(existingTask, pendingReminders);
            }
            dialog.dismiss();
        });

        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        etName.requestFocus();
    }

    /**
     * Nếu calendar đang trong quá khứ, đẩy về thời điểm hiện tại + 1 phút.
     */
    private static void adjustIfPast(Calendar cal) {
        if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
            Calendar now = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY));
            cal.set(Calendar.MINUTE, now.get(Calendar.MINUTE) + 1);
            cal.set(Calendar.SECOND, 0);
            // Nếu vẫn trong quá khứ (hiếm nhưng có thể), đẩy thêm
            if (cal.getTimeInMillis() <= System.currentTimeMillis()) {
                cal.add(Calendar.MINUTE, 1);
            }
        }
    }

    /**
     * Hiển thị dialog thêm nhắc nhở.
     */
    private static void showAddReminderDialog(Context context, long dueDate, OnReminderAddedListener listener) {
        AddReminderBottomSheet.show(context, dueDate, reminder -> {
            if (listener != null) listener.onReminderAdded(reminder);
        });
    }

    /**
     * Tính thời gian nhắc nhở dựa trên deadline và khoảng cách.
     */
    /**
     * Thêm UI item cho pending reminder (chưa lưu DB).
     */
    private static void addPendingReminderToUI(Context context, LinearLayout container,
                                                PendingReminder pending, List<PendingReminder> pendingList) {
        addReminderItemToUI(
                context,
                container,
                "🔔 " + pending.displayText,
                () -> pendingList.remove(pending)
        );
    }

    /**
     * Thêm UI item cho reminder đã có trong DB (khi edit task).
     */
    private static void addExistingReminderToUI(Context context, LinearLayout container,
                                                 Reminder reminder, List<Reminder> existingList,
                                                 long dueDate, SimpleDateFormat sdf) {
        String timeText = "🔔 Nhắc nhở lúc " + sdf.format(new Date(reminder.getReminderTime()));
        addReminderItemToUI(
                context,
                container,
                timeText,
                () -> existingList.remove(reminder)
        );
    }

    /**
     * Tạo 1 UI reminder item (gồm text + nút xoá).
     * Wrapper pending/existing chỉ cần truyền text và action remove khỏi list tương ứng.
     */
    private static void addReminderItemToUI(
            Context context,
            LinearLayout container,
            String text,
            Runnable onRemove
    ) {
        ReminderItemViewFactory.addReminderItem(context, container, text, onRemove);
    }

    interface OnReminderAddedListener {
        void onReminderAdded(PendingReminder reminder);
    }
}
