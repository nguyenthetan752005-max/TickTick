package hcmute.edu.vn.nguyenthetan;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import hcmute.edu.vn.nguyenthetan.model.Category;

public class CategoryDialogHelper {

    // Interface để gửi kết quả về cho MainActivity xử lý
    public interface CategoryDialogListener {
        void onCategoryAction(Category category, String action); // action: "ADD", "UPDATE", "DELETE"
    }

    public static void showAddEditDialog(Context context, Category category, CategoryDialogListener listener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Nếu truyền category null -> Thêm mới, nếu có -> Sửa
        boolean isEdit = (category != null);
        builder.setTitle(isEdit ? "Sửa danh sách" : "Thêm danh sách mới");

        final EditText input = new EditText(context);
        input.setHint("Tên danh sách");
        if (isEdit) input.setText(category.getName());

        builder.setView(input);

        builder.setPositiveButton(isEdit ? "Cập nhật" : "Thêm", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) {
                if (isEdit) {
                    category.setName(name);
                    listener.onCategoryAction(category, "UPDATE");
                } else {
                    listener.onCategoryAction(new Category(name), "ADD");
                }
            }
        });

        builder.setNegativeButton("Hủy", null);

        // Nếu là Sửa thì cho thêm nút Xóa
        if (isEdit) {
            builder.setNeutralButton("Xóa", (dialog, which) -> {
                listener.onCategoryAction(category, "DELETE");
            });
        }

        builder.show();
    }
}