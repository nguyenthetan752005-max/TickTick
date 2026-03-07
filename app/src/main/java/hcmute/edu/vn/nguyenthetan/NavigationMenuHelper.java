
package hcmute.edu.vn.nguyenthetan;

import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.widget.Toast;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;

import java.util.List;

import hcmute.edu.vn.nguyenthetan.model.Category;
import hcmute.edu.vn.nguyenthetan.repository.CategoryRepository;

public class NavigationMenuHelper {

    public static void loadCategories(MainActivity activity, NavigationView navView,
                                      CategoryRepository repo, DrawerLayout drawer) {
        Menu menu = navView.getMenu();

        // SỬA LỖI: Thêm R.id cho các ID lấy từ file XML menu
        MenuItem listsItem = menu.findItem(R.id.nav_lists_header);
        if (listsItem == null) return;

        SubMenu subMenu = listsItem.getSubMenu();
        if (subMenu == null) return;

        // Xóa group cũ
        subMenu.removeGroup(R.id.main_group_lists);

        List<Category> categories = repo.getAllCategories();
        int index = 0;
        for (Category cat : categories) {
            // Dùng ID của database làm ID cho MenuItem
            MenuItem item = subMenu.add(R.id.main_group_lists, cat.getId(), index++, cat.getName());
            item.setIcon(android.R.drawable.ic_menu_directions);

            // 1 CHẠM (1 NGÓN): HIỆN TOAST
            // Trong file NavigationMenuHelper.java, tìm đoạn xử lý Click và sửa thành:
            item.setOnMenuItemClickListener(it -> {
                Toast.makeText(activity, "Bạn vừa ấn vào: " + cat.getName(), Toast.LENGTH_SHORT).show();

                // THÊM 2 DÒNG NÀY ĐỂ LỌC DANH SÁCH
                activity.setCategoryFilter(cat.getId());

                drawer.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        // MẸO: ĐỢI MENU VẼ XONG RỒI GÁN SỰ KIỆN NHẤN GIỮ (Long Click)
        navView.post(() -> {
            for (Category cat : categories) {
                // SỬA LỖI: Tìm View con dựa trên ID của MenuItem
                View itemView = navView.findViewById(cat.getId());
                if (itemView != null) {
                    itemView.setOnLongClickListener(v -> {
                        // NHẤN GIỮ: HIỆN DIALOG SỬA/XÓA
                        CategoryDialogHelper.showAddEditDialog(activity, cat, (updatedCat, action) -> {
                            if (action.equals("UPDATE")) {
                                repo.updateCategory(updatedCat);
                            } else if (action.equals("DELETE")) {
                                repo.deleteCategory(updatedCat);
                            }
                            // Gọi lại chính hàm này để refresh giao diện
                            loadCategories(activity, navView, repo, drawer);
                        });
                        return true;
                    });
                }
            }
        });
    }
}