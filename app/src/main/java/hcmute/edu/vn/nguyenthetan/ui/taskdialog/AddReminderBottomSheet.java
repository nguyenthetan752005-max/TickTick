package hcmute.edu.vn.nguyenthetan.ui.taskdialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import hcmute.edu.vn.nguyenthetan.R;
import hcmute.edu.vn.nguyenthetan.TaskDialogHelper;

public final class AddReminderBottomSheet {
    private AddReminderBottomSheet() {}

    public interface Listener {
        void onReminderAdded(TaskDialogHelper.PendingReminder reminder);
    }

    public static void show(Context context, long dueDate, Listener listener) {
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
                long reminderTime = ReminderTimeCalculator.calculateReminderTime(dueDate, value, unitIndex);

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
                long reminderTime = ReminderTimeCalculator.calculateReminderTime(dueDate, value, unitIndex);

                if (reminderTime <= System.currentTimeMillis()) {
                    Toast.makeText(context, "Thời gian nhắc nhở phải trong tương lai!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (reminderTime >= dueDate) {
                    Toast.makeText(context, "Thời gian nhắc nhở phải trước deadline!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String[] unitNames = {"phút", "giờ", "ngày"};
                String displayText = value + " " + unitNames[unitIndex] + " trước deadline";

                TaskDialogHelper.PendingReminder pending =
                        new TaskDialogHelper.PendingReminder(value, unitIndex, reminderTime, displayText);
                if (listener != null) listener.onReminderAdded(pending);
                reminderDialog.dismiss();
            } catch (NumberFormatException e) {
                etValue.setError("Số không hợp lệ");
            }
        });

        reminderDialog.show();
    }
}

