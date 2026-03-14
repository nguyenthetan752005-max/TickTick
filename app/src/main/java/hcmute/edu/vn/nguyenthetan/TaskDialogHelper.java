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

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
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

    // Giữ backward compatibility
    public interface SimpleTaskCallback {
        void onSave(Task task);
    }

    public static void showTaskDialog(Context context, List<Category> categories, Task existingTask, SimpleTaskCallback simpleCallback) {
        showTaskDialog(context, categories, existingTask, (task, pendingReminders) -> {
            simpleCallback.onSave(task);
        });
    }

    public static void showTaskDialog(Context context, List<Category> categories, Task existingTask, TaskCallback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_task, null);
        dialog.setContentView(view);

        // THIẾT LẬP ĐỂ ĐẨY DIALOG KHI BÀN PHÍM HIỆN LÊN
        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);

            ViewCompat.setOnApplyWindowInsetsListener(bottomSheet, (v, insets) -> {
                int keyboardHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
                v.setPadding(0, 0, 0, keyboardHeight);
                return insets;
            });
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
        LinearLayout btnAddReminder = view.findViewById(R.id.btnAddReminder);
        LinearLayout layoutReminderList = view.findViewById(R.id.layoutReminderList);

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

        if (existingTask != null) {
            etName.setText(existingTask.getName());
            etDesc.setText(existingTask.getDescription());
            selectedCalendar.setTimeInMillis(existingTask.getDueDate());
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
        }
        tvDate.setText(sdfDate.format(selectedCalendar.getTime()));
        tvTime.setText(sdfTime.format(selectedCalendar.getTime()));

        // ===== Chọn ngày =====
        btnDateContainer.setOnClickListener(v -> {
            DatePickerDialog dpd = new DatePickerDialog(context, (view1, year, month, day) -> {
                selectedCalendar.set(Calendar.YEAR, year);
                selectedCalendar.set(Calendar.MONTH, month);
                selectedCalendar.set(Calendar.DAY_OF_MONTH, day);

                // Kiểm tra nếu ngày+giờ đã chọn nằm trong quá khứ → đẩy giờ về hiện tại
                adjustIfPast(selectedCalendar);

                tvDate.setText(sdfDate.format(selectedCalendar.getTime()));
                tvTime.setText(sdfTime.format(selectedCalendar.getTime()));
            }, selectedCalendar.get(Calendar.YEAR), selectedCalendar.get(Calendar.MONTH), selectedCalendar.get(Calendar.DAY_OF_MONTH));

            // Không cho chọn ngày trong quá khứ
            dpd.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
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
                    Toast.makeText(context, "Không thể chọn thời gian trong quá khứ!", Toast.LENGTH_SHORT).show();
                    return;
                }

                selectedCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selectedCalendar.set(Calendar.MINUTE, minute);
                selectedCalendar.set(Calendar.SECOND, 0);

                tvTime.setText(sdfTime.format(selectedCalendar.getTime()));
            }, selectedCalendar.get(Calendar.HOUR_OF_DAY), selectedCalendar.get(Calendar.MINUTE), true).show();
        });

        // ===== Xử lý nút "Thêm nhắc nhở" =====
        if (btnAddReminder != null) {
            btnAddReminder.setOnClickListener(v -> {
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

            long deadlineTime = selectedCalendar.getTimeInMillis();

            // Kiểm tra deadline phải trong tương lai
            if (deadlineTime <= System.currentTimeMillis()) {
                Toast.makeText(context, "Deadline phải trong tương lai!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Kiểm tra tất cả pending reminders vẫn hợp lệ với deadline hiện tại
            for (int i = pendingReminders.size() - 1; i >= 0; i--) {
                PendingReminder pr = pendingReminders.get(i);
                long recalculated = calculateReminderTime(deadlineTime, pr.value, pr.unitIndex);
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

                // Xử lý xóa reminders đã bị remove khỏi UI
                new Thread(() -> {
                    ReminderRepository repo = new ReminderRepository(context);
                    List<Reminder> currentDbReminders = repo.getRemindersByTaskId(existingTask.getId());
                    for (Reminder dbReminder : currentDbReminders) {
                        boolean stillExists = false;
                        for (Reminder er : existingReminders) {
                            if (er.getId() == dbReminder.getId()) {
                                stillExists = true;
                                break;
                            }
                        }
                        if (!stillExists) {
                            repo.deleteReminder(dbReminder);
                        }
                    }

                    // Thêm pending reminders mới
                    for (PendingReminder pr : pendingReminders) {
                        long reminderTime = calculateReminderTime(deadlineTime, pr.value, pr.unitIndex);
                        Reminder reminder = new Reminder(existingTask.getId(), reminderTime);
                        long id = repo.addReminder(reminder);
                        reminder.setId((int) id);
                        repo.scheduleReminder(reminder, existingTask.getName());
                    }
                }).start();

                callback.onSave(existingTask, pendingReminders);
            }
            dialog.dismiss();
        });

        dialog.show();
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
        BottomSheetDialog reminderDialog = new BottomSheetDialog(context, R.style.CustomDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_reminder, null);
        reminderDialog.setContentView(view);

        EditText etValue = view.findViewById(R.id.etReminderValue);
        Spinner spUnit = view.findViewById(R.id.spReminderUnit);
        TextView tvPreview = view.findViewById(R.id.tvReminderPreview);
        View btnSave = view.findViewById(R.id.btnSaveReminder);
        View btnCancel = view.findViewById(R.id.btnCancelReminder);

        String[] units = {"Phút trước", "Giờ trước", "Ngày trước"};
        ArrayAdapter<String> unitAdapter = new ArrayAdapter<>(context, R.layout.spinner_item_dark, units);
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spUnit.setAdapter(unitAdapter);

        SimpleDateFormat sdfFull = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

        Runnable updatePreview = () -> {
            String valStr = etValue.getText().toString().trim();
            if (valStr.isEmpty()) {
                tvPreview.setText("Thời gian nhắc nhở: --");
                return;
            }
            try {
                int value = Integer.parseInt(valStr);
                int unitIndex = spUnit.getSelectedItemPosition();
                long reminderTime = calculateReminderTime(dueDate, value, unitIndex);

                if (reminderTime <= System.currentTimeMillis()) {
                    tvPreview.setText("⚠️ Thời gian nhắc nhở trong quá khứ!");
                } else if (reminderTime >= dueDate) {
                    tvPreview.setText("⚠️ Nhắc nhở phải trước deadline!");
                } else {
                    tvPreview.setText("Nhắc nhở lúc: " + sdfFull.format(new Date(reminderTime)));
                }
            } catch (NumberFormatException e) {
                tvPreview.setText("Vui lòng nhập số hợp lệ");
            }
        };

        etValue.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) { updatePreview.run(); }
        });

        spUnit.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int position, long id) {
                updatePreview.run();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        btnCancel.setOnClickListener(v -> reminderDialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String valStr = etValue.getText().toString().trim();
            if (valStr.isEmpty()) {
                etValue.setError("Vui lòng nhập số");
                return;
            }

            try {
                int value = Integer.parseInt(valStr);
                if (value <= 0) {
                    etValue.setError("Phải lớn hơn 0");
                    return;
                }

                int unitIndex = spUnit.getSelectedItemPosition();
                long reminderTime = calculateReminderTime(dueDate, value, unitIndex);

                // Kiểm tra: reminder phải trong tương lai
                if (reminderTime <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Thời gian nhắc nhở phải trong tương lai!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Kiểm tra: reminder phải trước deadline
                if (reminderTime >= dueDate) {
                    Toast.makeText(context, "Thời gian nhắc nhở phải trước deadline!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] unitNames = {"phút", "giờ", "ngày"};
                String displayText = value + " " + unitNames[unitIndex] + " trước deadline";

                PendingReminder pending = new PendingReminder(value, unitIndex, reminderTime, displayText);
                listener.onReminderAdded(pending);
                reminderDialog.dismiss();
            } catch (NumberFormatException e) {
                etValue.setError("Số không hợp lệ");
            }
        });

        reminderDialog.show();
    }

    /**
     * Tính thời gian nhắc nhở dựa trên deadline và khoảng cách.
     */
    private static long calculateReminderTime(long dueDate, int value, int unitIndex) {
        long offset;
        switch (unitIndex) {
            case 0: offset = value * 60 * 1000L; break;         // Phút
            case 1: offset = value * 60 * 60 * 1000L; break;    // Giờ
            case 2: offset = value * 24 * 60 * 60 * 1000L; break; // Ngày
            default: offset = 0;
        }
        return dueDate - offset;
    }

    /**
     * Thêm UI item cho pending reminder (chưa lưu DB).
     */
    private static void addPendingReminderToUI(Context context, LinearLayout container,
                                                PendingReminder pending, List<PendingReminder> pendingList) {
        LinearLayout itemLayout = createReminderItemLayout(context, "🔔 " + pending.displayText);

        ImageView btnRemove = new ImageView(context);
        btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                dpToPx(context, 20), dpToPx(context, 20));
        removeParams.gravity = Gravity.CENTER_VERTICAL;
        btnRemove.setLayoutParams(removeParams);
        btnRemove.setOnClickListener(v -> {
            pendingList.remove(pending);
            container.removeView(itemLayout);
        });
        itemLayout.addView(btnRemove);

        container.addView(itemLayout);
    }

    /**
     * Thêm UI item cho reminder đã có trong DB (khi edit task).
     */
    private static void addExistingReminderToUI(Context context, LinearLayout container,
                                                 Reminder reminder, List<Reminder> existingList,
                                                 long dueDate, SimpleDateFormat sdf) {
        String timeText = "🔔 Nhắc nhở lúc " + sdf.format(new Date(reminder.getReminderTime()));
        LinearLayout itemLayout = createReminderItemLayout(context, timeText);

        ImageView btnRemove = new ImageView(context);
        btnRemove.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(
                dpToPx(context, 20), dpToPx(context, 20));
        removeParams.gravity = Gravity.CENTER_VERTICAL;
        btnRemove.setLayoutParams(removeParams);
        btnRemove.setOnClickListener(v -> {
            existingList.remove(reminder);
            container.removeView(itemLayout);
        });
        itemLayout.addView(btnRemove);

        container.addView(itemLayout);
    }

    /**
     * Tạo layout item cho 1 dòng reminder.
     */
    private static LinearLayout createReminderItemLayout(Context context, String text) {
        LinearLayout itemLayout = new LinearLayout(context);
        itemLayout.setOrientation(LinearLayout.HORIZONTAL);
        itemLayout.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams itemParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        itemParams.setMargins(0, dpToPx(context, 4), 0, dpToPx(context, 4));
        itemLayout.setLayoutParams(itemParams);
        itemLayout.setPadding(dpToPx(context, 8), dpToPx(context, 6), dpToPx(context, 8), dpToPx(context, 6));

        TextView tvReminder = new TextView(context);
        tvReminder.setText(text);
        tvReminder.setTextColor(0xFFEBEBF5);
        tvReminder.setTextSize(13);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        tvReminder.setLayoutParams(textParams);
        itemLayout.addView(tvReminder);

        return itemLayout;
    }

    private static int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    interface OnReminderAddedListener {
        void onReminderAdded(PendingReminder reminder);
    }
}
