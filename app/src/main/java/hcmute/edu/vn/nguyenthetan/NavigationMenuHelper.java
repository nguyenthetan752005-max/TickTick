
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
        MenuItem listsItem = menu.findItem(R.id.nav_lists_header);
        if (listsItem == null) return;

        SubMenu subMenu = listsItem.getSubMenu();
        if (subMenu == null) return;

        subMenu.removeGroup(R.id.main_group_lists);

        List<Category> categories = repo.getAllCategories();
        int index = 0;
        for (Category cat : categories) {
            MenuItem item = subMenu.add(R.id.main_group_lists, cat.getId(), index++, cat.getName());
            item.setIcon(android.R.drawable.ic_menu_directions);
            item.setOnMenuItemClickListener(it -> {
                Toast.makeText(activity, "Bạn vừa ấn vào: " + cat.getName(), Toast.LENGTH_SHORT).show();
                activity.setCategoryFilter(cat.getId());
                drawer.closeDrawer(GravityCompat.START);
                return true;
            });
        }

        navView.post(() -> {
            for (Category cat : categories) {
                View itemView = navView.findViewById(cat.getId());
                if (itemView != null) {
                    itemView.setOnLongClickListener(v -> {
                        CategoryDialogHelper.showAddEditDialog(activity, cat, (updatedCat, action) -> {
                            if (action.equals("UPDATE")) {
                                repo.updateCategory(updatedCat);
                            } else if (action.equals("DELETE")) {
                                repo.deleteCategory(updatedCat);
                            }
                            loadCategories(activity, navView, repo, drawer);
                        });
                        return true;
                    });
                }
            }
        });
    }
}