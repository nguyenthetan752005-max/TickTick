/**
 * TaskDialogHelper: Lớp hỗ trợ hiển thị hộp thoại thêm/sửa nhiệm vụ dạng BottomSheet.
 */
package hcmute.edu.vn.nguyenthetan;

import android.app.DatePickerDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.model.Task;

public class TaskDialogHelper {

    public interface TaskCallback {
        void onSave(Task task);
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

            // Sử dụng WindowInsets để tự động tính toán khoảng cách bàn phím
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
        View btnDateContainer = view.findViewById(R.id.btnDateContainer);
        ImageButton btnSave = view.findViewById(R.id.btnSaveTask);
        
        String[] catNames = new String[categories.size()];
        for(int i=0; i<categories.size(); i++) catNames[i] = categories.get(i).getName();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, R.layout.spinner_item_dark, catNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spCategory.setAdapter(adapter);

        final long[] selectedDate = {System.currentTimeMillis()};
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        if (existingTask != null) {
            etName.setText(existingTask.getName());
            etDesc.setText(existingTask.getDescription());
            selectedDate[0] = existingTask.getDueDate();
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).getId() == existingTask.getCategoryId()) {
                    spCategory.setSelection(i);
                    break;
                }
            }
        }
        tvDate.setText(sdf.format(new Date(selectedDate[0])));

        btnDateContainer.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(selectedDate[0]);
            new DatePickerDialog(context, (view1, year, month, day) -> {
                c.set(year, month, day);
                selectedDate[0] = c.getTimeInMillis();
                tvDate.setText(day + "/" + (month+1) + "/" + year);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                etName.setError("Vui lòng nhập tên");
                return;
            }
            int catId = categories.get(spCategory.getSelectedItemPosition()).getId();
            if (existingTask == null) {
                callback.onSave(new Task(name, etDesc.getText().toString(), catId, selectedDate[0]));
            } else {
                existingTask.setName(name);
                existingTask.setDescription(etDesc.getText().toString());
                existingTask.setCategoryId(catId);
                existingTask.setDueDate(selectedDate[0]);
                callback.onSave(existingTask);
            }
            dialog.dismiss();
        });

        dialog.show();
        etName.requestFocus();
    }
}
