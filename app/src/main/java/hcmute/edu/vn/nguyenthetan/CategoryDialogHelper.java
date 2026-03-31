
package hcmute.edu.vn.nguyenthetan;

import android.content.Context;
import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.util.DialogUtils;

public class CategoryDialogHelper {

    // Interface để gửi kết quả về cho MainActivity xử lý
    public interface CategoryDialogListener {
        void onCategoryAction(Category category, String action); // action: "ADD", "UPDATE", "DELETE"
    }

    public static void showAddEditDialog(Context context, Category category, CategoryDialogListener listener) {
        boolean isEdit = (category != null);

        DialogUtils.showTextInputDialog(
                context,
                isEdit ? "Sửa danh sách" : "Thêm danh sách mới",
                "Tên danh sách",
                isEdit ? category.getName() : null,
                isEdit ? "Cập nhật" : "Thêm",
                "Hủy",
                isEdit ? "Xóa" : null,
                new DialogUtils.OnTextInputListener() {
                    @Override
                    public void onPositive(String text) {
                        String name = text == null ? "" : text.trim();
                        if (name.isEmpty()) return;

                        if (isEdit) {
                            category.setName(name);
                            listener.onCategoryAction(category, "UPDATE");
                        } else {
                            listener.onCategoryAction(new Category(name), "ADD");
                        }
                    }

                    @Override
                    public void onNeutral(String text) {
                        listener.onCategoryAction(category, "DELETE");
                    }
                }
        );
    }
}